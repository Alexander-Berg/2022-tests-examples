#!/usr/bin/env bash

# список файлов на тестирование
files=$(ls *.xml *.xsl | egrep -v '_result\.(xml|xsl)$')
stylesheet=../identify.xsl

for f in $files
do
    filename=$(basename $f)
    name=${filename%.*}
    ext=${filename##*.}
    output=${name}_result\.$ext
    echo "Processing: $f -> $output"

    xsltproc -o $output $stylesheet $f
done

