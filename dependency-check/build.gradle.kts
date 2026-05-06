plugins {
    id("com.kotori316.common")
}

dependencies {
    // JEI
    implementation("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-common-api:${project.property("jei_neoforge_version")}")
    implementation("mezz.jei:jei-${project.property("jei_fabric_repo_version")}-fabric:${project.property("jei_fabric_version")}")
    implementation("mezz.jei:jei-${project.property("jei_neoforge_repo_version")}-neoforge:${project.property("jei_neoforge_version")}")
    // Scala 3
    implementation("org.scala-lang:scala3-library_3:${project.property("scala3_version")}")
}
