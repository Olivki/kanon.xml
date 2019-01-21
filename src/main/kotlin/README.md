[TOC levels=6]: # "## Table of Contents"

## Table of Contents
- [XmlBuilder](#xmlbuilder)
    - [Accents](#accents)
        - [Elements](#elements)
        - [Attributes](#attributes)
        - [TextNodes](#textnodes)
    - [Builder How-To](#builder-how-to)
- [XmlParser](#xmlparser)
    - [Parser How-To](#parser-how-to)

## XmlBuilder

### Accents

kanon.xml supports a variety of "accents"[^1], the accents vary from verbose to rather direct, this is so that the user can pick what they prefer.[^2]

*These functions **only** work inside of the DSL scope.*

**Name Explanation**:
- `/* ... */` represents an area that can be filled with code.
- "root level" means directly inside of the `xml(...) { ... }` closure.
- "element level" means inside of another element closure.
- Any strings/names used in these examples can and *should* be replaced with appropriate names.

**Note**: Because these documents will *always*[^3] have a `root` element, any function that works on an Element, will work at the root level, they will just be appended onto the `root` element instead.

#### Elements
- - -
Elements are essentially just containers.

If nothing is specified inside of the closure, it will become a self-closing tag.

- Root Level:
    ```kotlin
    xml("root") {
        // String Invoke
        "firstElement" { /*...*/ }
        
        // Function 
        element(tagName = "secondElement") { /*...*/ }
    }
    ```
    XML Output:
    ```xml
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <root>
      <firstElement/>
      <secondElement/>
    </root>
    ```
- Element Level *(This can be repeated as many times as needed.)*
    ```kotlin
    xml("root") {
        element("firstElement") {
            element("elementChild") { /* ... */ }
        }
    }
    ```
    XML Output:
    ```xml
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <root>
      <firstElement>
        <elementChild/>
      </firstElement>
    </root>
    ```
    
#### Attributes
- - -
Attributes, much like Elements, can be created at the root level and inside of an element, though the latter is more common.

Something to keep in mind when working with attributes is that they're *not* appended onto the element in the order they were added, but rather in alphabetical order, as per the XML Specification.

```kotlin
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
                attribute("meaning") { 42 }
                attribute("rock", "kent")
            }
        }
    }
```
XML Output:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<root false="false" name="Slim Shady" one="1" true="true" two="2">
  <element darling="2" death-and-taxes="true" erio="touwa" meaning="42" rock="kent"/>
</root>
```

#### TextNodes
- - -
TextNodes *(or Text as they're actually called)* are quite simply, just text.

TextNodes are a bit different than the rest of the available functions, in the way that the two different functions available for creating them actually accomplish two *very* different things.

They're also different because they're the only object in which all of the functions *aren't* available at the root level.

- **Closure**
    ```kotlin
    xml("root") {
        // text { ... } can not be used at root level because of how it works.
        // so we need to create an element to use it.
        element("textContainer") {
            // This essentially turns this element into a text container.
            // You can still add sub-elements onto this element, but it's advised against, because you'll end up with weird looking XML.
            // It's also recommended to use the function version if you're not going to add any attributes to this text container.
            text { "I'm a text container!" }
        }
    }
    ```
    XML Output:
    ```xml
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <root>
      <textContainer>I'm a text container!</textContainer>
    </root>
    ```
- **Function**
    ```kotlin
    xml("root") {
        // This accomplishes the same thing as the example shown for the closure variant.
        // The function version creates the element container and then appends the specified text.
        // This means that unless you need to do additional work on the text container, this is the preferred method.
        text(tagName = "textContainer") { "I'm a text container!" }
    }
    ```
    XML Output:
    ```xml
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <root>
      <textContainer>I'm a text container!</textContainer>
    </root>
    ```

### Builder How-To
- - -

To start building your XML file you'll need to call the `xml` function, which takes a string parameter called `root`[^3].

## XmlParser

### Parser How-To



[^1]: Accents/Syntax, simply just the way you write the DSL.
[^2]: It is recommended to stick with one accent per DSL rather than switching it up within the same DSL, this is to make the code look coherent. Wildly switching between the different accents in the same DSL will end up with code that looks confusing and ugly.
[^3]: As per the [XML Specification (Fifth Edition)](https://www.w3.org/TR/REC-xml/); "[Definition: There is exactly one element, called the root, or document element, no part of which appears in the content of any other element.]" kanon.xml requires you to supply a root element and does *not*, and will *not*, support making a rootless XML document because that would create a faulty XML document.