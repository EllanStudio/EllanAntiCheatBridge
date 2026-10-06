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
    compileOnly("io.papermc.paper:paper-api:26.3.build.157-beta")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
}

tasks.jar {
    archiveBaseName = "EllanAntiCheatBridge-Paper"
    from(project(":common").layout.buildDirectory.dir("classes/java/main"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
