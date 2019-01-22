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

import moe.kanon.kextensions.ExperimentalFeature
import moe.kanon.kextensions.dom.*
import moe.kanon.kextensions.io.div
import moe.kanon.kextensions.io.not
import org.w3c.dom.*
import java.io.StringWriter
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * The [DslMarker] for the XML builder.
 */
@DslMarker
public annotation class XmlMarker

/**
 * A container representation for the [Document] class.
 *
 * @param rootName The tagName for the root element.
 */
@XmlMarker
public class XmlDocumentContainer(rootName: String) {
    
    /**
     * The source [Document] of this container.
     */
    public val source: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    
    /**
     * The [XmlElementContainer] representation of the [Element] root of this document.
     */
    public val root: XmlElementContainer = XmlElementContainer(this, rootName, null)
    
    /**
     * The [Transformer] instance this document uses to serialize itself.
     */
    public val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
    
    init {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    }
    
    // Transformer
    /**
     * Creates a new instance of the [XmlTransformerContainer] scope, applying all the properties defined inside of the
     * [properties] closure to the `transformer` of this document.
     */
    @XmlMarker
    public inline fun transformer(properties: XmlTransformerContainer.() -> Unit) =
        XmlTransformerContainer(this).apply(properties)
    
    // Attributes
    /**
     * Creates a new instance of the [XmlAttributesContainer] scope, applying all the attributes defined inside of
     * the [attributes] closure to the [root] element of this document.
     */
    @XmlMarker
    public inline fun attributes(attributes: XmlAttributesContainer.() -> Unit) =
        XmlAttributesContainer(root).apply(attributes)
    
    /**
     * Appends all the entries from the [attributes] `array` to the [root] element of this document.
     *
     * The [Pair] entries inside of `attributes` are applied in such a way that [Pair.first] is assumed to be the `name`
     * of the attribute, and [Pair.second] is assumed to be the `value` of the attribute.
     */
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            root.source.attributeMap[name] = value.toString()
        }
    }
    
    // Might implement this system at a later date.
    /*public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            try {
                root.source.attributeMap[name] = value.toString()
            } catch (e: Exception) {
                when (e) {
                    is XmlBuilderException -> throw e
                    else -> XmlBuilderException("root.$root.name(attribute[name=$name, value=$value])", e)
                }
            }
        }
    }*/
    
    // Element
    /**
     * Creates and appends a new [Element] to the [root] of this document.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.getNodeName] of the `Element` will be set to [this] string.
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @receiver The string receiver of this function is used as the [nodeName][Element.getNodeName] of this `Element`.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    public inline operator fun String.invoke(container: XmlElementContainer.() -> Unit = {}): XmlElementContainer =
        XmlElementContainer(this@XmlDocumentContainer, this).apply(container)
    
    /**
     * Creates and appends a new [Element] to the [root] of this document.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.getNodeName] of the `Element` will be set to the specified [tagName].
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: XmlElementContainer.() -> Unit = {}): XmlElementContainer =
        XmlElementContainer(this@XmlDocumentContainer, tagName).apply(container)
    
    // Text
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to the [root] of this document.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.getNodeName] of the `Element` is set to the specified [tagName].
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
    public inline fun text(tagName: String, data: String.() -> String): Element =
        root.source.appendTextContainer(tagName, String().data())
    
    // Comment
    /**
     * Creates and appends a new [Comment] to the [root] of this document.
     *
     * The newly created `Comment`'s [data][Comment.getData] property is set to the string returned from the [data]
     * closure.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public inline fun comment(data: String.() -> String): Comment = root.source.appendComment(String().data())
    
    /**
     * Creates and appends a new [Comment] to the [root] of this document.
     *
     * The newly created `Comment`'s [data][Comment.getData] property is set to the specified [data] parameter.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public fun comment(data: String): Comment = root.source.appendComment(data)
    
    /**
     * Saves the contents of this document to an XML file using the specified [fileName] to the [directory].
     *
     * @param directory The directory in which the file will be saved.
     * @param fileName The name of the file. *(This is the full file name, including the extensions.)*
     */
    public fun saveTo(directory: Path, fileName: String): Path {
        val file = directory / fileName
        val source = DOMSource(source)
        val result = StreamResult(!file)
        
        transformer.transform(source, result)
        
        return file
    }
    
    public override fun toString(): String {
        val source = DOMSource(source)
        
        val writer = StringWriter()
        transformer.transform(source, StreamResult(writer))
        
        return writer.buffer.toString()
    }
}

/**
 * A container representation of the [Element] class.
 *
 * If no content is specified inside of this, it will be rendered as a self-closing tag.
 *
 * @property document A reference to the overarching [XmlDocumentContainer] that all elements in this scope adhere to.
 * @property name The `tagName` of this element, this also serves as the [Node.nodeName][org.w3c.dom.Node.getNodeName]
 * for the [source] property.
 * @property parent The parent of this element, this is needed to properly append this element and any children defined
 * in here to the correct parent.
 *
 * This property should *only* be null during the creation of the `root` element of the [document], if another element
 * other than the `root` element has a `null` parent, this class will not work as intended.
 *
 * ([XmlDocumentContainer.root] by default)
 */
@XmlMarker
public class XmlElementContainer(
    public val document: XmlDocumentContainer,
    public val name: String,
    public val parent: XmlElementContainer? = document.root
) {
    
    /**
     * The source [Element] of this container.
     *
     * Any children/attributes defined inside of this container will be appended onto this.
     *
     * On creation, this will attempt to append itself onto the [parent] property, however, in the case that [parent]
     * is `null` it will append itself directly onto the [document]. This marks this element as the `root` of the
     * document.
     *
     * Appending directly to the document can ***only*** be done once, if done more than once, exceptions will be
     * thrown.
     */
    public val source: Element = parent?.source?.appendElement(name) ?: document.source.appendElement(name)
    
    /**
     * The [Transformer] instance this element uses to serialize itself.
     */
    public val transformer: Transformer by lazy {
        val trans = TransformerFactory.newInstance().newTransformer()
        
        trans.setOutputProperty(OutputKeys.INDENT, "yes")
        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        
        trans
    }
    
    // Attributes
    /**
     * Creates a new instance of the [XmlAttributesContainer] scope, applying all the attributes defined inside of
     * the [attributes] closure to this element.
     */
    @XmlMarker
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit): XmlAttributesContainer =
        XmlAttributesContainer(this).apply(block)
    
    /**
     * Appends all the entries from the [attributes] `array` to to this element.
     *
     * The [Pair] entries inside of `attributes` are applied in such a way that [Pair.first] is assumed to be the `name`
     * of the attribute, and [Pair.second] is assumed to be the `value` of the attribute.
     */
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            source.attributeMap[name] = value.toString()
        }
    }
    
    // Element
    /**
     * Creates and appends a new [Element] to this element.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.getNodeName] of the `Element` will be set to [this] string.
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @receiver The string receiver of this function is used as the [nodeName][Element.getNodeName] of this `Element`.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    public inline operator fun String.invoke(container: XmlElementContainer.() -> Unit = {}): XmlElementContainer =
        XmlElementContainer(this@XmlElementContainer.document, this, this@XmlElementContainer).apply(container)
    
    /**
     * Creates and appends a new [Element] to this element.
     *
     * The newly created `Element` will have everything defined inside of the [container] closure appended onto it, and
     * the [name][Element.getNodeName] of the `Element` will be set to the specified [tagName].
     *
     * **Note:** If nothing is defined inside of the [container] closure, the container will be rendered as a
     * self-closing tag in the XML output.
     *
     * @return The newly created `Element`.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: XmlElementContainer.() -> Unit = {}): XmlElementContainer =
        XmlElementContainer(document, tagName, this).apply(container)
    
    // Text
    /**
     * Creates and appends a [TextNode][Text] to this element.
     *
     * The `TextNode`'s [data][Text.getData] property is set to the string returned from the [data] closure.
     *
     * @return The newly created `TextNode`.
     */
    @XmlMarker
    public inline fun text(data: String.() -> String): Text = source.appendTextNode(String().data())
    
    /**
     * Creates and appends a new [Element] containing a [TextNode][Text] child to this element.
     *
     * The newly created `Element` essentially just acts as a container for the `TextNode`, the
     * [name][Element.getNodeName] of the `Element` is set to the specified [tagName].
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
    public inline fun text(tagName: String, data: String.() -> String): Element =
        source.appendTextContainer(tagName, String().data())
    
    // Comment
    /**
     * Creates and appends a new [Comment] to this element.
     *
     * The newly created `Comment`'s [data][Comment.getData] property is set to the string returned from the [data]
     * closure.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public inline fun comment(data: String.() -> String): Comment = source.appendComment(String().data())
    
    /**
     * Creates and appends a new [Comment] to this element.
     *
     * The newly created `Comment`'s [data][Comment.getData] property is set to the specified [data] parameter.
     *
     * @param data The contents of the comment.
     *
     * @return The newly created `Comment`.
     */
    @XmlMarker
    public fun comment(data: String): Comment = source.appendComment(data)
    
    public override fun toString(): String {
        val source = DOMSource(source)
        
        val writer = StringWriter()
        transformer.transform(source, StreamResult(writer))
        
        return writer.buffer.toString()
    }
    
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
public class XmlAttributesContainer(public val parent: XmlElementContainer) {
    
    /**
     * Creates and appends a new [Attr] to the [parent] of this container.
     *
     * - The [name][Attr.getName] of the `Attr` is set to the contents of the [String] that invokes this function.
     * - The [value][Attr.getValue] of the `Attr` is set to the contents of the [value] closure, but it's first
     * converted into a string by calling `toString()` on the returned value.
     *
     * @receiver This receiver determines the `name` property of the `Attr`.
     *
     * @return The newly created `Attr`.
     */
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public inline operator fun <V : Any> String.invoke(value: Any.() -> V): Attr {
        parent.source.attributeMap[this] = Any().value().toString()
        return parent.source.attributes[this] as Attr
    }
    
    /**
     * Creates and appends a new [Attr] to the [parent] of this container.
     *
     * - The [name][Attr.getName] of the `Attr` is set to the specified [name] parameter.
     * - The [value][Attr.getValue] of the `Attr` is set to the contents of the [value] closure, but it's first
     * converted into a string by calling `toString()` on the returned value.
     *
     * @return The newly created `Attr`.
     */
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public inline fun <V : Any> attribute(name: String, value: Any.() -> V): Attr {
        parent.source.attributeMap[name] = Any().value().toString()
        return parent.source.attributes[name] as Attr
    }
    
    /**
     * Creates and appends a new [Attr] to the [parent] of this container.
     *
     * - The [name][Attr.getName] of the `Attr` is set to the specified [name] parameter.
     * - The [value][Attr.getValue] of the `Attr` is set to the specified [value] parameter, but it's first converted
     * into a string by calling `toString()` on it.
     *
     * @return The newly created `Attr`.
     */
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attribute(name: String, value: V): Attr {
        parent.source.attributeMap[name] = value.toString()
        return parent.source.attributes[name] as Attr
    }
    
    @UseExperimental(ExperimentalFeature::class)
    public override fun toString(): String =
        "${parent.name}(${parent.source.attributeMap.asSequence().joinToString { "[${it.key}:${it.value}]" }})"
}

/**
 * A container class that enables easy access to setting the output properties of the XML generator.
 *
 * @property document The document for which this container will apply the transformer changes to.
 */
@XmlMarker
public class XmlTransformerContainer(public val document: XmlDocumentContainer) {
    
    /**
     * Clears all the parameters from the [transformer][XmlDocumentContainer.transformer].
     *
     * @see Transformer.clearParameters
     */
    @XmlMarker
    public fun clearParameters() {
        document.transformer.clearParameters()
    }
    
    /**
     * Sets a `parameter` on the [transformer][XmlDocumentContainer.transformer] with the specified [name] to the
     * specified [value].
     *
     * @see Transformer.setParameter
     */
    @XmlMarker
    public inline fun <V : Any> parameter(name: String, value: Any.() -> V) {
        document.transformer.setParameter(name, Any().value())
    }
    
    /**
     * Sets an `output property` on the [transformer][XmlDocumentContainer.transformer] with the specified [name] to
     * the specified return value of the [value] closure.
     *
     * @see OutputKeys
     * @see Transformer.setOutputProperty
     */
    @XmlMarker
    public inline fun property(name: String, value: String.() -> String) {
        document.transformer.setOutputProperty(name, String().value())
    }
    
    /**
     * Sets an `output property` on the [transformer][XmlDocumentContainer.transformer] with the specified [name] to
     * the specified [value].
     *
     * @see OutputKeys
     * @see Transformer.setOutputProperty
     */
    @XmlMarker
    public fun property(name: String, value: String) {
        document.transformer.setOutputProperty(name, value)
    }
    
    public override fun toString(): String = "${document.root}(${document.transformer.outputProperties})"
}

/**
 * Creates a new [XmlDocumentContainer] with the specified [root] parameter as the root element for the document.
 *
 * @return The newly created `XmlDocumentContainer`.
 */
@XmlMarker
public inline fun xml(root: String, body: XmlDocumentContainer.() -> Unit): XmlDocumentContainer =
    XmlDocumentContainer(root).apply(body)
