static String genBuildFile(String minecraftVersion, String mappings) {
"""
plugins {
\tid 'fabric-loom'
\tid 'me.i509.spunbric.mappings'
}

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

dependencies {
\tminecraft 'com.mojang:minecraft:${minecraftVersion}'
\tmappings ${mappings}
\tmodImplementation "net.fabricmc:fabric-loader:0.10.3+build.211"
}

tasks.withType(JavaCompile) {
\toptions.encoding = "UTF-8"
}
"""
}

static String genPropertiesFile() {
"""
org.gradle.jvmargs=-Xmx2G
"""
}

static String genSettingsFile() {
"""
pluginManagement {
\trepositories {
\t\tjcenter()
\t\tgradlePluginPortal()
\t\t//mavenLocal()

\t\tmaven {
\t\t\tname = 'Fabric'
\t\t\turl = 'https://maven.fabricmc.net/'
\t\t}
\t}
}
"""
}