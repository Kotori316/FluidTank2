import net.fabricmc.loom.task.RemapJarTask
import org.gradle.jvm.tasks.Jar
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("architectury-plugin")
    id("dev.architectury.loom")
    id("com.github.johnrengelman.shadow")
}

val minecraftVersion = project.property("minecraft_version") as String
val platformVersion = project.version.toString()
val modId = "FluidTank".lowercase()
configurations {
    val common = create("common")
    create("shadowCommon") // Don't use shadow from the shadow plugin because we don't want IDEA to index this.
    compileClasspath { extendsFrom(common) }
    if (project.name != "forge" || System.getenv("RUN_JUNIT") != null) {
        runtimeClasspath { extendsFrom(common) }
    }
    testCompileClasspath { extendsFrom(compileClasspath.get()) }
    testRuntimeClasspath { extendsFrom(runtimeClasspath.get()) }
}

val jarAttributeMap = mapOf(
    "Specification-Title" to "FluidTank",
    "Specification-Vendor" to "Kotori316",
    "Specification-Version" to "1",
    "Implementation-Title" to "FluidTank",
    "Implementation-Vendor" to "Kotori316",
    "Implementation-Version" to platformVersion,
    "Implementation-Timestamp" to ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
    "Automatic-Module-Name" to modId,
)

tasks {
    jar {
        archiveClassifier = "dev-only-platform"
        manifest {
            attributes(jarAttributeMap)
        }
    }
    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(project.configurations.getAt("shadowCommon"))
        archiveClassifier = "dev"
        manifest {
            attributes(jarAttributeMap)
        }
    }
    named("remapJar", RemapJarTask::class) {
        val shadowJarProvider = provider { project }.flatMap { p -> p.tasks.shadowJar }
        inputFile = shadowJarProvider.flatMap { j -> j.archiveFile }
        dependsOn(shadowJarProvider)
        archiveClassifier = null
        manifest {
            attributes(jarAttributeMap)
        }
    }
    named("sourcesJar", Jar::class) {
        val commonSources = provider { project(":common") }.flatMap { p -> p.tasks.named("sourcesJar", Jar::class) }
        dependsOn(commonSources)
        from(commonSources.flatMap { it.archiveFile }.map { zipTree(it) })
    }
}

dependencies {

}

components.named("java", AdhocComponentWithVariants::class) {
    withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

afterEvaluate {
    tasks.findByName("gameTestClasses")?.let { c ->
        tasks.findByName("runClient")?.dependsOn(c)
        tasks.findByName("runGameTest")?.dependsOn(c)
    }
    tasks.findByName("runGameClasses")?.let { c ->
        tasks.findByName("runClient")?.dependsOn(c)
        tasks.findByName("runServer")?.dependsOn(c)
    }
    tasks.findByName("genDataClasses")?.let { c ->
        tasks.findByName("runData")?.dependsOn(c)
    }
    tasks.withType(ProcessResources::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        inputs.property("version", platformVersion)
        inputs.property("minecraftVersion", minecraftVersion)
        listOf("fabric.mod.json", "META-INF/mods.toml").forEach { fileName ->
            filesMatching(fileName) {
                expand(
                    "version" to platformVersion,
                    "update_url" to "https://version.kotori316.com/get-version/${minecraftVersion}/${project.name}/${modId}",
                    "mc_version" to minecraftVersion,
                )
            }
        }
    }
}
