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

/**
 * Thrown whenever an exception is encountered when trying to build an XML document with the builder DSL.
 *
 * This is needed because the default exceptions thrown by the XML generator don't actually detail *what* caused the
 * exception, and with DSLs in Kotlin it's hard to accurately pin-point the exact error location. So this "wrapper"
 * is to ease the pain of debugging faulty XML.
 *
 * @param entity This is the string representation of where the builder was when the exception was thrown.
 */
public class XmlBuilderException(entity: String, cause: Throwable) :
    Exception("kanon.xml encountered a problem when trying to create \"$entity\", stacktrace:", cause)

/**
 * Thrown whenever an exception is encountered when trying to parse an XML document with the parser DSL.
 *
 * Reason for this existing is the same as that of [XmlBuilderException].
 *
 * @param entity This is the string representation of where the parser was when the exception was thrown.
 */
public class XmlParserException(entity: String, cause: Throwable) :
    Exception("kanon.xml encountered a problem when trying to parse \"$entity\", stacktrace:", cause)