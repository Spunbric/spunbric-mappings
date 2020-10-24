package me.i509.spunbric.gradle.mapping;

import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.artifacts.Dependency;

final class ChannelProviders {
	private ChannelProviders() {
	}

	static class McpMappingChannel implements MappingChannelProvider {
		private final String name;
		private final boolean javadoc;

		McpMappingChannel(String name, boolean javadoc) {
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
		public void applyMappings(MappingSet mappingSet) {

		}

		@Override
		public Dependency createDependency(String version) {
			return new McpDependency(version);
		}

		private class McpDependency implements Dependency {
			private final String version;

			public McpDependency(String version) {
				this.version = version;
			}

			@Override
			public String getGroup() {
				return "de.oceanlabs.mcp";
			}

			@Override
			public String getName() {
				return "mcp_" + McpMappingChannel.this.name;
			}

			@Override
			public String getVersion() {
				return this.version;
			}

			@Override
			public boolean contentEquals(Dependency dependency) {
				if (dependency instanceof McpDependency) {
					return ((McpDependency) dependency).version.equals(this.version);
				}

				return false;
			}

			@Override
			public Dependency copy() {
				return new McpDependency(this.version);
			}

			@Override
			public String getReason() {
				return null;
			}

			@Override
			public void because(String reason) {
			}
		}
	}
}
