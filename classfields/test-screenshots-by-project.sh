#!/usr/bin/env bash

CORE_PROJECTS=(auto-core/react/components/common auto-core/react/components/desktop auto-core/react/components/islands auto-core/react/components/mobile auto-core/react/components/z-index.browser.js)
PROJECTS=(`ls -1 | grep www`)

exit_code=0;

for i in "${CORE_PROJECTS[@]}"
do
  npm run test:jest-screenshots -- --passWithNoTests ${i}/
  test_result=$?
  echo "exit code for $i is $test_result"
  echo "exit code for all for now is $exit_code"
  if [[ $test_result -ne 0 ]]; then
    echo "exit code changed from $exit_code -> $test_result"
    exit_code=$test_result;
  fi
done

for i in "${PROJECTS[@]}"
do
  npm run test:jest-screenshots -- --passWithNoTests ${i}/
  test_result=$?
  echo "exit code for $i is $test_result"
  echo "exit code for all for now is $exit_code"
  if [[ $test_result -ne 0 ]]; then
    echo "exit code changed from $exit_code -> $test_result"
    exit_code=$test_result;
  fi
done

echo "exit code for all is $exit_code"
exit $exit_code;
