package me.i509.spunbric.gradle.mapping;

import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.artifacts.Dependency;

public interface MappingChannelProvider {
	boolean hasJavadoc();

	void applyMappings(MappingSet mappingSet);

	Dependency createDependency(String version);
}
