from infra.reconf.examples.cortesian.builder import TREE, ProxyConfSet


def test_confs_structure():
    expected = {
        'top.example.com:http': {
            'children': {
                'mid-one.example.com:http': {
                    'children': {
                        'back-one.example.com:http': None,
                        'back-two.example.com:http': None
                    }
                },
                'mid-two.example.com:http': {
                    'children': {
                        'back-one.example.com:http': None
                    }
                }
            }
        },
        'top.example.com:https': {
            'children': {
                'mid-one.example.com:https': {
                    'children': {
                        'back-one.example.com:https': None,
                        'back-two.example.com:https': None
                    }
                },
                'mid-two.example.com:https': {
                    'children': {
                        'back-one.example.com:https': None
                    }
                }
            }
        }
    }

    assert expected == ProxyConfSet(TREE)


def test_confs_fully_built():
    expected = {
        'top.example.com:http': {
            'children': {
                'mid-one.example.com:http': {
                    'children': {
                        'back-one.example.com:http': None,
                        'back-two.example.com:http': None
                    },
                    'conn_timeout': 1,
                    'log_file': '/var/log/proxy.log',
                    'log_level': 'INFO',
                    'read_timeout': 4
                },
                'mid-two.example.com:http': {
                    'children': {
                        'back-one.example.com:http': None
                    },
                    'conn_timeout': 1,
                    'log_file': '/var/log/proxy.log',
                    'log_level': 'INFO',
                    'read_timeout': 4
                }
            },
            'conn_timeout': 1,
            'log_file': '/var/log/proxy.log',
            'log_level': 'INFO',
            'read_timeout': 4
        },
        'top.example.com:https': {
            'children': {
                'mid-one.example.com:https': {
                    'children': {
                        'back-one.example.com:https': None,
                        'back-two.example.com:https': None
                    },
                    'conn_timeout': 1,
                    'log_file': '/var/log/proxy.log',
                    'log_level': 'DEBUG',
                    'read_timeout': 4
                },
                'mid-two.example.com:https': {
                    'children': {
                        'back-one.example.com:https': None
                    },
                    'conn_timeout': 1,
                    'log_file': '/var/log/proxy.log',
                    'log_level': 'DEBUG',
                    'read_timeout': 4
                }
            },
            'conn_timeout': 1,
            'log_file': '/var/log/proxy.log',
            'log_level': 'DEBUG',
            'read_timeout': 4
        }
    }

    assert expected == ProxyConfSet(TREE).build()
