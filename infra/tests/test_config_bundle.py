import copy
import logging

import inject
import mock
import pytest

from awacs.lib.ya_vault import IYaVaultClient
from awacs.model.balancer.transport_config_bundle import ConfigBundle
from infra.awacs.proto import model_pb2
from awtest import check_log


DEFAULT_INSTANCE_SPEC = {
    'instance_spec': {
        'instancectl': {
            'version': '1.172',
        }
    }
}


@pytest.fixture
def instancectl_component():
    instancectl = model_pb2.Component()
    instancectl.meta.type = model_pb2.ComponentMeta.INSTANCECTL
    instancectl.meta.version = '2.3'
    instancectl.spec.source.sandbox_resource.task_type = 'BUILD_INSTANCE_CTL'
    instancectl.spec.source.sandbox_resource.task_id = '701125213'
    instancectl.spec.source.sandbox_resource.resource_type = 'INSTANCECTL'
    instancectl.spec.source.sandbox_resource.resource_id = '1560753547'
    instancectl.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:5b5b5e12d5666df1c2f22c7282a4b76b43739722'
    instancectl.status.status = instancectl.status.PUBLISHED
    return instancectl


@pytest.fixture
def pginx_binary_component():
    pginx_binary = model_pb2.Component()
    pginx_binary.meta.type = model_pb2.ComponentMeta.PGINX_BINARY
    pginx_binary.meta.version = '185-4'
    pginx_binary.spec.source.sandbox_resource.task_type = 'BUILD_BALANCER_BUNDLE'
    pginx_binary.spec.source.sandbox_resource.task_id = '686430061'
    pginx_binary.spec.source.sandbox_resource.resource_type = 'BALANCER_EXECUTABLE'
    pginx_binary.spec.source.sandbox_resource.resource_id = '1529393937'
    pginx_binary.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:71a99b7cc74e49cd06a0a37eb07b9b5bfc53472e'
    pginx_binary.status.status = pginx_binary.status.PUBLISHED
    return pginx_binary


@pytest.fixture
def endpoint_root_certs_component_url():
    endpoint_root_certs = model_pb2.Component()
    endpoint_root_certs.meta.type = model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS
    endpoint_root_certs.meta.version = '0.0.1'
    endpoint_root_certs.spec.source.url_file.url = 'https://crls.yandex.net/allCAs.pem'
    endpoint_root_certs.status.status = endpoint_root_certs.status.PUBLISHED
    return endpoint_root_certs


@pytest.fixture
def endpoint_root_certs_component_sb():
    endpoint_root_certs = model_pb2.Component()
    endpoint_root_certs.meta.type = model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS
    endpoint_root_certs.meta.version = '0.1.0'
    endpoint_root_certs.spec.source.sandbox_resource.task_type = 'MDS_UPLOAD'
    endpoint_root_certs.spec.source.sandbox_resource.task_id = '882719648'
    endpoint_root_certs.spec.source.sandbox_resource.resource_type = 'AWACS_ENDPOINT_ROOT_CERTS'
    endpoint_root_certs.spec.source.sandbox_resource.resource_id = '1960876121'
    endpoint_root_certs.spec.source.sandbox_resource.rbtorrent = 'rbtorrent:52e5fc085af3e9ea68193cc9fc0664039ef2c5ab'
    endpoint_root_certs.status.status = endpoint_root_certs.status.PUBLISHED
    return endpoint_root_certs


class YavClient(object):
    @staticmethod
    def get_token(service_id, secret_id):
        return 'token_{}_{}'.format(service_id, secret_id)

    @staticmethod
    def get_version(version):
        return {
            'yav-s1': {
                'version': 'yav-v1',
                'value':
                    {
                        '5BA3342B00020009C005_private_key': 'c1_private_key_value',
                        '5BA3342B00020009C005_certificate': 'c1_certificate_value',
                    },
            },
            'yav-s2': {
                'version': 'yav-v1',
                'value':
                    {
                        '01AAB5C9194CB1C2AFE170FD441FEB5B_private_key': 'c1_private_key_value',
                        'some_certificate': 'c1_certificate_value',
                    },
            },
            'yav-s3': {
                'version': 'yav-v1',
                'value':
                    {
                        '01AAB5C9194CB1C2AFE170FD441FEB5B_private_key': 'c1_private_key_value',
                        'some_private_key': 'c1_private_key_value',
                        'some_certificate': 'c1_certificate_value',
                    },
            },
        }.get(version)

    @staticmethod
    def add_certs_to_secret(*_, **__):
        return 'v2'

    @staticmethod
    def check_token(token, *_, **__):
        return bool(token)


@pytest.fixture
def yav_client():
    client = YavClient()

    def configure(binder):
        binder.bind(IYaVaultClient, client)

    inject.clear_and_configure(configure)
    yield client
    inject.clear()


@pytest.fixture(autouse=True)
def setup_logging(caplog):
    caplog.set_level(logging.DEBUG)


@pytest.fixture
def config_bundle():
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       component_pbs_to_set={},
                       components_to_remove=set(),
                       ctl_version=0,
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())
    cfg._runtime_attrs_content = copy.deepcopy(DEFAULT_INSTANCE_SPEC)
    return cfg


@pytest.fixture
def yav_cert_spec_pb():
    cert_spec_pb = model_pb2.CertificateSpec()
    cert_spec_pb.storage.type = model_pb2.CertificateSpec.Storage.YA_VAULT
    cert_spec_pb.storage.ya_vault_secret.secret_id = 'yav-s1'
    cert_spec_pb.storage.ya_vault_secret.secret_ver = 'yav-v1'
    cert_spec_pb.fields.serial_number = '432745928323474932744197'
    return cert_spec_pb


@pytest.fixture
def yav_cert_spec_pb_inexact_file_names():
    cert_spec_pb = model_pb2.CertificateSpec()
    cert_spec_pb.storage.type = model_pb2.CertificateSpec.Storage.YA_VAULT
    cert_spec_pb.storage.ya_vault_secret.secret_id = 'yav-s2'
    cert_spec_pb.storage.ya_vault_secret.secret_ver = 'yav-v1'
    cert_spec_pb.fields.serial_number = '2215605510564605947266274012786846555'
    return cert_spec_pb


@pytest.fixture
def yav_cert_spec_pb_duplicate_file_names():
    cert_spec_pb = model_pb2.CertificateSpec()
    cert_spec_pb.storage.type = model_pb2.CertificateSpec.Storage.YA_VAULT
    cert_spec_pb.storage.ya_vault_secret.secret_id = 'yav-s3'
    cert_spec_pb.storage.ya_vault_secret.secret_ver = 'yav-v1'
    cert_spec_pb.fields.serial_number = '2215605510564605947266274012786846555'
    return cert_spec_pb


@pytest.fixture
def nanny_vault_cert_spec_pb():
    cert_spec_pb = model_pb2.CertificateSpec()
    cert_spec_pb.storage.type = model_pb2.CertificateSpec.Storage.NANNY_VAULT
    cert_spec_pb.storage.nanny_vault_secret.keychain_id = 'vault-k1'
    cert_spec_pb.storage.nanny_vault_secret.secret_id = 'vault-s1'
    cert_spec_pb.storage.nanny_vault_secret.secret_revision_id = 'vault-r1'
    return cert_spec_pb


def test_certs_old_instancectl(ctx, config_bundle):
    old_instancectl_instance_spec = {
        'instance_spec': {
            'instancectl': {
                'version': '1.153',
            }
        }
    }
    config_bundle.new_cert_spec_pbs = {'a': 1}
    config_bundle._runtime_attrs_content = copy.deepcopy(old_instancectl_instance_spec)
    with pytest.raises(RuntimeError) as e:
        config_bundle._add_cert_volumes(ctx)
    e.match(r'Instancectl version in service "svc" must be 1\.172\+ to support certificates')


def test_new_yav_cert(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_c1',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'v2',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token_svc_yav-s1',
                            'secretName': 'secrets_c1'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: tarball was updated: added secrets.tgz' in log.records_text()
        assert 'cert n1/c1: acquired new delegation token' in log.records_text()
        assert 'cert n1/c1: created new nanny volume' in log.records_text()


def test_new_yav_cert_inexact_file_names(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb_inexact_file_names):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb_inexact_file_names
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_c1',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'v2',
                            'secretId': 'yav-s2',
                            'delegationToken': 'token_svc_yav-s2',
                            'secretName': 'secrets_c1'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: secret contains files with inexact name matches' in log.records_text()
        assert 'cert n1/c1: tarball was updated: added secrets.tgz' in log.records_text()
        assert 'cert n1/c1: acquired new delegation token' in log.records_text()
        assert 'cert n1/c1: created new nanny volume' in log.records_text()


def test_yav_cert_duplicate_file_names(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb_duplicate_file_names):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb_duplicate_file_names
    }
    with pytest.raises(RuntimeError) as e:
        config_bundle._add_cert_volumes(ctx)
    e.match('Encountered a second file that could match "private key"')


def test_new_nanny_vault_cert(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets_c1',
                    'secretVolume': {
                        'secretName': 'secrets_c1',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: created new nanny volume' in log.records_text()

        assert 'tarball' not in log.records_text()
        assert 'delegation token' not in log.records_text()


def test_existing_yav_volume(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    yav_client.add_certs_to_secret = lambda *args, **kwargs: 'yav-v1'
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_blablabla',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v1',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token',
                            'secretName': 'secrets_blablabla'
                        }
                    },
                    'itsVolume': {
                        "maxRetryPeriodSeconds": 300,
                        "periodSeconds": 60,
                        "itsUrl": "http://its.yandex-team.ru/v1"
                    },
                    'secretVolume': {
                        "keychainSecret": {
                            "keychainId": "",
                            "secretId": "",
                            "secretRevisionId": ""
                        },
                        "secretName": ""
                    },
                    'templateVolume': {
                        "template": []
                    },
                    'version': ""
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = copy.deepcopy(expected_attrs_content)
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: secret_id has not changed, preserving volume name' in log.records_text()
        assert 'cert n1/c1: delegation token is up to date' in log.records_text()
        assert 'cert n1/c1: nanny volume is up to date' in log.records_text()


def test_existing_yav_volume_no_secret_name(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    yav_client.add_certs_to_secret = lambda *args, **kwargs: 'yav-v1'
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_blablabla',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v1',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token',
                            'secretName': ''
                        }
                    },
                    'itsVolume': {
                        "maxRetryPeriodSeconds": 300,
                        "periodSeconds": 60,
                        "itsUrl": "http://its.yandex-team.ru/v1"
                    },
                    'secretVolume': {
                        "keychainSecret": {
                            "keychainId": "",
                            "secretId": "",
                            "secretRevisionId": ""
                        },
                        "secretName": ""
                    },
                    'templateVolume': {
                        "template": []
                    },
                    'version': ""
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = copy.deepcopy(expected_attrs_content)
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: secret_id has not changed, preserving volume name' in log.records_text()
        assert 'cert n1/c1: delegation token is up to date' in log.records_text()
        assert 'cert n1/c1: nanny volume is up to date' in log.records_text()


def test_existing_nanny_vault_volume(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets_blablabla',
                    'secretVolume': {
                        'secretName': 'secrets_blablabla',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = copy.deepcopy(expected_attrs_content)
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: storage type has not changed, preserving volume name' in log.records_text()
        assert 'cert n1/c1: nanny volume is up to date' in log.records_text()

        assert 'delegation token' not in log.records_text()


def test_existing_nanny_vault_volume_no_secret_name(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb):
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets_blablabla',
                    'secretVolume': {
                        'secretName': '',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = copy.deepcopy(expected_attrs_content)
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'New cert ids to add to nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: storage type has not changed, preserving volume name' in log.records_text()
        assert 'cert n1/c1: nanny volume is up to date' in log.records_text()

        assert 'delegation token' not in log.records_text()


def test_cert_renewal(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb, yav_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets',
                    'secretVolume': {
                        'secretName': 'secrets',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_c1',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'v2',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token_svc_yav-s1',
                            'secretName': 'secrets_c1'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Cert ids to maybe modify in nanny service: "n1/c1"' in log.records_text()
        assert 'tarball was updated: added secrets.tgz' in log.records_text()
        assert 'acquired new delegation token' in log.records_text()
        assert 'storage contents have changed, updating volume name' in log.records_text()
        assert 'modified existing nanny volume' in log.records_text()


def test_cert_restore_backup(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb, yav_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_c1',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v1',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token_svc_yav-s1',
                            'secretName': 'secrets_c1'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets_c1',
                    'secretVolume': {
                        'secretName': 'secrets_c1',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Cert ids to maybe modify in nanny service: "n1/c1"' in log.records_text()
        assert 'tarball was updated: added secrets.tgz' not in log.records_text()
        assert 'acquired new delegation token' not in log.records_text()
        assert 'storage contents have changed, updating volume name' in log.records_text()
        assert 'modified existing nanny volume' in log.records_text()


def test_cert_update_yav(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    new_yav_cert_spec_pb = model_pb2.CertificateSpec()
    new_yav_cert_spec_pb.CopyFrom(yav_cert_spec_pb)
    new_yav_cert_spec_pb.storage.ya_vault_secret.secret_ver = 'yav-v8'
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): new_yav_cert_spec_pb
    }
    yav_client.add_certs_to_secret = lambda *args, **kwargs: 'yav-v8'
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_old',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v1',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token',
                            'secretName': 'secrets_old'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_old',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v8',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token',
                            'secretName': 'secrets_old'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Cert ids to maybe modify in nanny service: "n1/c1"' in log.records_text()
        assert 'delegation token is up to date' in log.records_text()
        assert 'secret_id has not changed, preserving volume name' in log.records_text()
        assert 'modified existing nanny volume' in log.records_text()


def test_cert_update_nanny_vault(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    new_yav_cert_spec_pb = model_pb2.CertificateSpec()
    new_yav_cert_spec_pb.CopyFrom(nanny_vault_cert_spec_pb)
    new_yav_cert_spec_pb.storage.nanny_vault_secret.secret_revision_id = 'new_rev'
    config_bundle.new_cert_spec_pbs = {
        ('n1', 'c1'): new_yav_cert_spec_pb
    }
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets',
                    'secretVolume': {
                        'secretName': 'secrets',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets_c1',
                    'secretVolume': {
                        'secretName': 'secrets_c1',
                        'keychainSecret': {
                            'secretRevisionId': 'new_rev',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Cert ids to maybe modify in nanny service: "n1/c1"' in log.records_text()
        assert 'storage contents have changed, updating volume name' in log.records_text()
        assert 'modified existing nanny volume' in log.records_text()

        assert 'delegation token' not in log.records_text()
        assert 'tarball' not in log.records_text()


def test_cert_delete_nanny_vault(yav_client, caplog, ctx, config_bundle, nanny_vault_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): nanny_vault_cert_spec_pb
    }
    config_bundle.new_cert_spec_pbs = {}
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'SECRET',
                    'name': 'secrets',
                    'secretVolume': {
                        'secretName': 'secrets',
                        'keychainSecret': {
                            'secretRevisionId': 'vault-r1',
                            'secretId': 'vault-s1',
                            'keychainId': 'vault-k1',
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Current cert ids to remove from nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: removed nanny volume' in log.records_text()

    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Current cert ids to remove from nanny service: "n1/c1"' in log.records_text()
        assert "cert n1/c1: wanted to remove nanny volume, but it's already deleted" in log.records_text()


def test_cert_delete_yav(yav_client, caplog, ctx, config_bundle, yav_cert_spec_pb):
    config_bundle.current_cert_spec_pbs = {
        ('n1', 'c1'): yav_cert_spec_pb
    }
    config_bundle.new_cert_spec_pbs = {}
    current_attrs_content = {
        'instance_spec': {
            'volume': [
                {
                    'type': 'VAULT_SECRET',
                    'name': 'secrets_c1',
                    'vaultSecretVolume': {
                        'vaultSecret': {
                            'secretVer': 'yav-v1',
                            'secretId': 'yav-s1',
                            'delegationToken': 'token',
                            'secretName': 'secrets_c1'
                        }
                    }
                }
            ],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    expected_attrs_content = {
        'instance_spec': {
            'volume': [],
            'instancectl': {
                'version': '1.172',
            }
        }
    }
    config_bundle._runtime_attrs_content = current_attrs_content
    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Current cert ids to remove from nanny service: "n1/c1"' in log.records_text()
        assert 'cert n1/c1: removed nanny volume' in log.records_text()

    with check_log(caplog) as log:
        config_bundle._add_cert_volumes(ctx)
        assert config_bundle._runtime_attrs_content == expected_attrs_content
        assert 'Current cert ids to remove from nanny service: "n1/c1"' in log.records_text()
        assert "cert n1/c1: wanted to remove nanny volume, but it's already deleted" in log.records_text()


def test_container_spec(ctx):
    container_spec_pb = model_pb2.BalancerContainerSpec()
    tunnel_pb = container_spec_pb.outbound_tunnels.add(id='pumpurum')
    tunnel_pb.mode = tunnel_pb.IP6IP6
    tunnel_pb.remote_ip = '::1'
    tunnel_pb.rules.add(from_ip='::2')

    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=container_spec_pb,
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       component_pbs_to_set={},
                       components_to_remove=set(),
                       ctl_version=0,
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings(
                           service=model_pb2.BalancerSpec.CustomServiceSettings.DZEN
                       ))

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [],
        },
        'instance_spec': {},
    }
    info_attrs_content = {

    }

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )

    expected_spec_content = (
        '{'
        '\n  "outboundTunnels": ['
        '\n    {'
        '\n      "id": "pumpurum", '
        '\n      "mode": "IP6IP6", '
        '\n      "remoteIp": "::1", '
        '\n      "rules": ['
        '\n        {'
        '\n          "fromIp": "::2"'
        '\n        }'
        '\n      ]'
        '\n    }'
        '\n  ], '
        '\n  "requirements": ['
        '\n    {'
        '\n      "name": "shawshank"'
        '\n    }'
        '\n  ]'
        '\n}'
    )
    expected_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                },
                {
                    'local_path': 'awacs-balancer-container-spec.pb.json',
                    'content': expected_spec_content,
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                }
            ],
        },
        'instance_spec': {'network_properties': {'resolv_conf': 'USE_NAT64_LOCAL'}, 'volume': []},
    }
    assert runtime_attrs_content == expected_attrs_content

    cfg.container_spec_pb.outbound_tunnels[0].id = 'lapapam'
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                },
                {
                    'local_path': 'awacs-balancer-container-spec.pb.json',
                    'content': expected_spec_content.replace('pumpurum', 'lapapam'),
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                }
            ],
        },
        'instance_spec': {'network_properties': {'resolv_conf': 'USE_NAT64_LOCAL'}, 'volume': []},
    }
    assert runtime_attrs_content == expected_attrs_content

    del cfg.container_spec_pb.outbound_tunnels[:]
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )

    expected_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                }
            ],
        },
        'instance_spec': {'network_properties': {'resolv_conf': 'USE_NAT64_LOCAL'}, 'volume': []},
    }
    assert runtime_attrs_content == expected_attrs_content


def test_components(ctx, instancectl_component, pginx_binary_component, endpoint_root_certs_component_url):
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       ctl_version=1,
                       component_pbs_to_set={model_pb2.ComponentMeta.INSTANCECTL: instancectl_component,
                                             model_pb2.ComponentMeta.PGINX_BINARY: pginx_binary_component,
                                             model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS: endpoint_root_certs_component_url},
                       components_to_remove={model_pb2.ComponentMeta.GET_WORKERS_PROVIDER,
                                             model_pb2.ComponentMeta.JUGGLER_CHECKS_BUNDLE},
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [],
            'sandbox_files': [{
                'extract_path': '',
                'is_dynamic': False,
                'local_path': 'juggler-check-bundle-rtc-balancers.tar.gz',
                'resource_id': '',
                'resource_type': '',
                'task_id': '',
                'task_type': ''
            }]
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }
        },
    }
    info_attrs_content = {}

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                },
                {
                    'extract_path': '',
                    'is_dynamic': False,
                    'local_path': 'allCAs.pem',
                    'url': 'https://crls.yandex.net/allCAs.pem',
                }
            ],
            'sandbox_files': [{
                'extract_path': '',
                'is_dynamic': False,
                'local_path': 'balancer',
                'resource_id': '1529393937',
                'resource_type': 'BALANCER_EXECUTABLE',
                'task_id': '686430061',
                'task_type': 'BUILD_BALANCER_BUNDLE'
            }]
        },
        'instance_spec': {
            'volume': [],
            'instancectl': {
                'fetchableMeta': {
                    'sandboxResource': {'resourceId': '1560753547',
                                        'resourceType': 'INSTANCECTL',
                                        'taskId': '701125213'
                                        },
                    'type': 'SANDBOX_RESOURCE'
                },
                'url': [u'rbtorrent:5b5b5e12d5666df1c2f22c7282a4b76b43739722'],
                'version': u'2.3'
            }
        },
    }
    assert runtime_attrs_content == expected_attrs_content


def test_instance_tags(ctx):
    instance_tags_pb = model_pb2.InstanceTags()
    instance_tags_pb.ctype = 'test'
    instance_tags_pb.itype = 'balancer'
    instance_tags_pb.prj = 'some_prj'
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       ctl_version=2,
                       component_pbs_to_set={},
                       components_to_remove=set(),
                       instance_tags_pb=instance_tags_pb,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [],
            'sandbox_files': []
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            },
            'volume': []
        },
        'engines': {'engine_type': 'YP_LITE'},
        'instances': {'yp_pod_ids': {'orthogonal_tags': {
            'itype': 'balancer',
            'ctype': 'prod',
            'metaprj': 'balancer',
        }}}
    }
    info_attrs_content = {

    }

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [{'local_path': 'config.lua', 'url': 'http://ya.ru/'}],
            'sandbox_files': []
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            },
            'volume': []
        },
        'engines': {'engine_type': 'YP_LITE'},
        'instances': {'yp_pod_ids': {'orthogonal_tags': {
            'itype': 'balancer',
            'ctype': 'test',
            'prj': 'some_prj',
            'metaprj': 'balancer',
        }}}
    }
    assert runtime_attrs_content == expected_attrs_content


def test_info_attrs_content(ctx, instancectl_component, pginx_binary_component, endpoint_root_certs_component_url):
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       ctl_version=6,
                       component_pbs_to_set={model_pb2.ComponentMeta.INSTANCECTL: instancectl_component,
                                             model_pb2.ComponentMeta.PGINX_BINARY: pginx_binary_component,
                                             model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS: endpoint_root_certs_component_url},
                       components_to_remove={model_pb2.ComponentMeta.GET_WORKERS_PROVIDER,
                                             model_pb2.ComponentMeta.JUGGLER_CHECKS_BUNDLE},
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [],
            'sandbox_files': []
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }
        },
        'instances': {}
    }
    info_attrs_content = {}

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_info_attrs_content = {
        'awacs_managed_settings': {
            'components': {
                'instance_spec': False,
                'instancectl': True,
                'layers': False,
                'sandbox_files': ['balancer', 'allCAs.pem', 'juggler-check-bundle-rtc-balancers.tar.gz',
                                  'dump_json_get_workers_provider.lua', 'config.lua']
            }
        }
    }
    assert info_attrs_content == expected_info_attrs_content


def test_replace_url_file_by_sandbox(ctx, endpoint_root_certs_component_sb):
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       ctl_version=3,
                       component_pbs_to_set={model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS: endpoint_root_certs_component_sb},
                       components_to_remove={},
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [
                {
                    'extract_path': '',
                    'is_dynamic': False,
                    'local_path': 'allCAs.pem',
                    'url': 'https://crls.yandex.net/allCAs.pem',
                }
            ],
            'sandbox_files': []
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }
        },
    }
    info_attrs_content = {}

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                }
            ],
            'sandbox_files': [{
                'extract_path': '',
                'is_dynamic': False,
                'local_path': 'allCAs.pem',
                'resource_id': endpoint_root_certs_component_sb.spec.source.sandbox_resource.resource_id,
                'resource_type': endpoint_root_certs_component_sb.spec.source.sandbox_resource.resource_type,
                'task_id': endpoint_root_certs_component_sb.spec.source.sandbox_resource.task_id,
                'task_type': endpoint_root_certs_component_sb.spec.source.sandbox_resource.task_type
            }]
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }, 'volume': []
        },
    }

    expected_info_attrs_content = {
        'awacs_managed_settings': {
            'components': {
                'instance_spec': False,
                'instancectl': False,
                'layers': False,
                'sandbox_files': ['allCAs.pem']
            }
        }
    }
    assert runtime_attrs_content == expected_runtime_attrs_content
    assert info_attrs_content == expected_info_attrs_content


def test_remove_static_file_when_add_sandbox_component(ctx, endpoint_root_certs_component_sb):
    cfg = ConfigBundle(lua_config='cfg',
                       container_spec_pb=model_pb2.BalancerContainerSpec(),
                       new_cert_spec_pbs={},
                       current_cert_spec_pbs={},
                       ctl_version=3,
                       component_pbs_to_set={model_pb2.ComponentMeta.ENDPOINT_ROOT_CERTS: endpoint_root_certs_component_sb},
                       components_to_remove={},
                       instance_tags_pb=None,
                       service_id='svc',
                       custom_service_settings_pb=model_pb2.BalancerSpec.CustomServiceSettings())

    runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                },
                {
                    'content': 'lalalalalalala',
                    'local_path': 'allCAs.pem'
                }
            ],
            'url_files': [],
            'sandbox_files': []
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }
        },
    }
    info_attrs_content = {}

    gridfs_file = mock.Mock(checksum_url='http://ya.ru/')
    info_attrs_content, runtime_attrs_content = cfg.apply_to_service(
        info_attrs_content=info_attrs_content,
        runtime_attrs_content=runtime_attrs_content,
        gridfs_file=gridfs_file,
        ctx=ctx
    )
    expected_runtime_attrs_content = {
        'resources': {
            'static_files': [
                {
                    'local_path': 'smth.cfg',
                    'content': 'test',
                }
            ],
            'url_files': [
                {
                    'local_path': 'config.lua',
                    'url': 'http://ya.ru/',
                }
            ],
            'sandbox_files': [{
                'extract_path': '',
                'is_dynamic': False,
                'local_path': 'allCAs.pem',
                'resource_id': endpoint_root_certs_component_sb.spec.source.sandbox_resource.resource_id,
                'resource_type': endpoint_root_certs_component_sb.spec.source.sandbox_resource.resource_type,
                'task_id': endpoint_root_certs_component_sb.spec.source.sandbox_resource.task_id,
                'task_type': endpoint_root_certs_component_sb.spec.source.sandbox_resource.task_type
            }]
        },
        'instance_spec': {
            'instancectl': {
                'version': '1.190'
            }, 'volume': []
        },
    }

    expected_info_attrs_content = {
        'awacs_managed_settings': {
            'components': {
                'instance_spec': False,
                'instancectl': False,
                'layers': False,
                'sandbox_files': ['allCAs.pem']
            }
        }
    }
    assert runtime_attrs_content == expected_runtime_attrs_content
    assert info_attrs_content == expected_info_attrs_content
