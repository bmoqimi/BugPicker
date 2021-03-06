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

/**
 * @author Michael Eichberg
 */
trait SignatureElement {
    def accept[T](sv: SignatureVisitor[T]): T
}

trait ReturnTypeSignature extends SignatureElement {
    // EMPTY
}

trait TypeSignature extends ReturnTypeSignature {
    // EMPTY
}

sealed trait ThrowsSignature extends SignatureElement {
    // EMPTY
}

/**
 * An attribute-level signature as defined in the JVM specification.
 */
sealed trait Signature extends SignatureElement with Attribute

case class ClassSignature(
    formalTypeParameters: Option[List[FormalTypeParameter]],
    superClassSignature: ClassTypeSignature,
    superInterfacesSignature: List[ClassTypeSignature])
        extends Signature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = ClassSignature.KindId

}
object ClassSignature {

    final val KindId = 12

}

case class MethodTypeSignature(
    formalTypeParameters: Option[List[FormalTypeParameter]],
    parametersTypeSignatures: List[TypeSignature],
    returnTypeSignature: ReturnTypeSignature,
    throwsSignature: List[ThrowsSignature])
        extends Signature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = MethodTypeSignature.KindId

}
object MethodTypeSignature {

    final val KindId = 13

}

trait FieldTypeSignature extends Signature with TypeSignature

case class ArrayTypeSignature(
    typeSignature: TypeSignature)
        extends FieldTypeSignature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = ArrayTypeSignature.KindId

}
object ArrayTypeSignature {

    final val KindId = 14

}

case class ClassTypeSignature(
    packageIdentifier: Option[String],
    simpleClassTypeSignature: SimpleClassTypeSignature,
    classTypeSignatureSuffix: List[SimpleClassTypeSignature])
        extends FieldTypeSignature
        with ThrowsSignature {

    def objectType: ObjectType = {
        val className = new java.lang.StringBuilder(packageIdentifier.getOrElse(""))
        className.append(simpleClassTypeSignature.simpleName)
        classTypeSignatureSuffix foreach { scts ⇒
            className.append('$')
            className.append(scts.simpleName)
        }

        ObjectType(className.toString)
    }

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = ClassTypeSignature.KindId

}
object ClassTypeSignature {

    final val KindId = 15

}

case class TypeVariableSignature(
    identifier: String)
        extends FieldTypeSignature
        with ThrowsSignature {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)

    override def kindId: Int = TypeVariableSignature.KindId

}
object TypeVariableSignature {

    final val KindId = 16

}

case class SimpleClassTypeSignature(
        simpleName: String,
        typeArguments: Option[List[TypeArgument]]) {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

case class FormalTypeParameter(
        identifier: String,
        classBound: Option[FieldTypeSignature],
        interfaceBound: Option[FieldTypeSignature]) {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

sealed trait TypeArgument extends SignatureElement

case class ProperTypeArgument(
    varianceIndicator: Option[VarianceIndicator],
    fieldTypeSignature: FieldTypeSignature)
        extends TypeArgument {

    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}

/**
 * Indicates a TypeArgument's variance.
 */
sealed trait VarianceIndicator extends SignatureElement {
    // EMPTY
}
/**
 * If you have a declaration such as &lt;? extends Entry&gt; then the "? extends" part
 * is represented by the `CovariantIndicator`.
 */
sealed trait CovariantIndicator extends VarianceIndicator {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
case object CovariantIndicator extends CovariantIndicator

/**
 * A declaration such as <? super Entry> is represented in class file signatures
 * by the ContravariantIndicator ("? super") and a FieldTypeSignature.
 */
sealed trait ContravariantIndicator extends VarianceIndicator {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
case object ContravariantIndicator extends ContravariantIndicator

/**
 * If a type argument is not further specified (e.g. List<?> l = …) then the
 * type argument "?" is represented by this object.
 */
sealed trait Wildcard extends TypeArgument {
    def accept[T](sv: SignatureVisitor[T]) = sv.visit(this)
}
case object Wildcard extends Wildcard

/**
 * Facilitates matching the `ObjectType` that is defined by a `ClassTypeSignature`.
 * Ignores all further type parameters.
 */
object BasicClassTypeSignature {
    def unapply(cts: ClassTypeSignature): Option[ObjectType] = {
        Some(cts.objectType)
    }
}

/**
 * Facilitates matching fields with generic types.
 *
 * @example
 * {{{
 *  val f : Field = ...
 *  f.fieldTypeSignature match {
 *      case GenericContainer(ContainerType,ElementType) => ...
 *      case _ => ...
 *  }
 * }}}
 *
 * @author Michael Eichberg
 */
object GenericContainer { // matches : List<Object>
    def unapply(cts: ClassTypeSignature): Option[(ObjectType, ObjectType)] = {
        cts match {
            case ClassTypeSignature(
                Some(cpn),
                SimpleClassTypeSignature(
                    csn,
                    Some(List(ProperTypeArgument(None, BasicClassTypeSignature(tp))))),
                List()
                ) ⇒
                Some((ObjectType(cpn + csn), tp))
            case _ ⇒
                None
        }
    }
}
