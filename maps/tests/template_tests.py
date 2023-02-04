import jinja2
import pytest
import subprocess

from unittest.mock import Mock, call

import yatest.common


@pytest.fixture
def account_template() -> str:
    return subprocess.run(
        [
            yatest.common.binary_path('maps/infra/quotateka/tools/yasm_panels/yasm_panels_builder'),
            '--staging=stable',
            'client', 'display'
        ],
        capture_output=True,
        text=True,
        check=True
    ).stdout


@pytest.fixture
def provider_template() -> str:
    return subprocess.run(
        [
            yatest.common.binary_path('maps/infra/quotateka/tools/yasm_panels/yasm_panels_builder'),
            '--staging=stable',
            'provider', 'display'
        ],
        capture_output=True,
        text=True,
        check=True
    ).stdout


@pytest.fixture
def yasm_jinja_env() -> jinja2.Environment:
    jinja_env = jinja2.Environment(
        block_start_string='<%', block_end_string='%>',
        variable_start_string='<<', variable_end_string='>>',
        comment_start_string='<#', comment_end_string='#>',
        extensions=['jinja2.ext.with_',
                    'jinja2.ext.loopcontrols',
                    'jinja2.ext.do'],
        trim_blocks=True,
        lstrip_blocks=True,
        keep_trailing_newline=True
    )
    jinja_env.globals['make_color'] = Mock()
    jinja_env.globals['suggest'] = Mock(**{
        'clear.return_value': '',
        'add_var.return_value': '',
        'set_choice_list.return_value': ''
    })

    mock_layout = Mock(**{'coords.return_value': '"col": 0, "row": 0'})
    jinja_env.globals['create_main_layout'] = Mock(
        return_value=Mock(**{'sub.return_value': mock_layout})
    )
    return jinja_env


@pytest.fixture
def account_fetch_preset() -> Mock:
    return Mock(return_value={
        "identities": [
            {"id": 12345, "name": "stable"},
            {"id": 67890, "name": "кек"}  # NB: cyrillic kek
        ],
        "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af",
        "provider": {
            "abc": "maps-core-xxx",
            "id": "core-xxx",
            "resources": [
                {
                    "id": "general",
                    "endpoints": [{"path": "/one", "cost": 1}, {"path": "/two", "cost": 5}]
                },
                {
                    "id": "heavy",
                    "endpoints": [{"path": "/wat"}]
                }
            ]
        }
    })


@pytest.fixture
def provider_fetch_preset() -> Mock:
    return Mock(return_value={
        "provider": {
            "id" : "core-xxx",
            "abc" : "maps-core-xxx",
            "resources": [
                {
                    "id": "general",
                    "endpoints": [{"cost": 1, "path": "/one"}, {"cost": 2, "path": "/two"}]
                }
            ]
        },
        "clients": [
            {
                "abc": "client-asdf",
                "accounts": [
                    {
                        "slug": "account-aaa",
                        "uuid5": "02b79189-8de2-553a-b70e-dd4f584184af"
                    },
                    {
                        "slug": "account-bbb",
                        "uuid5": "923f207f-cc6a-5cd3-8439-0005ac3468d3"
                    }
                ],
                "identities": [{"tvm": 12345}, {"tvm": 67890}]
            },
            {
                "abc": "client-qwerty",
                "accounts": [
                    {
                        "slug": "account-ttt",
                        "uuid5": "5394bbd2-b158-528a-b9b0-bda6b4d4c9c1"
                    }
                ],
                "identities": [{"tvm": 42}]
            }
        ]
    })


def strip_blank_lines(text: str) -> str:
    return ''.join([
        line for line in text.splitlines(keepends=True) if line.strip()
    ])


def test_account(yasm_jinja_env: jinja2.Environment,
                 account_template: str,
                 account_fetch_preset: Mock) -> None:
    template_context = {
        'abc': 'xyz',
        'account': 'plush',
    }
    yasm_jinja_env.globals['fetch'] = account_fetch_preset

    generated = yasm_jinja_env.from_string(account_template).render(template_context)

    assert account_fetch_preset.call_args_list == [
        call('http://core-quotateka-server.maps.yandex.net/panel/account?client_abc=xyz&account_slug=plush',
             parse='json'),
    ]
    return strip_blank_lines(generated)


def test_account_hide_endpoints(yasm_jinja_env: jinja2.Environment,
                                account_template: str,
                                account_fetch_preset: Mock) -> None:
    template_context = {
        'abc': 'xyz',
        'account': 'plush',
        'endpoints': 'no'
    }
    yasm_jinja_env.globals['fetch'] = account_fetch_preset

    generated = yasm_jinja_env.from_string(account_template).render(template_context)

    assert account_fetch_preset.call_args_list == [
        call('http://core-quotateka-server.maps.yandex.net/panel/account?client_abc=xyz&account_slug=plush',
             parse='json'),
    ]
    return strip_blank_lines(generated)


def test_provider(yasm_jinja_env: jinja2.Environment,
                  provider_template: str,
                  provider_fetch_preset: Mock) -> None:
    template_context = {
        'abc': 'maps-core-xxx'
    }
    yasm_jinja_env.globals['fetch'] = provider_fetch_preset

    generated = yasm_jinja_env.from_string(provider_template).render(template_context)

    assert provider_fetch_preset.call_args_list == [
        call('http://core-quotateka-server.maps.yandex.net/panel/provider?provider_abc=maps-core-xxx',
             parse='json'),
    ]
    return strip_blank_lines(generated)
