#!/bin/bash

if [ $# -ne 3 ]
then
  # First unused "-n" parameter helps to get compatibility with the timetail utility
  echo "Usage: $0 -n <number of seconds> <log path>"
  exit
fi

# Directory where the script has been called
current_path="$(readlink -f $(dirname "$0"))"

# Common monitoring path
common_path="$current_path/.."
# Include common script settings
source $common_path/common.sh

# time in seconds, offset from the last record time
time_interval=$2

log_path=$3


get_seconds()
{
    date=$1
    formatted_date=`echo $date | tr "\/" "-"`
    seconds=$((`date -d "$formatted_date" +%s`))
}

get_date()
{
    line=$1
    date=${line:1:19}
}

get_lines()
{
    number_of_lines=$1
    lines=`tail $log_path --lines $number_of_lines | tr "\n" "$unused_char$"`
}

get_seconds_by_number_of_lines()
{
    number_of_lines=$1
    get_lines $number_of_lines
    get_date "$lines"
    get_seconds "$date"
}

get_seconds_by_number_of_lines 1
seconds_last=$seconds

# number of seconds we are searching
seconds_threshold=$((seconds_last - time_interval))

# find min{K} for which date of (2^K)-th line from tail is less than needed
cur_lines_number=1
lines=
working=1
while [ $working == 1 ]
do
    last_lines=$lines
    ((cur_lines_number*=2))
    get_seconds_by_number_of_lines $cur_lines_number
    if [ ${#lines} == ${#last_lines} ]
    then
      working=0
    fi
    if [ $seconds -lt $seconds_threshold ]
    then
      working=0
    fi
done

# find exact number of lines, starting at which we have the needed date
right_border=$cur_lines_number
left_border=$((right_border/2))

found=0
while [ $found == 0 ]
do
    mid=$(( (left_border + right_border + 1) / 2 ))
    get_seconds_by_number_of_lines $mid
    if [ $seconds -lt $seconds_threshold ]
    then
      right_border=$((mid - 1))
    else
      left_border=$((mid))
    fi
    # echo left=$left_border
    # echo right=$right_border
    # echo "seconds=$seconds, thres=$seconds_threshold"
    if [ $left_border -eq $right_border ]
    then
      found=1
    fi
done
get_seconds_by_number_of_lines $left_border
if [ $seconds -lt $seconds_threshold ]
then
  lines=""
else
  get_lines $left_border
fi

echo $lines | tr "$unused_char" "\n"

