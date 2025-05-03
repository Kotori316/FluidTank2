import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("scala")
    id("idea")
}

val minecraftVersion = project.property("minecraft_version") as String

base {
    archivesName = "${project.property("archives_base_name")}-${project.name}"
    group = project.findProperty("maven_group") as String
    version = project.findProperty("mod_version") as String
}

repositories {
    maven {
        name = "Minecraft-Manually"
        url = uri("https://libraries.minecraft.net/")
        content {
            includeGroup("org.lwjgl")
            includeGroup("com.mojang")
        }
    }
    mavenCentral()
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "Kotori316-main"
        url = uri("https://maven.kotori316.com")
        val catsVersion = project.property("cats_version") as String
        content {
            includeVersion("org.typelevel", "cats-core_3", catsVersion)
            includeVersion("org.typelevel", "cats-kernel_3", catsVersion)
            includeVersion("org.typelevel", "cats-core_2.13", catsVersion)
            includeVersion("org.typelevel", "cats-kernel_2.13", catsVersion)
            includeGroup("com.kotori316")
        }
    }
    maven {
        name = "Curse"
        url = uri("https://www.cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "JEI"
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }
    maven {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
        content {
            includeVersion("appeng", "appliedenergistics2-forge", project.property("ae2_forge_version") as String)
            includeVersion("appeng", "appliedenergistics2", project.property("ae2_neoforge_version") as String)
            includeVersion("appeng", "appliedenergistics2-fabric", project.property("ae2_fabric_version") as String)
        }
    }
    maven {
        name = "MavenTestGCP"
        url = uri("https://storage.googleapis.com/kotori316-maven-test-storage/maven/")
    }
    mavenLocal()
}

configurations {
    create("junit")
}

dependencies {
    implementation(
        group = "org.typelevel",
        name = "cats-core_3",
        version = project.property("cats_version") as String
    ) { exclude("org.scala-lang") }

    testImplementation(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")
    "junit"(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    "junit"("org.junit.jupiter:junit-jupiter")
    "junit"("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

scala {
    scalaVersion = project.property("scala3_version").toString()
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    withType(ScalaCompile::class) {
        //scalaCompileOptions.additionalParameters.add("-no-indent")
        scalaCompileOptions.additionalParameters.add("-old-syntax")
        scalaCompileOptions.additionalParameters.add("-source:3.4-migration")
        scalaCompileOptions.additionalParameters.add("-rewrite")
        options.encoding = "UTF-8"
    }

    withType(ProcessResources::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    withType(Jar::class) {
        exclude(".cache/")
    }
    named("sourcesJar", Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
        excludeDirs = excludeDirs + file("run") + file("runs") + file("run-server")
    }
}
