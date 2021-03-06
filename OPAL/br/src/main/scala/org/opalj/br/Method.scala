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

import bi.ACC_ABSTRACT
import bi.ACC_STRICT
import bi.ACC_NATIVE
import bi.ACC_BRIDGE
import bi.ACC_VARARGS
import bi.ACC_SYNCHRONIZED
import bi.AccessFlagsContexts
import bi.AccessFlags
import org.opalj.bi.ACC_PUBLIC

/**
 * Represents a single method.
 *
 * Method objects are constructed using the companion object's factory methods.
 *
 * @note Methods have – by default – no link to their defining [[ClassFile]]. However,
 *      if a [[analyses.Project]] is available then it is possible to get a `Method`'s
 *      [[ClassFile]] by using `Project`'s `classFile(Method)` method.
 * @note Equality of methods is – by purpose – reference based.
 *
 * @param accessFlags The ''access flags'' of this method. Though it is possible to
 *     directly work with the `accessFlags` field, it may be more convenient to use
 *     the respective methods (`isNative`, `isAbstract`,...) to query the access flags.
 * @param name The name of the method. The name is interned (see `String.intern()`
 *      for details) to enable reference comparisons.
 * @param descriptor This method's descriptor.
 * @param body The body of the method if any.
 * @param attributes This method's defined attributes. (Which attributes are available
 *      generally depends on the configuration of the class file reader. However,
 *      the `Code_Attribute` is – if it was loaded – always directly accessible by
 *      means of the `body` attribute.).
 *
 * @author Michael Eichberg
 */
final class Method private (
    val accessFlags: Int,
    val name: String,
    val descriptor: MethodDescriptor,
    val body: Option[Code],
    val attributes: Attributes)
        extends ClassMember with scala.math.Ordered[Method] {

    /**
     * Returns true if this method and the given method have the same signature.
     *
     * @param ignoreReturnType If `false` (default), then the return type is taken
     *      into consideration. This models the behavior of the JVM w.r.t. method
     *      dispatch.
     *      However, if you want to determine whether this method potentially overrides
     *      the given one, you may want to specify that you want to ignore the return type.
     *      (The Java compiler generate the appropriate methods.)
     */
    def hasSameSignature(other: Method, ignoreReturnType: Boolean = false): Boolean = {
        this.name == other.name && {
            if (ignoreReturnType)
                this.descriptor.equalParameters(other.descriptor)
            else
                this.descriptor == other.descriptor
        }
    }

    final override def isMethod = true

    final override def asMethod = this

    def asVirtualMethod(declaringClassType: ObjectType): VirtualMethod =
        VirtualMethod(declaringClassType, name, descriptor)

    def runtimeVisibleParameterAnnotations: ParameterAnnotations =
        (attributes collectFirst {
            case RuntimeVisibleParameterAnnotationTable(pas) ⇒ pas
        }) match {
            case Some(annotations) ⇒ annotations
            case None              ⇒ IndexedSeq.empty
        }

    def runtimeInvisibleParameterAnnotations: ParameterAnnotations =
        (attributes collectFirst {
            case RuntimeInvisibleParameterAnnotationTable(pas) ⇒ pas
        }) match {
            case Some(annotations) ⇒ annotations
            case None              ⇒ IndexedSeq.empty
        }

    def parameterAnnotations: ParameterAnnotations =
        runtimeVisibleParameterAnnotations ++ runtimeInvisibleParameterAnnotations

    // This is directly supported due to its need for the resolution of signature 
    // polymorphic methods. 
    final def isNativeAndVarargs = Method.isNativeAndVarargs(accessFlags)

    final def isVarargs: Boolean = (ACC_VARARGS.mask & accessFlags) != 0

    final def isSynchronized: Boolean = (ACC_SYNCHRONIZED.mask & accessFlags) != 0

    final def isBridge: Boolean = (ACC_BRIDGE.mask & accessFlags) != 0

    final def isNative = (ACC_NATIVE.mask & accessFlags) != 0

    final def isStrict: Boolean = (ACC_STRICT.mask & accessFlags) != 0

    final def isAbstract: Boolean = (ACC_ABSTRACT.mask & accessFlags) != 0

    final def isConstructor: Boolean = name == "<init>"

    final def isStaticInitializer: Boolean = name == "<clinit>"

    final def isInitialzer: Boolean = isConstructor || isStaticInitializer

    def returnType = descriptor.returnType

    def parameterTypes = descriptor.parameterTypes

    /**
     * Each method optionally defines a method type signature.
     */
    def methodTypeSignature: Option[MethodTypeSignature] =
        attributes collectFirst { case s: MethodTypeSignature ⇒ s }

    def exceptionTable: Option[ExceptionTable] =
        attributes collectFirst { case et: ExceptionTable ⇒ et }

    /**
     * Defines an absolute order on `Method` instances based on their method signatures.
     *
     * The order is defined by lexicographically comparing the names of the methods
     * and – in case that the names of both methods are identical – by comparing
     * their method descriptors.
     */
    def compare(other: Method): Int = {
        if (this.name eq other.name) {
            this.descriptor.compare(other.descriptor)
        } else if (this.name < other.name)
            -1
        else {
            1
        }
    }

    def toJava(): String = descriptor.toJava(name)

    def fullyQualifiedSignature(declaringClassType: ObjectType): String = {
        descriptor.toJava(declaringClassType.toJava+"."+name)
    }

    override def toString(): String = {
        AccessFlags.toStrings(accessFlags, AccessFlagsContexts.METHOD).mkString("", " ", " ") +
            descriptor.toJava(name) +
            attributes.view.map(_.getClass.getSimpleName).mkString(" « ", ", ", " »")
    }

}
/**
 * Defines factory and extractor methods for `Method` objects.
 *
 * @author Michael Eichberg
 */
object Method {

    final val ACC_NATIVEAndVARARGS /*:Int*/ = ACC_NATIVE.mask | ACC_VARARGS.mask

    private def isNativeAndVarargs(accessFlags: Int) =
        (accessFlags & ACC_NATIVEAndVARARGS) == ACC_NATIVEAndVARARGS

    /**
     * @param name The name of the method. In case of a constructor the method
     *      name has to be "<init>". In case of a static initializer the name has to
     *      be "<clinit>".
     */
    def apply(
        accessFlags: Int,
        name: String,
        descriptor: MethodDescriptor,
        attributes: Attributes): Method = {

        val (bodySeq, remainingAttributes) = attributes partition { _.isInstanceOf[Code] }
        val theBody =
            if (bodySeq.nonEmpty)
                Some(bodySeq.head.asInstanceOf[Code])
            else
                None
        new Method(
            accessFlags,
            name.intern(),
            descriptor,
            theBody,
            remainingAttributes)
    }

    /**
     * Factory method for Method objects.
     *
     * @example A new method that is public abstract that takes no parameters and
     * returns void and has the name "myMethod" can be created as shown next:
     * {{{
     *  val myMethod = Method(name="myMethod");
     * }}}
     */
    def apply(
        accessFlags: Int = ACC_ABSTRACT.mask | ACC_PUBLIC.mask,
        name: String,
        parameterTypes: IndexedSeq[FieldType] = IndexedSeq.empty,
        returnType: Type = VoidType,
        attributes: Attributes = Seq.empty[Attribute]): Method = {
        Method(accessFlags, name, MethodDescriptor(parameterTypes, returnType), attributes)
    }

    def unapply(method: Method): Option[(Int, String, MethodDescriptor)] =
        Some((method.accessFlags, method.name, method.descriptor))
}
