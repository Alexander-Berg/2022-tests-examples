import pytest

from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema import load_testing as sedem_load_testing_schema


# == test GenericLoadConfig

@pytest.mark.parametrize('load_generator', ('pandora', 'phantom'))
def test_valid_long_config(load_generator: str) -> None:
    sedem_load_testing_schema.GenericLoadConfig(**{
        'load_profiles': ['line(1, 50000, 20m)'],
        'autostop': [
            'quantile(80, 100, 15)',
            'http(4xx, 1%, 5)',
            'http(5xx, 1%, 5)',
            'net(1xx, 1%, 20)',
        ],
        'instances': 10_000,
        load_generator: {},
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
    })


@pytest.mark.parametrize('load_generator', ('pandora', 'phantom'))
def test_valid_minimal_config(load_generator: str) -> None:
    sedem_load_testing_schema.GenericLoadConfig(**{
        'load_profiles': [],
        'autostop': [],
        load_generator: {},
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
    })


def test_negative_instances() -> None:
    with pytest.raises(ValidationError, match=r'instances\s+ensure this value is greater than or equal to 0'):
        sedem_load_testing_schema.GenericLoadConfig(**{
            'load_profiles': [],
            'autostop': [],
            'pandora': {},
            'instances': -123,
            'ammo': {
                'type': 'raw',
                'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
            },
        })


@pytest.mark.parametrize('missing_field', ('load_profiles', 'autostop', 'ammo',))
def test_missing_field(missing_field: str) -> None:
    config_dict = {
        'load_profiles': [],
        'autostop': [],
        'pandora': {},
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
    }
    del config_dict[missing_field]
    with pytest.raises(ValidationError, match=fr'{missing_field}\s+field required'):
        sedem_load_testing_schema.GenericLoadConfig(**config_dict)


@pytest.mark.parametrize('load_generators', [[], ['pandora', 'phantom']])
def test_wrong_load_generators_count(load_generators: list[str]) -> None:
    config_dict = {
        'load_profiles': [],
        'autostop': [],
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
    } | {
        load_generator: {}
        for load_generator in load_generators
    }
    with pytest.raises(ValidationError, match=r'One and only one of \[.*\] has to be defined.'
                                              r' Got \[.*\] defined instead.'):
        sedem_load_testing_schema.GenericLoadConfig(**config_dict)


@pytest.mark.parametrize('missing_field', ('type', 'url'))
def test_missing_ammo_field(missing_field: str) -> None:
    config_dict = {
        'load_profiles': [],
        'autostop': [],
        'pandora': {},
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
    }
    del config_dict['ammo'][missing_field]
    with pytest.raises(ValidationError, match=fr'{missing_field}\s+field required'):
        sedem_load_testing_schema.GenericLoadConfig(**config_dict)


def test_extra_field() -> None:
    sedem_load_testing_schema.GenericLoadConfig(**{
        'load_profiles': [],
        'autostop': [],
        'pandora': {},
        'ammo': {
            'type': 'raw',
            'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
        },
        'extra_field': 123,
    })


# == test LoadTestingSection

def test_valid_load_testing_section() -> None:
    sedem_load_testing_schema.LoadTestingSection.parse_obj({
        'st_ticket': 'GEOINFRA-0',
        'tests': {
            'abc/def_ghi-jkl mno': {
                'custom_load_config': {
                    'task_template': 'ABC_123',
                }
            }
        }
    })


def test_invalid_load_testing_section() -> None:
    with pytest.raises(ValidationError, match=r'st_ticket\s+field required'):
        sedem_load_testing_schema.LoadTestingSection.parse_obj({
            'tests': {
                'valid abc/def_ghi-jkl mno test_name': {
                    'custom_load_config': {
                        'task_template': 'ABC_123',
                    }
                }
            }
        })
    with pytest.raises(ValidationError, match=r'tests\s+field required'):
        sedem_load_testing_schema.LoadTestingSection.parse_obj({
            'st_ticket': 'GEOINFRA-0',
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestingSection.parse_obj({
            'st_ticket': 'geoinfra-123',
            'tests': {
                'valid abc/def_ghi-jkl mno test_name': {
                    'custom_load_config': {
                        'task_template': 'ABC_123',
                    }
                }
            }
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestingSection.parse_obj({
            'st_ticket': 'GEOINFRA-0',
            'tests': {
                'valid abc/def_ghi-jkl mno': {
                    'valid': False,
                }
            }
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestingSection.parse_obj({
            'st_ticket': 'GEOINFRA-0',
            'tests': {
                'invalid: test_name\n': {
                    'custom_load_config': {
                        'task_template': 'ABC_123',
                    }
                }
            }
        })


# == test CustomLoadConfig

def test_valid_custom_load_config() -> None:
    sedem_load_testing_schema.CustomLoadConfig.parse_obj({
        'task_template': 'SOME_SANDBOX_TASK_1'
    })


def test_invalid_custom_load_config() -> None:
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.CustomLoadConfig.parse_obj({
            'task_template': 'some string'
        })
    with pytest.raises(ValidationError, match=r'task_template\s+field required'):
        sedem_load_testing_schema.CustomLoadConfig.parse_obj({
        })


# == test LoadTestSpec

def test_valid_load_test_spec() -> None:
    sedem_load_testing_schema.LoadTestSpec.parse_obj({
        'generic_load_config': {
            'load_profiles': [],
            'autostop': [],
            'pandora': {},
            'ammo': {
                'type': 'raw',
                'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
            },
        },
    })
    sedem_load_testing_schema.LoadTestSpec.parse_obj({
        'custom_load_config': {
            'task_template': 'ABC_123',
        },
    })
    sedem_load_testing_schema.LoadTestSpec.parse_obj({
        'custom_load_config': {
            'task_template': 'ABC_123',
        },
       'slug': 'maps-core-infra-teapot/imbalance regression_test',
        'st_ticket': 'GEOINFRA-0',
        'sla_rps': 123,
    })
    sedem_load_testing_schema.LoadTestSpec.parse_obj({
        'custom_load_config': {
            'task_template': 'ABC_123',
        },
        'slug': 'some slug',
        'st_ticket': 'GEOINFRA-3000',
        'sla_rps': '0',
    })


def test_no_load_config() -> None:
    error_message = r"One and only one of \['generic_load_config', 'custom_load_config'\] has to be defined." \
                    r" Got \[\] defined instead."
    with pytest.raises(ValidationError, match=error_message):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({})


def test_two_load_configs() -> None:
    error_message = r"One and only one of \['generic_load_config', 'custom_load_config'\] has to be defined." \
                    r" Got \['generic_load_config', 'custom_load_config'\] defined instead."
    with pytest.raises(ValidationError, match=error_message):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'generic_load_config': {
                'load_profiles': [],
                'autostop': [],
                'pandora': {},
                'ammo': {
                    'type': 'raw',
                    'url': 'https://proxy.sandbox.yandex-team.ru/853958422',
                },
            },
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
        })


def test_invalid_load_test_specs() -> None:
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'generic_load_config': {'valid': False},
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'slug': ': ',
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'slug': '\n',
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'slug': '',
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'st_ticket': 'geoinfra-0',
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'sla_rps': -1,
        })
    with pytest.raises(ValidationError):
        sedem_load_testing_schema.LoadTestSpec.parse_obj({
            'custom_load_config': {
                'task_template': 'ABC_123',
            },
            'sla_rps': None,
        })
