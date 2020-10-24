package me.i509.spunbric.gradle.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.Nullable;

public class MappingsExtension {
	private final Map<String, MappingChannelProvider> channels = new HashMap<>();
	private final Project project;
	private String mappingChannel;
	private String mappingVersion;

	public MappingsExtension(Project project) {
		this.project = project;
		this.registerDefaultChannels();
	}

	public void registerMappingChannel(String name, MappingChannelProvider provider) {
		Objects.requireNonNull(name, "Channel name cannot be null");
		Objects.requireNonNull(provider, "Channel provider cannot be null");

		this.channels.put(name, provider);
	}

	@Input
	public String getMappingChannel() {
		return this.mappingChannel;
	}

	public void setMappingChannel(String channel) {
		this.mappingChannel = Objects.requireNonNull(channel, "Mapping channel cannot be null");
	}

	@Input
	public String getMappingVersion() {
		return this.mappingVersion;
	}

	public void setMappingVersion(String version) {
		this.mappingVersion = Objects.requireNonNull(version, "Mapping version cannot be null");
	}

	@Nullable
	MappingChannelProvider getMappingChannelProvider() {
		if (this.mappingChannel != null) {
			if (this.mappingVersion == null) {
				throw new RuntimeException("No version specified for mapping channel");
			}

			// TODO: Check the list and fail if not present
			return this.channels.get(this.mappingChannel);
		}

		// Null = srg
		return null;
	}

	public Dependency mcpMappings() {
		return new McpMappingsDependency(this.project, this);
	}

	private void registerDefaultChannels() {
		this.registerMappingChannel("snapshot", new ChannelProviders.McpMappingChannel(this.project, "snapshot", true));
		this.registerMappingChannel("snapshot_nodoc", new ChannelProviders.McpMappingChannel(this.project, "snapshot", false));
		this.registerMappingChannel("stable", new ChannelProviders.McpMappingChannel(this.project, "stable", true));
		this.registerMappingChannel("stable_nodoc", new ChannelProviders.McpMappingChannel(this.project, "stable", false));
	}
}
