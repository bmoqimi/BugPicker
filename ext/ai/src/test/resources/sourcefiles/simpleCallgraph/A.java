/* License (BSD Style License):
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
package simpleCallgraph;

import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedConstructor;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethod;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethods;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class A implements Base {

    Base b = new B();

    @Override
    @InvokedMethod(receiverType = B.class, name = "string", lineNumber = 65)
    public String string() {
        return b.string();
    }

    @Override
    @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 72)
    @InvokedConstructor(receiverType = B.class, lineNumber = 72)
    public void method() {
        new B().method();
    }

    @Override
    @InvokedMethods({
            @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 80),
            @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 80) })
    public void methodParameter(@SuppressWarnings("hiding") Base b) {
        b.method();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 87),
            @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 88) })
    public void secondMethod() {
        new A().method();
        new B().method();
    }

}