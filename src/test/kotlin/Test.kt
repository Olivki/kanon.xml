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
            // When doing anything with attributes at the root level they will be appended to the root element.
        
            // All of these options accept *any* value as the actual value for the attribute, but keep in mind that
            // the way they get "serialized" is via the toString() method. If you want a certain object to be serialized
            // differently, you'll have to convert it to a string before-hand.
        
            // You can pick between using the attributes(vararg Pair<String, V>) function or the closure.
            // You *can* use both and no issues would arise, you can also use them multiple times on the same element/root
            // but this very much *not* recommended as it'd make your code very ugly.
        
            // Vararg function.
            // Note: This only accepts Pairs, which are created here using the "to" infix function.
            attributes("two" to 2, "true" to true)
        
            // Closure
            attributes {
                // Inside of the closure you have 3 different styles you can pick from.
                // String Invoke
                "one" { 1 }
                // String Get
                "two"[2]
                // Closure.
                // The value inside of the closure will be the actual value of the attribute.
                attribute(name = "false") { false }
                // Function.
                attribute(name = "name", value = "Slim Shady")
                // You do not need to specify which parameter you're setting like done here, this is just to show what
                // the parameters actually set. So: attribute("false") { false } and attribute("name", "Slim Shady")
                // would work just fine, and is actually the recommended syntax.
            }
        
            // Showcase of it on an element.
            element("element") {
                attributes("darling" to 0 + 2, "death-and-taxes" to true)
            
                attributes {
                    "erio" { "touwa" }
                    "fifty"[50]
                    attribute("meaning") { 42 }
                    attribute("rock", "kent")
                }
            }
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
                    text("name") { name }
                    text("gender") { gender.toString() }
                    text("age") { age.toString() }
                    text("occupation") { occupation }
                }
            }
        }
    
    @JvmStatic
    fun main(args: Array<String>) {
        println(testDoc)
    }
}

enum class Gender {
    MALE, FEMALE
}

data class Person(val name: String, val gender: Gender, val age: Int, val occupation: String)