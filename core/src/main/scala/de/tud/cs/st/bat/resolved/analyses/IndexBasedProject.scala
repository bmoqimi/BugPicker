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
package analyses

import util.graphs.{ Node, toDot }

import java.net.URL

/**
 * Primary abstraction of a Java project.
 * 
 * ==Initialization==
 * To create a representation of a project use the companion object's factory method.
 *
 * ==Implementation Details==
 * This class relies on the property that `ObjectType`s are associated with consecutive,
 * unique ids larger than 0 and that a `ClassFile`'s `hashCode` is equivalent to the
 * `id`/`hashCode` of the `ObjectType` it defines.
 *
 * @tparam S The type of the source of the class file. See [[ProjectLike.Source]] for 
 *      details.
 * @param classHierarchy This project's class hierarchy.
 *
 * @author Michael Eichberg
 */
class IndexBasedProject[S: reflect.ClassTag] private (
    val classFilesCount: Int,
    /* The arrays are private to avoid that clients accidentally mutate them! 
       I.e., this class' data structures are indeed mutable, but they are never
       mutated by this class and they are not exposed to clients either. */
    // Mapping between an ObjectType('s id) and the ClassFile object which defines the type
    private[this] val classesMap: Array[ClassFile],
    // Mapping between an ObjectType('s id) and its defining source file 
    private[this] val sourcesMap: Array[S],
    /* By default all classes are considered to belong to the library unless the class 
     * file is available and the class file was not explicitly identified as belonging 
     * to the library. */
    private[this] val libraryTypesMap: Array[Boolean],
    val classHierarchy: ClassHierarchy)
        extends ProjectLike {

    override type Source = S

    private[this] val classFileOfMethod = {
        val lookupTable = new Array[ClassFile](Method.methodsCount)
        foreachClassFile { classFile: ClassFile ⇒
            classFile.methods foreach { method ⇒ lookupTable(method.id) = classFile }
        }
        lookupTable
    }

    private[this] val classFileOfField = {
        val lookupTable = new Array[ClassFile](Field.fieldsCount)
        foreachClassFile { classFile: ClassFile ⇒
            classFile.fields foreach { field ⇒ lookupTable(field.id) = classFile }
        }
        lookupTable
    }

    import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf

    override def classFiles: Iterable[ClassFile] = classesMap.view.filter(_ ne null)

    override def projectClassFiles: Iterable[ClassFile] =
        classesMap.view.filter(cf ⇒ (cf ne null) && !isLibraryType(cf))

    override def libraryClassFiles: Iterable[ClassFile] =
        classesMap.view.filter(cf ⇒ (cf ne null) && isLibraryType(cf))

    override def isLibraryType(classFile: ClassFile): Boolean = {
        libraryTypesMap(classFile.thisType.id)
    }

    override def isLibraryType(objectType: ObjectType): Boolean = {
        libraryTypesMap(objectType.id)
    }

    override def source(objectType: ObjectType): Option[Source] = {
        // It may be the case that – after loading all class files – 
        // additional "ObjectType"s are created by some analysis which
        // will then have higher ids that are larger than the array's size!
        val id = objectType.id
        if (id < sourcesMap.size) Option(sourcesMap(id)) else None
    }

    override def classFile(objectType: ObjectType): Option[ClassFile] = {
        // It may be the case that – after loading all class files – 
        // additional "ObjectType"s are created by some analysis which
        // will then have ids that are larger than the array's size!
        val id = objectType.id
        if (id < classesMap.size) Option(classesMap(id)) else None
    }

    private[this] lazy val methodsMap: Array[Method] = {
        val map = new Array[Method](methodsCount)
        foreachClassFile { classFile ⇒
            classFile.methods foreach { method ⇒ map(method.id) = method }
        }
        map
    }

    /**
     * Returns the method with the specified id. If the id is not valid,
     * if the id is not valid, the result is undetermined.(An exception may be
     * thrown or `null` may be returned.)
     */
    def method(methodID: Int): Method = methodsMap(methodID)

    def classFile(objectTypeID: Int): ClassFile = classesMap(objectTypeID)

    /**
     * Looks up the ClassFile that contains the given field.
     *
     * The complexity of this operation is O(1).
     */
    override def classFile(field: Field): ClassFile = classFileOfField(field.id)

    /**
     * Looks up the ClassFile that contains the given method.
     *
     * The complexity of this operation is O(1).
     */
    override def classFile(method: Method): ClassFile = classFileOfMethod(method.id)

    override def foreachClassFile[U](f: ClassFile ⇒ U): Unit =
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            f(classFile)
        }

    override def forallClassFiles[U](f: ClassFile ⇒ Boolean): Boolean = {
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            if (!f(classFile))
                return false
        }
        true
    }

    override def foreachMethod[U](f: Method ⇒ U): Unit =
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            classFile.methods.foreach(f)
        }

    override def forallMethods[U](f: Method ⇒ Boolean): Boolean = {
        foreachNonNullValueOf(classesMap) { (id, classFile) ⇒
            if (!classFile.methods.forall(f))
                return false
        }
        true
    }

    override def statistics: String = {
        val classFiles = classesMap.filter(_ != null)
        "Project Statistics:"+
            "\n\tClasses: "+classesMap.count(_ != null)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.annotations.size)+
            "\n\tMethods: "+classFiles.foldLeft(0)(_ + _.methods.size)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.methods.foldLeft(0)((c, n) ⇒ c + n.annotations.size + n.parameterAnnotations.size))+
            "\n\tFields: "+classFiles.foldLeft(0)(_ + _.fields.size)+
            " - Annotations: "+classFiles.foldLeft(0)(_ + _.fields.foldLeft(0)((c, n) ⇒ c + n.annotations.size))+
            "\n\tInstructions: "+classFiles.foldLeft(0)(_ + _.methods.filter(_.body.isDefined).foldLeft(0)(_ + _.body.get.instructions.count(_ != null)))
    }

    override def toString: String = {
        val classesAndSources =
            (classesMap.view zip sourcesMap.view).view.filter(_._1 ne null)
        val classDescriptions =
            classesAndSources.map(cs ⇒ cs._1.thisType.toJava+" « "+cs._2.toString+" »")

        "IndexBasedProject( "+classDescriptions.mkString("\n\t", "\n\t", "\n")+")"
    }
}

/**
 * Defines factory methods to create
 * [[de.tud.cs.st.bat.resolved.analyses.IndexBasedProject]]s.
 *
 * @author Michael Eichberg
 */
object IndexBasedProject {

    /**
     * Creates a new IndexBasedProject.
     *
     * @param classFiles The list of class files of this project that are considered
     *    to belong to the application/library that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     * @param libraryClassFiles The list of class files of this project that make up
     *    the libraries used by the project that will be analyzed.
     *    [Thread Safety] The underlying data structure has to support concurrent access.
     */
    def apply[Source: reflect.ClassTag](
        classFiles: Iterable[(ClassFile, Source)],
        libraryClassFiles: Iterable[(ClassFile, Source)] = Iterable.empty): IndexBasedProject[Source] = {

        import concurrent.{ Future, Await, ExecutionContext, future }
        import concurrent.duration.Duration
        import ExecutionContext.Implicits.global

        val classHierarchyFuture: Future[ClassHierarchy] = future {
            ClassHierarchy(
                classFiles.view.map(_._1) ++ libraryClassFiles.view.map(_._1)
            )
        }

        val classes = new Array[ClassFile](ObjectType.objectTypesCount)
        val libraryTypes = {
            // by default all types are considered to be library types
            val libraryTypes = new Array[Boolean](ObjectType.objectTypesCount)
            java.util.Arrays.fill(libraryTypes, true)
            libraryTypes
        }
        val sources = new Array[Source](ObjectType.objectTypesCount)

        var classFilesCount = 0
        def processClassFiles(classFiles: Iterable[(ClassFile, Source)], isLibrary: Boolean): Unit = {
            for ((classFile, source) ← classFiles) {
                classFilesCount += 1
                val id = classFile.thisType.id
                classes(id) = classFile
                sources(id) = source
                libraryTypes(id) = isLibrary
            }
        }
        processClassFiles(classFiles, false)
        processClassFiles(libraryClassFiles, true)

        new IndexBasedProject(
            classFilesCount,
            classes,
            sources,
            libraryTypes,
            Await.result(classHierarchyFuture, Duration.Inf)
        )
    }
}
