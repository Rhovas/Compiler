plugins {
    kotlin("jvm") version "1.3.70"
}

group = "dev.rhovas.compiler"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.1")
}

tasks.test {
    useJUnitPlatform()
}
