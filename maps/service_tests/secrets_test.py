import copy

import mock

from maps.infra.sedem.cli.modules.vault_secrets_for_nanny import NannySecretsManager


_EXPECTED_SECRET_IDS = (
    "sec-01dm0pj3xxn4gq19h4ze8zf2j8",
    "sec-01dkvdrcatst3qfm8d4e82mh8h",
    "sec-01fe3j730d2jsnhnjgt69w1stp",
    "sec-01d16mfhkr10hzwdnme39cwxqx",
    "sec-maps-core-teapot-testing-tvm",
    "sec-GEOMONITORINGS_TELEGRAM_TOKEN",
)


class MockVaultClient(object):
    def __init__(self, *args, **kwargs):
        pass

    def list_secrets(self, query, query_type):
        assert query_type == "exact"
        assert query in ("GEOMONITORINGS_TELEGRAM_TOKEN", "maps-core-teapot-testing-tvm")
        return [{"uuid": "sec-" + query}]

    def get_secret(self, secret_uuid):
        assert secret_uuid in _EXPECTED_SECRET_IDS
        return {"name": "fake_secret_name-" + secret_uuid}

    def create_token(self, uuid, tvm_client_id, signature):
        assert tvm_client_id == 2002924
        assert uuid in _EXPECTED_SECRET_IDS
        assert signature == "maps_core_fake_srv_testing"
        return "fake-delegation-token-" + signature + uuid, ''


runtime_attrs = {
    "instance_spec": {
        "containers": [
            {
                "env": [
                    {
                        "name": "ENV_SECRET",
                        "valueFrom": {
                            "type": "VAULT_SECRET_ENV",
                            "vaultSecretEnv": {
                                "field": "",
                                "vaultSecret": {
                                    "delegationToken": "delegation-token-delagtion-token-xxxxxx-1111111111-yyyyyyyyyyy",
                                    "secretId": "sec-01d16mfhkr10hzwdnme39cwxqx",
                                    "secretName": "fake_secret_name-sec-maps-core-teapot-testing-tvm",
                                    "secretVer": "ver-01d16mfhm766cxjj7z0hr66gyd"
                                }
                            }
                        }
                    },
                    {
                        'name': 'TVM_ID',
                        'valueFrom': {
                            'literalEnv': {
                                'value': "2008809"
                            },
                            'type': 'LITERAL_ENV'
                        },
                    }
                ]
            }
        ],
        "volume":  [
            {
                "name": "test_user_secret",
                "type": "VAULT_SECRET",
                "vaultSecretVolume": {
                    "vaultSecret": {
                        "delegationToken": "delegation-token-delegation-token-yyyyyyyyyyyyy-22222222222-zz",
                        "secretId": "sec-01dbfkvv0mp0zq20548qbc9gxt",
                        "secretName": "fake_secret_name-sec-maps-core-teapot-user-credentials",
                        "secretVer": "ver-01dp11z81z0rxcgjqgqbx52xfk"
                    }
                },
                "version": ""
            }
        ]
    }
}

runtime_attrs_no_volume = {
    "instance_spec": {
        "containers": [
            {
                "env": [
                    {
                        "name": "ENV_SECRET",
                        "valueFrom": {
                            "type": "VAULT_SECRET_ENV",
                            "vaultSecretEnv": {
                                "field": "",
                                "vaultSecret": {
                                    "delegationToken": "delegation-token-delagtion-token-xxxxxx-1111111111-yyyyyyyyyyy",
                                    "secretId": "sec-01d16mfhkr10hzwdnme39cwxqx",
                                    "secretName": "fake_secret_name-sec-maps-core-teapot-testing-tvm",
                                    "secretVer": "ver-01d16mfhm766cxjj7z0hr66gyd"
                                }
                            }
                        }
                    }
                ]
            }
        ]
    }
}

runtime_attrs_no_env = {
    "instance_spec": {
        "containers": [{}],
        "volume":  [
            {
                "name": "test_user_secret",
                "type": "VAULT_SECRET",
                "vaultSecretVolume": {
                    "vaultSecret": {
                        "delegationToken": "delegation-token-delegation-token-yyyyyyyyyyyyy-22222222222-zz",
                        "secretId": "sec-01dbfkvv0mp0zq20548qbc9gxt",
                        "secretName": "fake_secret_name-sec-maps-core-teapot-user-credentials",
                        "secretVer": "ver-01dp11z81z0rxcgjqgqbx52xfk"
                    }
                },
                "version": ""
            }
        ]
    }
}


runtime_attrs_updated = {
    "instance_spec": {
        "containers": [
            {
                "env": [
                    {
                        'name': 'SECRET_FROM_SUBSERVICE_CONFIG',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'MAPS_ST_MONITOR_TELEGRAM_TOKEN',
                                'vaultSecret': {
                                    'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretId': 'sec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretName': 'fake_secret_name-sec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretVer': 'ver-01dkvdrcb718me5km8hmjn9x3z'
                                }
                            }
                        }
                    },
                    {
                        'name': 'ENV_SECRET',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'the_key',
                                'vaultSecret': {
                                    'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretId': 'sec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretName': 'fake_secret_name-sec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretVer': 'ver-01d16mfhm766cxjj7z0hr66gyd'
                                }
                            }
                        }
                    },
                    {
                        'name': 'TVM_SECRET',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'tvm_secret',
                                'vaultSecret': {
                                    'delegationToken': 'delegation-token-delagtion-token-xxxxxx-1111111111-yyyyyyyyyyy',
                                    'secretId': 'sec-01d16mfhkr10hzwdnme39cwxqx',
                                    'secretName': 'fake_secret_name-sec-01d16mfhkr10hzwdnme39cwxqx',
                                    'secretVer': 'ver-01d16mfhm766cxjj7z0hr66gyd'
                                }
                            }
                        }
                    },
                    {
                        'name': 'TVM_ID',
                        'valueFrom': {
                            'literalEnv': {
                                'value': "2008809"
                            },
                            'type': 'LITERAL_ENV'
                        },
                    }
                ]
            }
        ],
        "volume": [
            {
                "name": "path/to/secret/1",
                "type": "VAULT_SECRET",
                "vaultSecretVolume": {
                    "vaultSecret": {
                        "delegationToken": "fake-delegation-token-maps_core_fake_srv_testingsec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretId": "sec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretName": "fake_secret_name-sec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretVer": "ver-01dqf1hg78md3qndrtgjtawe4v"
                    }
                }
            },
            {
                'name': 'path/to/new/schema/secret/2',
                'type': 'VAULT_SECRET',
                'vaultSecretVolume': {
                    'vaultSecret': {
                        'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretId': 'sec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretName': 'fake_secret_name-sec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretVer': 'ver-01dqf1hg78md3qndrtgjtawe4v'
                    }
                }
            },
        ]
    }
}


runtime_attrs_new = {
    "instance_spec": {
        "containers": [
            {
                "env": [
                    {
                        'name': 'SECRET_FROM_SUBSERVICE_CONFIG',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'MAPS_ST_MONITOR_TELEGRAM_TOKEN',
                                'vaultSecret': {
                                    'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretId': 'sec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretName': 'fake_secret_name-sec-01dkvdrcatst3qfm8d4e82mh8h',
                                    'secretVer': 'ver-01dkvdrcb718me5km8hmjn9x3z'
                                }
                            }
                        }
                    },
                    {
                        'name': 'ENV_SECRET',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'the_key',
                                'vaultSecret': {
                                    'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretId': 'sec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretName': 'fake_secret_name-sec-01fe3j730d2jsnhnjgt69w1stp',
                                    'secretVer': 'ver-01d16mfhm766cxjj7z0hr66gyd'
                                }
                            }
                        }
                    },
                    {
                        'name': 'TVM_SECRET',
                        'valueFrom': {
                            'type': 'VAULT_SECRET_ENV',
                            'vaultSecretEnv': {
                                'field': 'tvm_secret',
                                'vaultSecret': {
                                    'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01d16mfhkr10hzwdnme39cwxqx',
                                    'secretId': 'sec-01d16mfhkr10hzwdnme39cwxqx',
                                    'secretName': 'fake_secret_name-sec-01d16mfhkr10hzwdnme39cwxqx',
                                    'secretVer': 'ver-01d16mfhm766cxjj7z0hr66gyd'
                                }
                            }
                        }
                    },
                    {
                        'name': 'TVM_ID',
                        'valueFrom': {
                            'literalEnv': {
                                'value': "2008809"
                            },
                            'type': 'LITERAL_ENV'
                        },
                    }
                ]
            }
        ],
        "volume": [
            {
                "name": "path/to/secret/1",
                "type": "VAULT_SECRET",
                "vaultSecretVolume": {
                    "vaultSecret": {
                        "delegationToken": "fake-delegation-token-maps_core_fake_srv_testingsec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretId": "sec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretName": "fake_secret_name-sec-01dm0pj3xxn4gq19h4ze8zf2j8",
                        "secretVer": "ver-01dqf1hg78md3qndrtgjtawe4v"
                    }
                }
            },
            {
                'name': 'path/to/new/schema/secret/2',
                'type': 'VAULT_SECRET',
                'vaultSecretVolume': {
                    'vaultSecret': {
                        'delegationToken': 'fake-delegation-token-maps_core_fake_srv_testingsec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretId': 'sec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretName': 'fake_secret_name-sec-01dm0pj3xxn4gq19h4ze8zf2j8',
                        'secretVer': 'ver-01dqf1hg78md3qndrtgjtawe4v'
                    }
                }
            },
        ]
    }
}


def update_runtime_info(runtime_attrs, service, staging, is_new_service):
    NannySecretsManager(service, staging, runtime_attrs, is_new_service).update_runtime_attrs()
    return runtime_attrs


@mock.patch("maps.infra.sedem.cli.modules.api.CreateVaultClient", MockVaultClient)
def test_secrets_setup(service_factory):
    service = service_factory('fake_srv')
    result = update_runtime_info(
        runtime_attrs=copy.deepcopy(runtime_attrs),
        service=service,
        staging="testing",
        is_new_service=False)
    assert runtime_attrs_updated == result


@mock.patch("maps.infra.sedem.cli.modules.api.CreateVaultClient", MockVaultClient)
def test_secrets_new_setup(service_factory):
    service = service_factory('fake_srv')
    result = update_runtime_info(
        runtime_attrs=copy.deepcopy(runtime_attrs),
        service=service,
        staging="testing",
        is_new_service=True)
    assert runtime_attrs_new == result


@mock.patch("maps.infra.sedem.cli.modules.api.CreateVaultClient", MockVaultClient)
def test_secrets_new_no_volume_setup(service_factory):
    service = service_factory('fake_srv')
    result = update_runtime_info(
        runtime_attrs=copy.deepcopy(runtime_attrs_no_volume),
        service=service,
        staging="testing",
        is_new_service=True)
    assert runtime_attrs_new == result


@mock.patch("maps.infra.sedem.cli.modules.api.CreateVaultClient", MockVaultClient)
def test_secrets_new_no_env_setup(service_factory):
    service = service_factory('fake_srv')
    result = update_runtime_info(
        runtime_attrs=copy.deepcopy(runtime_attrs_no_env),
        service=service,
        staging="testing",
        is_new_service=True)
    assert runtime_attrs_new == result
