plugins {
    id("io.micronaut.application") version "4.5.4"
    id("io.micronaut.aot") version "4.5.4"
    id("com.google.cloud.tools.jib") version "3.4.5"
}

micronaut {
    version = "4.9.3"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.micronaut.platform:micronaut-platform:4.9.3"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")

    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    compileOnly("io.micronaut:micronaut-http-client")
    implementation("ch.qos.logback:logback-classic")
    testImplementation("io.micronaut:micronaut-http-client")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    // Micronaut processor defined after Lombok
    annotationProcessor("io.micronaut:micronaut-inject-java")




    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")


    implementation("org.apache.commons:commons-lang3:3.19.0")


    runtimeOnly("org.yaml:snakeyaml")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass.set("org.home.work.App")
}

graalvmNative.toolchainDetection = false
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("example.micronaut.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

jib {
    from {
        image = "amazoncorretto:21.0.8-alpine3.22"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "homework:0.0.1"
    }
}
