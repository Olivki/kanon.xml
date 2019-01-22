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

package moe.kanon.test

import moe.kanon.xml.xml

object Test {
    
    val document =
        xml("root") {
            attributes {
                "high_level" { "it's amazing." }
            }
            "test" {
                attributes {
                    "test" { false }
                    "steve" { 1.0 }
                    attribute("darling", "02")
                    attribute("franxx") { "darling" }
                }
                text { "hello" }
            }
            element("evenStevens") {
                text("subChildText") { "I'm a text container." }
                element("subChildElement") {
                    attributes { "attr" { 13.37 } }
                    text { "hello" }
                }
            }
        }
    
    val testDoc =
        xml("root") {
            // Closure variant.
            comment { "I'm a comment!" }
            
            element("person") {
                attributes {
                    attribute("name") { "Hazuki Kanon" }
                    attribute("sweet") { false }
                }
                // You can put comments pretty much anywhere, they're good for explaining
                // concepts in an XML file that's supposed to be read by a human.
                comment { "We can be anywhere!" }
            }
            
            // Function variant.
            comment("I'm also a comment!")
        }
    
    val people = listOf(
        Person("John Doe", Gender.MALE, 20, "Chaser"),
        Person("Mary Sue", Gender.FEMALE, 22, "Mary Sue"),
        Person("Hazuki Kanon", Gender.FEMALE, 16, "High School Student"),
        Person("SCP-049", Gender.MALE, 2462, "Plauge Doctor")
    )
    
    val peopleDocument =
        xml("people") {
            for ((name, gender, age, occupation) in people) {
                element("person") {
                    attributes {
                        attribute("name") { name }
                        attribute("gender") { gender }
                        attribute("age") { age }
                        attribute("occupation") { occupation }
                    }
                }
            }
        }
    
    @JvmStatic
    fun main(args: Array<String>) {
        println(peopleDocument)
    }
}

enum class Gender {
    MALE, FEMALE
}

data class Person(val name: String, val gender: Gender, val age: Int, val occupation: String)