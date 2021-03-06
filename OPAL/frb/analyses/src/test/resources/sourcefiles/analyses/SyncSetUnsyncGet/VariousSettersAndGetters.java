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
package SyncSetUnsyncGet;

/**
 * A class with several setter/getter pairs. Some don't use synchronized at all, some use
 * synchronized on both setter & getter, and some use synchronized only on the setter, but
 * not the getter.
 * 
 * @author Daniel Klauer
 */
public class VariousSettersAndGetters {

    /**
     * s1 with synchronized setter, but unsynchronized getter (dangerous, this is an issue
     * that FindRealBugs should report)
     */
    private String a = "";

    public synchronized void setA(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }

    /**
     * Same as above, but with different data type.
     */
    private int b;

    public synchronized void setB(int b) {
        this.b = b;
    }

    public int getB() {
        return b;
    }

    /**
     * Here, both setter and getter are synchronized, so there's no issue.
     */
    private int c;

    public synchronized void setC(int c) {
        this.c = c;
    }

    public synchronized int getC() {
        return c;
    }

    /**
     * Here, neither the setter nor the getter is synchronized. This should not be
     * reported.
     */
    private int d;

    public void setD(int d) {
        this.d = d;
    }

    public int getD() {
        return d;
    }

    /**
     * Setter/getter pair where only the getter is synchronized. Currently this won't be
     * reported.
     */
    private int e;

    public void setE(int e) {
        this.e = e;
    }

    public synchronized int getE() {
        return e;
    }
}
