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
package ai

/**
 * Encapsulates the result of a computation in a domain. In general, the
 * result is either some value `V` or some exception(s) `E`. In some cases, however,
 * when the domain cannot '''precisely''' determine the result, it may be both: some
 * exceptional value(s) and a value. In the latter case OPAL-AI will generally follow all
 * possible paths. Please note, that a computation that declares to return a result
 * (i.e., `V` is not `Nothing`) must either return a result and/or throw an exception, but
 * is not allowed to return no result and no exceptions!
 *
 * @tparam V The result of the computation. Typically a `DomainValue`.
 *      If the computation is executed for its side
 *      effect (e.g., as in case of a `monitorenter` or `monitorexit` instruction)
 *      the type of `V` maybe `Nothing`.
 * @tparam E The exception(s) that maybe thrown by the computation. Typically,
 *      a `DomainValue` which represents a reference value with type
 *      `java.lang.Throwable` or a subtype thereof. If multiple exceptions may be
 *      thrown it may also be a set of `DomainValue`s.
 *
 * @author Michael Eichberg
 */
sealed trait Computation[+V, +E] {

    /**
     * The return value of the computation (if any); defined if and only if
     * `hasResult` returns true.
     */
    def result: V

    /**
     * Returns `true` if this computation may have a return value, `false` otherwise.
     */
    def hasResult: Boolean

    /**
     * The exception or exceptions when the computation raised an exception;
     * defined if and only if `throwsException` returns `true`.
     *
     * E.g., the invocation of a method may lead to several (checked/unchecked) exceptions.
     */
    def exceptions: E

    /**
     * Returns `true` if this computation ''may have raised an exception''.
     */
    def throwsException: Boolean

    /**
     * Returns `true` if this computation ''may have returned normally'' without
     * throwing an exception.
     */
    def returnsNormally: Boolean
}

/**
 * Factory for `Computation` objects.
 *
 * @author Michael Eichberg
 */
object Computation {

    def apply[E](es: Seq[E]): Computation[Nothing, Seq[E]] = {
        if (es.isEmpty)
            ComputationWithSideEffectOnly
        else
            ComputationWithSideEffectOrException(es)
    }

    def apply[V, E](v: V, es: Seq[E]): Computation[V, Seq[E]] = {
        if (es.isEmpty)
            ComputedValue(v)
        else
            ComputedValueAndException(v, es)
    }
}

/**
 * Encapsulates the result of a computation that returned normally and
 * that did not throw an exception.
 */
final case class ComputedValue[+V](
    result: V)
        extends Computation[V, Nothing] {

    def hasResult: Boolean = true

    def exceptions: Nothing =
        throw new UnsupportedOperationException(
            "the computation succeeded without an exception"
        )

    def throwsException: Boolean = false

    def returnsNormally: Boolean = true
}

/**
 * Encapsulates the result of a computation that either returned normally
 * or threw an exception.
 */
final case class ComputedValueAndException[+V, +E](
    result: V,
    exceptions: E)
        extends Computation[V, E] {

    def hasResult: Boolean = true

    def throwsException: Boolean = true

    def returnsNormally: Boolean = true

}

/**
 * Encapsulates the result of a computation that threw an exception.
 */
final case class ThrowsException[+E](
    exceptions: E)
        extends Computation[Nothing, E] {

    def returnsNormally: Boolean = false

    def hasResult: Boolean = false

    def result: Nothing =
        throw new UnsupportedOperationException(
            "the computation resulted in an exception"
        )

    def throwsException: Boolean = true

}

/**
 * Encapsulates the result of a computation that returned normally (but which
 * did not return some value) or that threw an exception/multiple exceptions.
 */
final case class ComputationWithSideEffectOrException[+E](
    exceptions: E)
        extends Computation[Nothing, E] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = false

    def result: Nothing =
        throw new UnsupportedOperationException(
            "the computation was executed for its side effect only"
        )

    def throwsException: Boolean = true
}

/**
 * Represents a computation that completed normally.
 */
case object ComputationWithSideEffectOnly extends Computation[Nothing, Nothing] {

    def returnsNormally: Boolean = true

    def hasResult: Boolean = false

    def result: Nothing =
        throw new UnsupportedOperationException(
            "the computation was executed for its side effect only"
        )

    def throwsException: Boolean = false

    def exceptions: Nothing =
        throw new UnsupportedOperationException(
            "the computation succeeded without an exception"
        )
}

object ComputationWithResultAndException {

    def unapply[V, E](c: Computation[V, E]): Option[(V, E)] =
        if (c.hasResult && c.throwsException) Some((c.result, c.exceptions)) else None
}

object ComputationWithResult {

    def unapply[V](c: Computation[V, _]): Option[V] =
        if (c.hasResult) Some(c.result) else None
}

object ComputationWithException {

    def unapply[E](c: Computation[_, E]): Option[E] =
        if (c.throwsException) Some(c.exceptions) else None
}