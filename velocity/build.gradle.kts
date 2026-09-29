plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    compileOnly("com.google.inject:guice:7.0.0")
}

tasks.jar {
    archiveBaseName = "EllanAntiCheatBridge-Velocity"
    from(project(":common").layout.buildDirectory.dir("classes/java/main"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
