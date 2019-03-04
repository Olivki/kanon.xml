## kanon.xml

kanon.xml is a DSL made  in Kotlin for making the task of parsing and generating XML more visually clear, it supports a few different accents, so that the code can be written in the style that fits the user best.

kanon.xml uses JDom as it's underlying API.

## Usage

A guide on how to use to kanon.xml can be found [here](./src/main/kotlin/README.md).

## Installation

Gradle

- Groovy

  ```groovy
  repositories {
    	jcenter()
  }
  
  dependencies {
      compile "moe.kanon.xml:kanon.xml:2.0.0"
  }
  ```

- Kotlin

  ```kotlin
  repositories {
      jcenter()
  }
  
  dependencies {
      compile(group = "moe.kanon.xml", name = "kanon.xml", version = "2.0.0")
  }
  ```

Maven

```xml
<dependency>
    <groupId>moe.kanon.xml</groupId>
    <artifactId>kanon.xml</artifactId>
    <version>2.0.0</version>
    <type>pom</type>
</dependency>

```

## License

````
Copyright 2019 Oliver Berg

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````