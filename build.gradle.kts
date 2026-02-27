/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

plugins {
    id("java-library")
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
}

group = "ai.protify"
version = "0.1.1"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

centralPortal {
    username = findProperty("sonatypeUsername") as String? ?: System.getenv("SONATYPE_USERNAME")
    password = findProperty("sonatypePassword") as String? ?: System.getenv("SONATYPE_PASSWORD")

    pom {
        name = "Protify AI"
        description = "Protify AI Zero Dependency Java SDK"
        url = "https://github.com/protifyconsulting/protifyai-java"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "protify"
                name = "Protify Consulting LLC"
                email = "jkuryla@protify.ai"
            }
        }

        scm {
            connection = "scm:git:git://github.com/protifyconsulting/protifyai-java.git"
            developerConnection = "scm:git:ssh://github.com/protifyconsulting/protifyai-java.git"
            url = "https://github.com/protifyconsulting/protifyai-java"
        }
    }
}