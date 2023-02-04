from unittest import mock

from click.testing import CliRunner

from billing.yandex_pay.yandex_pay.commands.pg_pinger_env import cli


def test_print_expected_env_variables(mocker, yandex_pay_settings):
    db_settings = yandex_pay_settings.DATABASE
    print_mock = mocker.patch('builtins.print')
    runner = CliRunner()

    runner.invoke(cli, [])

    expected = [
        mock.call('PINGER_HOSTS', db_settings['HOST'], sep='='),
        mock.call('PINGER_PORT', db_settings['PORT'], sep='='),
        mock.call('PINGER_USERNAME', db_settings['USER'], sep='='),
        mock.call('PINGER_PASSWORD', db_settings['PASSWORD'], sep='='),
        mock.call('PINGER_DATABASE', db_settings['NAME'], sep='='),
        mock.call('PINGER_SSLMODE', 'disable', sep='='),
        mock.call('PINGER_SYNC_MAX_LAG', db_settings.get('SYNC_MAX_LAG', 1000), sep='='),
    ]
    assert print_mock.call_args_list == expected
