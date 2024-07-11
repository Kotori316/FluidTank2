import gradle.kotlin.dsl.accessors._6b65b5a1365ce9e316fa6d785761b323.compileScala
import gradle.kotlin.dsl.accessors._6b65b5a1365ce9e316fa6d785761b323.compileTestScala
import gradle.kotlin.dsl.accessors._6b65b5a1365ce9e316fa6d785761b323.processResources
import gradle.kotlin.dsl.accessors._6b65b5a1365ce9e316fa6d785761b323.scala
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.github.johnrengelman.shadow")
}

val minecraftVersion = project.property("minecraft_version") as String
val projectVersion = project.version.toString()
val modId = "FluidTank".lowercase()

val jarAttributeMap = mapOf(
    "Specification-Title" to "FluidTank",
    "Specification-Vendor" to "Kotori316",
    "Specification-Version" to "1",
    "Implementation-Title" to "FluidTank",
    "Implementation-Vendor" to "Kotori316",
    "Implementation-Version" to projectVersion,
    "Implementation-Timestamp" to ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
    "Automatic-Module-Name" to modId,
)

val commonProject = project.findProject(":common")

dependencies {
    commonProject?.let { p ->
        compileOnly(p)
        testImplementation(p)
        // For Fabric
        configurations.findByName("datagen")?.let {
            "datagenImplementation"(p)
        }
    }
}

afterEvaluate {
    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(jarAttributeMap)
        }
    }
    tasks.compileScala {
        commonProject?.let {
            source(it.sourceSets.main.get().scala)
        }
    }
    tasks.compileTestScala {
        commonProject?.let {
            source(it.sourceSets.main.get().scala)
        }
    }
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
    tasks.processResources {
        commonProject?.let { from(it.sourceSets.main.get().resources) }
    }
    tasks.withType(ProcessResources::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        inputs.property("version", projectVersion)
        inputs.property("minecraftVersion", minecraftVersion)
        listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml").forEach { fileName ->
            filesMatching(fileName) {
                expand(
                    "version" to projectVersion,
                    "update_url" to "https://version.kotori316.com/get-version/${minecraftVersion}/${project.name}/${modId}",
                    "mc_version" to minecraftVersion,
                )
            }
        }
    }
}
