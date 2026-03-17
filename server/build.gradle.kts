plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    application
}

group = "com.callapp"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.1.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.1")
    implementation("io.ktor:ktor-server-config-yaml-jvm:3.1.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.1")
    implementation("io.ktor:ktor-server-auth-jvm:3.1.1")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.1")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.1.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.1.1")
    implementation("io.ktor:ktor-server-websockets-jvm:3.1.1")
    implementation("io.ktor:ktor-server-cors-jvm:3.1.1")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.1.1")
    testImplementation("io.ktor:ktor-client-websockets-jvm:3.1.1")
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.ApplicationKt",
                    "*.AppDependenciesKt",
                    "*.routes.*Dto",
                    "*.routes.ErrorResponse",
                    "*.routes.HealthResponse",
                    "*.database.DatabaseHealth"
                )
            }
        }

        total {
            html {
                onCheck = false
            }
            xml {
                onCheck = false
            }
            verify {
                rule {
                    bound {
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                        minValue = 92
                    }
                    bound {
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                        minValue = 45
                    }
                }
            }
        }
    }
}
