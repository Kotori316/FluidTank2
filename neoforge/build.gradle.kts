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
        shouldExportToIDE = false
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
    create("clientData") {
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
        runType("clientData")
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
    create("junit") {
        isJUnit = true
        unitTestSources.add("${modId}_test", sourceSets["test"])
    }
}

afterEvaluate {
    // Hack the NeoGradle setting, as it contains a stupid configuration
    tasks.test {
        // disable the test task as it fails due to accessing Minecraft resources
        // instead Neo adds another test task named "testJunit" and "build" depends on it
        enabled = false
    }
}

subsystems {
}

repositories {
}

dependencies {
    implementation("net.neoforged:neoforge:${project.property("neoforge_version")}")

    runtimeOnly(
        "com.kotori316:${"ScalableCatsForce-NeoForge".lowercase()}:${project.property("slp_neoforge_version")}:all"
    ) {
        isTransitive = false
    }

    implementation(
        "curse.maven:jade-324717:${project.property("jade_neoforge_id")}"
    )
    compileOnly(
        "curse.maven:the-one-probe-245211:${project.property("top_neoforge_id")}"
    )
    compileOnly(
        "appeng:appliedenergistics2:${project.property("ae2_neoforge_version")}"
    ) { isTransitive = false }
    compileOnly("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-neoforge-api:${project.property("jei_neoforge_version")}")
    runtimeOnly("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-neoforge:${project.property("jei_neoforge_version")}") {
        isTransitive = false
    }
    // Test Dependencies.
    // Required these libraries to execute the tests.
    // The library will avoid errors of ForgeRegistry and Capability.
    testImplementation(
        "org.mockito:mockito-core:${project.property("mockitoCoreVersion")}"
    )
    testImplementation(
        "org.mockito:mockito-inline:${project.property("mockitoInlineVersion")}"
    )
    implementation("com.kotori316:debug-utility-neoforge:${project.property("debug_util_version")}") {
        exclude(group = "org.mockito")
    }

    "gameTestImplementation"(sourceSets.main.get().output)
    "gameTestCompileOnly"(project(":gameTest:commonTest"))
    "gameTestImplementation"(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    "gameTestImplementation"("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileDataGenScala {
    source(project(":common").sourceSets["dataGen"].allSource)
}

tasks.named("compileGameTestScala", ScalaCompile::class) {
    source(project(":gameTest:commonTest").layout.projectDirectory.file("src/main/scala"))
}

val commonDataGenNeoForgeToml = tasks.register("commonDataGenNeoForgeToml", Delete::class) {
    dependsOn(
        tasks.named("compileGameTestScala", ScalaCompile::class),
    )
    delete(
        tasks.named("compileGameTestScala", ScalaCompile::class).flatMap { task ->
            task.destinationDirectory.map { it.file("META-INF/neoforge.mods.toml") }
        }
    )
}

afterEvaluate {
    tasks.named("testJunit") {
        outputs.upToDateWhen { false }
    }
    tasks.named("runCommonData") {
        dependsOn(commonDataGenNeoForgeToml)
    }
}

tasks.register("ideBeforeRun")
