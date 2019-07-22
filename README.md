## kanon.xml

[![Download](https://api.bintray.com/packages/olivki/kanon/kanon.xml/images/download.svg)](https://bintray.com/olivki/kanon/kanon.xml/_latestVersion)

kanon.xml is a Kotlin library that is made with the intent of making the act of building and/or traversing XML documents more visual, and thus making it clearer what is *actually* going on.

To accomplish this it provides two differenet DSLs, one for building documents, and one for traversing XML documents.

kanon.xml uses [JDOM2](http://www.jdom.org/) as the underlying XML engine for building and traversing documents.

Version 3.0 of kanon.xml is a complete rewrite from the previous major versions, and thus code that was made using the old system may not work, while the builder DSL is mainly the same, sans some missing functions to reduce clutter, the traveler *(or `parser` as it was called up until v3.0)* has been completely remade to make it a nicer experience.

## Usage

*(This section is currently very bare-bones, a more thorough guide will be written later)*

### Building Documents

To build a document, simple call the top-level function `xml`, which takes 3 arguments, the name of the root element, an optional `Namespace` for the root element, and the builder scope.

Example:

```kotlin
val doc = xml("root", Namespace("uri", "prefix")) {
    attributes {
        "key" { "value" }
        "bool" { true }
    }
    element("element") {
        attribute("key") { "value" }
        textElement("text_element") { "This is text" }
        element("child_element") {
            attribute("leet") { 1337 }
            text { "This is text" }
        }
    }
}

println(doc)
```

The above code will print this to the console:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<uri:root xmlns:uri="prefix" key="value" bool="true">
  <element key="value">
    <text_element>This is text</text_element>
    <child_element leet="1337">This is text</child_element>
  </element>
</uri:root>
```

The functions shown above are only a *few* of the functions that can be used, for now to find more functions, check out the source code.

You can also use the DSL to build a stand-alone `Element` instance by calling the top-level function `buildElement`, which accepts the same arguments as the `xml` function. The `ElementBuilder` container works essentially the same as the `DocumentBuilder` container, except that it's missing the `format` and `docType` functions.

### Traversing Documents

To start traversing a document simply invoke the top-level function `traverse` providing either a `Document` instance, or an `Element` instance as the argument.

To quickly showcase how one can traverse a document, let's first build a simple document that has *some* purpose, so let's suppose we have the following code:

```kotlin
data class Person(val name: String, val age: Int, val gender: Gender)

enum class Gender { MALE, FEMALE }

val people = listOf(
    Person("Steve Holder", 37, Gender.MALE),
    Person("Dave Schwift", 29, Gender.MALE),
    Person("Mari Kwartz", 17, Gender.FEMALE)
)

// people = [Person(name=Steve Holder, age=37, gender=MALE), Person(name=Dave Schwift, age=29, gender=MALE), Person(name=Mari Kwartz, age=17, gender=FEMALE)]

val doc = xml("people") {
    for (person in people) {
        element("person") {
            textElement("name") { person.name }
            textElement("age") { person.age }
            textElement("gender") { person.gender }
        }
    }
}
```

The above code builds a `Document` that looks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<people>
  <person>
    <name>Steve Holder</name>
    <age>37</age>
    <gender>MALE</gender>
  </person>
  <person>
    <name>Dave Schwift</name>
    <age>29</age>
    <gender>MALE</gender>
  </person>
  <person>
    <name>Mari Kwartz</name>
    <age>17</age>
    <gender>FEMALE</gender>
  </person>
</people>
```

Now to reconstruct the `people` list from the `doc`:

```kotlin
var people: List<Person> = emptyList()

// people = [] (empty)

// because the 'doc' variable refers to a 'DocumentBuilder' instance, we need to use the underlying
// 'Document' instance that the builder is wrapping around
traverse(doc.document) {
    people = elements("person") {
        Person(
            element("name", { it.text }, throwMissingElement("Missing 'name' element")),
            element("age", { it.text.toInt() }, throwMissingElement("Missing 'age' element")),
            element("gender", { Gender.valueOf(it.text) }, throwMissingElement("Missing 'gender' element"))
        )
    }
}

// people = [Person(name=Steve Holder, age=37, gender=MALE), Person(name=Dave Schwift, age=29, gender=MALE), Person(name=Mari Kwartz, age=17, gender=FEMALE)]
```

## Installation

Gradle

- Groovy

  ```groovy
  repositories {
    	jcenter()
  }
  
  dependencies {
      implementation "moe.kanon.xml:kanon.xml:${LATEST_VERSION}"
  }
  ```

- Kotlin

  ```kotlin
  repositories {
      jcenter()
  }
  
  dependencies {
      implementation(group = "moe.kanon.xml", name = "kanon.xml", version = "${LATEST_VERSION}")
  }
  ```

Maven

```xml
<dependency>
    <groupId>moe.kanon.xml</groupId>
    <artifactId>kanon.xml</artifactId>
    <version>${LATEST_VERSION}</version>
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