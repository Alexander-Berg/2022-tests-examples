#!/usr/bin/env bash


protoc -I. -I/usr/include --python_out=. *.proto
