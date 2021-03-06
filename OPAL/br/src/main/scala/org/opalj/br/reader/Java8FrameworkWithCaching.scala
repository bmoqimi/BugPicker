/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package reader

import org.opalj.bi.reader.CodeReader

/**
 * This "framework" can be used to read in Java 8 (version 52) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Michael Eichberg
 */
class Java8FrameworkWithCaching(cache: BytecodeInstructionsCache)
    extends Java8LibraryFrameworkWithCaching(cache)
    with CodeAttributeBinding
    with SourceDebugExtension_attributeBinding
    // THOUGH THE BOOTSTRAPMETHODS ATTRIBTUE IS A CLASS-LEVEL ATTRIBUTE
    // IT IS OF NO USE IF WE DO NOT ALSO REIFY THE METHOD BODY
    with BootstrapMethods_attributeBinding
    with StackMapTable_attributeBinding
    with CompactLineNumberTable_attributeBinding
    with LocalVariableTable_attributeBinding
    with LocalVariableTypeTable_attributeBinding
    with Exceptions_attributeBinding
    with CachedBytecodeReaderAndBinding
    with CodeReader

