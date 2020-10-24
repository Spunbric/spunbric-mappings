package me.i509.spunbric.gradle.mapping;

import java.io.IOException;
import java.util.Objects;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

final class V1McpConfigReader {
	static McpConfigSpecification read(JsonReader reader) throws IOException {
		String version = null;
		String mappings = null;

		while (reader.hasNext()) {
			final String key = reader.nextName();

			switch (key) {
			case "version":
				assertString(reader, "version");
				version = reader.nextString();

				break;
			case "data":
				if (reader.peek() != JsonToken.BEGIN_OBJECT) {
					throw new RuntimeException("\"data\" must be an object");
				}

				reader.beginObject();

				mappings = readMappingsFromData(reader);

				reader.endObject();

				break;
			default:
				// Ignore keys we don't care for
				reader.skipValue();
			}
		}

		Objects.requireNonNull(version);
		Objects.requireNonNull(mappings);

		return new V1McpConfigSpecification(version, mappings);
	}

	private static String readMappingsFromData(JsonReader reader) throws IOException {
		String mappings = null;

		while (reader.hasNext()) {
			final String key = reader.nextName();

			switch (key) {
			case "mappings":
				assertString(reader, "data/mappings");
				mappings = reader.nextString();

				break;
			default:
				reader.skipValue();
			}
		}

		if (mappings == null) {
			throw new RuntimeException("No mappings found in data!");
		}

		return mappings;
	}

	private static void assertString(JsonReader reader, String field) throws IOException {
		if (reader.peek() != JsonToken.STRING) {
			throw new RuntimeException(field);
		}
	}

	private V1McpConfigReader() {
	}

	private static final class V1McpConfigSpecification implements McpConfigSpecification {
		private final String version;
		private final String mappings;

		public V1McpConfigSpecification(String version, String mappings) {
			this.version = version;
			this.mappings = mappings;
		}

		@Override
		public int getSpecVersion() {
			return 1;
		}

		public String getVersion() {
			return this.version;
		}

		public String getMappings() {
			return this.mappings;
		}
	}
}
