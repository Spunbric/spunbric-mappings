package me.i509.spunbric.gradle.mapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.gradle.api.Project;
import org.zeroturnaround.zip.Zips;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.util.DownloadUtil;

final class ChannelProviders {
	private ChannelProviders() {
	}

	static class McpMappingChannel implements MappingChannelProvider {
		private static final String MAPPINGS_CONFIGURATION = "mappingChannel";
		private final Project project;
		private final String name;
		private final boolean javadoc;

		McpMappingChannel(Project project, String name, boolean javadoc) {
			this.project = project;
			this.name = name;
			this.javadoc = javadoc;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean hasJavadoc() {
			return this.javadoc;
		}

		@Override
		public void applyMappings(MappingSet mappingSet, Set<File> mappingsFiles) {
			final File file = mappingsFiles.iterator().next();
			final Zips zips = Zips.get(file);

			// Big boi mappings need some preallocated capacity for speedier reading
			final Map<String, String> fields = new HashMap<>(10000);
			final Map<String, String> methods = new HashMap<>(10000);
			final Map<String, String> params = new HashMap<>(10000);

			try {
				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(zips.getEntry("fields.csv"))))) {
					reader.readLine();

					String line;
					while ((line = reader.readLine()) != null) {
						String[] parts = line.split(",");
						fields.put(parts[0], parts[1]);
					}
				}

				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(zips.getEntry("methods.csv"))))) {
					reader.readLine();

					String line;

					while ((line = reader.readLine()) != null) {
						String[] parts = line.split(",");
						methods.put(parts[0], parts[1]);
					}
				}

				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(zips.getEntry("params.csv"))))) {
					reader.readLine();

					String line;

					while ((line = reader.readLine()) != null) {
						String[] parts = line.split(",");
						params.put(parts[0], parts[1]);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Apply
			MappingUtils.iterateClasses(mappingSet, classMapping -> {
				for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
					final String rename = fields.get(fieldMapping.getDeobfuscatedName());

					if (rename != null) {
						fieldMapping.setDeobfuscatedName(rename);
					}
				}

				for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
					final String rename = methods.get(methodMapping.getDeobfuscatedName());

					if (rename != null) {
						methodMapping.setDeobfuscatedName(rename);
					}

					// TODO: Apply param names7
				}
			});
		}

		@Override
		public Set<File> resolve(Project project, String version) {
			//final Configuration mappingChannelConfig = this.project.getConfigurations().maybeCreate(MAPPINGS_CONFIGURATION);
			// https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_snapshot/20140908-1.7.10/mcp_snapshot-20140908-1.7.10.zip
			//this.project.getDependencies().add(MAPPINGS_CONFIGURATION, "de.oceanlabs.mcp:mcp_" + this.getName() + ":" + version);

			// Too damn lazy to figure out maven right now, I'll fix it later
			final File mappingsFile = project.getExtensions()
					.getByType(LoomGradleExtension.class)
					.getMappingsProvider()
					.getMappingsDir()
					.resolve("de.oceanlabs.mcp.mcp_" + this.getName() + "-" + version + ".zip")
					.toFile();

			mappingsFile.delete();

			try {
				// Just make a pom lmao
				DownloadUtil.downloadIfChanged(new URL("https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_" + this.getName() + "/" + version + "/" + "mcp_snapshot-" + version + ".zip"), mappingsFile, this.project.getLogger());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return Collections.singleton(mappingsFile);
		}
	}
}
