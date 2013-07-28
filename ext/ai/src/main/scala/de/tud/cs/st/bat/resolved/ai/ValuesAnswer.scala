/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * Encapsulates the answer of the domain w.r.t. the concrete value(s) of
 * a DomainValue (abstraction).
 *
 * @author Michael Eichberg
 */
sealed trait ValuesAnswer[+V] {
    def values: V
}

case class Values[+V](
    values: V)
        extends ValuesAnswer[V]

case object ValuesUnknown extends ValuesAnswer[Nothing] {
    def values: Nothing = AIImplementationError("the values are unknown")
}

private[ai] sealed trait ComputationWithValue[+V] {

    def values: V

}

private[ai] sealed trait ComputationWithException[+E] {

    def exceptions: E

}

/**
 * Encapsulates the result of a computation in a domain. In general, the
 * result is either some value(s) V or some exception(s). In some cases, however,
 * when the domain cannot "precisely" determine the result, it may be both: some
 * exceptional value(s) and a value.
 */
sealed trait Computation[+V, +E] {

    def values: V

    def exceptions: E

}

case class ComputedValue[+V](
    values: V)
        extends Computation[V, Nothing] {

    def exceptions = AIImplementationError("the computation succeeded without an exception")

}

case class ComputedValueAndException[+V, +E](
    values: V,
    exceptions: E)
        extends Computation[V, E] {

}

case class ThrowsException[+E](
    exceptions: E)
        extends Computation[Nothing, E] {

    def values = AIImplementationError("the computation resulted in an exception")

}

case object ComputationWithSideEffectOnly
        extends Computation[Nothing, Nothing] {

    def exceptions = AIImplementationError("the computation succeeded without an exception")

    def values = AIImplementationError("the computation was executed for its side effect only")

}
      


