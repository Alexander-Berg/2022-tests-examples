#!/bin/bash

function logex() {
    printf "${1}"; echo
}

sleep 1

logex "Hello, I am test script!"
logex "Confirm master switch? [y/N]"
read confirm

if [ "${confirm}" = "y" ] || [ "${confirm}" = "Y" ]; then
    logex "Some string"
    sleep 1
    logex "Master switched to test-host"
fi

exit 0