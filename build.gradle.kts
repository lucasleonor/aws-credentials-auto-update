plugins {
    kotlin("jvm") version "1.6.10"
}

group = "com.jlr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.beust:klaxon:5.5")
}
