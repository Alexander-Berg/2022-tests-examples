#!/bin/bash

# Run functional tests on remote device with filter

ya make -tA -DREMOTE_TEST --test-threads=1 "$@" --test-param testing_mode=station_presmoke
