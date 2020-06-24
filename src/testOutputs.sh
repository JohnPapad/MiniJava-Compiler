#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'         
NC='\033[0m' # No Color

INPUT_FOLDER="./Tests"

#if there are no java files exit
if [ $(find $INPUT_FOLDER -maxdepth 1 -type f -name '*.java' -printf x | wc -c) = 0 ]; then
    echo -e "${RED}-ERROR:${NC} No input files found"
    exit 0
fi

for filename in $INPUT_FOLDER/*.java; do
    echo -----------------------------------------

    purefn=$(basename "$filename" .java)
    echo --- Now testing input file: "$purefn" ---

    # check if the corresponding LLVM file has been generated
    if [ $(find $INPUT_FOLDER -maxdepth 1 -type f -name "$purefn.ll" -printf x | wc -c) = 0 ]; then
        echo -e "${RED}-ERROR:${NC} $purefn.ll not found"
        continue
    fi

    # add the 'public' keyword at the start of the java file (if not present)
    first_word=$(awk '{print $1; exit}' $filename)
    if [ $first_word != "public" ]; then 
        sed -i '1s/^/public /' $filename 
    fi


    echo - Java: Compiling "$purefn.java" ...
    javac "$filename"

    echo - Java: Executing "$purefn.java" ...
    # java "Tests/$purefn" > "$purefn".java.out
    java -cp $INPUT_FOLDER $purefn > "$INPUT_FOLDER/$purefn".java.out || echo Out of bounds > "$INPUT_FOLDER/$purefn".java.out

    # check for execution errors

    echo - LLVM: Compiling "$purefn".ll ...
    clang-4.0 -Wno-override-module -o "$INPUT_FOLDER/$purefn".ll.exe "$INPUT_FOLDER/$purefn".ll
    echo - LLVM: Executing "$purefn".ll ...
    "$INPUT_FOLDER/$purefn".ll.exe > "$INPUT_FOLDER/$purefn".ll.out

    echo "- Comparing the javac's output with the LLVM's one ..."
    cmp --silent "$INPUT_FOLDER/$purefn".java.out "$INPUT_FOLDER/$purefn".ll.out && echo -e "${GREEN}### SUCCESS ###${NC}" || echo -e "${RED}### FAILURE ###${NC}"    
    # rm -f *.class *~
    # rm -f *.out *~
    # rm -f *.exe *~
    
    
done

rm -f "$INPUT_FOLDER/"*.class "$INPUT_FOLDER/"*.exe "$INPUT_FOLDER/"*~


