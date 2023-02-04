#!/bin/bash

set -e

testset_directory=testsets
input_directory=/var/lib/yandex/maps/guv/testsets/json/

./join-tests.py \
        $input_directory/2_countries.json \
        $input_directory/2_capitals.json \
    | ./clear-tests.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:ru_RU \
    | ./add-testset-name.py -n "Countries and capitals" \
> $testset_directory/2_countries_capitals.json
testsets="2_countries_capitals.json"

./join-tests.py \
        $input_directory/2_metro.json \
        $input_directory/2_metro_spb.json \
        $input_directory/2_metro_others.json \
        $input_directory/2_railway.json \
        $input_directory/2_transport_foreign.json \
    | ./clear-tests.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:ru_RU \
    | ./add-testset-name.py -n "Metro, railways, airports" \
> $testset_directory/2_transport.json
testsets=$testsets" 2_transport.json"

./join-tests.py \
        $input_directory/2_cities.json \
        $input_directory/2_city_translete.json \
        $input_directory/2_streets.json \
        $input_directory/2_streets_foreign.json \
        $input_directory/2_spesial_streets.json \
        $input_directory/2_address.json \
    | ./clear-tests.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-request-parameter.py -a lang:ru_RU \
    | ./add-testset-name.py -n "Main citis addresses" \
> $testset_directory/2_cities.json
testsets=$testsets" 2_cities.json"

./join-tests.py \
        $input_directory/2_disputed.json \
    | ./clear-tests.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-testset-name.py -n "Disputed territories" \
> $testset_directory/2_disputed.json
testsets=$testsets" 2_disputed.json"

./join-tests.py \
        $input_directory/2_different.json \
    | ./clear-tests.py \
    | ./add-request-parameter.py -a origin:tester \
    | ./add-testset-name.py -n "Previous blockers" \
> $testset_directory/2_previous_fails.json
testsets=$testsets" 2_previous_fails.json"

# Print statistics:
for f in $testsets; do
    echo $f `./count-tests.py $testset_directory/$f`
done;
cd $testset_directory; ../count-tests.py $testsets

