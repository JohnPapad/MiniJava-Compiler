#!/bin/bash
for filename in ./Tests/*.java; do
    first_word=$(awk '{print $1; exit}' $filename)
    # echo $first_word
    if [ $first_word != "public" ]; then 
        sed -i '1s/^/public /' $filename 
    fi
done