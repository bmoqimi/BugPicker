# Overview
OPAL is an extensible library for analyzing Java bytecode. OPAL is completely written in Scala and leverages Scala's 
advanced language features to provide a new and previously unseen level of customizability and scalability. 
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* in mind. 

# Project Structure
OPAL consists of several projects which are found in the folder OPAL:

* **Common**(OPAL/common): Contains common helper classes.

* **Bytecode Infrastructure**(OPAL/bi): The necessary infrastructure for parsing Java bytecode.  

* **Bytecode Disassembler**(OPAL/da): A Java Bytecode Disassembler that creates a beautiful HTML file of the Java bytecode.

* **Bytecode Representation**(OPAL/br): OPAL's primary representation of Java bytecode. Implements all functionality for creating a representation of Java class files.  

* **Abstract Interpretation Framework**(OPAL/ai): Implementation of an abstract interpretation framework that can be used to easily implement analyses at very different levels of precision. 

* **Dependencies Extraction**(OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Valiation**(OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Demos**(OPAL/demo): Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demo" (`project Demo`).

* **FindREALBugs**: (This project is in its very early stages!) FindBugs reloaded. For further information go to: [FindREALBugs](https://bitbucket.org/delors/opal/wiki/FindREALBugs)

# Further Information #

* [OPAL Project](http://www.opal-project.de)

* [OPAL's Wiki](https://bitbucket.org/delors/opal/wiki/Home)
 