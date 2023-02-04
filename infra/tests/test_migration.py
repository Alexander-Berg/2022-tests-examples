from __future__ import absolute_import

from skycore.kernel_util.unittest import TestCase, main
from skycore.kernel_util.sys.dirut import TempDir
from skycore.migratetools import fixup_state
from skycore.framework.utils import Path

from cStringIO import StringIO
import yaml


pre_ancient_state = """
infra:
  namespace: infra
  rundir: /Berkanavt/supervisor/var/skycore/var/infra
  services:
  - cfg:
      api: null
      basepath: /Berkanavt/supervisor/var/skycore/namespaces/infra/d5d8b9d3d58c0c46488e721adc166f0a
      cgroup: null
      check: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"
        check'
      conf_action: 10
      conf_format: yaml
      conf_sections:
      - netmon.agent
      env: {}
      executables:
      - '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      install_as_privileged: true
      install_script: ./fixcap --path ./netmon-agent
      limits: {}
      max_check_interval: 600.0
      name: netmonagent
      porto: auto
      porto_container: null
      porto_options:
        cpu_guarantee: 0.2c
        cpu_limit: 1c
        isolate: true
        memory_guarantee: 128MB
        memory_limit: 384MB
      requirements: []
      restart_on_upgrade: true
      stop: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}" stop'
      uninstall_script: null
      user: nobody
      version: 1
    meta:
      filename: yandex-netmon-agent.3065357.tar.gz
      md5: d5d8b9d3d58c0c46488e721adc166f0a
      size: 18779136
      svn_url: null
      version: '[d5d8b9d3] juggler/netmon/agent/bundle/agent.json (342703716)'
    procs:
    - raw_args: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      root_container: skycore/infra/netmonagent
      stderr_offset: 213
      stdout_offset: 0
      type: porto
      uuid: 2fb1297a-90f2-30a1-b477-0be6807cfb1a
    registry_state:
      netmon.agent: 748869c0e5919d02cda001463dada2ee1a4a4802
    required_state: RUNNING
    service: netmonagent
    state: RUNNING
  workdir: /Berkanavt/supervisor/var/skycore/namespaces/infra
skynet:
  namespace: skynet
  rundir: /Berkanavt/supervisor/var/skycore/var/skynet
  services:
  - cfg:
      aliases: {}
      api: null
      basepath: /Berkanavt/supervisor/var/skycore/namespaces/skynet/0cb42203688a5be08f52b6390305dd00
      cgroup: null
      check: null
      conf_action: 2
      conf_format: yaml
      conf_sections:
      - skynet.services.portoshell
      env:
        PYTHONPATH: ${SUPERVISOR}:${SKYNET}
      executables:
      - '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port} -s
        ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: portoshell
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      scripts: {}
      stop: 2
      uninstall_script: null
      user: root
      version: 3
    meta:
      filename: portoshell.2.7.2.tar.gz
      md5: 0cb42203688a5be08f52b6390305dd00
      size: 19621888
      svn_url: arcadia:/arc/trunk/arcadia
      version: '[0cb42203] portoshell-2.7.2 (337797000)'
    procs:
    - pid: 51584
      raw_args: '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port}
        -s ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      rundir: /Berkanavt/supervisor/var/skycore/var
      tags:
      - service
      - portoshell
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: portoshell
    state: RUNNING
  - cfg:
      api:
        python:
          args:
          - ${CURDIR}
          - ${RUNDIR}
          call: true
          import_paths:
          - ${CURDIR}/lib
          module: ya.skynet.services.heartbeatclient.api
          object: make_connection
          requires:
          - skynet-heartbeat-client-service
      basepath: /Berkanavt/supervisor/var/skycore/namespaces/skynet/f96b1392231cbf0f322ebc538084a3fe
      cgroup: null
      check: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/check" "${NAMESPACE}"'
      conf_action: null
      conf_format: yaml
      conf_sections:
      - skynet.services.heartbeat-client
      env:
        PYTHONPATH: ${CURDIR}/lib:${env:PYTHONPATH}
        SKYNET_PROCMANUSER: nobody
      executables:
      - '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize" "-C" "AppPath=${CURDIR}"
        "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: heartbeat-client
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      stop: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/stop" "${NAMESPACE}"'
      uninstall_script: null
      user: root
      version: 1
    meta:
      filename: heartbeat-client-1.1.tgz
      md5: f96b1392231cbf0f322ebc538084a3fe
      size: 156672
      svn_url: svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/skynet/release-16.4@3043746
      version: '[f96b1392] heartbeat-client-1.1.tgz (heartbeat-client 1.1) (329820331)'
    procs:
    - pid: 52011
      raw_args: '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize"
        "-C" "AppPath=${CURDIR}" "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      rundir: /Berkanavt/supervisor/var/skycore/var
      tags:
      - service
      - heartbeat-client
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: heartbeat-client
    state: RUNNING
  workdir: /Berkanavt/supervisor/var/skycore/namespaces/skynet
""".strip()

ancient_state = """
infra:
  namespace: infra
  rundir: /Berkanavt/supervisor/var/skycore/var/infra
  services:
  - cfg:
      api: null
      basepath: /Berkanavt/supervisor/var/skycore/ns/infra/d5d8b9d3d58c0c46488e721adc166f0a
      cgroup: null
      check: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"
        check'
      conf_action: 10
      conf_format: yaml
      conf_sections:
      - netmon.agent
      env: {}
      executables:
      - '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      install_as_privileged: true
      install_script: ./fixcap --path ./netmon-agent
      limits: {}
      max_check_interval: 600.0
      name: netmonagent
      porto: auto
      porto_container: null
      porto_options:
        cpu_guarantee: 0.2c
        cpu_limit: 1c
        isolate: true
        memory_guarantee: 128MB
        memory_limit: 384MB
      requirements: []
      restart_on_upgrade: true
      stop: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}" stop'
      uninstall_script: null
      user: nobody
      version: 1
    meta:
      filename: yandex-netmon-agent.3065357.tar.gz
      md5: d5d8b9d3d58c0c46488e721adc166f0a
      size: 18779136
      svn_url: null
      version: '[d5d8b9d3] juggler/netmon/agent/bundle/agent.json (342703716)'
    procs:
    - raw_args: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      root_container: skycore/infra/netmonagent
      stderr_offset: 213
      stdout_offset: 0
      type: porto
      uuid: 2fb1297a-90f2-30a1-b477-0be6807cfb1a
    registry_state:
      netmon.agent: 748869c0e5919d02cda001463dada2ee1a4a4802
    required_state: RUNNING
    service: netmonagent
    state: RUNNING
  workdir: /Berkanavt/supervisor/var/skycore/ns/infra
skynet:
  namespace: skynet
  rundir: /Berkanavt/supervisor/var/skycore/var/skynet
  services:
  - cfg:
      aliases: {}
      api: null
      basepath: /Berkanavt/supervisor/var/skycore/ns/skynet/0cb42203688a5be08f52b6390305dd00
      cgroup: null
      check: null
      conf_action: 2
      conf_format: yaml
      conf_sections:
      - skynet.services.portoshell
      env:
        PYTHONPATH: ${SUPERVISOR}:${SKYNET}
      executables:
      - '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port} -s
        ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: portoshell
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      scripts: {}
      stop: 2
      uninstall_script: null
      user: root
      version: 3
    meta:
      filename: portoshell.2.7.2.tar.gz
      md5: 0cb42203688a5be08f52b6390305dd00
      size: 19621888
      svn_url: arcadia:/arc/trunk/arcadia
      version: '[0cb42203] portoshell-2.7.2 (337797000)'
    procs:
    - pid: 51584
      raw_args: '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port}
        -s ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      rundir: /Berkanavt/supervisor/var/skycore/var
      tags:
      - service
      - portoshell
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: portoshell
    state: RUNNING
  - cfg:
      api:
        python:
          args:
          - ${CURDIR}
          - ${RUNDIR}
          call: true
          import_paths:
          - ${CURDIR}/lib
          module: ya.skynet.services.heartbeatclient.api
          object: make_connection
          requires:
          - skynet-heartbeat-client-service
      basepath: /Berkanavt/supervisor/var/skycore/ns/skynet/f96b1392231cbf0f322ebc538084a3fe
      cgroup: null
      check: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/check" "${NAMESPACE}"'
      conf_action: null
      conf_format: yaml
      conf_sections:
      - skynet.services.heartbeat-client
      env:
        PYTHONPATH: ${CURDIR}/lib:${env:PYTHONPATH}
        SKYNET_PROCMANUSER: nobody
      executables:
      - '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize" "-C" "AppPath=${CURDIR}"
        "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: heartbeat-client
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      stop: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/stop" "${NAMESPACE}"'
      uninstall_script: null
      user: root
      version: 1
    meta:
      filename: heartbeat-client-1.1.tgz
      md5: f96b1392231cbf0f322ebc538084a3fe
      size: 156672
      svn_url: svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/skynet/release-16.4@3043746
      version: '[f96b1392] heartbeat-client-1.1.tgz (heartbeat-client 1.1) (329820331)'
    procs:
    - pid: 52011
      raw_args: '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize"
        "-C" "AppPath=${CURDIR}" "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      rundir: /Berkanavt/supervisor/var/skycore/var
      tags:
      - service
      - heartbeat-client
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: heartbeat-client
    state: RUNNING
  workdir: /Berkanavt/supervisor/var/skycore/ns/skynet
""".strip()

old_state = """
infra:
  namespace: infra
  rundir: /Berkanavt/supervisor/skycore/var/infra
  services:
  - cfg:
      api: null
      basepath: /Berkanavt/supervisor/skycore/ns/infra/d5d8b9d3d58c0c46488e721adc166f0a
      cgroup: null
      check: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"
        check'
      conf_action: 10
      conf_format: yaml
      conf_sections:
      - netmon.agent
      env: {}
      executables:
      - '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      install_as_privileged: true
      install_script: ./fixcap --path ./netmon-agent
      limits: {}
      max_check_interval: 600.0
      name: netmonagent
      porto: auto
      porto_container: null
      porto_options:
        cpu_guarantee: 0.2c
        cpu_limit: 1c
        isolate: true
        memory_guarantee: 128MB
        memory_limit: 384MB
      requirements: []
      restart_on_upgrade: true
      stop: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}" stop'
      uninstall_script: null
      user: nobody
      version: 1
    meta:
      filename: yandex-netmon-agent.3065357.tar.gz
      md5: d5d8b9d3d58c0c46488e721adc166f0a
      size: 18779136
      svn_url: null
      version: '[d5d8b9d3] juggler/netmon/agent/bundle/agent.json (342703716)'
    procs:
    - raw_args: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      root_container: skycore/infra/netmonagent
      stderr_offset: 213
      stdout_offset: 0
      type: porto
      uuid: 2fb1297a-90f2-30a1-b477-0be6807cfb1a
    registry_state:
      netmon.agent: 748869c0e5919d02cda001463dada2ee1a4a4802
    required_state: RUNNING
    service: netmonagent
    state: RUNNING
  workdir: /Berkanavt/supervisor/skycore/ns/infra
skynet:
  namespace: skynet
  rundir: /Berkanavt/supervisor/skycore/var/skynet
  services:
  - cfg:
      aliases: {}
      api: null
      basepath: /Berkanavt/supervisor/skycore/ns/skynet/0cb42203688a5be08f52b6390305dd00
      cgroup: null
      check: null
      conf_action: 2
      conf_format: yaml
      conf_sections:
      - skynet.services.portoshell
      env:
        PYTHONPATH: ${SUPERVISOR}:${SKYNET}
      executables:
      - '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port} -s
        ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: portoshell
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      scripts: {}
      stop: 2
      uninstall_script: null
      user: root
      version: 3
    meta:
      filename: portoshell.2.7.2.tar.gz
      md5: 0cb42203688a5be08f52b6390305dd00
      size: 19621888
      svn_url: arcadia:/arc/trunk/arcadia
      version: '[0cb42203] portoshell-2.7.2 (337797000)'
    procs:
    - pid: 51584
      raw_args: '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port}
        -s ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      rundir: /Berkanavt/supervisor/skycore/var
      tags:
      - service
      - portoshell
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: portoshell
    state: RUNNING
  - cfg:
      api:
        python:
          args:
          - ${CURDIR}
          - ${RUNDIR}
          call: true
          import_paths:
          - ${CURDIR}/lib
          module: ya.skynet.services.heartbeatclient.api
          object: make_connection
          requires:
          - skynet-heartbeat-client-service
      basepath: /Berkanavt/supervisor/skycore/ns/skynet/f96b1392231cbf0f322ebc538084a3fe
      cgroup: null
      check: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/check" "${NAMESPACE}"'
      conf_action: null
      conf_format: yaml
      conf_sections:
      - skynet.services.heartbeat-client
      env:
        PYTHONPATH: ${CURDIR}/lib:${env:PYTHONPATH}
        SKYNET_PROCMANUSER: nobody
      executables:
      - '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize" "-C" "AppPath=${CURDIR}"
        "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: heartbeat-client
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      stop: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/stop" "${NAMESPACE}"'
      uninstall_script: null
      user: root
      version: 1
    meta:
      filename: heartbeat-client-1.1.tgz
      md5: f96b1392231cbf0f322ebc538084a3fe
      size: 156672
      svn_url: svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/skynet/release-16.4@3043746
      version: '[f96b1392] heartbeat-client-1.1.tgz (heartbeat-client 1.1) (329820331)'
    procs:
    - pid: 52011
      raw_args: '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize"
        "-C" "AppPath=${CURDIR}" "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      rundir: /Berkanavt/supervisor/skycore/var
      tags:
      - service
      - heartbeat-client
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: heartbeat-client
    state: RUNNING
  workdir: /Berkanavt/supervisor/skycore/ns/skynet
""".strip()


new_state = """
infra:
  namespace: infra
  rundir: var/infra
  services:
  - cfg:
      api: null
      basepath: ns/infra/d5d8b9d3d58c0c46488e721adc166f0a
      cgroup: null
      check: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"
        check'
      conf_action: 10
      conf_format: yaml
      conf_sections:
      - netmon.agent
      env: {}
      executables:
      - '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      install_as_privileged: true
      install_script: ./fixcap --path ./netmon-agent
      limits: {}
      max_check_interval: 600.0
      name: netmonagent
      porto: auto
      porto_container: null
      porto_options:
        cpu_guarantee: 0.2c
        cpu_limit: 1c
        isolate: true
        memory_guarantee: 128MB
        memory_limit: 384MB
      requirements: []
      restart_on_upgrade: true
      stop: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir "${RUNDIR}/"
        --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}" stop'
      uninstall_script: null
      user: nobody
      version: 1
    meta:
      filename: yandex-netmon-agent.3065357.tar.gz
      md5: d5d8b9d3d58c0c46488e721adc166f0a
      size: 18779136
      svn_url: null
      version: '[d5d8b9d3] juggler/netmon/agent/bundle/agent.json (342703716)'
    procs:
    - raw_args: '"${CURDIR}/netmon-agent" --pid-path "${RUNDIR}/agent.pid" --var-dir
        "${RUNDIR}/" --log-path "${RUNDIR}/agent.log" --config-path "${RUNDIR}/${CFGFILE}"'
      root_container: skycore/infra/netmonagent
      stderr_offset: 213
      stdout_offset: 0
      type: porto
      uuid: 2fb1297a-90f2-30a1-b477-0be6807cfb1a
    registry_state:
      netmon.agent: 748869c0e5919d02cda001463dada2ee1a4a4802
    required_state: RUNNING
    service: netmonagent
    state: RUNNING
  workdir: ns/infra
skynet:
  namespace: skynet
  rundir: var/skynet
  services:
  - cfg:
      aliases: {}
      api: null
      basepath: ns/skynet/0cb42203688a5be08f52b6390305dd00
      cgroup: null
      check: null
      conf_action: 2
      conf_format: yaml
      conf_sections:
      - skynet.services.portoshell
      env:
        PYTHONPATH: ${SUPERVISOR}:${SKYNET}
      executables:
      - '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port} -s
        ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: portoshell
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      scripts: {}
      stop: 2
      uninstall_script: null
      user: root
      version: 3
    meta:
      filename: portoshell.2.7.2.tar.gz
      md5: 0cb42203688a5be08f52b6390305dd00
      size: 19621888
      svn_url: arcadia:/arc/trunk/arcadia
      version: '[0cb42203] portoshell-2.7.2 (337797000)'
    procs:
    - pid: 51584
      raw_args: '"${CURDIR}/portoshell" -p ${cfg:skynet.services.portoshell:server.port}
        -s ${cfg:skynet.services.portoshell:server.sshport} -t portoshell-utils.1.0.tar.gz
        --pidfile "${RUNDIR}/portoshell.pid"'
      rundir: var
      tags:
      - service
      - portoshell
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: portoshell
    state: RUNNING
  - cfg:
      api:
        python:
          args:
          - ${CURDIR}
          - ${RUNDIR}
          call: true
          import_paths:
          - ${CURDIR}/lib
          module: ya.skynet.services.heartbeatclient.api
          object: make_connection
          requires:
          - skynet-heartbeat-client-service
      basepath: ns/skynet/f96b1392231cbf0f322ebc538084a3fe
      cgroup: null
      check: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/check" "${NAMESPACE}"'
      conf_action: null
      conf_format: yaml
      conf_sections:
      - skynet.services.heartbeat-client
      env:
        PYTHONPATH: ${CURDIR}/lib:${env:PYTHONPATH}
        SKYNET_PROCMANUSER: nobody
      executables:
      - '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize" "-C" "AppPath=${CURDIR}"
        "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      install_as_privileged: false
      install_script: null
      limits: null
      max_check_interval: 600.0
      name: heartbeat-client
      porto: false
      porto_container: null
      porto_options: {}
      requirements: []
      restart_on_upgrade: true
      stop: '"${SKYNET_PYTHON}" "-sBtt" "${CURDIR}/bin/stop" "${NAMESPACE}"'
      uninstall_script: null
      user: root
      version: 1
    meta:
      filename: heartbeat-client-1.1.tgz
      md5: f96b1392231cbf0f322ebc538084a3fe
      size: 156672
      svn_url: svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/skynet/release-16.4@3043746
      version: '[f96b1392] heartbeat-client-1.1.tgz (heartbeat-client 1.1) (329820331)'
    procs:
    - pid: 52011
      raw_args: '"${SKYNET_PYTHON}" ''-sBtt'' "${CURDIR}/bin/daemon" "--daemonize"
        "-C" "AppPath=${CURDIR}" "-C" "WorkdirPath=${RUNDIR}" "-C" "SupervisorPath=${SUPERVISOR}"'
      rundir: var
      tags:
      - service
      - heartbeat-client
      - '@skynet'
      type: liner
      uuid: 05724fd0-02b8-7e4a-1441-3684e132fdfb
    registry_state:
      skynet.services.portoshell: 1e26d046d1bd3c2df8b7cc03abca584f069f082a
    required_state: RUNNING
    service: heartbeat-client
    state: RUNNING
  workdir: ns/skynet
""".strip()


class TestMigration(TestCase):
    def test_fixup_state(self):
        io = StringIO(new_state)
        correct_state = yaml.load(io, Loader=getattr(yaml, 'CSafeLoader', yaml.SafeLoader))

        self.maxDiff = None

        with TempDir() as td:
            tempdir = Path(td)
            tempdir.join('var', 'skynet').ensure(dir=1)
            tempdir.join('var', 'infra').ensure(dir=1)
            tempdir.join('ns', 'skynet', '0cb42203688a5be08f52b6390305dd00').ensure(dir=1)
            tempdir.join('ns', 'skynet', 'f96b1392231cbf0f322ebc538084a3fe').ensure(dir=1)
            tempdir.join('ns', 'infra', 'd5d8b9d3d58c0c46488e721adc166f0a').ensure(dir=1)

            for first_state in (pre_ancient_state, ancient_state, old_state, new_state):
                io = StringIO(first_state)
                state = yaml.load(io, Loader=getattr(yaml, 'CSafeLoader', yaml.SafeLoader))
                fixup_state(td, state)

                self.assertEqual(state, correct_state)


if __name__ == '__main__':
    main()
