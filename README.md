# MiniJava Compiler

### Instructions used
- **declare** is used for the declaration of external methods. Only a few specific methods *(e.g., calloc, printf)* need to be declared.  
```declare i32 @puts(i8*)*```

- **define** is used for defining our own methods. The return and argument types need to be specified, and the method needs to end with a *ret* instruction of the same type.   
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

- **icmp** is used for comparing two operands. *icmp slt* for instance does a signed comparison of the operands and will return *i1 1*, if the first operand is less than the second, otherwise *i1 0*.   
```%case = icmp slt i32 %a, %b```

- **br** with a *i1* operand and two labels will jump to the first label if the *i1* is one, and to the second label otherwise.   
```br i1 %case, label %if, label %else```

- **br** with only a single label will jump to that label.   
```br label %goto```

- **label:** declares a label with the given name. The instruction before declaring a label needs to be a br operation, even if that *br* is simply a jump to the label.   
```label123:```

- **bitcast** is used to cast between different pointer types. It takes the value and type to be cast, and the type that it will be cast to.   
```%ptr = bitcast i32* %ptr2 to i8**```

- **getelementptr** is used to get the pointer to an element of an array from a pointer to that array and the index of the element. The result is also a pointer to the type that is passed as the first parameter *(in the case below it's an i8**). This example is like doing: ```ptr_idx = &ptr[idx]``` in C (you still need to do a load to get the actual value at that position).    
```%ptr_idx = getelementptr i8, i8* %ptr, i32 %idx```

- **constant** is used to define a *constant*, such as a *string*. The size of the *constant* needs to be declared too. In the example below, the *string* is 12 bytes *([12 x i8])*. The result is a pointer to the given type.     
(in the example below, ```@.str is a [12 x i8]*```)   
```@.str = constant [12 x i8] c"Hello world\00"```

- **global** is used for declaring *global* variables (something that is needed for creating *V-Tables*). Just like *constant*, the result is a pointer to the given type.    
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