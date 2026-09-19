plugins {
    id("com.kotori316.common")
}

dependencies {
    // JEI
    implementation("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-common-api:${project.property("jei_neoforge_version")}") {
        isTransitive = false
    }
    implementation("mezz.jei:jei-${project.property("jei_fabric_repo_version")}-fabric:${project.property("jei_fabric_version")}") {
        isTransitive = false
    }
    implementation("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-neoforge:${project.property("jei_neoforge_version")}") {
        isTransitive = false
    }
    // Scala 3
    implementation("org.scala-lang:scala-library:${project.property("scala3_version")}") {
        isTransitive = false
    }
}
