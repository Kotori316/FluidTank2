plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    id("com.kotori316.dg")
    alias(libs.plugins.forge.gradle)
    alias(libs.plugins.forge.parchment)
}
val minecraftVersion = project.property("minecraft_version").toString()

val modId = "fluidtank"

sourceSets {
    val mainSourceSet by main
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

    val dataGenSourceSet by dataGen
    create("runDataGen") {
        val sourceSet = this
        project.configurations {
            named(sourceSet.compileClasspathConfigurationName) {
                extendsFrom(
                    project.configurations.named(mainSourceSet.compileClasspathConfigurationName).get(),
                    project.configurations.named(dataGenSourceSet.compileClasspathConfigurationName).get(),
                )
            }
            named(sourceSet.runtimeClasspathConfigurationName) {
                extendsFrom(
                    project.configurations.named(mainSourceSet.runtimeClasspathConfigurationName).get(),
                )
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
            property("forge.enabledGameTestNamespaces", modId)
            workingDirectory(project.file("run"))
        }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", modId)
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

        create("data") {
            workingDirectory(project.file("run-server"))
            args(
                "--mod",
                modId,
                "--all",
                "--output",
                file("src/generated/resources/"),
                "--existing",
                file("src/main/resources/")
            )

            mods {
                create(modId) {
                    source(sourceSets["runDataGen"])
                }
            }
        }
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

tasks.named("compileRunDataGenScala", ScalaCompile::class) {
    dependsOn("processDataGenResources")
    project.findProject(":common")?.let {
        source(it.sourceSets.main.get().scala)
        source(it.sourceSets.dataGen.get().scala)
    }
    source(project.sourceSets.main.get().scala)
    source(project.sourceSets.dataGen.get().scala)
}

tasks.named("processRunDataGenResources", ProcessResources::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    project.findProject(":common")?.let {
        from(it.sourceSets.main.get().resources)
        from(it.sourceSets.dataGen.get().resources)
    }
    from(project.sourceSets.main.get().resources)
    from(project.sourceSets.dataGen.get().resources)

    val projectVersion = project.version.toString()
    val minecraft = minecraftVersion
    inputs.property("version", projectVersion)
    inputs.property("minecraftVersion", minecraft)
    listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml").forEach { fileName ->
        filesMatching(fileName) {
            expand(
                "version" to projectVersion,
                "update_url" to "https://version.kotori316.com/get-version/${minecraft}/${project.name}/${modId}",
                "mc_version" to minecraft,
            )
        }
    }
}

tasks.named("compileDataGenScala") {
    dependsOn("processDataGenResources")
}
