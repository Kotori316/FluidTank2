plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    id("com.kotori316.dg")
    alias(libs.plugins.fabric.loom)
}

sourceSets {
    create("clientTest") {
        val s = this
        project.configurations {
            named(s.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.compileClasspath.get())
                extendsFrom(project.configurations.testCompileClasspath.get())
            }
            named(s.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.runtimeClasspath.get())
                extendsFrom(project.configurations.testRuntimeClasspath.get())
            }
        }
    }
}

fabricApi {
}

loom {
    runs {
        named("client") {
            displayName = "Fabric Client"
            runDirectory = file("run")
            sourceSet = sourceSets["test"].name
        }
        named("server") {
            displayName = "Fabric Server"
            runDirectory = file("run-server")
        }

        create("gameTestServer") {
            displayName = "Fabric GameTest"
            server()
            jvmArguments.add("-ea")
            systemProperties.put("fabric-api.GameTest".lowercase(), "")
            systemProperties.put("fabric-api.GameTest.report-file".lowercase(), "game-test/test-results/game_test.xml")
            systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
            systemProperties.put("mixin.debug.export", "true")
            runDirectory = file("game-test")
            sourceSet = sourceSets["test"].name
        }

        create("data") {
            client()
            displayName = "Data"
            runDirectory = file("build/dataGen")
            systemProperties.put("fabric-api.DataGen".lowercase(), "")
            systemProperties.put("fabric-api.DataGen.output-dir".lowercase(), "${file("src/generated/resources")}")
            systemProperties.put("fabric-api.DataGen.strict-validation".lowercase(), "")
            systemProperties.put("fabric-api.DataGen.ModId".lowercase(), "fluidtank_data")

            generateRunConfig = true
            sourceSet = sourceSets["dataGen"].name
        }

        create("gameTestClient") {
            client()
            displayName = "Fabric Client GameTest"
            systemProperties.put("fabric.client.gametest", "")
            systemProperties.put("fabric.client.gametest.disableNetworkSynchronizer", "true")
            runDirectory = file("run-client")
            sourceSet = sourceSets["clientTest"].name
            // val openalLib = System.getenv("OPENAL_LIB")
            // if (!openalLib.isNullOrEmpty()) {
            //     systemProperties.put("org.lwjgl.openal.libname", openalLib)
            //     environmentVars.put("LD_PRELOAD", openalLib)
            // }
        }
    }
}

repositories {

}

val minecraftVersion = project.property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")

    implementation(
        "net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}"
    )
    implementation(
        "net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}"
    )

    runtimeOnly(
        "com.kotori316:scalable-cats-force-fabric:${project.property("slp_fabric_version")}:dev"
    ) { isTransitive = false }

    // Other mods
    implementation(
        "curse.maven:jade-324717:${project.property("jade_fabric_id")}"
    )
    compileOnly("mezz.jei:jei-${project.property("jei_fabric_repo_version")}-fabric-api:${project.property("jei_fabric_version")}")
    runtimeOnly("mezz.jei:jei-${project.property("jei_fabric_repo_version")}-fabric:${project.property("jei_fabric_version")}")
    //noinspection SpellCheckingInspection
    implementation(
        "teamreborn:energy:${project.property("fabric_energy_version")}"
    )
    implementation("com.kotori316:debug-utility-fabric:${project.property("debug_util_version")}") {
        exclude("net.fabricmc.fabric-api", "fabric-api")
    }
    implementation("com.kotori316:VersionCheckerMod:${project.property("automatic_potato_version")}") {
        isTransitive = false
    }

    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("fabric_loader_version")}")
    testImplementation(project(":gameTest:commonTest"))

    "clientTestImplementation"(project.sourceSets.main.get().output)
}

tasks {
    /*val jksSignJar = register("jksSignJar", JarSignTask::class) {
        jarTask = project.tasks.jar
    }
    jar.configure {
        finalizedBy(jksSignJar)
    }*/
}
