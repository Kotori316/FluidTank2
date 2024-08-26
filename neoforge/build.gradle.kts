plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    alias(libs.plugins.neoforge.gradle)
}

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
}

runs {
    configureEach {
        systemProperty("neoforge.enabledGameTestNamespaces", "fluidtank")
        systemProperty("mixin.debug.export", "true")
        modSources.add("fluidtank", sourceSets["main"])
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
        modSources.add("fluidtank_gametest", sourceSets["gameTest"])
        dependencies {
            runtime(project.configurations["junit"])
        }
    }
    create("junit") {
        unitTestSources.add("fluidtank_test", sourceSets["test"])
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
        name = "jei-1.21-neoforge",
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
    implementation("com.kotori316:test-utility-neoforge:${project.property("test_util_version")}") {
        exclude(group = "org.mockito")
    }
    implementation("com.kotori316:debug-utility-neoforge:${project.property("debug_util_version")}")

    "gameTestImplementation"(sourceSets.main.get().output)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
