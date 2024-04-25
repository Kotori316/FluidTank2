plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

sourceSets {
    create("gameTest") {
        scala {
            srcDir("src/gameTest/scala")
        }
        resources {
            srcDir("src/gameTest/resources")
        }
    }
}

loom {
    neoForge {
        // useForgeLoggerConfig = true
    }

    runs {
        named("client") {
            configName = "Client"
            property("neoforge.enabledGameTestNamespaces", "fluidtank")
            runDir = "run"
            mods {
                create("main") {
                    sourceSet("main")
                }
                create("gameTest") {
                    sourceSet("gameTest")
                }
            }
        }
        named("server") {
            configName = "Server"
            runDir = "run-server"
        }
        create("gameTest") {
            configName = "GameTest"
            environment("gameTestServer")
            forgeTemplate("gameTestServer")
            vmArg("-ea")
            property("fabric.dli.env", "gameTestServer")
            property("neoforge.enabledGameTestNamespaces", "fluidtank")
            runDir = "game-test"
            mods {
                create("main") {
                    sourceSet("main")
                }
                create("gameTest") {
                    sourceSet("gameTest")
                }
            }
        }
    }
}

configurations {
    named("developmentNeoForge").get().extendsFrom(common.get())
    named("gameTestCompileClasspath").get().extendsFrom(compileClasspath.get())
    named("gameTestRuntimeClasspath").get().extendsFrom(runtimeClasspath.get())
}

repositories {
    maven {
        url = uri("https://prmaven.neoforged.net/NeoForge/pr794")
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${project.property("neoforge_version")}")

    runtimeOnly(
        group = "com.kotori316",
        name = "ScalableCatsForce-NeoForge".lowercase(),
        version = project.property("slp_neoforge_version").toString(),
        classifier = "with-library"
    ) {
        isTransitive = false
    }

    common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionNeoForge")) { isTransitive = false }

    modCompileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_neoforge_id").toString()
    )
    modCompileOnly(
        group = "curse.maven",
        name = "the-one-probe-245211",
        version = project.property("top_neoforge_id").toString()
    )
    modCompileOnly(
        group = "appeng",
        name = "appliedenergistics2-neoforge",
        version = project.property("ae2_neoforge_version").toString()
    ) { isTransitive = false }
    /*modLocalRuntime(
        group = "mezz.jei",
        name = "jei-1.20.4-neoforge",
        version = project.property("jei_neoforge_version").toString()
    ) { isTransitive = false }*/
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
    forgeRuntimeLibrary(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    forgeRuntimeLibrary("org.junit.jupiter:junit-jupiter")
    modImplementation("com.kotori316:test-utility-neoforge:${project.property("test_util_version")}") {
        exclude(group = "org.mockito")
    }
    modImplementation("com.kotori316:debug-utility-neoforge:${project.property("debug_util_version")}")

    "gameTestImplementation"(sourceSets.main.get().output)
}

ext {
    set(
        "changelogHeader", """
        # Large Fluid Tank for neoforge
        
        | Dependency | Version |
        | -- | -- |
        | Minecraft | ${project.property("minecraft_version")} |
        | NeoForge | ${project.property("neoforge_version")} |
        | scalable-cats-force | ${project.property("slp_neoforge_version")} |
        """.trimIndent()
    )
}
