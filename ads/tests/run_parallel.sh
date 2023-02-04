#!/bin/bash

if [ ! -f gtest_parallel.py ]; then
  echo Getting gtest_parallel.py
  curl -XGET https://raw.githubusercontent.com/google/gtest-parallel/master/gtest_parallel.py -o gtest_parallel.py	
fi

time python gtest_parallel.py -w 16 ./*_tests
