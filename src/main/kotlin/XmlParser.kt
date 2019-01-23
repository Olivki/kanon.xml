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

@file:JvmName("XmlParser")
@file:Suppress("MemberVisibilityCanBePrivate")

package moe.kanon.xml

import moe.kanon.kextensions.dom.*
import moe.kanon.kextensions.io.newInputStream
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.io.StringWriter
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


/**
 * A container used for more easily traversing a [Document].
 *
 * @property source The [Document] where everything is taken from.
 */
@XmlMarker
public data class ParserDocument(public val source: Document) {
    
    /**
     * The root element of this document.
     */
    public val root: Element = source.documentElement // source[0] as Element
    
    @PublishedApi internal val sequence: Sequence<Node> = root.childNodes.asSequence()
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an element that has a
     * [nodeName][Element.getNodeName] that matches with the [tagName] parameter, if one is found the [container]
     * closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: ParserElement.() -> Unit) {
        if (tagName in root && root[tagName] is Element) {
            ParserElement(this, root[root.childNodes.indexOf(tagName)] as Element, root).apply(container)
        }
    }
    
    /**
     * Calls the [container] closure on **every** occurrence of a child node inside of the [root] that is an [Element].
     */
    @XmlMarker
    public inline fun elements(container: ParserElement.() -> Unit) {
        sequence.filter { it is Element }.forEach {
            ParserElement(this, it as Element, root).apply(container)
        }
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an element that has a [nodeName][Element.getNodeName] that
     * matches with the [tagName] parameter, if any are found the [container] closure is applied to them
     * *(sequentially)*, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun elements(tagName: String, container: ParserElement.() -> Unit) {
        sequence.filter { it is Element && it.nodeName == tagName }.forEach {
            ParserElement(this, it as Element, root).apply(container)
        }
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an element that has a [nodeName][Element.getNodeName] that
     * matches with any of the defined values inside of the [tagNames] parameter, if any are found the [container]
     * closure is applied to them *(sequentially)*
     */
    @XmlMarker
    public inline fun elements(vararg tagNames: String, container: ParserElement.() -> Unit) {
        sequence.filter { it is Element && it.nodeName in tagNames }.forEach {
            ParserElement(this, it as Element, root).apply(container)
        }
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [name] parameter, if one is found the [container]
     * closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attribute(name: String, container: ParserAttribute.() -> Unit) {
        if (name in root.attributes) {
            ParserAttribute(this, root.attributes[name] as Attr, root).apply(container)
        }
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [Pair.first] property, and a [nodeValue][Attr.getNodeValue]
     * that matches with [Pair.second], if one is found the [container] closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun <V : Any> attribute(attribute: Pair<String, V>, container: ParserAttribute.() -> Unit) {
        val (name, value) = attribute
        if (name in root.attributes && root.attributes[name].nodeValue == value.toString()) {
            ParserAttribute(this, root.attributes[name] as Attr, root).apply(container)
        }
    }
    
    /**
     * Loops through **all** of the attributes appended onto the [root] of this document, and calls the [container]
     * closure on all of them.
     */
    @XmlMarker
    public inline fun attributes(container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
    
        source.attributes.forEach {
            tempSet += this as Attr
        }
    
        ParserAttributes(
            this,
            root,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an attribute that has a [nodeName][Element.getNodeName]
     * that matches with any of the defined values inside of the [names] parameter, if any are found the [container]
     * closure is applied to them, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attributes(vararg names: String, container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
    
        source.attributes.asSequence().filter { it.nodeName in names }.forEach {
            tempSet += it as Attr
        }
    
        ParserAttributes(
            this,
            root,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [Pair.first] property, and a [nodeValue][Attr.getNodeValue]
     * that matches with [Pair.second], if any are found the [container] closure is applied to them, otherwise nothing
     * happens.
     */
    @XmlMarker
    public inline fun <V : Any> attributes(vararg attributes: Pair<String, V>, container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
        
        for ((name, value) in attributes) {
            if (name in source.attributes && source.attributes[name].nodeValue == value.toString()) {
                tempSet += source.attributes[name] as Attr
            }
        }
        
        ParserAttributes(
            this,
            root,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
}

/**
 * A container used for more easily traversing an [Element].
 *
 * @property document An instance of the over-arching [ParserDocument].
 * @property source The [Element] instance that this container is wrapping around.
 * @property parent The parent of this element.
 */
@XmlMarker
public data class ParserElement(
    public val document: ParserDocument,
    public val source: Element,
    public val parent: Element
) {
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
    
    @PublishedApi internal val sequence: Sequence<Node> = source.childNodes.asSequence()
    
    /**
     * Looks for exactly **one** occurrence inside of the [source] of an element that has a
     * [nodeName][Element.getNodeName] that matches with the [tagName] parameter, if one is found the [container]
     * closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: ParserElement.() -> Unit) {
        if (tagName in source && source[tagName] is Element) {
            ParserElement(document, source[tagName] as Element, source).apply(container)
        }
    }
    
    /**
     * Calls the [container] closure on **every** occurrence of a child node inside of this element that is an
     * [Element].
     */
    @XmlMarker
    public inline fun elements(container: ParserElement.() -> Unit) {
        sequence.filter { it is Element }.forEach {
            ParserElement(document, it as Element, source).apply(container)
        }
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an element that has a [nodeName][Element.getNodeName]
     * that matches with the [tagName] parameter, if any are found the [container] closure is applied to them
     * *(sequentially)*, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun elements(tagName: String, container: ParserElement.() -> Unit) {
        sequence.filter { it is Element && it.nodeName == tagName }.forEach {
            ParserElement(document, it as Element, source).apply(container)
        }
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an element that has a [nodeName][Element.getNodeName]
     * that matches with any of the defined values inside of the [tagNames] parameter, if any are found the [container]
     * closure is applied to them *(sequentially)*
     */
    @XmlMarker
    public inline fun elements(vararg tagNames: String, container: ParserElement.() -> Unit) {
        sequence.filter { it is Element && it.nodeName in tagNames }.forEach {
            ParserElement(document, it as Element, source).apply(container)
        }
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [source] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [name] parameter, if one is found the [container]
     * closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attribute(name: String, container: ParserAttribute.() -> Unit) {
        if (name in source.attributes) {
            ParserAttribute(document, source.attributes[name] as Attr, source).apply(container)
        }
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [source] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [Pair.first] property, and a [nodeValue][Attr.getNodeValue]
     * that matches with [Pair.second], if one is found the [container] closure is applied to it, otherwise nothing
     * happens.
     */
    @XmlMarker
    public inline fun <V : Any> attribute(attribute: Pair<String, V>, container: ParserAttribute.() -> Unit) {
        val (name, value) = attribute
        if (name in source.attributes && source.attributes[name].nodeValue == value.toString()) {
            ParserAttribute(document, source.attributes[name] as Attr, source).apply(container)
        }
    }
    
    /**
     * Loops through **all** of the attributes appended onto the [root] of this document, and calls the [container]
     * closure on all of them.
     */
    @XmlMarker
    public inline fun attributes(container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
        
        source.attributes.forEach {
            tempSet += this as Attr
        }
        
        ParserAttributes(
            document,
            source,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an attribute that has a [nodeName][Element.getNodeName]
     * that matches with any of the defined values inside of the [names] parameter, if any are found the [container]
     * closure is applied to them, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attributes(vararg names: String, container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
        
        source.attributes.asSequence().filter { it.nodeName in names }.forEach {
            tempSet += it as Attr
        }
        
        ParserAttributes(
            document,
            source,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an attribute that has a
     * [nodeName][Attr.getNodeName] that matches with the [Pair.first] property, and a [nodeValue][Attr.getNodeValue]
     * that matches with [Pair.second], if any are found the [container] closure is applied to them, otherwise nothing
     * happens.
     */
    @XmlMarker
    public inline fun <V : Any> attributes(vararg attributes: Pair<String, V>, container: ParserAttributes.() -> Unit) {
        val tempSet = HashSet<Attr>()
        
        for ((name, value) in attributes) {
            if (name in source.attributes && source.attributes[name].nodeValue == value.toString()) {
                tempSet += source.attributes[name] as Attr
            }
        }
        
        ParserAttributes(
            document,
            source,
            tempSet.associateBy({ it.nodeName }, { it.nodeValue }),
            tempSet.associateBy({ it.nodeName }, { it })
        ).apply(container)
    }
    
    public override fun toString(): String {
        val source = DOMSource(source)
        
        val writer = StringWriter()
        transformer.transform(source, StreamResult(writer))
        
        return writer.buffer.toString()
    }
}

/**
 * An empty container for working on [Attr] instances retrieved from either [ParserDocument] or [ParserElement].
 *
 * @property document An instance of the over-arching [ParserDocument].
 * @property source The [Attr] instance that this container is wrapping around.
 * @property parent The parent of this element.
 */
@XmlMarker
public data class ParserAttribute(
    public val document: ParserDocument,
    public val source: Attr,
    public val parent: Element
)

/**
 * A container for when working with multiple attributes, comes with two different HashMaps for easy access.
 *
 * @property document An instance of the over-arching [ParserDocument].
 * @property parent The parent of this element.
 * @property attributes Contains all the found attributes broken down into `nodeName:nodeValue`.
 * @property _attributes Contains all the found attributes stored as `nodeName:attribute`.
 *
 * **Note:** If you do **not** need anything specific from the [Attr] instance, it's recommended to use the
 * [attributes] over this one.
 */
@XmlMarker
public data class ParserAttributes(
    public val document: ParserDocument,
    public val parent: Element,
    public val attributes: Map<String, String>,
    public val _attributes: Map<String, Attr>
)

/**
 * Wraps a [ParserDocument] over this document and returns the container.
 */
public inline fun Document.parse(container: ParserDocument.() -> Unit): ParserDocument =
    ParserDocument(this).apply(container)

/**
 * Attempts to parse this file into a [Document], and then wrap a [ParserDocument] around the document and then
 * return the container.
 */
public inline fun Path.parseAsDocument(container: ParserDocument.() -> Unit): ParserDocument {
    val inputStream = this.newInputStream()
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.isIgnoringComments = true
    factory.isIgnoringElementContentWhitespace = true
    return ParserDocument(factory.newDocumentBuilder().parse(inputStream)).apply(container)
}

/**
 * Attempts to parse this input stream into a [Document], and then wrap a [ParserDocument] around the document and then
 * return the container.
 */
public inline fun InputStream.parseAsDocument(container: ParserDocument.() -> Unit): ParserDocument {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.isIgnoringComments = true
    factory.isIgnoringElementContentWhitespace = true
    return ParserDocument(factory.newDocumentBuilder().parse(this)).apply(container)
}