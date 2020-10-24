package me.i509.spunbric.gradle.mapping;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

final class MappingsPlugin implements Plugin<Project> {
	@Override
	public void apply(Project target) {
		target.getLogger().lifecycle("Spunbric Mappings: 0.1-SNAPSHOT");

		// Add forge maven
		target.getRepositories().maven(repository -> {
			repository.setUrl("https://files.minecraftforge.net/maven");
			repository.setName("Forge");
		});

		target.getExtensions().create("spunbricMappings", MappingsExtension.class, target);
	}
}
