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
package ai.taint;

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

public class CheckPackageAccess {
	
	public static Class<?> wrapperWithCheck(String n) throws ClassNotFoundException {
		String name = getNameWithCheck(n); 
		return loadIt(name);
	}

	public static Class<?> wrapperWithoutCheck(String n) throws ClassNotFoundException {
		String name = getNameWithoutCheck(n); 
		return loadIt(name);
	}

	@SuppressWarnings("restriction")
	public static String getNameWithCheck(String className) {
		checkPackageAccess(className);
		return className;
	}

	private static String getNameWithoutCheck(String className) {
		return className;
	}

	private static Class<?> loadIt(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public static Class<?> wrapperWithCheck() throws ClassNotFoundException {
		String name = getNameWithCheck(); 
		return loadIt(name);
	}

	public static Class<?> wrapperWithoutCheck() throws ClassNotFoundException {
		String name = getNameWithoutCheck(); 
		return loadIt(name);
	}

	@SuppressWarnings("restriction")
	private static String getNameWithCheck() {
		String className = "foo";  //this is constant, so don't bother
		checkPackageAccess(className);
		return className;
	}

	private static String getNameWithoutCheck() {
		String className = "foo";  //this is constant, so don't bother
		return className;
	}

	
	
}
