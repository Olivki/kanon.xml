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

import org.jdom2.Attribute
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jdom2.xpath.XPathExpression
import org.jdom2.xpath.XPathFactory
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path


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
    public val root: Element = source.rootElement // source[0] as Element
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an element that has a
     * [nodeName][Element.name] that matches with the [tagName] parameter, if one is found the [container]
     * closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: ParserElement.() -> Unit): ParserDocument {
        if (root.getChild(tagName) != null) {
            ParserElement(this, root.getChild(tagName), root).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes the *first* result of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun element(
        expression: XPathExpression<Element>,
        container: ParserElement.() -> Unit
    ): ParserDocument {
        val first = expression.evaluate(root).firstOrNull()
        if (first != null) {
            ParserElement(this, first, root).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes all the results of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun elements(
        expression: XPathExpression<Element>,
        container: ParserElement.() -> Unit
    ): ParserDocument {
        for (element in expression.evaluate(root)) ParserElement(this, element, root).apply(container)
        
        return this
    }
    
    /**
     * Calls the [container] closure on **every** occurrence of a child node inside of the [root] that is an [Element].
     */
    @XmlMarker
    public inline fun elements(container: ParserElement.() -> Unit): ParserDocument {
        for (element in root.children) {
            ParserElement(this, element, root).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an element that has a [nodeName][Element.name] that
     * matches with the [tagName] parameter, if any are found the [container] closure is applied to them
     * *(sequentially)*, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun elements(tagName: String, container: ParserElement.() -> Unit): ParserDocument {
        for (element in root.children.filter { it.name == tagName }) {
            ParserElement(this, element, root).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an element that has a [nodeName][Element.name] that
     * matches with any of the defined values inside of the [tagNames] parameter, if any are found the [container]
     * closure is applied to them *(sequentially)*
     */
    @XmlMarker
    public inline fun elements(vararg tagNames: String, container: ParserElement.() -> Unit): ParserDocument {
        for (element in root.children.filter { it.name in tagNames }) {
            ParserElement(this, element, root).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an attribute that has a [nodeName][Attribute.name]
     * that matches with the [name] parameter, if one is found the [container] closure is applied to it, otherwise
     * nothing happens.
     */
    @XmlMarker
    public inline fun attribute(name: String, container: ParserAttribute.() -> Unit): ParserDocument {
        if (root.attributes.any { it.name == name }) ParserAttribute(this, root.getAttribute(name), root).apply(
            container
        )
        
        return this
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [root] of an attribute that has a
     * [nodeName][Attribute.name] that matches with the [Pair.first] property, and a [nodeValue][Attribute.name] that
     * matches with [Pair.second], if one is found the [container] closure is applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun <V : Any> attribute(
        attribute: Pair<String, V>,
        container: ParserAttribute.() -> Unit
    ): ParserDocument {
        val (name, value) = attribute
        
        if (root.attributes.any { it.name == name } && root.getAttribute(name).value == value.toString()) {
            ParserAttribute(this, root.getAttribute(name), root).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes the *first* result of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun attribute(
        expression: XPathExpression<Attribute>,
        container: ParserAttribute.() -> Unit
    ): ParserDocument {
        val first = expression.evaluate(root).firstOrNull()
        if (first != null) {
            ParserAttribute(this, first, root).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes all the results of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun attributes(
        expression: XPathExpression<Attribute>,
        container: ParserAttribute.() -> Unit
    ): ParserDocument {
        for (element in expression.evaluate(root)) ParserAttribute(this, element, root).apply(container)
        
        return this
    }
    
    /**
     * Loops through **all** of the attributes appended onto the [root] of this document, and calls the [container]
     * closure on all of them.
     */
    @XmlMarker
    public inline fun attributes(container: ParserAttributes.() -> Unit): ParserDocument {
        ParserAttributes(
            this,
            root,
            root.attributes.associateBy({ it.name }, { it.value }),
            root.attributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an attribute that has a [nodeName][Element.name] that
     * matches with any of the defined values inside of the [names] parameter, if any are found the [container]
     * closure is applied to them, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attributes(vararg names: String, container: ParserAttributes.() -> Unit): ParserDocument {
        val foundAttributes = root.attributes.filter { it.name in names }
        
        ParserAttributes(
            this,
            root,
            foundAttributes.associateBy({ it.name }, { it.value }),
            foundAttributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [root] of an attribute that has a [nodeName][Attribute.name] that
     * matches with the [Pair.first] property, and a [nodeValue][Attribute.name] that matches with [Pair.second], if
     * any are found the [container] closure is applied to them, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun <V : Any> attributes(
        vararg attributes: Pair<String, V>,
        container: ParserAttributes.() -> Unit
    ): ParserDocument {
        val foundAttributes = attributes.filter { (name, value) ->
            root.attributes.any { it.name == name } && root.getAttribute(name).value == value.toString()
        }.map { (name) -> root.getAttribute(name) }
        
        ParserAttributes(
            this,
            root,
            foundAttributes.associateBy({ it.name }, { it.value }),
            foundAttributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
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
     * Looks for exactly **one** occurrence inside of the [source] of an element that has a [nodeName][Element.name]
     * that matches with the [tagName] parameter, if one is found the [container] closure is applied to it, otherwise
     * nothing happens.
     */
    @XmlMarker
    public inline fun element(tagName: String, container: ParserElement.() -> Unit): ParserElement {
        if (source.getChild(tagName) != null) {
            ParserElement(document, source.getChild(tagName), source).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes the *first* result of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun element(
        expression: XPathExpression<Element>,
        container: ParserElement.() -> Unit
    ): ParserElement {
        val first = expression.evaluate(source).firstOrNull()
        if (first != null) {
            ParserElement(document, first, source).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes all the results of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun elements(
        expression: XPathExpression<Element>,
        container: ParserElement.() -> Unit
    ): ParserElement {
        for (element in expression.evaluate(source)) ParserElement(document, element, source).apply(container)
        
        return this
    }
    
    /**
     * Calls the [container] closure on **every** occurrence of a child node inside of this element that is an
     * [Element].
     */
    @XmlMarker
    public inline fun elements(container: ParserElement.() -> Unit): ParserElement {
        for (element in source.children) {
            ParserElement(document, element, source).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an element that has a [nodeName][Element.name] that
     * matches with the [tagName] parameter, if any are found the [container] closure is applied to them
     * *(sequentially)*, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun elements(tagName: String, container: ParserElement.() -> Unit): ParserElement {
        for (element in source.children.filter { it.name == tagName }) {
            ParserElement(document, element, source).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an element that has a [nodeName][Element.name] that
     * matches with any of the defined values inside of the [tagNames] parameter, if any are found the [container]
     * closure is applied to them *(sequentially)*
     */
    @XmlMarker
    public inline fun elements(vararg tagNames: String, container: ParserElement.() -> Unit): ParserElement {
        for (element in source.children.filter { it.name in tagNames }) {
            ParserElement(document, element, source).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [source] of an attribute that has a
     * [nodeName][Attribute.name] that matches with the [name] parameter, if one is found the [container] closure is
     * applied to it, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attribute(name: String, container: ParserAttribute.() -> Unit): ParserElement {
        if (source.attributes.any { it.name == name }) {
            ParserAttribute(document, source.getAttribute(name), source).apply(container)
        }
        
        return this
    }
    
    /**
     * Looks for exactly **one** occurrence inside of the [source] of an attribute that has a
     * [nodeName][Attribute.name] that matches with the [Pair.first] property, and a [nodeValue][Attribute.value]
     * that matches with [Pair.second], if one is found the [container] closure is applied to it, otherwise nothing
     * happens.
     */
    @XmlMarker
    public inline fun <V : Any> attribute(
        attribute: Pair<String, V>,
        container: ParserAttribute.() -> Unit
    ): ParserElement {
        val (name, value) = attribute
        
        if (source.attributes.any { it.name == name } && source.getAttribute(name).value == value.toString()) {
            ParserAttribute(document, source.getAttribute(name), source).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes the *first* result of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun attribute(
        expression: XPathExpression<Attribute>,
        container: ParserAttribute.() -> Unit
    ): ParserElement {
        val first = expression.evaluate(source).firstOrNull()
        if (first != null) {
            ParserAttribute(document, first, source).apply(container)
        }
        
        return this
    }
    
    /**
     * Invokes all the results of the specified [expression] on the specified [container], if none is found, nothing
     * will happen.
     */
    @XmlMarker
    public inline fun attributes(
        expression: XPathExpression<Attribute>,
        container: ParserAttribute.() -> Unit
    ): ParserElement {
        for (element in expression.evaluate(source)) ParserAttribute(document, element, source).apply(container)
        
        return this
    }
    
    /**
     * Loops through **all** of the attributes appended onto the [root] of this document, and calls the [container]
     * closure on all of them.
     */
    @XmlMarker
    public inline fun attributes(container: ParserAttributes.() -> Unit): ParserElement {
        ParserAttributes(
            document,
            source,
            source.attributes.associateBy({ it.name }, { it.value }),
            source.attributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an attribute that has a [nodeName][Element.name] that
     * matches with any of the defined values inside of the [names] parameter, if any are found the [container] closure
     * is applied to them, otherwise nothing happens.
     */
    @XmlMarker
    public inline fun attributes(vararg names: String, container: ParserAttributes.() -> Unit): ParserElement {
        val foundAttributes = source.attributes.filter { it.name in names }
        
        ParserAttributes(
            document,
            source,
            foundAttributes.associateBy({ it.name }, { it.value }),
            foundAttributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
    }
    
    /**
     * Looks for **any** occurrences inside of the [source] of an attribute that has a
     * [nodeName][Attribute.value] that matches with the [Pair.first] property, and a [nodeValue][Attribute.value] that
     * matches with [Pair.second], if any are found the [container] closure is applied to them, otherwise nothing
     * happens.
     */
    @XmlMarker
    public inline fun <V : Any> attributes(
        vararg attributes: Pair<String, V>,
        container: ParserAttributes.() -> Unit
    ): ParserElement {
        val foundAttributes = attributes.filter { (name, value) ->
            source.attributes.any { it.name == name } && source.getAttribute(name).value == value.toString()
        }.map { (name) -> source.getAttribute(name) }
        
        ParserAttributes(
            document,
            source,
            foundAttributes.associateBy({ it.name }, { it.value }),
            foundAttributes.associateBy({ it.name }, { it })
        ).apply(container)
        
        return this
    }
    
    public override fun toString(): String =
        XMLOutputter(Format.getPrettyFormat().setOmitDeclaration(true).setOmitEncoding(true)).outputString(source)
}

/**
 * An empty container for working on [Attribute] instances retrieved from either [ParserDocument] or [ParserElement].
 *
 * @property document An instance of the over-arching [ParserDocument].
 * @property source The [Attribute] instance that this container is wrapping around.
 * @property parent The parent of this element.
 */
@XmlMarker
public data class ParserAttribute(
    public val document: ParserDocument,
    public val source: Attribute,
    public val parent: Element
)

/**
 * A container for when working with multiple attributes, comes with two different HashMaps for easy access.
 *
 * @property document An instance of the over-arching [ParserDocument].
 * @property parent The parent of this element.
 * @property attributes Contains all the found attributes broken down into `nodeName:nodeValue`.
 * @property attributeMap Contains all the found attributes stored as `nodeName:attribute`.
 *
 * If you do **not** need anything specific from the [Attribute] instance, it's recommended to use the
 * [attributes] property over this one.
 */
@XmlMarker
public data class ParserAttributes(
    public val document: ParserDocument,
    public val parent: Element,
    public val attributes: Map<String, String>,
    public val attributeMap: Map<String, Attribute>
)

/**
 * Wraps a [ParserDocument] over this document and returns the container.
 */
public inline fun Document.parse(container: ParserDocument.() -> Unit): ParserDocument =
    ParserDocument(this).apply(container)

/**
 * Attempts to parse `this` [file][Path] into a [Document] using [SAXBuilder] and the specified [validator], and then
 * wrap a [ParserDocument] around the document and return the resulting container.
 *
 * @receiver the [file][Path] to parse as a document.
 *
 * @param [validator] What to use to validate the document when parsing it.
 *
 * ([DTDVALIDATING][XMLReaders.DTDVALIDATING] by default.)
 * @param [features] A map containing user set features, the entries will be invoked on the [SAXBuilder.setFeature]
 * function.
 *
 * ([emptyMap] by default.)
 * @param [properties] A map containing user set properties, the entries will be invoked on the
 * [SAXBuilder.setProperty] function.
 *
 * ([emptyMap] by default.)
 */
@JvmOverloads
public inline fun Path.parseAsDocument(
    validator: XMLReaders = XMLReaders.DTDVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    container: ParserDocument.() -> Unit
): ParserDocument {
    val builder = SAXBuilder(validator)
    for ((key, bool) in features) builder.setFeature(key, bool)
    for ((key, obj) in properties) builder.setProperty(key, obj)
    return ParserDocument(builder.build(this.toFile())).apply(container)
}

/**
 * Attempts to parse `this` [file][File] into a [Document] using [SAXBuilder] and the specified [validator], and then
 * wrap a [ParserDocument] around the document and return the resulting container.
 *
 * @receiver the [file][File] to parse as a document.
 *
 * @param [validator] What to use to validate the document when parsing it.
 *
 * ([DTDVALIDATING][XMLReaders.DTDVALIDATING] by default.)
 * @param [features] A map containing user set features, the entries will be invoked on the [SAXBuilder.setFeature]
 * function.
 *
 * ([emptyMap] by default.)
 * @param [properties] A map containing user set properties, the entries will be invoked on the
 * [SAXBuilder.setProperty] function.
 *
 * ([emptyMap] by default.)
 */
@JvmOverloads
public inline fun File.parseAsDocument(
    validator: XMLReaders = XMLReaders.DTDVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    container: ParserDocument.() -> Unit
): ParserDocument {
    val builder = SAXBuilder(validator)
    for ((key, bool) in features) builder.setFeature(key, bool)
    for ((key, obj) in properties) builder.setProperty(key, obj)
    return ParserDocument(builder.build(this)).apply(container)
}

/**
 * Attempts to parse `this` input-stream into a [Document] using [SAXBuilder] and the specified [validator], and then
 * wrap a [ParserDocument] around the document and return the resulting container.
 *
 * @receiver the [input-stream][InputStream] to parse as a document.
 *
 * @param [validator] What to use to validate the document when parsing it.
 *
 * ([DTDVALIDATING][XMLReaders.DTDVALIDATING] by default.)
 * @param [features] A map containing user set features, the entries will be invoked on the [SAXBuilder.setFeature]
 * function.
 *
 * ([emptyMap] by default.)
 * @param [properties] A map containing user set properties, the entries will be invoked on the
 * [SAXBuilder.setProperty] function.
 *
 * ([emptyMap] by default.)
 */
@JvmOverloads
public inline fun InputStream.parseAsDocument(
    validator: XMLReaders = XMLReaders.DTDVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    container: ParserDocument.() -> Unit
): ParserDocument {
    val builder = SAXBuilder(validator)
    for ((key, bool) in features) builder.setFeature(key, bool)
    for ((key, obj) in properties) builder.setProperty(key, obj)
    return ParserDocument(builder.build(this)).apply(container)
}

/**
 * Attempts to parse `this` URL into a [Document] using [SAXBuilder] and the specified [validator], and then
 * wrap a [ParserDocument] around the document and return the resulting container.
 *
 * @receiver the [input-stream][InputStream] to parse as a document.
 *
 * @param [validator] What to use to validate the document when parsing it.
 *
 * ([DTDVALIDATING][XMLReaders.DTDVALIDATING] by default.)
 * @param [features] A map containing user set features, the entries will be invoked on the [SAXBuilder.setFeature]
 * function.
 *
 * ([emptyMap] by default.)
 * @param [properties] A map containing user set properties, the entries will be invoked on the
 * [SAXBuilder.setProperty] function.
 *
 * ([emptyMap] by default.)
 */
@JvmOverloads
public inline fun URL.parseAsDocument(
    validator: XMLReaders = XMLReaders.DTDVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    container: ParserDocument.() -> Unit
): ParserDocument {
    val builder = SAXBuilder(validator)
    for ((key, bool) in features) builder.setFeature(key, bool)
    for ((key, obj) in properties) builder.setProperty(key, obj)
    return ParserDocument(builder.build(this)).apply(container)
}

/**
 * Compiles `this` [String] as a [XPathExpression]`<F>`.
 */
@Suppress("UNCHECKED_CAST")
public fun <F> String.compile(): XPathExpression<F> =
    XPathFactory.instance().compile(this) as XPathExpression<F>