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
    
    @JvmStatic
    fun main(args: Array<String>) {
        println(document)
    }
}