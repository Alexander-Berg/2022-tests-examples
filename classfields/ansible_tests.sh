#!/bin/bash

RETRIES_COUNT=10

run() {
    local _playbooks _playbook _attempt1 _attempt2

    _playbooks=$(ls -a | grep -E "yml|yaml" | grep -vE 'poc.yml|.hubfile.yml')

    for _playbook in ${_playbooks}; do
        for _attempt1 in $(seq ${RETRIES_COUNT}); do
            echo "=== Running check for ${_playbook} playbook, attempt ${_attempt1} ==="
            ansible-playbook ${_playbook} --check && break
        done

        if [ "${?}" -ne 0 ]; then
            echo "=== Failed to check ${_playbook} playbook ==="
            exit 1
        fi
    done
}


cd sreMonitoring;
run

