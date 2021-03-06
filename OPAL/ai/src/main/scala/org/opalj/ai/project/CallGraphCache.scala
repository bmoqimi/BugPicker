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
package project

import br._

/**
 * A '''thread-safe''' cache for information that is associated
 * with a specific `ObjectType` and an additional key (`Contour`). Conceptually, the cache
 * is a `Map` of `Map`s where the keys of the first map are `ObjectType`s and which
 * return values that are maps where the keys are `Contour`s and the values are the
 * stored/cached information.
 *
 * To minimize contention the cache's maps are all preinitialized based on the number of
 * different types that we have seen. This ensure that two
 * threads can always concurrently access the cache (without blocking)
 * if the information is associated with two different `ObjectType`s. If two threads
 * want to access information that is associated with the same `ObjectType` the
 * data-structures try to minimize potential contention. Hence, this is not a general
 * purpose cache. Using this cache is only appropriate if you need/will cache a lot
 * of information that is associated with different object types.
 *
 * '''It is required that the cache object is created before the threads are created
 * that use the cache!'''
 *
 * ==Example Usage==
 * To store the result of the computation of all target methods for a
 * virtual method call (given some declaring class type and a method signature), the
 * cache could be instantiated as follows:
 * {{{
 * val cache = new CallGraphCache[MethodSignature,Iterable[Method]]
 * }}}
 *
 * @note Creating a new cache is comparatively expensive and depends
 *      on the number of `ObjectType`s in a project.
 *
 * @author Michael Eichberg
 */
class CallGraphCache[Contour, Value] {

    // RECALL: scala.collection.concurrent.Map's getOrElseUpdate 
    // 			is – as of Scala 2.11.0 – NOT THREAD SAFE

    import java.util.concurrent.{ ConcurrentHashMap ⇒ CHMap }

    private[this] val baseCache: CHMap[ObjectType, Value] = new CHMap(512)

    def getOrElseUpdate(key: ObjectType)(f: ⇒ Value): Value = {
        // we don't care if we calculate the result multiple times..
        var cachedValue = baseCache.get(key)
        if (cachedValue == null) {
            cachedValue = f
            baseCache.put(key, cachedValue)
        }
        cachedValue
    }

    private[this] val cache: Array[CHMap[Contour, Value]] = {
        // The cache is 5% larger than the number of "seen" ObjectType's to have
        // room for "new ObjectType"s discovered, e.g., by a reflection analysis
        val size = ObjectType.objectTypesCount * 105 / 100
        Array.fill(size)(new CHMap(16))
    }

    // We use the overflow cache to cache values associated with ObjectTypes
    // that are discovered after the project was loaded and for which we have
    // not reserved regular space.
    private[this] val overflowCache: CHMap[ObjectType, CHMap[Contour, Value]] =
        new CHMap(cache.length / 20 /* ~ 5%*/ )

    //    private[this] val cacheHits = new java.util.concurrent.atomic.AtomicInteger(0)
    //    private[this] val cacheUpdates = new java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * If a value is already stored in the cache that value is returned, otherwise
     * `f` is evaluated and the cache is updated accordingly before the value is returned.
     * In some rare cases it may be the case that two or more functions that are associated
     * with the same `declaringClass` and `contour` are evaluated concurrently. In such
     * a case the result of only one function is stored in the cache and will later be
     * returned.
     */
    def getOrElseUpdate(
        key: ObjectType,
        contour: Contour)(
            f: ⇒ Value, syncOnEvaluation: Boolean = true): Value = {

        val typeBasedCache = {
            val id = key.id
            if (id < cache.length) cache(id)
            else {
                val typeBasedCache = overflowCache.get(key)
                if (typeBasedCache == null) {
                    val newCache = new CHMap[Contour, Value](16)
                    val existingCache = overflowCache.putIfAbsent(key, newCache)
                    if (existingCache != null)
                        existingCache
                    else
                        newCache
                } else {
                    typeBasedCache
                }
            }
        }
        val cachedValue = typeBasedCache.get(contour)
        if (cachedValue != null) {
            //            cacheHits.incrementAndGet()
            cachedValue
        } else {
            if (syncOnEvaluation) {
                // we assume that `f` is expensive to compute
                typeBasedCache.synchronized {
                    //                    cacheUpdates.incrementAndGet()
                    val value = f
                    typeBasedCache.put(contour, value)
                    value
                }
            } else {
                //                cacheUpdates.incrementAndGet()
                val value = f
                typeBasedCache.put(contour, value)
                value
            }
        }
    }

    //    lazy val statistics: Map[String, Int] =
    //        Map(
    //            "Cache Hits" -> cacheHits.get,
    //            "Cache Updates" -> cacheUpdates.get,
    //            "Cache Entries" -> (cache.foldLeft(0)(_ + _.size))
    //        )
}

