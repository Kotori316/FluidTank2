plugins {
    alias(libs.plugins.idea.ext)
}

version = project.findProperty("mod_version") as String

tasks.named("wrapper", Wrapper::class) {
    gradleVersion = "9.7.0"
    distributionType = Wrapper.DistributionType.BIN
}
