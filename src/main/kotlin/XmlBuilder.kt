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

@file:Suppress("MemberVisibilityCanBePrivate")
@file:JvmName("XmlBuilder")

package moe.kanon.xml

import org.jdom2.Attribute
import org.jdom2.Comment
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


/**
 * The [DslMarker] for the XML builder.
 */
@DslMarker
public annotation class XmlMarker

/**
 * A container representation for the [Document] class.
 *
 * @param [document] The underlying document that handles all the operations.
 */
@XmlMarker
public class XmlDocumentContainer(public val document: Document) {
    /**
     * The root [Element] of the underlying [document].
     */
    public val root: Element = document.rootElement
    
    /**
     * The [XmlElementContainer] representation of the [Element] root of this document.
     */
    public val rootContainer: XmlElementContainer = XmlElementContainer(this, root.name, null, true)
    
    /**
     * The underlying [Format] that's used when serializing this document using either [toString] or [saveTo].
     *
     * The property is set using the [format] closure, if it is never set during it's life-time, it will automatically
     * be set to [pretty-print][Format.getPrettyFormat] format.
     */
    @PublishedApi
    @set:JvmSynthetic
    @get:JvmSynthetic
    @field:JvmSynthetic
    internal lateinit var outputFormat: Format
    
    @PublishedApi
    @JvmSynthetic
    internal fun hasFormatBeenSet(): Boolean = ::outputFormat.isInitialized
    
    // Format
    /**
     * The closure to configure the [outputFormat] of this document, [startingFormat] determines the starting values.
     *
     * @param [startingFormat] Determines the starting values of the closure.
     *
     * The available formats from the default JDom2 implementation are; [pretty-format][Format.getPrettyFormat],
     * [compact-format][Format.getCompactFormat] and [raw-format][Format.getRawFormat].
     *
     * ([pretty-format][Format.getPrettyFormat] by default.)
     */
    @XmlMarker
    @JvmOverloads
    public inline fun format(
        startingFormat: Format = Format.getPrettyFormat(),
        closure: Format.() -> Unit = {}
    ): XmlDocumentContainer {
        outputFormat = startingFormat.apply(closure)
        return this
    }
    
    // Attributes
    /**
     * Creates a new instance of the [XmlAttributesContainer] scope, applying all the attributes defined inside of
     * the [attributes] closure to the [rootContainer] element of `this` document.
     */
    @XmlMarker
    public inline fun attributes(attributes: XmlAttributesContainer.() -> Unit): XmlDocumentContainer =
        XmlAttributesContainer(rootContainer, this).apply(attributes).document
    
    /**
     * Appends all the entries from the [attributes] `array` to the [rootContainer] element of `this` document.
     *
     * The [Pair] entries inside of `attributes` are applied in such a way that [Pair.first] is assumed to be the `name`
     * of the attribute, and [Pair.second] is assumed to be the `value` of the attribute.
     */
    @XmlMarker
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>): XmlDocumentContainer {
        for ((name, value) in attributes) root.setAttribute(name, value.toString())
        
        return this
    }
    
    // Element
    /**
     * Creates and appends a new [Element] to the [rootContainer] of `this` document.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.name] of the `Element` will be set to [this] string.
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @receiver the string receiver of this function is used as the [nodeName][Element.getNodeName] of this `Element`.
     */
    @XmlMarker
    @JvmSynthetic
    public inline operator fun String.invoke(container: XmlElementContainer.() -> Unit = {}): XmlElementContainer =
        XmlElementContainer(this@XmlDocumentContainer, this).apply(container)
    
    /**
     * Creates and appends a new [Element] to the [rootContainer] of `this` document.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.name] of the `Element` will be set to the specified [tagName].
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: XmlElementContainer.() -> Unit = {}): XmlDocumentContainer {
        XmlElementContainer(this@XmlDocumentContainer, tagName).apply(container)
        return this
    }
    
    // Text
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to the [rootContainer] of `this` document.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.name] of the `Element` is set to the specified [tagName].
     *
     * The contents of the [Text] is taken from the string returned from the [text] closure.
     *
     * @param [text] The contents of the text node.
     */
    @XmlMarker
    public fun text(tagName: String, text: String.() -> Any): XmlDocumentContainer {
        root.addContent(Element(tagName).setText(String().text().toString()))
        return this
    }
    
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to the [rootContainer] of `this` document.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.name] of the `Element` is set to the specified [tagName].
     *
     * The contents of the `TextNode` is derived from the given [data].
     *
     * Unlike the attributes functions, the `data` parameter *only* accepts a [String], if you want to put anything
     * that's not a string in here, you'll need to manually convert it to a string.
     *
     * @param [data] The contents of the text node.
     */
    @XmlMarker
    public fun text(tagName: String, data: String): XmlDocumentContainer {
        root.addContent(Element(tagName).setText(data))
        return this
    }
    
    /**
     * Appends all the entries from the [texts] `array` to the [rootContainer] element of `this` document.
     *
     * The [Pair] entries inside of `texts` are applied in such a way that [Pair.first] is assumed to be the `tagName`
     * of the [Element] container, and [Pair.second] is assumed to be the `data` of the [TextNode][Text].
     */
    @XmlMarker
    public fun texts(vararg texts: Pair<String, String>): XmlDocumentContainer {
        for ((tagName, data) in texts) root.addContent(Element(tagName).setText(data))
        return this
    }
    
    // Comment
    /**
     * Creates and appends a new [Comment] to the [rootContainer] of `this` document.
     *
     * The newly created `Comment`'s [data][Comment.text] property is set to the string returned from the [data]
     * closure.
     *
     * @param [data] The contents of the comment.
     */
    @XmlMarker
    public inline fun comment(data: String.() -> String): XmlDocumentContainer {
        root.addContent(Comment(String().data()))
        return this
    }
    
    /**
     * Creates and appends a new [Comment] to the [rootContainer] of `this` document.
     *
     * The newly created `Comment`'s [data][Comment.text] property is set to the specified [data] parameter.
     *
     * @param [data] The contents of the comment.
     */
    @XmlMarker
    public fun comment(data: String): XmlDocumentContainer {
        root.addContent(Comment(data))
        return this
    }
    
    /**
     * Serializes `this` document into a file stored in the specified [directory], with the specified [fileName], the
     * output format is determined by the [outputFormat] property.
     *
     * **Note:** If the [outputFormat] property has ***not*** been set yet, and this function is being invoked from
     * *within* the [xml] closure, then this function will fail with an exception, if invoked from *outside* of the
     * [xml] closure, then it will not fail, as the [outputFormat] property has been automatically set at that point.
     *
     * @param [directory] The directory in which the file will be saved.
     * @param [fileName] The name of the file. *(This is the full file name, including the extension.)*
     */
    public fun saveTo(directory: Path, fileName: String): Path {
        val file = directory.resolve(fileName)
        val output = XMLOutputter(outputFormat)
        
        output.output(document, Files.newOutputStream(file, StandardOpenOption.CREATE))
        
        return file
    }
    
    /**
     * Returns `this` document serialized into a [String] according to the specified [format].
     *
     * @param [format] The format to serialize `this` document into.
     */
    fun toString(format: Format): String = XMLOutputter(format).outputString(document)
    
    /**
     * Returns `this` document serialized into a [String] according to the [outputFormat] property.
     *
     * **Note:** If the [outputFormat] property has ***not*** been set yet, and this function is being invoked from
     * *within* the [xml] closure, then this function will fail with an exception, if invoked from *outside* of the
     * [xml] closure, then it will not fail, as the [outputFormat] property has been automatically set at that point.
     */
    public override fun toString(): String = XMLOutputter(outputFormat).outputString(document)
}

/**
 * A container representation of the [Element] class.
 *
 * If no content is specified inside of this, it will be rendered as a self-closing tag.
 *
 * @property [documentContainer] A reference to the overarching [XmlDocumentContainer] that all elements in this scope adhere to.
 * @property [name] The `tagName` of this element, this also serves as the [Element.name] for the [source] property.
 * @property [parent] The parent of this element, this is needed to properly append this element and any children defined
 * in here to the correct parent.
 *
 * This property should *only* be null during the creation of the `root` element of the [documentContainer], if another element
 * other than the `root` element has a `null` parent, this class will not work as intended.
 *
 * ([XmlDocumentContainer.rootContainer] by default)
 */
@XmlMarker
public class XmlElementContainer(
    public val documentContainer: XmlDocumentContainer,
    public val name: String,
    public val parent: XmlElementContainer? = documentContainer.rootContainer,
    public val isRootContainer: Boolean = false
) {
    /**
     * The source [Element] of `this` container.
     *
     * Any children/attributes defined inside of this container will be appended onto this.
     *
     * On creation, this will attempt to append itself onto the [parent] property, however, in the case that [parent]
     * is `null` it will append itself directly onto the [documentContainer]. This marks this element as the root of
     * the document.
     *
     * Appending directly to the document can ***only*** be done once, if done more than once, exceptions will be
     * thrown.
     */
    public val source: Element = when (isRootContainer) {
        true -> documentContainer.root
        else -> {
            val element = Element(name)
            parent!!.source.addContent(element)
            element
        }
    }
    
    // Attributes
    /**
     * Creates a new instance of the [XmlAttributesContainer] scope, applying all the attributes defined inside of
     * the [attributes] closure to `this` element.
     */
    @XmlMarker
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit): XmlElementContainer =
        XmlAttributesContainer(this, documentContainer).apply(block).parent
    
    /**
     * Appends all the entries from the [attributes] `array` to to `this` element.
     *
     * The [Pair] entries inside of `attributes` are applied in such a way that [Pair.first] is assumed to be the `name`
     * of the attribute, and [Pair.second] is assumed to be the `value` of the attribute.
     */
    @XmlMarker
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>): XmlElementContainer {
        for ((name, value) in attributes) source.setAttribute(name, value.toString())
        return this
    }
    
    // Element
    /**
     * Creates and appends a new [Element] to `this` element.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.name] of the `Element` will be set to [this] string.
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @receiver The string receiver of this function is used as the [nodeName][Element.name] of this `Element`.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    @JvmSynthetic
    public inline operator fun String.invoke(container: XmlElementContainer.() -> Unit = {}): XmlElementContainer {
        XmlElementContainer(this@XmlElementContainer.documentContainer, this, this@XmlElementContainer).apply(container)
        return this@XmlElementContainer
    }
    
    /**
     * Creates and appends a new [Element] to `this` element.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.name] of the `Element` will be set to the specified [tagName].
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: XmlElementContainer.() -> Unit = {}): XmlElementContainer {
        XmlElementContainer(documentContainer, tagName, this).apply(container)
        return this
    }
    
    // Text
    /**
     * Creates and appends a [TextNode][Text] to `this` element.
     *
     * The `TextNode`'s [data][Text.getText] property is set to the string returned from the [data] closure.
     *
     * @return The newly created `TextNode`.
     */
    @XmlMarker
    public inline fun text(data: String.() -> String): XmlElementContainer {
        source.text = String().data()
        return this
    }
    
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to `this` element.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.name] of the `Element` is set to the specified [tagName].
     *
     * The contents of the `TextNode` is taken from the string returned from the [data] closure.
     *
     * Unlike the attributes functions, the [data] closure *only* accepts a [String], if you want to put anything
     * that's not a string in here, you'll need to manually convert it to a string. It's also *not* allowed to leave
     * this closure empty.
     *
     * @param data The contents of the text node.
     *
     * @return The `Element` container surrounding the `TextNode`.
     */
    @XmlMarker
    public inline fun text(tagName: String, data: String.() -> String): XmlElementContainer {
        source.addContent(Element(tagName).setText(String().data()))
        return this
    }
    
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to `this` document.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.name] of the `Element` is set to the specified [tagName].
     *
     * The contents of the `TextNode` is derived from the given [data].
     *
     * Unlike the attributes functions, the `data` parameter *only* accepts a [String], if you want to put anything
     * that's not a string in here, you'll need to manually convert it to a string.
     *
     * @param data The contents of the text node.
     *
     * @return The `Element` container surrounding the `TextNode`.
     */
    @XmlMarker
    public fun text(tagName: String, data: String): XmlElementContainer {
        source.addContent(Element(tagName).setText(data))
        return this
    }
    
    /**
     * Appends all the entries from the [texts] `array` to `this` element.
     *
     * The [Pair] entries inside of `texts` are applied in such a way that [Pair.first] is assumed to be the `tagName`
     * of the [Element] container, and [Pair.second] is assumed to be the `data` of the [TextNode][Text].
     */
    @XmlMarker
    public fun texts(vararg texts: Pair<String, String>): XmlElementContainer {
        for ((tagName, data) in texts) source.addContent(Element(tagName).setText(data))
        return this
    }
    
    // Comment
    /**
     * Creates and appends a new [Comment] to `this` element.
     *
     * The newly created `Comment`'s [data][Comment.text] property is set to the string returned from the [data]
     * closure.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public inline fun comment(data: String.() -> String): XmlElementContainer {
        source.addContent(Comment(String().data()))
        return this
    }
    
    /**
     * Creates and appends a new [Comment] to `this` element.
     *
     * The newly created `Comment`'s [data][Comment.text] property is set to the specified [data] parameter.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public fun comment(data: String): XmlElementContainer {
        source.addContent(Comment(data))
        return this
    }
    
    /**
     * Returns `this` element serialized into a [String] according to the specified [format].
     *
     * @param [format] The format to serialize `this` document into.
     */
    fun toString(format: Format): String = XMLOutputter(format).outputString(source)
    
    /**
     * Returns `this` element serialized into a [String] according to the
     * [outputFormat][XmlDocumentContainer.outputFormat] property of the parent [documentContainer].
     *
     * **Note:** If the [outputFormat][XmlDocumentContainer.outputFormat] property has ***not*** been set yet, and this
     * function is being invoked from *within* the [xml] closure, then this function will fail with an exception, if
     * invoked from *outside* of the [xml] closure, then it will not fail, as the
     * [outputFormat][XmlDocumentContainer.outputFormat] property has been automatically set at that point.
     */
    override fun toString(): String = XMLOutputter(documentContainer.outputFormat).outputString(source)
}

/**
 * A container that acts as a scope for attribute related actions.
 *
 * @property parent The parent [container][XmlElementContainer] of this attribute container.
 *
 * This element is the one that all attributes defined inside of this container will be appended to.
 *
 * **Note:** Attributes are *not* appended onto the parent in the order that they are declared, but rather in
 * alphabetical order, per the XML Specifications.
 */
@XmlMarker
public class XmlAttributesContainer(public val parent: XmlElementContainer, public val document: XmlDocumentContainer) {
    
    /**
     * Creates and appends a new [Attribute] to the [parent] of `this` container.
     *
     * - The [name][Attribute.getName] of the `Attribute` is set to the contents of the [String] that invokes this
     * function.
     * - The [value][Attribute.getValue] of the `Attribute` is set to the contents of the [value] closure, but it's
     * first converted into a string by calling `toString()` on the returned value.
     *
     * @receiver this receiver determines the `name` property of the `Attribute`.
     *
     * @return the newly created `Attr`.
     */
    @XmlMarker
    @JvmSynthetic
    public inline operator fun <V : Any> String.invoke(value: Any.() -> V): Attribute {
        parent.source.setAttribute(this, Any().value().toString())
        return parent.source.getAttribute(this)
    }
    
    /**
     * Creates and appends a new [Attribute] to the [parent] of `this` container.
     *
     * - The [name][Attribute.getName] of the `Attribute` is set to the specified [name] parameter.
     * - The [value][Attribute.getValue] of the `Attribute` is set to the contents of the [value] closure, but it's
     * first converted into a string by calling `toString()` on the returned value.
     *
     * @return the newly created `Attr`.
     */
    @XmlMarker
    public inline fun <V : Any> attribute(name: String, value: Any.() -> V): Attribute {
        parent.source.setAttribute(name, Any().value().toString())
        return parent.source.getAttribute(name)
    }
    
    /**
     * Creates and appends a new [Attribute] to the [parent] of `this` container.
     *
     * - The [name][Attribute.getName] of the `Attribute` is set to the specified [name] parameter.
     * - The [value][Attribute.getValue] of the `Attribute` is set to the specified [value] parameter, but it's first
     * converted into a string by calling `toString()` on it.
     *
     * @return the newly created `Attr`.
     */
    @XmlMarker
    public fun <V : Any> attribute(name: String, value: V): Attribute {
        parent.source.setAttribute(name, value.toString())
        return parent.source.getAttribute(name)
    }
    
    public override fun toString(): String =
        "${parent.name}(${parent.source.attributes.joinToString { "[${it.name}:${it.value}]" }})"
}

/**
 * Creates a new [XmlDocumentContainer] with the specified [root] parameter as the root element for the document.
 *
 * @return the newly created [XmlDocumentContainer].
 */
@XmlMarker
public inline fun xml(root: String, body: XmlDocumentContainer.() -> Unit): XmlDocumentContainer {
    val container = XmlDocumentContainer(Document(Element(root))).apply(body)
    
    if (!container.hasFormatBeenSet()) container.outputFormat = Format.getPrettyFormat()
    
    return container
}

/**
 * Creates a new [XmlDocumentContainer] with the specified [root] parameter as the root element for the document.
 *
 * @return the newly created [XmlDocumentContainer].
 */
@XmlMarker
public inline fun xml(root: Element, body: XmlDocumentContainer.() -> Unit): XmlDocumentContainer {
    val container = XmlDocumentContainer(Document(root)).apply(body)
    
    if (!container.hasFormatBeenSet()) container.outputFormat = Format.getPrettyFormat()
    
    return container
}

/**
 * Creates a new [XmlDocumentContainer] using the specified [document] as the underlying document.
 *
 * @return the newly created [XmlDocumentContainer].
 */
@XmlMarker
public inline fun xml(document: Document, body: XmlDocumentContainer.() -> Unit): XmlDocumentContainer {
    val container = XmlDocumentContainer(document).apply(body)
    
    if (!container.hasFormatBeenSet()) container.outputFormat = Format.getPrettyFormat()
    
    return container
}

/**
 * Creates a new [XmlDocumentContainer] using `this` [Document] as the underlying document.
 */
@XmlMarker
@JvmSynthetic
public inline fun Document.expandWithDsl(body: XmlDocumentContainer.() -> Unit): Document {
    XmlDocumentContainer(this).apply(body)
    return this
}
