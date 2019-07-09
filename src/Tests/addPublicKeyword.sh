#!/bin/bash
for filename in *.java; do
    sed -i '1s/^/public /' $filename 
done