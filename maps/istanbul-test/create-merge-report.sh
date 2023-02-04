#!/bin/sh

dir_name="$HOME/istanbul-test/`date +%Y-%m-%d`"
file_name="jams__`date +%H-%M`.csv"

mkdir -p $dir_name
sed "s/{\"whenGenerated\":\([0-9]\+\).*/\1/" /var/cache/yandex/maps/jams/built_reports/Istanbul/EN/_info.json | xargs -I {} echo "{} / 1000" | bc | xargs -I {} date --rfc-3339='seconds' -d @{} > $dir_name/$file_name
echo -e "\nLength;Speed;Merged Edges" >> $dir_name/$file_name
xsltproc $HOME/istanbul-test/merge-report.xsl /var/cache/yandex/maps/jams/built_reports/Istanbul/EN/merge-view.xml | sort -n -r -t ';' >> $dir_name/$file_name
