/* License (BSD Style License):
 *  Copyright (c) 2009 - 2013
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.resolved
package analyses
package findbugs_inspired

/**
 *
 * @author Ralf Mitschke
 */
object MS_PKGPROTECT extends (Project ⇒ Iterable[(ClassFile, Field)]) {

    val hashTableType = ObjectType("java/util/Hashtable")

    def isHashTable(t: FieldType) = t == hashTableType

    def isArray(t: FieldType) = t.isArrayType

    def apply(project: Project) = {
        // list of tuples in the form (packageName, FieldEntry)
        val readFieldsFromPackage = BaseAnalyses.readFields(project.classFiles)
            .map(entry ⇒ (entry._1._1.thisClass.packageName, entry._2))
        for (
            classFile ← project.classFiles if (!classFile.isInterfaceDeclaration);
            declaringClass = classFile.thisClass;
            packageName = declaringClass.packageName;
            field @ Field(_, name, fieldType, _) ← classFile.fields if (field.isFinal &&
                field.isStatic &&
                !field.isSynthetic &&
                !field.isVolatile &&
                (field.isPublic || field.isProtected) &&
                (isArray(field.fieldType) || isHashTable(field.fieldType)) &&
                !readFieldsFromPackage.exists(entry ⇒ entry._2 == (declaringClass, name, fieldType) && entry._1 != packageName)
            )
        ) yield {
            (classFile, field)
        }
    }

}