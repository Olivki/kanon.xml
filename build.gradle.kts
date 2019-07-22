import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import name.remal.gradle_plugins.plugins.publish.bintray.RepositoryHandlerBintrayExtension
import name.remal.gradle_plugins.dsl.extensions.*

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("name.remal:gradle-plugins:1.0.129")
    }
}

plugins {
    kotlin("jvm").version("1.3.41")

    `maven-publish`
}

apply(plugin = "name.remal.maven-publish-bintray")

project.group = "moe.kanon.xml"
project.description = "A DSL made in Kotlin for parsing and generating XML."
project.version = "3.0.0"
val gitUrl = "https://gitlab.com/Olivki/kanon-xml"

// General Tasks
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "org.jdom", name = "jdom2", version = "2.0.6")
    implementation(group = "jaxen", name = "jaxen", version = "1.1.6")
    
    // Test Dependencies
    testImplementation(group = "io.kotlintest", name = "kotlintest-runner-junit5", version = "3.1.11")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

project.afterEvaluate {
    publishing.publications.withType<MavenPublication> {
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

    publishing.repositories.convention[RepositoryHandlerBintrayExtension::class.java].bintray {
        owner = "olivki"
        repositoryName = "kanon"
        packageName = "kanon.events"
    }
}