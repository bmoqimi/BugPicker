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
package ai
package domain

/**
 * A configuration that forces abstract interpretation to always create an exception
 * if it is not possible to deduce that a specific exception will not be thrown.
 *
 * ==Usage==
 * If you need to adapt a setting just override the respective method in your domain
 * or create a class that inherits from [[Configuration]].
 *
 * ==Core Properties==
 *  - Concrete base implementation of the [[Configuration]] trait that can
 *    be used to create a final domain.
 *  - Thread safe.
 *
 * @author Michael Eichberg
 */
trait ThrowAllPotentialExceptionsConfiguration extends Configuration {

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwAllHandledExceptionsOnMethodCall: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMethodCall: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnFieldAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArithmeticExceptions: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwIllegalMonitorStateException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnMonitorAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNullPointerExceptionOnArrayAccess: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArrayIndexOutOfBoundsException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwArrayStoreException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwNegativeArraySizeException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwClassCastException: Boolean = true

    /**
     * @inheritdoc
     *
     * @return `true`
     */
    /*implements(not override!)*/ def throwClassNotFoundException: Boolean = true

}
