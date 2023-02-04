import logging
from typing import Callable

import pytest
from click.testing import CliRunner

from itsman.cli import listing, close, restore, main


@pytest.fixture
def mock_get_token(monkeypatch):
    monkeypatch.setattr('itsman.client.get_token', lambda: 'xxxx')


@pytest.fixture
def run():
    runner = CliRunner()

    def run_(cmd: Callable, args: str, **kwargs):
        result = runner.invoke(cmd, args, **kwargs)  # noqa
        if result.exception:
            raise result.exception
        return result

    return run_


@pytest.fixture
def responses_basic(datafix_read):
    return [
        'GET https://its.yandex-team.ru/v2/l7/heavy/?group_id=billing -> 200:'
        f'{datafix_read("listing.json")}',

        'GET https://its.yandex-team.ru/v2/l7/heavy/dwh-test.yandex-team.ru/weights/values/ -> 200:'
        f'{datafix_read("weights.json")}'
    ]


@pytest.fixture
def responses_update(responses_basic, datafix_read):

    return responses_basic + [
        'POST https://its.yandex-team.ru/v2/l7/heavy/dwh-test.yandex-team.ru/weights/values/ -> 200:'
        f'{datafix_read("weights.json")}',

        'POST https://its.yandex-team.ru/v2/l7/heavy/dwh-test.yandex-team.ru/weights/its_value/ -> 200:{}'
    ]


def test_main():

    with pytest.raises(SystemExit):
        main()


def test_listing(response_mock, run, responses_basic, mock_get_token):

    with response_mock(responses_basic, bypass=False):
        result = run(listing, 'billing --alias dwh --env test')

    out = result.output
    assert 'dwh-test.yandex-team.ru' in out
    assert 'dwh.yandex-team.ru' not in out
    assert 'ift' not in out
    assert 'env: test' in out
    assert 'env: prod' not in out
    assert 'default: 34' in out


def test_close(response_mock, run, responses_update, caplog, mock_get_token):
    caplog.set_level(logging.INFO)

    with response_mock(responses_update, bypass=False):
        run(close, 'billing "sas,vla" --alias dwh --env test')

    log = caplog.text
    assert 'group "billing", alias "dwh", env "test"' in log
    assert 'dwh-test.yandex-team.ru@billing section dwh: MAN: 100/33, SAS: 0/33, VLA: 0/34' in log


def test_restore(response_mock, run, responses_update, caplog, mock_get_token):
    caplog.set_level(logging.INFO)

    responses_update[1] = responses_update[1].replace('18', '22')  # подменим веса в данных

    with response_mock(responses_update, bypass=False):
        run(restore, 'billing --alias dwh --env test')

    log = caplog.text
    assert 'group "billing", alias "dwh", env "test"' in log
    assert 'dwh-test.yandex-team.ru@billing section dwh: MAN: 33/33, SAS: 33/33, VLA: 34/34' in log
