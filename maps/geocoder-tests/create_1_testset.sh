#!/bin/bash

set -e

testset_directory=testsets
input_directory=/var/lib/yandex/maps/guv/testsets/json/

./join-tests.py \
        $input_directory/russia.json \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:ru_RU \
    | ./add-testset-name.py -n "Russia stream" \
> $testset_directory/1_russia.json
testsets="1_russia.json"

./join-tests.py \
        $input_directory/ukraine.json \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:ru_RU \
    | ./add-testset-name.py -n "Ukraine stream" \
> $testset_directory/1_ukraine.json
testsets=$testsets" 1_ukraine.json"

./join-tests.py \
        $input_directory/turkey.json \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:tr_TR \
    | ./add-testset-name.py -n "Turkey stream" \
> $testset_directory/1_turkey.json
testsets=$testsets" 1_turkey.json"

./join-tests.py \
        $input_directory/france_1_set.json \
        $input_directory/france_2_set.json \
    | ./en-to-fr.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:fr_FR \
    | ./add-testset-name.py -n "Paris Metrix tune set" \
> $testset_directory/1_france.json
testsets=$testsets" 1_france.json"

./join-tests.py \
        $input_directory/euro_usa.json \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-testset-name.py -n "Europe and USA stream" \
> $testset_directory/1_euro_usa.json
testsets=$testsets" 1_euro_usa.json"

# Print statistics:
for f in $testsets; do
    echo $f `./count-tests.py $testset_directory/$f`
done;
cd $testset_directory; ../count-tests.py $testsets

