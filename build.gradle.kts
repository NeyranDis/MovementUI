plugins {
    kotlin("jvm") version "2.1.0-RC"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
group = "top.eternal.neyran.movementUI"
version = "1.1.4"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }

    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation ("org.bstats:bstats-bukkit:3.1.0")
    compileOnly("com.github.toxicity188:BetterHud:1.9")
    compileOnly(fileTree("libs"))
    compileOnly("org.projectlombok:lombok:1.18.34")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("net.kyori:adventure-api:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.3.0")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
