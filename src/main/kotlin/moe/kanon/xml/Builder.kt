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

@file:Suppress("unused", "FunctionName")

package moe.kanon.xml

import org.jdom2.Attribute
import org.jdom2.AttributeType
import org.jdom2.CDATA
import org.jdom2.Comment
import org.jdom2.Content
import org.jdom2.DocType
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@DslMarker annotation class BuilderMarker

/**
 * A [Document] container used by the XML [builder][xml] DSL.
 *
 * @property [document] The [Document] instance that `this` builder is wrapping around.
 */
@BuilderMarker class DocumentBuilder(val document: Document) {
    /**
     * Returns the [root][Document.getRootElement] element of `this` document.
     *
     * @see [detachedRoot]
     */
    val root: Element get() = document.rootElement

    /**
     * Returns the [detached root][Document.detachRootElement] element of `this` document.
     *
     * The detached root element has no relation to `this` document, and can be freely added to other documents without
     * any issues.
     *
     * @see [root]
     */
    val detachedRoot: Element get() = document.detachRootElement()

    // -- MISC -- \\
    /**
     * Adds the given [content] to the [root] of `this` document.
     */
    @BuilderMarker fun addContent(content: Content) {
        root.addContent(content)
    }

    // -- FORMAT -- \\
    @PublishedApi internal lateinit var outputFormat: Format
    @PublishedApi internal val isFormatSet: Boolean get() = this::outputFormat.isInitialized

    /**
     * The closure to configure the [outputFormat] of `this` document, [startingFormat] determines the starting values.
     *
     * @param [startingFormat] Determines the starting values of the closure.
     *
     * The available formats from the default JDom2 implementation are; [pretty-format][Format.getPrettyFormat],
     * [compact-format][Format.getCompactFormat] and [raw-format][Format.getRawFormat].
     *
     * ([pretty-format][Format.getPrettyFormat] by default.)
     */
    @BuilderMarker inline fun format(
        startingFormat: Format = Format.getPrettyFormat(),
        closure: Format.() -> Unit = {}
    ) {
        outputFormat = startingFormat.apply(closure)
    }

    // -- DOC-TYPE -- \\
    /**
     * Sets the [DocType] of `this` document.
     */
    @BuilderMarker fun docType(elementName: String, publicID: String? = null, systemID: String? = null) {
        document.docType = DocType(elementName, publicID, systemID)
    }

    // -- ATTRIBUTE -- \\
    /**
     * Adds a new attribute to the [root] of `this` document using the given arguments.
     *
     * The value of the attribute is determined by invoking [toString][Any.toString] on the given [value].
     *
     * @return the newly created attribute
     */
    @BuilderMarker inline fun attribute(
        key: String,
        type: AttributeType = AttributeType.UNDECLARED,
        nameSpace: Namespace = Namespace.NO_NAMESPACE,
        value: () -> Any
    ): Attribute = root.setAttribute(Attribute(key, value().toString(), type, nameSpace)).getAttribute(key)

    /**
     * Scopes into a [AttributesBuilder] instance allowing for cleaner addition of attributes.
     */
    @BuilderMarker inline fun attributes(scope: AttributesBuilder.() -> Unit): AttributesBuilder =
        AttributesBuilder(root).apply(scope)

    /**
     * Adds the given [attributes] as attributes on the [root] element of `this` document.
     */
    @BuilderMarker fun attributes(vararg attributes: Pair<String, Any>) {
        for ((key, value) in attributes) root.setAttribute(key, value.toString())
    }

    // -- ELEMENT -- \\
    /**
     * Adds the [Element] created by the given [scope] to the [root] of `this` document.
     */
    @BuilderMarker inline fun element(
        tagName: String,
        namespace: Namespace = root.namespace,
        scope: ElementBuilder.() -> Unit = {}
    ): ElementBuilder = ElementBuilder(root, Element(tagName, namespace)).apply(scope)

    // -- TEXT -- \\
    /**
     * Adds a new [Text] element to the [root] of `this` document, the [text][Text.getText] is determined by invoking
     * [toString][Any.toString] on the given [contents].
     */
    @BuilderMarker inline fun text(contents: () -> Any) {
        addContent(Text(contents().toString()))
    }

    /**
     * Adds a new [Element] created using the given [tagName] and [namespace] containing *only* the result of invoking
     * [toString][Any.toString] on the given [text] function to the root of `this` document.
     */
    @BuilderMarker inline fun textElement(
        tagName: String,
        namespace: Namespace = root.namespace,
        text: () -> Any
    ) {
        addContent(Element(tagName, namespace).setText(text().toString()))
    }

    // -- CDATA -- \\
    /**
     * Adds a new [CDATA] element to the [root] of `this` document, the [text][Text.getText] is determined by invoking
     * [toString][Any.toString] on the given [contents].
     */
    @BuilderMarker inline fun cdata(contents: () -> Any) {
        addContent(CDATA(contents().toString()))
    }

    /**
     * Adds a new [Element] containing only a [CDATA] element to the [root] of `this` document.
     *
     * The [text][Element.getText] of the this element is determined by invoking [toString][Any.toString] on the given
     * [contents].
     */
    @BuilderMarker inline fun cdataElement(
        tagName: String,
        namespace: Namespace = root.namespace,
        contents: () -> Any
    ) {
        addContent(Element(tagName, namespace).setContent(CDATA(contents().toString())))
    }

    // -- COMMENT -- \\
    /**
     * Adds a new [Comment] element to the [root] of `this` document.
     *
     * The [content][Comment.getText] of the comment is determined by invoking [toString][Any.toString] on the given
     * [data].
     */
    @BuilderMarker inline fun comment(data: () -> Any) {
        addContent(Comment(data().toString()))
    }

    // -- SERIALIZATION -- \\
    /**
     * Serializes `this` document into a file stored in the specified [directory], with the specified [fileName], the
     * output format is determined by the [outputFormat] property.
     *
     * **Note:** If the [outputFormat] property has ***not*** been set yet, and this function is being invoked from
     * *within* the [xml] closure, then this function will fail with an exception, if invoked from *outside* of the
     * [xml] closure, then it will not fail, as the [outputFormat] property has been automatically set at that point.
     *
     * @param [directory] the directory in which the file will be saved
     * @param [fileName] the name of the file. *(This is the full file name, including the extension.)*
     */
    fun saveTo(directory: Path, fileName: String): Path {
        val file = directory.resolve(fileName)

        Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
            XMLOutputter(outputFormat).output(document, it)
        }

        return file
    }

    /**
     * Returns `this` document serialized into a [String] according to the specified [format].
     *
     * @param [format] the format to serialize `this` document into
     */
    @BuilderMarker fun toString(format: Format): String = XMLOutputter(format).outputString(document)

    /**
     * Returns `this` document serialized into a [String] according to the [outputFormat] property.
     *
     * **Note:** If the [outputFormat] property has ***not*** been set yet, and this function is being invoked from
     * *within* the [xml] closure, then this function will fail with an exception, if invoked from *outside* of the
     * [xml] closure, then it will not fail, as the [outputFormat] property has been automatically set at that point.
     */
    @BuilderMarker override fun toString(): String = XMLOutputter(outputFormat).outputString(document)
}

/**
 * An [Element] container used by the XML [builder][xml] DSL.
 *
 * @property [parent] The [parent][Element.getParentElement] of the [source] element that `this` builder is wrapping
 * around.
 *
 * Note that if the `source` element does *not* have a `parent`, then this property will return the same instance as
 * `source`.
 * @property [source] The [Element] instance that `this` builder is wrapping around.
 */
@BuilderMarker class ElementBuilder(val parent: Element, val source: Element = parent) {
    /**
     * Returns [source] but with any references to its [parent] removed.
     */
    val detachedSource: Element get() = source.detach()

    init {
        if (source != parent) parent.addContent(source)
    }

    // -- MISC -- \\
    /**
     * Adds the given [content] to `this` element.
     */
    @BuilderMarker fun addContent(content: Content) {
        source.addContent(content)
    }

    // -- ATTRIBUTE -- \\
    /**
     * Adds a new attribute to `this` element.
     *
     * The value of the attribute is determined by invoking [toString][Any.toString] on the given [value].
     *
     * @return the newly created attribute
     */
    @BuilderMarker inline fun attribute(
        key: String,
        type: AttributeType = AttributeType.UNDECLARED,
        nameSpace: Namespace = Namespace.NO_NAMESPACE,
        value: () -> Any
    ): Attribute = source.setAttribute(Attribute(key, value().toString(), type, nameSpace)).getAttribute(key)

    /**
     * Scopes into a [AttributesBuilder] instance allowing for cleaner addition of attributes.
     */
    @BuilderMarker inline fun attributes(scope: AttributesBuilder.() -> Unit): AttributesBuilder =
        AttributesBuilder(source).apply(scope)

    /**
     * Adds the given [attributes] as attributes on `this` element.
     */
    @BuilderMarker fun attributes(vararg attributes: Pair<String, Any>) {
        for ((key, value) in attributes) source.setAttribute(key, value.toString())
    }

    // -- ELEMENT -- \\
    /**
     * Adds the [Element] created by the given [scope] to `this` element.
     */
    @BuilderMarker inline fun element(
        tagName: String,
        namespace: Namespace = parent.namespace,
        scope: ElementBuilder.() -> Unit = {}
    ): ElementBuilder = ElementBuilder(source, Element(tagName, namespace)).apply(scope)

    // -- TEXT -- \\
    /**
     * Adds a new [Text] element to `this` element, the [text][Text.getText] is determined by invoking
     * [toString][Any.toString] on the given [contents].
     */
    @BuilderMarker inline fun text(contents: () -> Any) {
        addContent(Text(contents().toString()))
    }

    /**
     * Adds a new [Element] created using the given [tagName] and [namespace] containing *only* the result of invoking
     * [toString][Any.toString] on the given [text] function to `this` element.
     */
    @BuilderMarker inline fun textElement(
        tagName: String,
        namespace: Namespace = parent.namespace,
        text: () -> Any
    ) {
        source.addContent(Element(tagName, namespace).setText(text().toString()))
    }

    // -- CDATA -- \\
    /**
     * Adds a new [CDATA] element to `this` element., the [text][Text.getText] is determined by invoking
     * [toString][Any.toString] on the given [contents].
     */
    @BuilderMarker inline fun cdata(contents: () -> Any) {
        addContent(CDATA(contents().toString()))
    }

    /**
     * Adds a new [Element] containing only a [CDATA] element to `this` element..
     *
     * The [text][Element.getText] of the this element is determined by invoking [toString][Any.toString] on the given
     * [contents].
     */
    @BuilderMarker inline fun cdataElement(
        tagName: String,
        namespace: Namespace = parent.namespace,
        contents: () -> Any
    ) {
        addContent(Element(tagName, namespace).setContent(CDATA(contents().toString())))
    }

    // -- COMMENT -- \\
    /**
     * Adds a new [Comment] element to `this` element..
     *
     * The [content][Comment.getText] of the comment is determined by invoking [toString][Any.toString] on the given
     * [data].
     */
    @BuilderMarker inline fun comment(data: () -> Any) {
        addContent(Comment(data().toString()))
    }

    /**
     * Returns `this` element serialized into a `String` according to the specified [format].
     *
     * @param [format] the format to serialize `this` element into
     *
     * @see [XMLOutputter.outputString]
     */
    @BuilderMarker fun toString(format: Format): String = XMLOutputter(format).outputString(source)

    /**
     * Returns `this` element serialized into a `String` using the [pretty-format][Format.getPrettyFormat].
     *
     * @see [XMLOutputter.outputString]
     */
    @BuilderMarker override fun toString(): String =
        XMLOutputter(Format.getPrettyFormat()).outputString(source)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is ElementBuilder -> false
        parent != other.parent -> false
        source != other.source -> false
        else -> true
    }

    override fun hashCode(): Int {
        var result = parent.hashCode()
        result = 31 * result + source.hashCode()
        return result
    }
}

/**
 * An [Attribute] container used by the XML [builder][xml] DSL.
 *
 * @property [source] The [Element] instance that the attributes defined inside of `this` container will be added to.
 */
@BuilderMarker class AttributesBuilder(val source: Element) {
    /**
     * Adds a new [Attribute] to the underlying [source] element.
     *
     * The [Attribute] is created from the given parameters, and the [value][Attribute.value] of the attribute is
     * determined by invoking [toString][Any.toString] on the result of the given [value] function.
     */
    @BuilderMarker inline fun attribute(
        key: String,
        type: AttributeType = AttributeType.UNDECLARED,
        namespace: Namespace = Namespace.NO_NAMESPACE,
        value: () -> Any
    ): Attribute = source.setAttribute(Attribute(key, value().toString(), type, namespace)).getAttribute(key)

    /**
     * Adds a new [Attribute] to the underlying [source] element.
     *
     * The [name][Attribute.name] of the attribute is `this` string, and the [value][Attribute.value] of the attribute
     * is determined by invoking [toString][Any.toString] on the result of the given [value] function.
     */
    @BuilderMarker inline operator fun String.invoke(value: () -> Any): Attribute =
        source.setAttribute(this, value().toString()).getAttribute(this)

    @BuilderMarker override fun toString(): String =
        "${source.name}(${source.attributes.joinToString { "[${it.name}:${it.value}]" }})"

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is AttributesBuilder -> false
        source != other.source -> false
        else -> true
    }

    override fun hashCode(): Int = source.hashCode()
}

/**
 * Returns a new [Namespace] based on the given [uri].
 *
 * @see [Namespace.getNamespace]
 */
fun Namespace(uri: String): Namespace = Namespace.getNamespace(uri)

/**
 * Returns a new [Namespace] based on the given [prefix] and [uri].
 *
 * @see [Namespace.getNamespace]
 */
fun Namespace(prefix: String, uri: String): Namespace = Namespace.getNamespace(prefix, uri)

/**
 * Returns the result of applying the given [scope] to a newly created [DocumentBuilder].
 *
 * The [rootName] and [namespace] parameters are used to create a new [Element] instance that is used as the
 * [root][Document.getRootElement] for the underlying [document][DocumentBuilder.document] instance of the newly
 * created `DocumentBuilder`.
 */
@BuilderMarker inline fun xml(
    rootName: String,
    namespace: Namespace = Namespace.NO_NAMESPACE,
    scope: DocumentBuilder.() -> Unit
): DocumentBuilder = DocumentBuilder(Document(Element(rootName, namespace))).apply(scope).also {
    if (!it.isFormatSet) it.outputFormat = Format.getPrettyFormat()
}

/**
 * Returns the result of wrapping `this` [document][Document] in a [DocumentBuilder] and applying the given [scope]
 * function to it.
 */
@BuilderMarker inline fun Document.mutate(scope: DocumentBuilder.() -> Unit): Document =
    DocumentBuilder(this).apply(scope).document

/**
 * Returns the result of applying [scope] to a [ElementBuilder].
 */
@BuilderMarker inline fun buildElement(
    tagName: String,
    namespace: Namespace = Namespace.NO_NAMESPACE,
    scope: ElementBuilder.() -> Unit
): ElementBuilder = ElementBuilder(Element(tagName, namespace)).apply(scope)