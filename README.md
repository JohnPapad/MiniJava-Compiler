# MiniJava Compiler

 This project consists of designing and building a compiler for MiniJava, a subset of Java.   
 MiniJava is designed so that its programs can be compiled by a full Java compiler like *javac*.  
 It was developed for the class of ***"Compilers"*** in the Informatics Department.

### Table of Contents


[MiniJava Specifics](#MiniJava)

[LLVM](#LLVM)

[Tools used](#tools)

[How to run](#run)



<a name="MiniJava"/>

## MiniJava Specifics


Here is a partial, textual description of the language. Most of the following are well defined in the [grammar](http://cgi.di.uoa.gr/~thp06/18_19/project_files/minijava-new/minijava.html) or derived from the requirement that each MiniJava program is also a Java program:

- MiniJava is fully object-oriented, like Java. It does not allow global functions, only classes, fields and methods. The basic types are ```int, boolean, int []``` which is an array of int.

- MiniJava supports single inheritance but not interfaces. It does not support function overloading, which means that each method name must be unique. In addition, all methods are inherently polymorphic (i.e., “virtual” in C++ terminology). This means that foo can be defined in a subclass if it has the same return type and argument types (ordered) as in the parent, but it is an error if it exists with other argument types or return type in the parent. Also all methods must have a return type (there are no void methods). Fields in the base and derived class are allowed to have the same names, and are essentially different fields.
    
- All MiniJava methods are *“public”* and all fields *“protected”*. A class method cannot access fields of another class, with the exception of its superclasses. Methods are visible, however. A class's own methods can be called via *“this”*. E.g., ```this.foo(5)``` calls the object's own foo method, ```a.foo(5)``` calls the foo method of object a. Local variables are defined only at the beginning of a method. A name cannot be repeated in local variables (of the same method) and cannot be repeated in fields (of the same class). A local variable x shadows a field x of the surrounding class.

- In MiniJava, constructors and destructors are not defined. The new operator calls a default void constructor. In addition, there are no inner classes and there are no static methods or fields. By exception, the pseudo-static method *“main”* is handled specially in the grammar. A MiniJava program is a file that begins with a special class that contains the main method and specific arguments that are not used. The special class has no fields. After it, other classes are defined that can have fields and methods.
  
- Notably, an A class can contain a field of type B, where B is defined later in the file. But when we have ```class B extends A```, A must be defined before B. As you'll notice in the grammar, MiniJava offers very simple ways to construct expressions and only allows ```<``` comparisons. There are no lists of operations, e.g., ```1 + 2 + 3```, but a method call on one object may be used as an argument for another method call. In terms of logical operators, MiniJava allows the logical and (```&&```) and the logical not (```!```). For int arrays, the assignment and [] operators are allowed, as well as the ```a.length expression```, which returns the size of array a. There are also ```while``` and ```if``` code blocks. The latter are always followed by an ```else```. Finally, the assignment ```A a = new B();``` when B extends A is correct, and the same applies when a method expects a parameter of type A and a B instance is given instead.


<a name="LLVM"/>

## LLVM

Visitors that convert MiniJava code into the intermediate representation used by the *LLVM compiler project* were implemented.    
The *LLVM* language is documented in the [LLVM Language Reference Manual](https://llvm.org/docs/LangRef.html#instruction-reference), although only a subset of the instructions were used.

### Types used

- ```i1``` : a single bit, used for booleans *(practically takes up one byte)*
- ```i8``` : a single byte
- ```i8*``` : similar to a ```char*``` pointer
- ```i32``` : a single integer
- ```i32*``` : a pointer to an integer, can be used to point to an integer array
- *static arrays* : e.g. ```[20 x i8]``` *(a constant array of 20 characters)*


### Instructions used
- **declare** is used for the declaration of external methods. Only a few specific methods *(e.g., calloc, printf)* need to be declared.  
```declare i32 @puts(i8*)*```

- **define** is used for defining our own methods. The return and argument types need to be specified, and the method needs to end with a ```ret``` instruction of the same type.   
```define i32 @main(i32 %argc, i8** argv) {...}```

- **ret** is the return instruction. It is used to return the control flow and a value to the caller of the current function.  
```ret i32 %rv```

- **alloca** is used to allocate space on the stack of the current function for local variables. It returns a pointer to the given type. This space is freed when the method returns.   
```%ptr = alloca i32```

- **store** is used to store a value to a memory location. The parameters are the value to be stored and a pointer to the memory.   
```store i32 %val, i32* %ptr```

- **load** is used to load a value from a memory location. The parameters are the type of the value and a pointer to the memory.   
```%val = load i32, i32* %ptr```

- **call** is used to call a method. The result can be assigned to a register *(LLVM bitcode temporary variables are called "registers").*  
The return type and parameters (with their types) need to be specified.   
```%result = call i8* @calloc(i32 1, i32 %val)```

- **add, and, sub, mul, xor** are used for mathematical operations. The result is the same type as the operands.   
```%sum = add i32 %a, %b```

- **icmp** is used for comparing two operands. ```icmp slt``` for instance does a signed comparison of the operands and will return ```i1 1```, if the first operand is less than the second, otherwise ```i1 0```.   
```%case = icmp slt i32 %a, %b```

- **br** with a ```i1``` operand and two labels will jump to the first label if the ```i1``` is one, and to the second label otherwise.   
```br i1 %case, label %if, label %else```

- **br** with only a single label will jump to that label.   
```br label %goto```

- **label:** declares a label with the given name. The instruction before declaring a label needs to be a ```br``` operation, even if that ```br``` is simply a jump to the label.   
```label123:```

- **bitcast** is used to cast between different pointer types. It takes the value and type to be cast, and the type that it will be cast to.   
```%ptr = bitcast i32* %ptr2 to i8**```

- **getelementptr** is used to get the pointer to an element of an array from a pointer to that array and the index of the element. The result is also a pointer to the type that is passed as the first parameter (*in the case below it's an* ```i8*```). This example is like doing: ```ptr_idx = &ptr[idx]``` in C (you still need to do a ```load``` to get the actual value at that position).    
```%ptr_idx = getelementptr i8, i8* %ptr, i32 %idx```

- **constant** is used to define a *constant*, such as a *string*. The size of the *constant* needs to be declared too. In the example below, the *string* is 12 bytes (```[12 x i8]```). The result is a pointer to the given type.     
(in the example below, ```@.str is a [12 x i8]*```)   
```@.str = constant [12 x i8] c"Hello world\00"```

- **global** is used for declaring *global* variables (something that is needed for creating *V-tables*). Just like ```constant```, the result is a pointer to the given type.    
    ```
    @.vtable = global [2 x i8*] [i8* bitcast (i32 ()* @func1 to i8*), i8* bitcast (i8* (i32, i32*)* @func2 to i8*)]
    ```

- **phi** is used for selecting a value from previous basic blocks, depending on which one was executed before the current block. *Phi* instructions must be the first in a basic block. It takes as arguments a list of pairs. Each pair contains the value to be selected and the predecessor block for that value. This is necessary in single-assignment languages, in places where multiple control-flow paths join, such as *if-else statements*, if one wants to select a value from the different paths. In the context of the project, this is needed for *short-circuiting* and *(&&) expressions*.   
    ```
    br i1 1, label %lb1, label %lb2
    lb1:
        %a = add i32 0, 100
        br label %lb3
    lb2:
        %b = add i32 0, 200
        br label %lb3
    lb3:
        %c = phi i32 [%a, %lb1], [%b, %lb2]
    ```

### V-table

 A virtual table *(V-table)* is essentially a table of function pointers, pointed at by the first 8 bytes of an object. The *V-table* defines an address for each dynamic function the object supports. Consider a function ```foo``` in position 0 and ```bar``` in position 1 of the table *(with actual offset 8)*. If a method is overridden, the overriding version is inserted in the same location of the virtual table as the overridden version. Virtual calls are implemented by finding the address of the function to call through the virtual table. If we wanted to depict this in C, imagine that object ```obj``` is located at location ```x``` and we are calling ```foo``` which is in the 3rd position *(offset 16)* of the *V-table*. The address of the function that is going to be called is in memory location ```(*x) + 16```.
 

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