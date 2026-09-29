plugins {
    base
}

allprojects {
    group = "studio.ellan"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
