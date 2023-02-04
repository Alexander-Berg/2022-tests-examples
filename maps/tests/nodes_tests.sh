#!/bin/bash

script_name="../nodes.sh"
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 0" 0
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 100" 0
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 10000" 0
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 10723" 2
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 10724" 1
AddTest "/var/log/yandex/maps/analyzer/tests/nodes_users_1.log 1543143" 1
