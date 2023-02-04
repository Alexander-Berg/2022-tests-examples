#!/bin/bash

# Common monitoring path
common_path="..";

# Include common script settings
source "$common_path/common.sh"

export test_mode=1

if [ $# -ne 3 ]
then
  echo "Usage: $0 <script name> <parameters> <correct answer>"
  echo "Got $# parameters"
  exit
fi

script_name=$1
params=$2
correct_answer=$3

echo_result()
{
    color=$1
    text="$2"
    echo -e "\E[$color;40m$text"
    tput sgr0
}

answer=`echo "$params" | xargs $script_name`
if [ $answer -ne $correct_answer ]
then
  echo_result 31 "WA"
  echo "Script is $script_name, parameters are \"$params\""
  echo "Correct answer is $correct_answer, answer given is $answer"
else
  echo_result 32 "OK"
fi

export test_mode=
