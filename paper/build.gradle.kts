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
    val paperApiVersion = providers.gradleProperty("paperApiVersion").orElse("26.3.build.157-beta").get()

    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
}

tasks.jar {
    archiveBaseName = "EllanAntiCheatBridge-Paper"
    from(project(":common").layout.buildDirectory.dir("classes/java/main"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
