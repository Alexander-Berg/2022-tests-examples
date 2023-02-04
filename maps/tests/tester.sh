#!/bin/bash

# Common monitoring path
common_path="..";

if [ $# -ne 1 ]
then
  echo "Usage: $0 <script test file>"
  exit
fi

script_test=$1
test_performer="./perform_test.sh"

tests=()
answers=()
AddTest()
{
    new_test="$1"
    new_answer=$2
    tests[${#tests[*]}]="$new_test"
    answers[${#answers[*]}]=$new_answer
}

# the script must fill in the 'tests' array
source $script_test

number_of_tests=${#tests[*]}
for (( iTest = 0 ; iTest < number_of_tests; iTest++ ))
do
    test=${tests[iTest]}
    answer=${answers[iTest]}
    echo -e "Perfoming test \"$test\"..."
    $test_performer $script_name "$test" $answer
done
