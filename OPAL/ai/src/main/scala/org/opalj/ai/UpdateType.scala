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

/**
 * Specifies the type of an update. The type hierarchies of [[Update]] and [[UpdateType]]
 * are aligned and it is possible to conveniently switch between them. Contrary to
 * an `Update` object an `UpdateType` object never has any payload, it just characterizes
 * an update. However, by passing a value to an `UpdateType` the `UpdateType`
 * is turned into a corresponding [[org.opalj.ai.Update]] object.
 *
 * ==Example==
 * {{{
 * val updateType : UpdateType = ...
 * val update : Update = updateType(<someValue>)
 * }}}
 *
 * @author Michael Eichberg
 */
sealed abstract class UpdateType {
    /**
     * Lift this update type to an [[Update]] of the corresponding type which contains
     * the given value.
     */
    def apply[V](value: ⇒ V): Update[V]

    /**
     * Returns `true` if `this` `UpdateType` represents the `NoUpdateType`.
     */
    def noUpdate: Boolean

    /**
     * Returns `true` if `this` `UpdateType` is a [[MetaInformationUpdateType]].
     */
    def isMetaInformationUpdate: Boolean

    /**
     * Merges this `UpdateType` with the given one. That is, it is determined which
     * type is the more qualified one (`NoUpdateType` < `MetaInformationUpdateType` <
     * `StructuralUpdateType`) and that one is returned.
     */
    def &:(updateType: UpdateType): UpdateType

    /**
     * Merges this `UpdateType` with the given `Update` object and returns an `UpdateType`
     * object that characterizes the update.
     */
    def &:(update: Update[_]): UpdateType

}

case object NoUpdateType extends UpdateType {

    override def apply[V](value: ⇒ V): Update[V] = NoUpdate

    override def noUpdate: Boolean = true

    override def isMetaInformationUpdate: Boolean = false

    override def &:(updateType: UpdateType): UpdateType = updateType

    override def &:(update: Update[_]): UpdateType = update.updateType

}

sealed trait MetaInformationUpdateType extends UpdateType {

    override def apply[V](value: ⇒ V): Update[V] = MetaInformationUpdate(value)

    override def noUpdate: Boolean = false

    override def isMetaInformationUpdate: Boolean = true

    override def &:(updateType: UpdateType): UpdateType =
        if (updateType == StructuralUpdateType)
            StructuralUpdateType
        else
            this

    override def &:(update: Update[_]): UpdateType = update.updateType &: this
}
case object MetaInformationUpdateType extends MetaInformationUpdateType

case object StructuralUpdateType extends UpdateType {

    override def apply[V](value: ⇒ V): Update[V] = StructuralUpdate(value)

    override def noUpdate: Boolean = false

    override def isMetaInformationUpdate: Boolean = false

    override def &:(updateType: UpdateType): UpdateType = StructuralUpdateType

    override def &:(update: Update[_]): UpdateType = StructuralUpdateType
}
