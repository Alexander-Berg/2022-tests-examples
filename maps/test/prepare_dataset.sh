#!/bin/bash

mkdir data
cd data
wget https://proxy.sandbox.yandex-team.ru/818563958 -O test_dataset.tar.gz
tar -xzf test_dataset.tar.gz
rm -r test_dataset.tar.gz
cd ..
