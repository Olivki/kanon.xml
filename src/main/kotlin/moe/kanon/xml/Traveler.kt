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

package moe.kanon.xml

import org.jdom2.Attribute
import org.jdom2.CDATA
import org.jdom2.Comment
import org.jdom2.Content
import org.jdom2.DocType
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.filter.Filter
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.XMLReaders
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jdom2.xpath.XPathExpression
import org.jdom2.xpath.XPathFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

@DslMarker annotation class TravelerScope

/**
 * A document container used by the XML [traveler][traverse] DSL.
 *
 * @property [document] The [Document] instance that `this` traveler is wrapping around.
 */
@TravelerScope class DocumentTraveler(val document: Document) {
    val root: Element get() = document.rootElement

    @PublishedApi internal val elements: Sequence<Element> get() = root.children.asSequence()
    @PublishedApi internal val contents: Sequence<Content> get() = root.content.asSequence()
    @PublishedApi internal val attributes: Sequence<Attribute> get() = root.attributes.asSequence()

    // -- TOP-LEVEL CONSTRUCTS -- \\
    /**
     * Returns the [DocType] used by `this` document.
     */
    @TravelerScope val docType: DocType get() = document.docType

    // -- ATTRIBUTE -- \\
    /**
     * Returns the result of applying [ifPresent] to the *first* attribute of `this` document that has a matching
     * [name][Element.name] and [namespace][Element.namespace] to the given [name] and [namespace], or the result of
     * invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        name: String,
        namespace: Namespace,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = root.getAttribute(name, namespace)?.let(ifPresent) ?: ifMissing()

    /**
     * Returns the result of applying [ifPresent] to the *first* attribute of `this` document that has a matching
     * [name][Element.name] to the given [name], or the result of invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        name: String,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = attribute(name, Namespace.NO_NAMESPACE, ifPresent, ifMissing)

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` document that the given [expression]
     * matches with, or [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        expression: XPathExpression<Attribute>,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = expression.evaluateFirst(root)?.let(ifPresent) ?: ifMissing()

    // -- ATTRIBUTES -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` document that have a matching [name][Element.name] and [namespace][Element.namespace] to the given
     * [name] and [namespace], respectively.
     */
    @TravelerScope fun <R> attributes(
        name: String,
        namespace: Namespace = Namespace.NO_NAMESPACE,
        transformer: (Attribute) -> R
    ): List<R> = attributes.filter { it.name == name && it.namespace == namespace }.map(transformer).toList()

    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` document that matched the given [expression].
     */
    @TravelerScope inline fun <R> attributes(
        expression: XPathExpression<Attribute>,
        transformer: (Attribute) -> R
    ): List<R> = expression.evaluate(root).map(transformer)

    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` document that passed the given [predicate].
     */
    @TravelerScope fun <R> attributes(
        predicate: (Attribute) -> Boolean = { true },
        transformer: (Attribute) -> R
    ): List<R> = attributes.filter(predicate).map(transformer).toList()

    // -- ELEMENT -- \\
    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` document that has a matching
     * [name][Element.name] and [namespace][Element.namespace] to the given [name] and [namespace], or the result of
     * invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(
        name: String,
        namespace: Namespace,
        ifPresent: (Element) -> R,
        ifMissing: () -> R
    ): R = root.getChild(name, namespace)?.let(ifPresent) ?: ifMissing()

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` document that has a matching
     * [name][Element.name] to the given [name], or the result of invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(name: String, ifPresent: (Element) -> R, ifMissing: () -> R): R =
        element(name, Namespace.NO_NAMESPACE, ifPresent, ifMissing)

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` document that the given [expression]
     * matches with, or [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(
        expression: XPathExpression<Element>,
        ifPresent: (Element) -> R,
        ifMissing: () -> R
    ): R = expression.evaluateFirst(root)?.let(ifPresent) ?: ifMissing()

    // -- ELEMENTS -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` document that have a matching [name][Element.name] and [namespace][Element.namespace] to the given
     * [name] and [namespace], respectively.
     */
    @TravelerScope inline fun <R> elements(
        name: String,
        namespace: Namespace = Namespace.NO_NAMESPACE,
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = elements.filter { it.name == name && it.namespace == namespace }
        .map { with(ElementTraveler(root, it), transformer) }
        .toList()

    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` document that matched the given [expression].
     */
    @TravelerScope inline fun <R> elements(
        expression: XPathExpression<Element>,
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = expression.evaluate(root).map { with(ElementTraveler(root, it), transformer) }

    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` document that passed the given [predicate].
     */
    @TravelerScope inline fun <R> elements(
        noinline predicate: (Element) -> Boolean = { true },
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = elements.filter(predicate).map { with(ElementTraveler(root, it), transformer) }.toList()

    // -- COMMENTS -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all comments of `this` document that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> comments(predicate: (Comment) -> Boolean = { true }, transformer: (Comment) -> R): List<R> =
        contents.filterIsInstance<Comment>().filter(predicate).map(transformer).toList()

    // -- TEXT -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all texts of `this` document that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> texts(predicate: (Text) -> Boolean = { true }, transformer: (Text) -> R): List<R> =
        contents.filterIsInstance<Text>().filter(predicate).map(transformer).toList()

    // -- CDATA -- \\\
    /**
     * Returns a list containing the result of applying the given [transformer] to all cdatas of `this` document that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> cdatas(predicate: (CDATA) -> Boolean = { true }, transformer: (CDATA) -> R): List<R> =
        contents.filterIsInstance<CDATA>().filter(predicate).map(transformer).toList()

    // -- MISC -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all the
     * [children][Document.getContent] of `this` document that match the given [expression].
     */
    @TravelerScope inline fun <T, R> match(expression: XPathExpression<T>, transformer: (T) -> R): List<R> =
        expression.evaluate(root).map(transformer)

    /**
     * Returns a new [XPathExpression] for the given [expression].
     *
     * @param [T] the type used for setting the [Filter] of the `XPathExpression`
     */
    inline fun <reified T> compile(
        expression: String,
        variables: Map<String, Any> = emptyMap(),
        namespaces: Collection<Namespace> = emptyList()
    ): XPathExpression<T> =
        XPathFactory.instance().compile(expression, Filters.fclass(T::class.java), variables, namespaces)

    /**
     * Returns a function that throws a [NoSuchElementException] using the given [message].
     *
     * The intention of this function is to provide a nicer syntax for the single matching functions *([buildElement],
     * [attribute], etc)* `ifMissing` parameter. The idea is that if the element you're matching for is not found, and
     * said element *should* be there then you most likely want to throw an exception.
     *
     * So rather than writing this:
     * ```kotlin
     *  traverse(...) {
     *      element("person", { ... }) { throw NoSuchElementException("Missing person element!") }
     *  }
     * ```
     * one could write:
     * ```kotlin
     *  traverse(...) {
     *      element("person", { ... }, throwMissingElement("Missing person element!"))
     *  }
     * ```
     *
     * However, most likely one would want to raise a more specific exception rather than a [NoSuchElementException]
     * if the XML one is parsing is malformed. Similar syntax to this can be achieved for domain specific exceptions by
     * making use of extension functions to mimic this function.
     *
     * *Alternatively* if one always want the same type of exception to be raised when an element is missing, one could
     * "implement" a default behaviour for it using extensions functions;
     *
     * ```kotlin
     *  @TravelerScope inline fun <R> DocumentTraveler.element(name: String, transformer: (Element) -> R): R = element(name, transformer, { throw NoSuchElementException("Could not find element with name <$name>") })
     * ```
     */
    fun <T> throwMissingElement(message: String): () -> T = { throw NoSuchElementException(message) }

    // -- OVERRIDES -- \\
    /**
     * Returns `this` document serialized into a `String` according to the specified [format].
     *
     * @param [format] the format to serialize `this` document into
     *
     * @see [XMLOutputter.outputString]
     */
    @BuilderMarker fun toString(format: Format): String = XMLOutputter(format).outputString(root)

    /**
     * Returns `this` document serialized into a `String` using the [pretty-format][Format.getPrettyFormat].
     *
     * @see [XMLOutputter.outputString]
     */
    @BuilderMarker override fun toString(): String =
        XMLOutputter(Format.getPrettyFormat()).outputString(root)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is DocumentTraveler -> false
        document != other.document -> false
        else -> true
    }

    override fun hashCode(): Int = document.hashCode()
}

/**
 * A container used for the `traverser` DSL.
 *
 * @property [parent] The [parent][Element.getParentElement] of the [source] element that `this` traveler is wrapping
 * around.
 *
 * Note that if the `source` element does *not* have a `parent`, then this property will return the same instance as
 * `source`.
 * @property [source] The [Element] that `this` traveler is wrapping around.
 */
@TravelerScope class ElementTraveler(val parent: Element, val source: Element) {
    @PublishedApi internal val elements: Sequence<Element> get() = source.children.asSequence()
    @PublishedApi internal val contents: Sequence<Content> get() = source.content.asSequence()
    @PublishedApi internal val attributes: Sequence<Attribute> get() = source.attributes.asSequence()

    // -- ATTRIBUTE -- \\
    /**
     * Returns the result of applying [ifPresent] to the *first* attribute of `this` element that has a matching
     * [name][Element.name] and [namespace][Element.namespace] to the given [name] and [namespace], or the result of
     * invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        name: String,
        namespace: Namespace,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = source.getAttribute(name, namespace)?.let(ifPresent) ?: ifMissing()

    /**
     * Returns the result of applying [ifPresent] to the *first* attribute of `this` element that has a matching
     * [name][Element.name] to the given [name], or the result of invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        name: String,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = attribute(name, Namespace.NO_NAMESPACE, ifPresent, ifMissing)

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` element that the given [expression]
     * matches with, or [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> attribute(
        expression: XPathExpression<Attribute>,
        ifPresent: (Attribute) -> R,
        ifMissing: () -> R
    ): R = expression.evaluateFirst(source)?.let(ifPresent) ?: ifMissing()

    // -- ATTRIBUTES -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` element that have a matching [name][Element.name] and [namespace][Element.namespace] to the given
     * [name] and [namespace], respectively.
     */
    @TravelerScope fun <R> attributes(
        name: String,
        namespace: Namespace = Namespace.NO_NAMESPACE,
        transformer: (Attribute) -> R
    ): List<R> = attributes.filter { it.name == name && it.namespace == namespace }.map(transformer).toList()

    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` element that matched the given [expression].
     */
    @TravelerScope inline fun <R> attributes(
        expression: XPathExpression<Attribute>,
        transformer: (Attribute) -> R
    ): List<R> = expression.evaluate(source).map(transformer)

    /**
     * Returns a list containing the result of applying the given [transformer] to the [attributes][Element.getAttributes]
     * of `this` element that passed the given [predicate].
     */
    @TravelerScope fun <R> attributes(
        predicate: (Attribute) -> Boolean = { true },
        transformer: (Attribute) -> R
    ): List<R> = attributes.filter(predicate).map(transformer).toList()

    // -- ELEMENT -- \\
    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` element that has a matching
     * [name][Element.name] and [namespace][Element.namespace] to the given [name] and [namespace], or the result of
     * invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(
        name: String,
        namespace: Namespace,
        ifPresent: (Element) -> R,
        ifMissing: () -> R
    ): R = source.getChild(name, namespace)?.let(ifPresent) ?: ifMissing()

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` element that has a matching
     * [name][Element.name] to the given [name], or the result of invoking [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(name: String, ifPresent: (Element) -> R, ifMissing: () -> R): R =
        element(name, Namespace.NO_NAMESPACE, ifPresent, ifMissing)

    /**
     * Returns the result of applying [ifPresent] to the *first* child of `this` element that the given [expression]
     * matches with, or [ifMissing] if none is found.
     */
    @TravelerScope inline fun <R> element(
        expression: XPathExpression<Element>,
        ifPresent: (Element) -> R,
        ifMissing: () -> R
    ): R = expression.evaluateFirst(source)?.let(ifPresent) ?: ifMissing()

    // -- ELEMENTS -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` element that have a matching [name][Element.name] and [namespace][Element.namespace] to the given
     * [name] and [namespace], respectively.
     */
    @TravelerScope inline fun <R> elements(
        name: String,
        namespace: Namespace = Namespace.NO_NAMESPACE,
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = elements.filter { it.name == name && it.namespace == namespace }
        .map { with(ElementTraveler(source, it), transformer) }
        .toList()

    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` element that matched the given [expression].
     */
    @TravelerScope inline fun <R> elements(
        expression: XPathExpression<Element>,
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = expression.evaluate(source).map { with(ElementTraveler(source, it), transformer) }

    /**
     * Returns a list containing the result of applying the given [transformer] to the [children][Element.getChildren]
     * of `this` element that passed the given [predicate].
     */
    @TravelerScope inline fun <R> elements(
        noinline predicate: (Element) -> Boolean = { true },
        crossinline transformer: ElementTraveler.() -> R
    ): List<R> = elements.filter(predicate).map { with(ElementTraveler(source, it), transformer) }.toList()

    // -- COMMENTS -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all comments of `this` element that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> comments(predicate: (Comment) -> Boolean = { true }, transformer: (Comment) -> R): List<R> =
        contents.filterIsInstance<Comment>().filter(predicate).map(transformer).toList()

    // -- TEXT -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all texts of `this` element that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> texts(predicate: (Text) -> Boolean = { true }, transformer: (Text) -> R): List<R> =
        contents.filterIsInstance<Text>().filter(predicate).map(transformer).toList()

    // -- CDATA -- \\\
    /**
     * Returns a list containing the result of applying the given [transformer] to all cdatas of `this` element that
     * passed the given [predicate].
     */
    @TravelerScope fun <R> cdatas(predicate: (CDATA) -> Boolean = { true }, transformer: (CDATA) -> R): List<R> =
        contents.filterIsInstance<CDATA>().filter(predicate).map(transformer).toList()

    // -- MISC -- \\
    /**
     * Returns a list containing the result of applying the given [transformer] to all the
     * [children][Document.getContent] of `this` element that match the given [expression].
     */
    @TravelerScope inline fun <T, R> match(expression: XPathExpression<T>, transformer: (T) -> R): List<R> =
        expression.evaluate(source).map(transformer)

    /**
     * Returns a new [XPathExpression] for the given [expression].
     *
     * @param [T] the type used for setting the [Filter] of the `XPathExpression`
     */
    inline fun <reified T> compile(
        expression: String,
        variables: Map<String, Any> = emptyMap(),
        namespaces: Collection<Namespace> = emptyList()
    ): XPathExpression<T> =
        XPathFactory.instance().compile(expression, Filters.fclass(T::class.java), variables, namespaces)

    /**
     * Returns a function that throws a [NoSuchElementException] using the given [message].
     *
     * The intention of this function is to provide a nicer syntax for the single matching functions *([buildElement],
     * [attribute], etc)* `ifMissing` parameter. The idea is that if the element you're matching for is not found, and
     * said element *should* be there then you most likely want to throw an exception.
     *
     * So rather than writing this:
     * ```kotlin
     *  traverse(...) {
     *      element("person", { ... }) { throw NoSuchElementException("Missing person element!") }
     *  }
     * ```
     * one could write:
     * ```kotlin
     *  traverse(...) {
     *      element("person", { ... }, throwMissingElement("Missing person element!"))
     *  }
     * ```
     *
     * However, most likely one would want to raise a more specific exception rather than a [NoSuchElementException]
     * if the XML one is parsing is malformed. Similar syntax to this can be achieved for domain specific exceptions by
     * making use of extension functions to mimic this function.
     *
     * *Alternatively* if one always want the same type of exception to be raised when an element is missing, one could
     * "implement" a default behaviour for it using extensions functions;
     *
     * ```kotlin
     *  @TravelerScope inline fun <R> DocumentTraveler.element(name: String, transformer: (Element) -> R): R = element(name, transformer, { throw NoSuchElementException("Could not find element with name <$name>") })
     * ```
     */
    fun <T> throwMissingElement(message: String): () -> T = { throw NoSuchElementException(message) }

    // -- OVERRIDES -- \\
    /**
     * Returns `this` element serialized into a `String` according to the specified [format].
     *
     * @param [format] the format to serialize `this` document into
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
        other !is ElementTraveler -> false
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
 * Returns the result of wrapping the given [document] into a [DocumentTraveler] and applying the given [scope]
 * function it.
 */
@TravelerScope inline fun traverse(document: Document, scope: DocumentTraveler.() -> Unit): DocumentTraveler =
    DocumentTraveler(document).apply(scope)

/**
 * Returns the result of wrapping the given [element] into a [ElementTraveler] and applying the given [scope] function
 * to it.
 */
@TravelerScope inline fun traverse(element: Element, scope: ElementTraveler.() -> Unit): ElementTraveler =
    ElementTraveler(element.parentElement ?: element, element).apply(scope)

/**
 * Attempts to parse the given [file] into a [Document] instance using the given parameters, and then wrapping the
 * parsed `Document` instance in a [DocumentTraveler] and return the result of applying the given [scope] function to
 * it.
 *
 * @throws [JDOMException] when errors occur in parsing
 * @throws [IOException] if an I/O error occurs / when an I/O error prevents a document from being fully parsed
 *
 * @see [Files.newInputStream]
 * @see [SAXBuilder]
 * @see [traverse]
 */
@Throws(JDOMException::class, IOException::class)
@JvmOverloads @TravelerScope inline fun traverse(
    file: Path,
    validator: XMLReaders = XMLReaders.NONVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    scope: DocumentTraveler.() -> Unit
): DocumentTraveler = traverse(Files.newInputStream(file), validator, features, properties, scope)

/**
 * Attempts to parse the given [input] into a [Document] instance using the given parameters, and then wrapping the
 * parsed `Document` instance in a [DocumentTraveler] and return the result of applying the given [scope] function to
 * it.
 *
 * @throws [JDOMException] when errors occur in parsing
 * @throws [IOException] if an I/O error occurs / when an I/O error prevents a document from being fully parsed
 *
 * @see [SAXBuilder]
 * @see [traverse]
 */
@Throws(JDOMException::class, IOException::class)
@JvmOverloads @TravelerScope inline fun traverse(
    input: InputStream,
    validator: XMLReaders = XMLReaders.NONVALIDATING,
    features: Map<String, Boolean> = emptyMap(),
    properties: Map<String, Any> = emptyMap(),
    scope: DocumentTraveler.() -> Unit
): DocumentTraveler = traverse(createBuilder(validator, features, properties).build(input), scope)

@PublishedApi internal fun createBuilder(
    validator: XMLReaders,
    features: Map<String, Boolean>,
    properties: Map<String, Any>
): SAXBuilder = SAXBuilder(validator).also { builder ->
    for ((key, bool) in features) builder.setFeature(key, bool)
    for ((key, obj) in properties) builder.setProperty(key, obj)
}