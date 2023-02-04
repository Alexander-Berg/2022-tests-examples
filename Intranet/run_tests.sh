#!/bin/sh
export NO_FORK_TESTS="1"
test_path=$1
shift

split_path=$(echo $test_path |cut -d':' -f1)
file_path=$(echo $split_path | sed -e 's:\.:/:g; s|/py$|\.py|')

echo Running ya make -ttt  -AF "$test_path" --test-filename "$file_path" -DNO_FORK_TESTS "$@" tests
ya make -ttt --test-stdout  -AF "$test_path" --test-filename "$file_path" -DNO_FORK_TESTS "$@" tests
