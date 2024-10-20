import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("scala")
    id("idea")
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

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(jarAttributeMap)
    }
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

afterEvaluate {
    tasks.compileScala {
        commonProject?.let {
            source(it.sourceSets.main.get().scala)
        }
    }
    tasks.findByName("gameTestClasses")?.let { c ->
        tasks.findByName("runClient")?.dependsOn(c)
        tasks.findByName("runGameTestServer")?.dependsOn(c)
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
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
