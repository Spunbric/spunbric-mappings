import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

// Credit to fabric-loom for the general testing setup
class SetupProjectTest extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    File settingsFile
    File propertiesFile
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        propertiesFile = testProjectDir.newFile('gradle.properties')
    }

    @Unroll
    def "empty build succeeds using Minecraft #mcVersion with #mappings"() {
        given:
        buildFile << BuildUtils.genBuildFile(mcVersion, mappings)
        settingsFile << BuildUtils.genSettingsFile()
        propertiesFile << BuildUtils.genPropertiesFile()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--refresh-dependencies')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.task(":build").outcome == TaskOutcome.SUCCESS

        where:
        mcVersion | mappings
        '1.14.4' | "spunbricMappings.mcpMappings()"
        '1.15' | "spunbricMappings.mcpMappings()"
        '1.15.1' | "spunbricMappings.mcpMappings()"
        '1.15.2' | "spunbricMappings.mcpMappings()"
        '1.16' | "spunbricMappings.mcpMappings()"
        '1.16.1' | "spunbricMappings.mcpMappings()"
        '1.16.2' | "spunbricMappings.mcpMappings()"
        '1.16.3' | "spunbricMappings.mcpMappings()"
    }
}
