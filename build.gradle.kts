/*
 * Copyright 2019 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version("1.3.21")

    id("com.adarshr.test-logger").version("1.6.0") // For pretty-printing for tests.
    id("com.jfrog.bintray").version("1.8.4") // For publishing to BinTray.
    id("org.jetbrains.dokka").version("0.9.17") // The KDoc engine.
    id("com.github.ben-manes.versions").version("0.20.0") // For checking for new dependency versions.

    `maven-publish`
}

// Project Specific Variables
project.group = "moe.kanon.xml"
project.description = "A DSL made in Kotlin for parsing and generating XML."
project.version = "2.0.0"
val artifactName = "kanon.xml"
val gitUrl = "https://gitlab.com/Olivki/kanon-xml"

// General Tasks
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Normal Dependencies
    compile(kotlin("stdlib-jdk8"))
    compile(group = "org.jdom", name = "jdom2", version = "2.0.6")
    compile(group = "jaxen", name = "jaxen", version = "1.1.6")
    
    // Test Dependencies
    testImplementation(group = "io.kotlintest", name = "kotlintest-runner-junit5", version = "3.1.11")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
    kotlinOptions.jvmTarget = "1.8"
}

// Dokka Tasks
val dokkaJavaDoc by tasks.creating(DokkaTask::class) {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"
    inputs.dir("src/main/kotlin")
    includeNonPublic = false
    skipEmptyPackages = true
    jdkVersion = 8
}

// Test Tasks
testlogger {
    setTheme("mocha")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Artifact Tasks
val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    description = "Assembles the sources of this project into a *-sources.jar file."
    classifier = "sources"
    
    from(project.sourceSets["main"].allSource)
}

val javaDocJar by tasks.creating(Jar::class) {
    description = "Creates a *-javadoc.jar from the generated dokka output."
    classifier = "javadoc"
    
    from(dokkaJavaDoc)
}

artifacts {
    add("archives", sourcesJar)
    add("archives", javaDocJar)
}

// Publishing Tasks
// BinTray
bintray {
    // Credentials.
    user = getVariable("BINTRAY_USER")
    key = getVariable("BINTRAY_KEY")
    
    // Whether or not the "package" should automatically be published.
    publish = true
    
    // Sets the publication to our created maven publication instance.
    setPublications("mavenPublication")
    
    // Details for the actual package that's going up on BinTray.
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "kanon"
        desc = project.description
        name = artifactName
        websiteUrl = gitUrl
        vcsUrl = "$gitUrl.git"
        publicDownloadNumbers = true
        setLicenses("Apache-2.0")
        setLabels("kotlin")
        
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = project.version.toString()
            released = `java.util`.Date().toString()
        })
    })
}

// Maven Tasks
publishing {
    publications.invoke {
        register("mavenPublication", MavenPublication::class.java) {
            from(components["java"])
            
            afterEvaluate {
                // General project information.
                groupId = project.group.toString()
                version = project.version.toString()
                artifactId = artifactName
                
                // Any extra artifacts that need to be added, ie: sources & javadoc jars.
                artifact(sourcesJar)
                artifact(javaDocJar)
                
                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set(gitUrl)
                    
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    
                    developers {
                        developer {
                            email.set("oliver@berg.moe")
                            id.set("Olivki")
                            name.set("Oliver Berg")
                        }
                    }
                    
                    scm {
                        url.set(gitUrl)
                    }
                }
            }
        }
    }
}

// Misc Functions & Properties
fun getVariable(name: String) = System.getenv(name)!!
