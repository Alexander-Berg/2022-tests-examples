#!/bin/bash

set -xe
git config --global core.sshCommand 'ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no'
DIR=$(dirname "$0")
cd $DIR
./do-all
