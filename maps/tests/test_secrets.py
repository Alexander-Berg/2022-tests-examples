import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema import ServiceConfig
from maps.infra.sedem.lib.config.schema.secrets import Secret
from maps.infra.sedem.lib.config.schema.tests.shared import DEFAULT_CONFIG_CONTENT, extract_errors


def test_valid_env_secret() -> None:
    secret = Secret.parse_obj({
        'secret_id': 'sec-abc',
        'version': 'ver-xyz',
        'key': 'token',
        'env': 'SOME_TOKEN',
    })

    assert secret == Secret.construct(
        secret_id='sec-abc',
        version='ver-xyz',
        key='token',

        env='SOME_TOKEN',
        self_tvm_id=None,
        dir=None,
    )


def test_valid_tvm_secret() -> None:
    secret = Secret.parse_obj({
        'secret_id': 'sec-abc',
        'version': 'ver-xyz',
        'key': 'tvm_secret',
        'self_tvm_id': 12345,
    })

    assert secret == Secret.construct(
        secret_id='sec-abc',
        version='ver-xyz',
        key='tvm_secret',

        env=None,
        self_tvm_id=12345,
        dir=None,
    )


def test_valid_dir_secret() -> None:
    secret = Secret.parse_obj({
        'secret_id': 'sec-abc',
        'version': 'ver-xyz',
        'dir': 'secrets',
    })

    assert secret == Secret.construct(
        secret_id='sec-abc',
        version='ver-xyz',
        key=None,

        env=None,
        self_tvm_id=None,
        dir='secrets',
    )


def test_invalid_secret_of_all_types() -> None:
    with pytest.raises(ValidationError) as exc:
        Secret.parse_obj({
            'secret_id': 'sec-abc',
            'version': 'ver-xyz',
            'key': 'token',
            'env': 'SOME_TOKEN',
            'self_tvm_id': 12345,
        })

    assert extract_errors(exc) == [
        'one and only one of "dir", "env" or "self_tvm_id" must be defined'
    ]


def test_invalid_dir_secret_with_key() -> None:
    with pytest.raises(ValidationError) as exc:
        Secret.parse_obj({
            'secret_id': 'sec-abc',
            'version': 'ver-xyz',
            'key': 'token',
            'dir': 'secrets',
        })

    assert extract_errors(exc) == [
        '"key" is not allowed when "dir" specified'
    ]


def test_invalid_env_secret_without_key() -> None:
    with pytest.raises(ValidationError) as exc:
        Secret.parse_obj({
            'secret_id': 'sec-abc',
            'version': 'ver-xyz',
            'env': 'SOME_TOKEN',
        })

    assert extract_errors(exc) == [
        '"key" required when "env" / "self_tvm_id" specified'
    ]


def test_invalid_tvm_secret_without_key() -> None:
    with pytest.raises(ValidationError) as exc:
        Secret.parse_obj({
            'secret_id': 'sec-abc',
            'version': 'ver-xyz',
            'self_tvm_id': 12345,
        })

    assert extract_errors(exc) == [
        '"key" required when "env" / "self_tvm_id" specified'
    ]


def test_valid_secrets_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'secrets': {
            'stable': [{
                'secret_id': 'sec-abc',
                'version': 'ver-xyz',
                'key': 'token',
                'env': 'SOME_TOKEN',
            }],
            'testing': [{
                'secret_id': 'sec-def',
                'version': 'ver-hbz',
                'key': 'tvm_secret',
                'self_tvm_id': 42,
            }],
        },
    })

    assert config.secrets == {
        'stable': [Secret(
            secret_id='sec-abc',
            version='ver-xyz',
            key='token',

            env='SOME_TOKEN',
            self_tvm_id=None,
            dir=None,
        )],
        'testing': [Secret(
            secret_id='sec-def',
            version='ver-hbz',
            key='tvm_secret',

            env=None,
            self_tvm_id=42,
            dir=None,
        )],
    }


def test_invalid_secrets_with_two_tvm_ids() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'secrets': {
                'stable': [{
                    'secret_id': 'sec-abc',
                    'version': 'ver-xyz',
                    'key': 'tvm_secret',
                    'self_tvm_id': 42,
                }, {
                    'secret_id': 'sec-def',
                    'version': 'ver-hbz',
                    'key': 'tvm',
                    'self_tvm_id': 84,
                }],
            },
        })

    assert extract_errors(exc) == [
        'more than one "self_tvm_id" in stable'
    ]
