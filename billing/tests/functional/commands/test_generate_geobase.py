import json
import re

from click.testing import CliRunner

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries

from billing.yandex_pay.yandex_pay.commands.generate_geobase import cli


def test_generate_geobase(aioresponses_mocker):
    aioresponses_mocker.get(
        re.compile('^http://geoexport.yandex.ru.*root=10.*'),
        status=200,
        payload=[{'Id': 1}, {'Id': 2}]
    )
    aioresponses_mocker.get(
        re.compile('^http://geoexport.yandex.ru.*root=20.*'),
        status=200,
        payload=[{'Id': 3}, {'Id': 4}]
    )
    runner = CliRunner()

    result = runner.invoke(cli, ['--root-id', 10, '--root-id', 20])

    try:
        check_result = json.loads(result.output)
    except Exception:
        raise RuntimeError(f'Bad cmd result^\n{result.output}')

    assert_that(
        check_result,
        has_entries({
            'forbidden_regions': contains_inanyorder({'Id': 1}, {'Id': 2}, {'Id': 3}, {'Id': 4}),
        }),
    )


def test_generate_geobase_defaults(mocker, aioresponses_mocker):
    mocker.patch('billing.yandex_pay.yandex_pay.commands.generate_geobase.DEFAULT_ROOTS', [600])
    aioresponses_mocker.get(
        re.compile('^http://geoexport.yandex.ru.*root=600.*'),
        status=200,
        payload=[{'Id': 1}]
    )
    runner = CliRunner()

    result = runner.invoke(cli, [])

    try:
        check_result = json.loads(result.output)
    except Exception:
        raise RuntimeError(f'Bad cmd result^\n{result.output}')

    assert_that(
        check_result,
        equal_to({
            'forbidden_regions': [{'Id': 1}],
        }),
    )
