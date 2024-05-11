import net.fabricmc.loom.LoomGradleExtension
import java.nio.file.Files
import java.nio.file.Path

plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
}

architectury {
    platformSetupLoomIde()
    forge()
}

sourceSets {
    create("genData") {
        scala {
            srcDir("src/genData/scala")
        }
        resources {
            srcDir("src/genData/resources")
        }
        val dir = layout.buildDirectory.dir("sourcesSets/${name}")
        output.setResourcesDir(dir)
        java.destinationDirectory = dir
        scala.destinationDirectory = dir
    }
    create("gameTest") {
        scala {
            srcDir("src/gameTest/scala")
        }
        resources {
            srcDir("src/gameTest/resources")
        }
        val dir = layout.buildDirectory.dir("sourcesSets/${name}")
        output.setResourcesDir(dir)
        java.destinationDirectory = dir
        scala.destinationDirectory = dir
    }

    create("runGame") {
        val dir = layout.buildDirectory.dir("sourcesSets/${name}")
        output.setResourcesDir(dir)
        java.destinationDirectory = dir
        scala.destinationDirectory = dir
    }
}

tasks.named("compileRunGameScala", ScalaCompile::class) {
    source(
        sourceSets.main.get().java, sourceSets.main.get().scala,
    )
    dependsOn("processRunGameResources")
}
tasks.named("processRunGameResources", ProcessResources::class) {
    from(sourceSets.main.get().resources)
}
tasks.named("compileGenDataScala", ScalaCompile::class) {
    source(
        sourceSets.main.get().java, sourceSets.main.get().scala,
        sourceSets.getByName("genData").java, sourceSets.getByName("genData").scala,
    )
    dependsOn("processGenDataResources")
}
tasks.named("processGenDataResources", ProcessResources::class) {
    from(sourceSets.main.get().resources)
}
tasks.named("compileGameTestScala", ScalaCompile::class) {
    source(
        sourceSets.main.get().java, sourceSets.main.get().scala,
        sourceSets.getByName("gameTest").java, sourceSets.getByName("gameTest").scala,
    )
    dependsOn("processGameTestResources")
}
tasks.named("processGameTestResources", ProcessResources::class) {
    from(sourceSets.main.get().resources)
}

loom {
    forge {
        useForgeLoggerConfig = true
    }

    runs {
        named("client") {
            configName = "Client"
            property("forge.enabledGameTestNamespaces", "fluidtank")
            runDir = "run"
            source("gameTest")
            mods {
                create("gameTest") {
                    sourceSet("gameTest")
                }
            }
        }
        named("server") {
            configName = "Server"
            runDir = "run-server"
            source("runGame")
        }
        create("gameTest") {
            configName = "GameTest"
            environment("gameTestServer")
            forgeTemplate("gameTestServer")
            vmArg("-ea")
            source("gameTest")
            property("fabric.dli.env", "gameTestServer")
            property("forge.enabledGameTestNamespaces", "fluidtank")
            runDir = "game-test"
            mods {
                create("gameTest") {
                    sourceSet("gameTest")
                }
            }
        }
        create("data") {
            configName = "Data"
            runDir = "run-server"
            programArgs(
                "--mod", "fluidtank", "--all",
                "--output", file("../common/src/generated/resources/").toString(),
                "--existing", file("../common/src/main/resources/").toString(),
            )
            source("genData")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            property("mixin.debug.export", "true")
            property("bsl.debug", "true")
            data()
            mods {
                create("genData") {
                    sourceSet("genData")
                }
            }
        }
    }
}

configurations {
    named("developmentForge") {
        extendsFrom(common.get())
    }

    named("runGameCompileClasspath") {
        extendsFrom(compileClasspath.get())
    }
    named("runGameRuntimeClasspath") {
        extendsFrom(runtimeClasspath.get())
    }
    named("genDataCompileClasspath").get().extendsFrom(compileClasspath.get())
    named("genDataRuntimeClasspath").get().extendsFrom(runtimeClasspath.get())
    named("gameTestCompileClasspath").get().extendsFrom(compileClasspath.get())
    named("gameTestRuntimeClasspath").get().extendsFrom(runtimeClasspath.get())
}

repositories {

}

val minecraftVersion = project.property("minecraft_version").toString()

dependencies {
    forge("net.minecraftforge:forge:${project.property("forge_version")}")

    runtimeOnly(
        group = "com.kotori316",
        name = "ScalableCatsForce".lowercase(),
        version = project.property("slpVersion").toString(),
        classifier = "with-library"
    ) {
        isTransitive = false
    }

    common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionForge")) { isTransitive = false }

    // Other mods
    modCompileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_forge_id").toString()
    )
    modCompileOnly(
        group = "curse.maven",
        name = "the-one-probe-245211",
        version = project.property("top_forge_id").toString()
    )
    if (System.getenv("RUN_GAME_TEST").toBoolean()) {
        modCompileOnly(
            group = "mezz.jei",
            name = "jei-1.20.4-forge",
            version = project.property("jei_forge_version").toString()
        ) { isTransitive = false }
    } else {
        modImplementation(
            group = "mezz.jei",
            name = "jei-1.20.4-forge",
            version = project.property("jei_forge_version").toString()
        ) { isTransitive = false }
    }
    modCompileOnly(
        group = "appeng",
        name = "appliedenergistics2-forge",
        version = project.property("ae2_forge_version").toString()
    ) { isTransitive = false }

    // Test Dependencies.
    // Required these libraries to execute the tests.
    // The library will avoid errors of ForgeRegistry and Capability.
    testImplementation(
        group = "org.mockito",
        name = "mockito-core",
        version = project.property("mockitoCoreVersion").toString()
    )
    testImplementation(
        group = "org.mockito",
        name = "mockito-inline",
        version = project.property("mockitoInlineVersion").toString()
    )
    // forgeRuntimeLibrary(platform(group="org.junit", name="junit-bom", version=project.jupiterVersion))
    // forgeRuntimeLibrary("org.junit.jupiter:junit-jupiter")
    modImplementation("com.kotori316:test-utility-forge:${project.property("test_util_version")}") {
        isTransitive = false
    }
    modImplementation("com.kotori316:debug-utility-forge:${project.property("debug_util_version")}") {
        isTransitive = false
    }

    "gameTestImplementation"(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    "gameTestImplementation"("org.junit.jupiter:junit-jupiter")
    "gameTestCompileOnly"(sourceSets.main.get().output)
    "genDataCompileOnly"(sourceSets.main.get().output)
}

tasks.register("checkResourceFiles") {
    doLast {
        val forgeVersion = project.property("forge_version")

        @Suppress("UnstableApiUsage")
        val parent = "${(loom as LoomGradleExtension).files.userCache}/${minecraftVersion}/forge/${forgeVersion}"
        Files.list(Path.of(parent))
            .forEach {
                System.out.printf("IsDir %b, Size %d, name %s%n", Files.isDirectory(it), Files.size(it), it)
            }
    }
}

ext {
    set(
        "changelogHeader", """
        # Large Fluid Tank for forge
        
        | Dependency | Version |
        | -- | -- |
        | Minecraft | $minecraftVersion |
        | Forge | ${project.property("forge_version")} |
        | scalable-cats-force | ${project.property("slpVersion")} |
        | Applied Energistics 2 | ${project.property("ae2_forge_version")} |
        | Jade | File id: ${project.property("jade_forge_id")} |
        | TheOneProbe | File id: ${project.property("top_forge_id")} |
        """.trimIndent()
    )
}
