#!/bin/bash
for filename in *.java; do
    #sed -i '1s/^/public /' $filename   
    echo --- Now testing input file "$filename" ---

    echo - Java Compiling "$filename" ...
    javac "$filename"
    purefn=$(basename "$filename" .java)

    echo - Java Executing "$purefn" ...
    java "$purefn" > "$purefn".java.out

    echo - LLVM Compiling "$purefn".ll ...
    clang-4.0 -Wno-override-module -o "$purefn".ll.exe "$purefn".ll
    echo - Executing "$purefn".ll ...
    ./"$purefn".ll.exe > "$purefn".ll.out

    cmp --silent "$purefn".java.out "$purefn".ll.out && echo '### SUCCESS ###' || echo '### FAILURE ###'    
    rm -f *.class *~
    rm -f *.out *~
    rm -f *.exe *~
    echo
    echo -----------------------------------------
    
done



