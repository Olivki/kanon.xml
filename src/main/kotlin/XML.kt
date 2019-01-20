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

package moe.kanon.xml

import moe.kanon.kextensions.io.div
import moe.kanon.kextensions.io.not
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

// TODO: Add comments
// TODO: Add the "Comment" entity thing.

/**
 * The [DslMarker] for kXML.
 */
@DslMarker
public annotation class XmlDsl

public sealed class XmlEntity

/**
 * The main document from where everything is built.
 *
 * @property rootName The name of the root element of the document.
 */
public class XmlDocument(rootName: String?) : XmlEntity() {
    
    public val source: Document by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    }
    
    public val root: XmlElement by lazy {
        if (rootName != null) XmlElement(this, rootName, null) else XmlElement(this, null, null)
    }
    
    /**
     * @see element
     */
    @XmlDsl
    public inline operator fun String.invoke(block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlDocument, this).apply(block)
    
    @XmlDsl
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit) = XmlAttributesContainer(root).apply(block)
    
    @XmlDsl
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            root.element[name] = value.toString()
        }
    }
    
    /**
     * @see String.invoke
     */
    @XmlDsl
    public inline fun element(name: String, block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlDocument, name).apply(block)
    
    @XmlDsl
    public inline fun text(name: String, block: String.() -> String) {
        root.element[ChildType.TEXT_NODE] = String().block()
    }
    
    /**
     * Saves the document to the specified [directory] with the specified [name].
     *
     * @param directory The directory in which the file will be saved.
     * @param name The name of the file.
     */
    public fun saveTo(directory: Path, name: String): Path {
        val file = directory / name
        val (source, transformer) = createSource()
        val result = StreamResult(!file)
        
        transformer.transform(source, result)
        
        return file
    }
    
    public override fun toString(): String {
        val (source, transformer) = createSource()
        
        val writer = StringWriter()
        transformer.transform(source, StreamResult(writer))
        
        return writer.buffer.toString()
    }
    
    internal fun createSource(): Pair<DOMSource, Transformer> {
        val factory = TransformerFactory.newInstance()
        val transformer = factory.newTransformer()
        
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        
        return DOMSource(source) to transformer
    }
}

public class XmlElement(
    public val document: XmlDocument,
    public val name: String?,
    public val parent: XmlElement? = document.root
) : XmlEntity() {
    
    public val element: Element by lazy {
        if (name != null) {
            if (parent != null) {
                parent.element.set(ChildType.ELEMENT, name) as Element
            } else {
                document.source.set(ChildType.ELEMENT, name) as Element
            }
        } else {
            document.source.documentElement
        }
    }
    
    @XmlDsl
    public inline operator fun String.invoke(block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlElement.document, this, parent = this@XmlElement).apply(block)
    
    @XmlDsl
    public inline fun text(block: String.() -> String) {
        element[ChildType.TEXT_NODE] = String().block()
    }
    
    @XmlDsl
    public inline fun text(name: String, block: String.() -> String) {
        val parent = element.appendChild(document.source.createElement(name))
        parent[ChildType.TEXT_NODE] = String().block()
    }
    
    @XmlDsl
    public inline fun element(name: String, block: XmlElement.() -> Unit): XmlElement =
        XmlElement(document, name, this).apply(block)
    
    @XmlDsl
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit) = XmlAttributesContainer(this).apply(block)
    
    @XmlDsl
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            element[name] = value.toString()
        }
    }
    
    public override fun toString(): String = element.toString()
    
}

public class XmlAttributesContainer(public val parent: XmlElement) : XmlEntity() {
    
    @XmlDsl
    public inline operator fun <V : Any> String.invoke(block: Any.() -> V) {
        parent.element[this] = Any().block().toString()
    }
    
    @XmlDsl
    public operator fun <V : Any> String.get(value: V) {
        parent.element[this] = value.toString()
    }
    
    @XmlDsl
    public fun <V : Any> attribute(name: String, value: V) {
        parent.element[name] = value.toString()
    }
    
    @XmlDsl
    public inline fun <V : Any> attribute(name: String, block: Any.() -> V) {
        parent.element[name] = Any().block().toString()
    }
}

@XmlDsl
public inline fun xml(root: String, body: XmlDocument.() -> Unit): XmlDocument = XmlDocument(root).apply(body)

@XmlDsl
public inline fun kxml(root: String, body: XmlDocument.() -> Unit): XmlDocument = XmlDocument(root).apply(body)