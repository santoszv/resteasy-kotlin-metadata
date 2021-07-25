plugins {
    war
    kotlin("jvm") version "1.5.21"
}

repositories {
    mavenCentral()
}

dependencies {

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Jakarta EE
    compileOnly("jakarta.platform:jakarta.jakartaee-api:9.0.0")

    // Rest Easy
    compileOnly(files("lib/resteasy-jaxrs-3.15.1.Final-ee9.jar"))
}