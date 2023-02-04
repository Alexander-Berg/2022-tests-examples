import pytest


DEFAULT_CONFIG_CONTENT = {
    'main': {
        'service_name': 'maps-core-fake',
    },
    'deploy': {
        'deploy_profile': 'default',
    },
    'deploy_profiles': {'default': {}},
}


def extract_errors(exc_info: pytest.ExceptionInfo) -> list[str]:
    return [error['msg'] for error in exc_info.value.errors()]
