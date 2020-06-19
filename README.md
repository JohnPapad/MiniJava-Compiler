# MiniJava Compiler



<a name="tools"/>

## Tools used

- [Java](https://www.java.com/en/) 
- [JavaCC *(Java Compiler Compiler)*](https://javacc.github.io/javacc/) : a tool for generating top-down *LL(k)* parsers and lexical analyzers for the Java programming language.
- [JTB *(Java Tree Builder)*](http://compilers.cs.ucla.edu/jtb/) : transforms *javacc* grammars in Java class hierarchies.
- [LLVM](https://llvm.org/) : a collection of modular and reusable compiler and toolchain technologies.
- [Clang](https://clang.llvm.org/) : a C language family frontend for *LLVM*, used for compiling *LLVM* files and producing executable ones.


<a name="run"/>

## How to run

> In Ubuntu Trusty:  
 (You must have **Java** installed on your machine)

- Install **Clang** (*version >= 4.0.0*) on your machine, in order to execute LLVM code:
    ```
    $ sudo apt update && sudo apt install clang-4.0
    ```
- Place your input MiniJava files (*<input_file>.java*) in the */src/Tests* folder manually, or  
  just use the existing MiniJava files that are provided for demo purposes in the *Inputs* folder:  
    ```
    $ make add_inputs
    ```
- Compile the project:  
    ```
    $ make 
    ```
- Run the MiniJava compiler:  
  - Using your own MiniJava files:
    ```
    $ java Main <file1.java> <file2.java> ... <fileN.java>
    ```
  - Using the provided, demo MiniJava files:
    ```
    $ make exec
    ```

- Execute the produced *LLVM* files, in order to see that their output is the same as compiling the input MiniJava files with *javac* and executing it with Java.  
  You can do that automatically with a provided bash script:
    ```
    $ make test_outputs

    > --- Now testing input file: test01 ---
    > - Java: Compiling test01.java ...
    > - Java: Executing test01.java ...
    > - LLVM: Compiling test01.ll ...
    > - LLVM: Executing test01.ll ...
    > - Comparing the javac's output with the LLVM's one ...
    > ### SUCCESS ###
    > -----------------------------------------
    ...
    ...
    ...
    > --- Now testing input file: testN ---
    > - Java: Compiling testN.java ...
    > - Java: Executing testN.java ...
    > - LLVM: Compiling testN.ll ...
    > - LLVM: Executing testN.ll ...
    > - Comparing the javac's output with the LLVM's one ...
    > ### SUCCESS ###
    ```

- *(Optional)* Delete all auto generated files:
    ```
    $ make clean
    ```
- *(Optional)* Delete */src/Tests*  contents:  
    ```
    $ make clean_tests
    ```