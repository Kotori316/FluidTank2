plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://maven.kotori316.com/") }
}

dependencies {
    mapOf(
        "com.kotori316.plugin.cf" to libs.versions.plugin.cf.get(),
        "com.github.johnrengelman.shadow" to libs.versions.plugin.shadow.get(),
        "me.modmuss50.mod-publish-plugin" to libs.versions.plugin.publish.all.get(),
    ).forEach { (name, version) ->
        implementation(group = name, name = "${name}.gradle.plugin", version = version)
    }
}
