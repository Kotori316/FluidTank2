import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("scala")
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
        name = "Kotori316BackUp"
        url = uri("https://storage.googleapis.com/kotori316-maven-storage/maven/")
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
            includeVersion("appeng", "appliedenergistics2-neoforge", project.property("ae2_neoforge_version") as String)
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

val enableScala2 = false

dependencies {

    compileOnly(
        group = "org.scala-lang",
        name = "scala-library",
        version = project.property("scala2_version") as String
    )
    testImplementation(
        group = "org.scala-lang",
        name = "scala-library",
        version = project.property("scala2_version") as String
    )
    if (enableScala2 && System.getProperty("idea.sync.active", "false").toBoolean() ||
        System.getenv("FORCE_SCALA2").toBoolean()
    ) {
        compileOnly(
            group = "org.typelevel",
            name = "cats-core_2.13",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
        testImplementation(
            group = "org.typelevel",
            name = "cats-core_2.13",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
    } else {
        compileOnly(
            group = "org.scala-lang",
            name = "scala3-library_3",
            version = project.property("scala3_version") as String
        )
        compileOnly(
            group = "org.typelevel",
            name = "cats-core_3",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
        testImplementation(
            group = "org.scala-lang",
            name = "scala3-library_3",
            version = project.property("scala3_version") as String
        )
        testImplementation(
            group = "org.typelevel",
            name = "cats-core_3",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
    }

    testImplementation(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    "junit"(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    "junit"("org.junit.jupiter:junit-jupiter")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    withType(ScalaCompile::class) {
        if (enableScala2 && System.getProperty("idea.sync.active", "false").toBoolean() ||
            System.getenv("FORCE_SCALA2").toBoolean()
        ) {
            scalaCompileOptions.additionalParameters = listOf("-X" + "source:3")
        }
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
