## 2.0.0 (2019-03-xx)

There's been a switch from the DOM based parser from `javax` to the JDom library.

- ### Gradle

  - #### General Changes

    - Switched from the Groovy based DSL to the Kotlin based DSL.

  - #### Dependencies

    - {+++} Added `org.jdom.jdom2` as a dependency.
    - {—} Removed the `moe.kanon.kextensions`, as it's no longer needed with the switch to JDom as the document parser and generator.
    - Upgraded the Kotlin version from 1.3.20 to 1.3.21

- ### XmlBuilder.kt

  - #### General Changes

    - The underlying API has been changed from the DOM based `javax` one to JDom.

  - #### Functions

    - Functions that previously returned the relevant element, i.e; `comment(...)` returning `Comment`, `text(...)` returning `Element`, etc, have been changed to return the appropriate itself, i.e;

      - Functions in `XmlDocumentContainer` will now return the `XmlDocumentContainer` instance.
      - Functions in `XmlElementContainer` will now return the `XmlElementContainer` instance.

      This change is to allow method chaining to work properly from the Java side, so that this library can also be utilized in Java, and not only from Kotlin. This also means that all the `invoke operator`s have been marked as `@JvmSynthetic` as to avoid useless clutter on the Java side.

- ### XmlParser.kt

  - #### General Changes

    - The underlying API has been changed from the DOM based `javax` one to JDom.

  - #### Functions

    - Previously, all the functions inside of `ParserDocument` & `ParserElement` returned `Unit` previously, that has been changed and all the functions in `ParserDocument` & `ParserElement` now return itself. This is to enable method chaining from the Java side, so that this library can also be used from the Java side. 
    - Some of the functions that used some "wonky" practices have been improved.

## 1.0.0 (2019-01-23)

Initial release.