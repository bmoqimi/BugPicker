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
package de.tud.cs.st
package bat
package resolved

/**
 * Represents constant values; i.e., values pushed onto the stack by the ldc(2)(_w)
 * instructions or type information required by the instructions to create arrays.
 *
 * @note A `MethodHandle` or ''MethodType'' (i.e., a `MethodDescriptor`) is also
 *      a `ConstantValue`.
 *      
 * @author Michael Eichberg
 */
trait ConstantValue[T >: Nothing] extends BootstrapArgument {

    /**
     * The concrete value.
     */
    def value: T

    /**
     * The type of the concrete value.
     */
    def valueType: Type

    /**
     * A string representation of the concrete value.
     */
    def valueToString: String
	
    def toBoolean: Boolean =
        throw new BATException(this+" cannot be converted to a boolean value")

    def toByte: Byte =
        throw new BATException(this+" cannot be converted to a byte value")

    def toChar: Char =
        throw new BATException(this+" cannot be converted to an char value")

    def toShort: Short =
        throw new BATException(this+" cannot be converted to a short value")

    def toInt: Int =
        throw new BATException(this+" cannot be converted to an int value")

    def toLong: Long =
        throw new BATException(this+" cannot be converted to a long value")

    def toFloat: Float =
        throw new BATException(this+" cannot be converted to a float value")

    def toDouble: Double =
        throw new BATException(this+" cannot be converted to a double value")

    def toUTF8: String =
        throw new BATException(this+" cannot be converted to a String(UTF8) value")
		
	def toReferenceType : ReferenceType =
		throw new BATException(this+" cannot be converted to a reference type") 
}

/**
 * ConstantClass is, e.g., used by `anewarray` and `multianewarray` instructions.
 *
 * A `ConstantClass` attribute is not a `Field` attribute. I.e., it is never used to
 * set the value of a static field.
 */
final case class ConstantClass(value: ReferenceType) extends ConstantValue[ReferenceType] {

    override def valueToString = value.toJava

    override def valueType = ObjectType.Class

	final override def toReferenceType : ReferenceType = value
}

/**
 * Facilitates matching constant values.
 *
 * @author Michael Eichberg
 */
object ConstantValue {

    def unapply[T](constantValue: ConstantValue[T]): Option[(T, Type)] =
        Some((constantValue.value, constantValue.valueType))
}
	
sealed trait ConstantFieldValue[T >: Nothing] extends Attribute with ConstantValue[T] 

final case class ConstantLong(value: Long) extends ConstantFieldValue[Long] {

    override def toLong = value

    override def valueToString = value.toString

    override def valueType = LongType

}

final case class ConstantInteger(value: Int) extends ConstantFieldValue[Int] {

    override def toBoolean = value != 0

    override def toByte = value.toByte

    override def toChar = value.toChar

    override def toShort = value.toShort

    override def toInt = value

    override def valueToString = value.toString

    override def valueType = IntegerType

}

final case class ConstantDouble(value: Double) extends ConstantFieldValue[Double] {

    override def toDouble = value

    override def valueToString = value.toString

    override def valueType = DoubleType

}

final case class ConstantFloat(value: Float) extends ConstantFieldValue[Float] {

    override def toFloat = value

    override def valueToString = value.toString

    override def valueType = FloatType

}

final case class ConstantString(value: String) extends ConstantFieldValue[String] {

    override def toUTF8 = value

    override def valueToString = value.toString

    override def valueType = ObjectType.String

}



