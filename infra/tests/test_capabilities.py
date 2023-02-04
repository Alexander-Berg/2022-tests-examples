import os
import sys
import random

import pytest

from infra.skylib import porto as portotools


full_caps = (
    'CHOWN;DAC_OVERRIDE;DAC_READ_SEARCH;FOWNER;FSETID;KILL;SETGID;SETUID;SETPCAP;'
    'LINUX_IMMUTABLE;NET_BIND_SERVICE;NET_BROADCAST;'
    'NET_ADMIN;NET_RAW;IPC_LOCK;IPC_OWNER;SYS_MODULE;SYS_RAWIO;SYS_CHROOT;SYS_PTRACE;'
    'SYS_PACCT;SYS_ADMIN;SYS_BOOT;SYS_NICE;'
    'SYS_RESOURCE;SYS_TIME;SYS_TTY_CONFIG;MKNOD;LEASE;AUDIT_WRITE;AUDIT_CONTROL;SETFCAP;'
    'MAC_OVERRIDE;MAC_ADMIN;SYSLOG;'
    'WAKE_ALARM;BLOCK_SUSPEND;AUDIT_READ'
)
full_caps_set = set(full_caps.split(';'))

configurations = (
    {
        'parent': {
            'capabilities': '',
            'capabilities_ambient': '',
            'memory_limit': 0,
            'isolate': False,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SETUID', 'SETGID', 'AUDIT_WRITE',
                                          'CHOWN', 'DAC_OVERRIDE', 'FOWNER'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                ),
            },
            {
                'properties': {
                    'capabilities': 'NET_RAW',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': {'NET_RAW'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': {'NET_RAW'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': '',
            'memory_limit': 1 << 30,
            'isolate': False,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SETUID', 'SETGID', 'AUDIT_WRITE',
                                          'CHOWN', 'DAC_OVERRIDE', 'FOWNER'},
                        'expected_ambient_caps': {'NET_RAW'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': '',
            'memory_limit': 0,
            'isolate': True,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SETUID', 'SETGID', 'AUDIT_WRITE', 'CHOWN',
                                          'DAC_OVERRIDE', 'FOWNER', 'SYS_PTRACE', 'KILL'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': '',
            'memory_limit': 0,
            'isolate': False,
            'net': 'none',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SETUID', 'SETGID', 'AUDIT_WRITE', 'CHOWN',
                                          'DAC_OVERRIDE', 'FOWNER', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': '',
            'memory_limit': 1 << 30,
            'isolate': True,
            'net': 'none',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'SETUID', 'SETGID', 'AUDIT_WRITE',
                                          'CHOWN', 'DAC_OVERRIDE', 'FOWNER', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': '',
            'memory_limit': 1 << 30,
            'isolate': True,
            'net': 'none',
            'root': '/tmp',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'SETUID', 'SETGID', 'AUDIT_WRITE',
                                          'CHOWN', 'DAC_OVERRIDE', 'FOWNER', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                        'expected_ambient_caps': {'NET_RAW', 'SYS_PTRACE', 'KILL', 'SETUID', 'SETGID', 'AUDIT_WRITE',
                                                  'CHOWN', 'DAC_OVERRIDE', 'FOWNER', 'NET_ADMIN', 'NET_BIND_SERVICE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 0,
            'isolate': False,
            'root': '/tmp',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': full_caps_set,
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': full_caps_set,
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 0,
            'isolate': False,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 1 << 30,
            'isolate': False,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'IPC_LOCK'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'IPC_LOCK'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 0,
            'isolate': True,
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE',
                                                  'SYS_BOOT', 'KILL', 'SYS_PTRACE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE',
                                                  'SYS_BOOT', 'KILL', 'SYS_PTRACE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 0,
            'isolate': False,
            'net': 'none',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'NET_ADMIN'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'NET_ADMIN'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 1 << 30,
            'isolate': True,
            'net': 'none',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'NET_ADMIN',
                                                  'IPC_LOCK', 'SYS_BOOT', 'KILL', 'SYS_PTRACE'},
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': {'NET_RAW', 'NET_BIND_SERVICE', 'NET_ADMIN',
                                                  'IPC_LOCK', 'SYS_BOOT', 'KILL', 'SYS_PTRACE'},
                    },
                ),
            },
        ),
    },
    {
        'parent': {
            'capabilities': full_caps,
            'memory_limit': 1 << 30,
            'isolate': True,
            'net': 'none',
            'root': '/tmp',
        },
        'children': (
            {
                'properties': {
                    'capabilities': '',
                },
                'runs': (
                    {
                        'user': 'daemon',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'root',
                        'own_user': 'daemon',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                    {
                        'user': 'daemon',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': full_caps_set,
                    },
                    {
                        'user': 'root',
                        'own_user': 'root',
                        'expected_caps': full_caps_set,
                        'expected_ambient_caps': full_caps_set,
                    },
                    {
                        'user': 'nobody',
                        'own_user': 'root',
                        'expected_caps': set(),
                        'expected_ambient_caps': set(),
                    },
                ),
            },
        ),
    },
)


def make_name():
    return 'ut-%s-%s' % (os.getpid(), random.randint(0, sys.maxsize))


@pytest.mark.skipif(os.getuid() != 0, reason='we are not root, cannot inspect containers')
def test_capabilities(portoconn):
    for configuration in configurations:
        parent = portoconn.CreateWeakContainer(make_name())
        try:
            for prop, value in configuration['parent'].items():
                parent.SetProperty(prop, value)
            parent.Start()

            for child in configuration['children']:
                c = portoconn.CreateWeakContainer(parent.name + '/' + make_name())
                for run in child['runs']:
                    for prop, value in child['properties'].items():
                        c.SetProperty(prop, value)

                    portotools.set_capabilities(portoconn, c, run['user'], run['own_user'])
                    caps = set(filter(None, c.GetProperty('capabilities').split(';')))
                    ambient_caps = set(filter(None, c.GetProperty('capabilities_ambient').split(';')))
                    run_info = 'parent = %s\nchild = %s\nrun = %s\ncaps = %s\nambient_caps = %s' % (
                        configuration['parent'], child['properties'], run, caps, ambient_caps
                    )
                    assert caps == run['expected_caps'], run_info
                    assert ambient_caps == run['expected_ambient_caps'], run_info
        finally:
            parent.Destroy()
