plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    alias(libs.plugins.forge.gradle)
    alias(libs.plugins.forge.parchment)
    idea
}
val minecraftVersion = project.property("minecraft_version").toString()

minecraft {
    val parchmentMc = project.property("parchment_mapping_mc")
    val mapping = project.property("parchment_mapping_version")
    mappings(
        mapOf(
            "channel" to "parchment",
            "version" to "$parchmentMc-$mapping-$minecraftVersion"
        )
    )

    reobf = false

    runs {
        create("client") {
            property("forge.enabledGameTestNamespaces", "fluidtank")
            workingDirectory(project.file("run"))
        }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", "fluidtank")
            property("mixin.debug.export", "true")
            property("bsl.debug", "true")
            workingDirectory(project.file("game-test"))
            jvmArgs("-ea")
            mods {
                afterEvaluate {
                    create("main") {
                        source(sourceSets["runGame"])
                    }
                }
            }
        }
    }
}

sourceSets {
    val mainSourceSet by main
    create("genData") {
        val sourceSet = this
        scala {
            srcDir("src/genData/scala")
        }
        resources {
            srcDir("src/genData/resources")
        }

        compileClasspath += mainSourceSet.output
        runtimeClasspath += mainSourceSet.output
        project.configurations {
            named(sourceSet.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.compileClasspath.get())
            }
            named(sourceSet.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.runtimeClasspath.get())
            }
        }
    }
    val gameTestSourceSet = create("gameTest") {
        val sourceSet = this
        scala {
            srcDir("src/gameTest/scala")
        }
        resources {
            srcDir("src/gameTest/resources")
        }
        compileClasspath += mainSourceSet.output
        project.configurations {
            named(sourceSet.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.compileClasspath.get())
            }
            named(sourceSet.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.runtimeClasspath.get())
            }
        }
    }

    create("runGame") {
        val sourceSet = this
        runtimeClasspath += mainSourceSet.output
        runtimeClasspath += gameTestSourceSet.output
        project.configurations {
            named(sourceSet.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.named(gameTestSourceSet.compileClasspathConfigurationName).get())
            }
            named(sourceSet.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.named(gameTestSourceSet.runtimeClasspathConfigurationName).get())
            }
        }
    }

    configureEach {
        val dir = layout.buildDirectory.dir("forgeSourceSets/${name}")
        output.setResourcesDir(dir)
        // java.destinationDirectory = dir
        scala.destinationDirectory = dir
    }
}

repositories {

}

configurations.all {
    resolutionStrategy.force("net.sf.jopt-simple:jopt-simple:5.0.4")
}

dependencies {
    minecraft("net.minecraftforge:forge:${project.property("forge_version")}")

    runtimeOnly(
        group = "com.kotori316",
        name = "ScalableCatsForce".lowercase(),
        version = project.property("slpVersion").toString(),
        classifier = "with-library"
    ) {
        isTransitive = false
    }

    // Other mods
    compileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_forge_id").toString()
    )
    compileOnly(
        group = "curse.maven",
        name = "the-one-probe-245211",
        version = project.property("top_forge_id").toString()
    )
    if (System.getenv("RUN_GAME_TEST").toBoolean()) {
        compileOnly(
            group = "mezz.jei",
            name = "jei-1.21-forge",
            version = project.property("jei_forge_version").toString()
        ) { isTransitive = false }
    } else {
        compileOnly(
            group = "mezz.jei",
            name = "jei-1.21-forge",
            version = project.property("jei_forge_version").toString()
        ) { isTransitive = false }
    }
    compileOnly(
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

    implementation("com.kotori316:test-utility-forge:${project.property("test_util_version")}") {
        isTransitive = false
    }
    implementation("com.kotori316:debug-utility-forge:${project.property("debug_util_version")}") {
        isTransitive = false
    }

    "gameTestImplementation"(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    "gameTestImplementation"("org.junit.jupiter:junit-jupiter")
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
        | Jade | File id: ${project.property("jade_forge_id")} |
        | TheOneProbe | File id: ${project.property("top_forge_id")} |
        """.trimIndent()
    )
}

tasks.named("compileRunGameScala", ScalaCompile::class) {
    project.findProject(":common")?.let {
        source(it.sourceSets.main.get().scala)
    }
    source(project.sourceSets.main.get().scala)
    source(project.sourceSets.named("gameTest").get().scala)
}

tasks.named("processRunGameResources", ProcessResources::class) {
    project.findProject(":common")?.let {
        from(it.sourceSets.main.get().resources)
    }
    from(project.sourceSets.main.get().resources)
    from(project.sourceSets.named("gameTest").get().resources)
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
