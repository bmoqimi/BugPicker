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
package util

import language.experimental.macros
import reflect.ClassTag
import reflect.macros.blackbox.Context
import reflect.api.Trees
import java.io.InputStream
import java.io.Closeable

/**
 * Defines additional control abstractions.
 *
 * ==Usage==
 * To use the control abstractions, it is sufficient to import them.
 *
 * @example
 * {{{
 *  import ControlAbstractions.repeat
 *  val result = repeat(5) { System.nanoTime }
 * }}}
 *
 *
 * @author Michael Eichberg
 */
object ControlAbstractions {

    /**
     * This function takes a `Closeable` resource and a function `r` that will
     * process the `Closeable` resource.
     * This function takes care of the correct handling of `Closeable` resources.
     * When `r` has finished processing the resource or throws an exception, the
     * resource is closed.
     * 
     * @note If `closable` is `null`, `null` is passed to `r`.
     * 
     * @param closable The `Closeable` resource. 
     * @param r The function that processes the `resource`.
     */
    def process[C <: Closeable, T](closable: C)(r: C ⇒ T): T = {
        // Implementation Note
        // Creating the closeable (I) in the try block doesn't make sense, hence
        // we don't need a by-name parameter.
        try {
            r(closable)
        } finally {
            if (closable != null) closable.close()
        }
    }

    /**
     * This function takes a `Source` and a function `r` that will
     * process the source.
     * This function takes care of the correct handling of resources.
     * When `r` has finished processing the source or throws an exception,
     * the source is closed.
     * 
     * @note If `source` is `null`, `null` is passed to `r`.
     */
    def processSource[C <: scala.io.Source, T](source: C)(r: C ⇒ T): T = {
        try {
            r(source)
        } finally {
            if (source != null) source.close()
        }
    }

    /**
     * Iterates over a given array `a` and calls the given function `f` for
     * each non-null value in the array.
     *
     * '''This is a macro.'''
     */
    def foreachNonNullValueOf[T <: AnyRef](
        a: Array[T])(
            f: (Int, T) ⇒ Unit): Unit = macro ControlAbstractionsImplementation.foreachNonNullValueOf[T]

    /**
     * Evaluates the given expression `f` with type `T` the given number of
     * `times` and stores the result in an `IndexedSeq[T]`.
     *
     * ==Example Usage==
     * {{{
     * val result = repeat(15) {
     *      System.in.read()
     * }
     * }}}
     *
     * '''This is a macro.'''
     *
     * @param times The number of times the expression `f` is evaluated. The `times`
     *      expression is evaluated exactly once.
     * @param f An expression that is evaluated the given number of times unless an
     *      exception is thrown. Hence, even though `f` is not a by-name parameter,
     *      it behaves in the same way.
     * @return The result of the evaluation of the expression `f` the given number of
     *      times stored in an `IndexedSeq`. If `times` is zero an empty sequence is
     *      returned.
     */
    def repeat[T](times: Int)(f: T): IndexedSeq[T] = macro ControlAbstractionsImplementation.repeat[T]
    // OLD IMPLEMENTATION USING HIGHER-ORDER FUNCTIONS
    // (DO NOT DELETE - TO DOCUMENT THE DESIGN DECISION FOR MACROS)
    //        def repeat[T](times: Int)(f: ⇒ T): IndexedSeq[T] = {
    //            val array = new scala.collection.mutable.ArrayBuffer[T](times)
    //            var i = 0
    //            while (i < times) {
    //                array += f
    //                i += 1
    //            }
    //            array
    //        }
    // The macro-based implementation has proven to be approx. 1,3 to 1,4 times faster 
    // when the number of times that we repeat an operation is small (e.g., 1 to 15 times)  
    // (which is very often the case when we read in Java class files)

}

/**
 * Implementation of the macros.
 *
 * @author Michael Eichberg
 */
private object ControlAbstractionsImplementation {

    def foreachNonNullValueOf[T <: AnyRef: c.WeakTypeTag](
        c: Context)(
            a: c.Expr[Array[T]])(
                f: c.Expr[(Int, T) ⇒ Unit]): c.Expr[Unit] = {
        import c.universe._

        reify {
            val array = a.splice // evaluate only once!
            val arrayLength = array.length
            var i = 0
            while (i < arrayLength) {
                val arrayEntry = array(i)
                if (arrayEntry ne null) f.splice(i, arrayEntry)
                i += 1
            }
        }
    }

    def repeat[T: c.WeakTypeTag](
        c: Context)(
            times: c.Expr[Int])(
                f: c.Expr[T]): c.Expr[IndexedSeq[T]] = {
        import c.universe._

        reify {
            val size = times.splice // => times is evaluated only once
            if (size == 0) {
                IndexedSeq.empty
            } else {
                val array = new scala.collection.mutable.ArrayBuffer[T](size)
                var i = 0
                while (i < size) {
                    val value = f.splice // => we evaluate f the given number of times
                    array += value
                    i += 1
                }
                array
            }
        }
    }
}
