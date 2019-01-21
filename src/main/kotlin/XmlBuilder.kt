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
import moe.kanon.kextensions.dom.appendElement
import moe.kanon.kextensions.dom.appendTextContainer
import moe.kanon.kextensions.dom.appendTextNode
import moe.kanon.kextensions.dom.attributeMap
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
// TODO: Add more entities

/**
 * The [DslMarker] for kanon.xml.
 */
@DslMarker
public annotation class XmlMarker

public sealed class XmlEntity

public class XmlDocument(rootName: String) : XmlEntity() {
    
    public val source: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    
    public val root: XmlElement = XmlElement(this, rootName, null)
    
    /**
     * @see element
     */
    @XmlMarker
    public inline operator fun String.invoke(block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlDocument, this).apply(block)
    
    @XmlMarker
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit) = XmlAttributesContainer(root).apply(block)
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            root.element.attributeMap[name] = value.toString()
        }
    }
    
    /**
     * @see String.invoke
     */
    @XmlMarker
    public inline fun element(tagName: String, block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlDocument, tagName).apply(block)
    
    @XmlMarker
    public inline fun text(tagName: String, block: String.() -> String) {
        root.element.appendTextContainer(tagName, String().block())
    }
    
    /**
     * Saves the xml document to the specified [directory] with the specified [name].
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
    public val name: String,
    public val parent: XmlElement? = document.root
) : XmlEntity() {
    
    public val element: Element = parent?.element?.appendElement(name) ?: document.source.appendElement(name)
    
    @XmlMarker
    public inline operator fun String.invoke(block: XmlElement.() -> Unit): XmlElement =
        XmlElement(this@XmlElement.document, this, parent = this@XmlElement).apply(block)
    
    @XmlMarker
    public inline fun text(block: String.() -> String) {
        element.appendTextNode(String().block())
    }
    
    @XmlMarker
    public inline fun text(tagName: String, block: String.() -> String) {
        element.appendTextContainer(tagName, String().block())
    }
    
    @XmlMarker
    public inline fun element(tagName: String, block: XmlElement.() -> Unit): XmlElement =
        XmlElement(document, tagName, this).apply(block)
    
    @XmlMarker
    public inline fun attributes(block: XmlAttributesContainer.() -> Unit) = XmlAttributesContainer(this).apply(block)
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attributes(vararg attributes: Pair<String, V>) {
        for ((name, value) in attributes) {
            element.attributeMap[name] = value.toString()
        }
    }
    
    public override fun toString(): String = element.toString()
    
}

public class XmlAttributesContainer(public val parent: XmlElement) : XmlEntity() {
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public inline operator fun <V : Any> String.invoke(block: Any.() -> V) {
        parent.element.attributeMap[this] = Any().block().toString()
    }
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public operator fun <V : Any> String.get(value: V) {
        parent.element.attributeMap[this] = value.toString()
    }
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public fun <V : Any> attribute(name: String, value: V) {
        parent.element.attributeMap[name] = value.toString()
    }
    
    @XmlMarker
    @UseExperimental(ExperimentalFeature::class)
    public inline fun <V : Any> attribute(name: String, block: Any.() -> V) {
        parent.element.attributeMap[name] = Any().block().toString()
    }
    
    @UseExperimental(ExperimentalFeature::class)
    public override fun toString(): String =
        "${parent.name}(${parent.element.attributeMap.asSequence().joinToString { "[${it.key}:${it.value}]" }})"
}

@XmlMarker
public inline fun xml(root: String, body: XmlDocument.() -> Unit): XmlDocument = XmlDocument(root).apply(body)
