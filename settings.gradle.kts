pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://maven.minecraftforge.net/") }
        maven { url = uri("https://maven.neoforged.net/releases/") }
        maven { url = uri("https://maven.parchmentmc.org") }
        maven { url = uri("https://maven.kotori316.com/") }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
    id("com.gradle.enterprise") version ("3.+")
}

gradleEnterprise {
    buildScan {
        if (System.getenv("CI").toBoolean()) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

includeBuild("gradle-plugins")

include("common")
if (!System.getenv("DISABLE_FABRIC").toBoolean()) {
    include("fabric")
}
if (!System.getenv("DISABLE_FORGE").toBoolean()) {
    include("forge")
}
if (!System.getenv("DISABLE_NEOFORGE").toBoolean()) {
    include("neoforge")
}

rootProject.name = "FluidTank-1.20"
