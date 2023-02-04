#!/bin/sh -ex

if [ -n "$LLVM_PROFILE_FILE" ]; then
    chmod 777 `dirname $LLVM_PROFILE_FILE`
fi
