package me.i509.spunbric.gradle.mapping;

import java.io.File;
import java.util.Set;

import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;

public interface MappingChannelProvider {
	boolean hasJavadoc();

	void applyMappings(MappingSet mappingSet, Set<File> mappingsFiles);

	Set<File> resolve(Project project, String version);
}
