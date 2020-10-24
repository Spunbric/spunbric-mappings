plugins {
    id("java")
    id("groovy")
    id("maven-publish")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "me.i509"
version = "0.1-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    gradlePluginPortal()

    maven(url = "https://maven.fabricmc.net") {
        name = "Fabric"
    }
}

dependencies {
    implementation("org.jetbrains:annotations:19.0.0")

    implementation(gradleApi())
    implementation("net.fabricmc:fabric-loom:0.5-SNAPSHOT")
    implementation("net.fabricmc:tiny-mappings-parser:0.2.2.14")

    implementation("org.cadixdev:lorenz:0.5.4")
    implementation("org.cadixdev:lorenz-asm:0.5.4")
    implementation("net.fabricmc:lorenz-tiny:2.0.0+build.2") {
        isTransitive = false
    }

    implementation ("org.zeroturnaround:zt-zip:1.13")
    implementation("com.google.code.gson:gson:2.8.5")

    // Testing
    testImplementation(gradleTestKit())
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.4") {
        exclude(module = "groovy-all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("spunbric-mappings") {
            id = "me.i509.spunbric.mappings"
            implementationClass = "me.i509.spunbric.gradle.mapping.MappingsPlugin"
        }
    }
}

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
}

tasks.named("testClasses") {
    dependsOn("pluginUnderTestMetadata")
}

publishing {
    repositories {
        mavenLocal()
    }
}
