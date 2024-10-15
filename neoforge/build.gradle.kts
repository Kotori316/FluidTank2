plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    id("com.kotori316.dg")
    alias(libs.plugins.neoforge.gradle)
}

val modId = "FluidTank".lowercase()

sourceSets {
    create("gameTest") {
        val sourceSet = this
        scala {
            srcDir("src/gameTest/scala")
        }
        resources {
            srcDir("src/gameTest/resources")
        }
        project.configurations {
            named(sourceSet.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.compileClasspath.get())
            }
            named(sourceSet.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.runtimeClasspath.get())
            }
        }
    }
    create("commonDataGen") {
        val s = this
        project.configurations {
            named(s.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.dataGenCompileClasspath.get())
            }
            named(s.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.dataGenRuntimeClasspath.get())
            }
        }
    }
}

tasks.named("processCommonDataGenResources", ProcessResources::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

runs {
    configureEach {
        systemProperty("neoforge.enabledGameTestNamespaces", modId)
        systemProperty("mixin.debug.export", "true")
        modSources.add(modId, sourceSets["main"])
    }

    create("client") {
        workingDirectory = project.file("run")
        arguments("--username", "Kotori")
    }
    create("server") {
        workingDirectory = project.file("run-server")
    }
    create("gameTestServer") {
        jvmArgument("-ea")
        workingDirectory = project.file("game-test")
        modSources.add("${modId}_gametest", sourceSets["gameTest"])
        dependencies {
            runtime(project.configurations["junit"])
        }
    }
    if (System.getenv("RUN_DATA_GEN").toBoolean()) {
        create("data") {
            client()
            workingDirectory = project.file("runs/data")
            arguments(
                "--mod",
                "${modId}_data",
                "--all",
                "--output",
                file("src/generated/resources/").toString(),
                "--existing",
                file("src/main/resources/").toString()
            )
            modSources.add("${modId}_data", sourceSets["dataGen"])
        }
        create("commonData") {
            runType("data")
            isDataGenerator = true
            workingDirectory.set(project.file("runs/commonData"))
            arguments.addAll(
                "--mod",
                "${modId}_common_data",
                "--all",
                "--output",
                project(":common").file("src/generated/resources/").toString(),
                "--existing",
                project(":common").file("src/main/resources/").toString()
            )

            modSources.add("${modId}_common_data", sourceSets["commonDataGen"])
        }
    }
    create("junit") {
        unitTestSources.add("${modId}_test", sourceSets["test"])
    }
}

afterEvaluate {
    // Hack the NeoGradle setting, as it contains stupid configuration
    tasks.test {
        // disable test task as it fails due to accessing Minecraft resources
        // instead Neo adds another test task named "testJunit" and "build" depends on it
        enabled = false
    }
}

subsystems {
    parchment {
        minecraftVersion = project.property("parchment_mapping_mc") as String
        mappingsVersion = project.property("parchment_mapping_version") as String
    }
}

repositories {
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm" && requested.name.startsWith("asm")) {
            useVersion("9.7")
        }
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${project.property("neoforge_version")}")

    runtimeOnly(
        group = "com.kotori316",
        name = "ScalableCatsForce-NeoForge".lowercase(),
        version = project.property("slp_neoforge_version").toString(),
        classifier = "with-library"
    ) {
        isTransitive = false
    }

    compileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_neoforge_id").toString()
    )
    compileOnly(
        group = "curse.maven",
        name = "the-one-probe-245211",
        version = project.property("top_neoforge_id").toString()
    )
    implementation(
        group = "appeng",
        name = "appliedenergistics2-neoforge",
        version = project.property("ae2_neoforge_version").toString()
    ) { isTransitive = false }
    /*modLocalRuntime(
        group = "mezz.jei",
        name = "jei-${project.property("jei_neoforge_repo_version")}-neoforge",
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
    implementation("com.kotori316:debug-utility-neoforge:${project.property("debug_util_version")}") {
        exclude(group = "org.mockito")
    }

    "gameTestImplementation"(sourceSets.main.get().output)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileDataGenScala {
    source(project(":common").sourceSets["dataGen"].allSource)
}
