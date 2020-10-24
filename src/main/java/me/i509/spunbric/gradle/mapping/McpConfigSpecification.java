package me.i509.spunbric.gradle.mapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

interface McpConfigSpecification {
	static McpConfigSpecification create(byte[] contents) throws IOException {
		try (JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(contents)))) {
			// Try to read the spec version.
			reader.beginObject();

			if (!reader.nextName().equals("spec")) {
				throw new RuntimeException("Spec must be first field in mcp config!");
			}

			if (reader.peek() != JsonToken.NUMBER) {
				throw new RuntimeException("First entry must be a number!");
			}

			final int specVersion = reader.nextInt();
			McpConfigSpecification config;

			switch (specVersion) {
			case 1:
				config = V1McpConfigReader.read(reader);
				break;
			default:
				throw new RuntimeException("Unsupported mcp config version " + specVersion);
			}

			reader.endObject();

			return config;
		}
	}

	int getSpecVersion();

	String getVersion();

	String getMappings();
}
