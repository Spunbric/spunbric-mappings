package me.i509.spunbric.gradle.mapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;

import org.cadixdev.bombe.analysis.CachingInheritanceProvider;
import org.cadixdev.bombe.analysis.CascadingInheritanceProvider;
import org.cadixdev.bombe.analysis.ReflectionInheritanceProvider;
import org.cadixdev.bombe.asm.analysis.ClassProviderInheritanceProvider;
import org.cadixdev.bombe.asm.jar.ClassProvider;
import org.cadixdev.bombe.asm.jar.JarFileClassProvider;
import org.cadixdev.bombe.type.ArrayType;
import org.cadixdev.bombe.type.BaseType;
import org.cadixdev.bombe.type.FieldType;
import org.cadixdev.bombe.type.PrimitiveType;
import org.cadixdev.bombe.type.VoidType;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingsReader;
import org.cadixdev.lorenz.io.MappingsWriter;
import org.cadixdev.lorenz.io.srg.tsrg.TSrgReader;
import org.cadixdev.lorenz.model.FieldMapping;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.tasks.TaskDependency;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.Zips;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.lorenztiny.TinyMappingsReader;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

final class McpMappingsDependency implements SelfResolvingDependency {
	private static final String MCP_CONFIG_CONFIGURATION = "mcpConfig";
	private static final String MAPPINGS_CONFIGURATION = "mappingChannel";
	private static final String GROUP = "de.oceanlabs.mcp";
	private static final String MODULE = "mcp_config";
	private final Project project;
	private final MappingsExtension extension;
	private final LoomGradleExtension loom;

	public McpMappingsDependency(Project project, MappingsExtension extension) {
		this.project = project;
		this.extension = extension;
		this.loom = project.getExtensions().getByType(LoomGradleExtension.class);
	}

	@Override
	public Set<File> resolve() {
		// Resolve MCPConfig
		final Configuration mcpConfig = this.project.getConfigurations().maybeCreate(MCP_CONFIG_CONFIGURATION);
		this.project.getDependencies().add(MCP_CONFIG_CONFIGURATION, this.getGroup() + ":" + this.getName() + ":" + this.getVersion());
		final Set<File> mcpConfigFiles = mcpConfig.resolve();

		final MappingChannelProvider channelProvider = extension.getMappingChannelProvider();

		Set<File> mappingChannelFiles;

		if (channelProvider != null) {
			final Configuration mappingChannelConfig = this.project.getConfigurations().maybeCreate(MAPPINGS_CONFIGURATION);
			this.project.getDependencies().add(MAPPINGS_CONFIGURATION, channelProvider.createDependency(this.extension.getMappingVersion()));
			mappingChannelFiles = mappingChannelConfig.resolve();
			System.out.println(mappingChannelFiles);
		} else {
			mappingChannelFiles = Collections.emptySet();
		}

		final Path mappingsDir = this.loom.getMappingsProvider().getMappingsDir();
		final Path mappingsFile;

		// TODO: Better names for files
		if (channelProvider != null) {
			mappingsFile = mappingsDir.resolve(String.format("%s.%s-%s-channel.tiny", GROUP, MODULE, this.getVersion()));
		} else {
			mappingsFile = mappingsDir.resolve(String.format("%s.%s-%s-srg.tiny", GROUP, MODULE, this.getVersion()));
		}

		// Only create if it does not exist OR --refresh-dependencies is set
		if (Files.notExists(mappingsFile) || this.project.getGradle().getStartParameter().isRefreshDependencies()) {
			MappingSet officialToSrg;

			// First create the SRG mappings
			try {
				officialToSrg = this.getSrgMappings(mcpConfigFiles);
			} catch (IOException e) {
				// TODO:
				throw new RuntimeException();
			}

			final MappingSet officialToIntermediary;

			try {
				officialToIntermediary = this.getIntermediaryMappings(this.loom.getMappingsProvider().getIntermediaryTiny());
			} catch (IOException e) {
				// TODO:
				throw new RuntimeException();
			}

			this.project.getLogger().lifecycle(":completing srg mappings");
			// Next let's propagate the fields using the client jar for context
			this.project.getLogger().lifecycle(String.valueOf(this.loom.getMinecraftProvider().getMergedJar()));

			try {
				this.propagateMappings(this.loom.getMinecraftProvider().getMergedJar(), officialToIntermediary, officialToSrg);
			} catch (IOException e) {
				// TODO:
				throw new RuntimeException();
			}

			final MappingSet intermediaryToOfficial = officialToIntermediary.reverse();

			// Merge the intermediary -> [official -> official] -> srg mappings
			final MappingSet intermediaryToSrgMappings = intermediaryToOfficial.merge(officialToSrg);
			MappingSet intermediaryToMapped;

			// Read mappings from the mapping channel if provided
			if (channelProvider != null) {
				// Now we can create the srg -> mcp mappings
				// Merge the intermediary -> [srg -> srg] -> mcp mappings
				intermediaryToMapped = intermediaryToSrgMappings;
				channelProvider.applyMappings(intermediaryToMapped);
			} else {
				intermediaryToMapped = intermediaryToSrgMappings;
			}

			// Reversal and merging does not preserve field descriptors :(
			intermediaryToMapped.addFieldTypeProvider(mapping -> {
				return intermediaryToOfficial.getClassMapping(mapping.getParent().getObfuscatedName()) // get the official class mapping using intermediary deobf name
						.flatMap(officialClassMapping -> officialToIntermediary.getClassMapping(officialClassMapping.getDeobfuscatedName())) // get the intermediary class mapping
						.flatMap(classMapping -> classMapping.getFieldMapping(mapping.getObfuscatedName())) // get the intermediary field mapping
						.flatMap(FieldMapping::getType) // Get the official field descriptor
						.flatMap(fieldType -> MappingUtils.remapFieldType(officialToIntermediary, fieldType)); // Remap the field descriptor
			});

			final StringWriter stringWriter = new StringWriter();

			// Write to tiny v2 and package
			try (MappingsWriter writer = new TinyV2Writer(this.project.getLogger(), "intermediary", "named", stringWriter)) {
				writer.write(intermediaryToMapped);
			} catch (IOException e) {
				throw new RuntimeException();
			}

			if (Files.exists(mappingsFile)) {
				try {
					Files.delete(mappingsFile);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			ZipUtil.pack(new ZipEntrySource[] {
					new ByteSource("mappings/mappings.tiny", stringWriter.toString().getBytes(StandardCharsets.UTF_8))
			}, mappingsFile.toFile());
		}

		return Collections.singleton(mappingsFile.toFile());
	}

	private void propagateMappings(File clientJar, MappingSet... mappings) throws IOException {
		try (final JarFile clientJarFile = new JarFile(clientJar)) {
			final CascadingInheritanceProvider cascadingInheritanceProvider = new CascadingInheritanceProvider();
			final List<ClassProvider> providers = new ArrayList<>();
			providers.add(new JarFileClassProvider(clientJarFile));

			cascadingInheritanceProvider.install(new ClassProviderInheritanceProvider(className -> {
				for (final ClassProvider provider : providers) {
					final byte[] classBytes = provider.get(className);

					if (classBytes != null) {
						return classBytes;
					}
				}

				return null;
			}));

			// Install JRE classes
			cascadingInheritanceProvider.install(new ReflectionInheritanceProvider(ClassLoader.getSystemClassLoader()));

			// Use a caching inheritance provider to do the heavy lifting
			final CachingInheritanceProvider inheritanceProvider = new CachingInheritanceProvider(cascadingInheritanceProvider);

			// Complete inheritance
			for (final MappingSet mappingSet : mappings) {
				MappingUtils.iterateClasses(mappingSet, classMapping -> {
					classMapping.complete(inheritanceProvider);
				});
			}
		}
	}

	private MappingSet getSrgMappings(Set<File> mcpConfigFiles) throws IOException {
		final Optional<File> mcpConfig = mcpConfigFiles.stream().findFirst();

		if (!mcpConfig.isPresent()) {
			throw new RuntimeException();
		}

		final Zips zips = Zips.get(mcpConfig.get());

		final byte[] configFile = zips.getEntry("config.json");

		if (configFile == null) {
			throw new RuntimeException(); // TODO
		}

		final McpConfigSpecification config = McpConfigSpecification.create(configFile);
		final MappingSet mappingSet = MappingSet.create();
		final byte[] joinedMappings = zips.getEntry(config.getMappings());

		try (final MappingsReader reader = new TSrgReader(new InputStreamReader(new ByteArrayInputStream(joinedMappings)))) {
			reader.read(mappingSet);
		}

		return mappingSet;
	}

	private MappingSet getIntermediaryMappings(Path intermediaryTiny) throws IOException {
		TinyTree tree;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(intermediaryTiny)))) {
			tree = TinyMappingFactory.load(reader);
		}

		return new TinyMappingsReader(tree, "official", "intermediary").read();
	}

	@Override
	public Set<File> resolve(boolean transitive) {
		return this.resolve();
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return task -> Collections.emptySet();
	}

	@Override
	public String getGroup() {
		return GROUP;
	}

	@Override
	public String getName() {
		return MODULE;
	}

	@Override
	public String getVersion() {
		return this.loom.getMinecraftProvider().getMinecraftVersion();
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		if (dependency instanceof McpMappingsDependency) {
			return dependency.getVersion().equals(this.getVersion());
		}

		return false;
	}

	@Override
	public Dependency copy() {
		return new McpMappingsDependency(this.project, this.extension);
	}

	@Override
	public String getReason() {
		return null;
	}

	@Override
	public void because(String reason) {
	}
}
