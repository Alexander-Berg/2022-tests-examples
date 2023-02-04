import logging
import typing as tp

import pytest
from click.testing import CliRunner

from maps.infra.quotateka.cli import cli
from maps.pylibs.terminal_utils.table import Table


logger = logging.getLogger(__name__)


def invoke_cli_command(args: tp.List[str], abc_slug: str = 'maps-core-infra-teapot') -> str:
    runner = CliRunner()
    result = runner.invoke(cli, ['--abc', abc_slug] + args)
    if result.exception:
        raise result.exception
    logger.info(result.output)
    assert result.exit_code == 0
    return result.output


def test_empty_info(table_absorber: tp.ContextManager[tp.List[Table]]) -> None:
    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info'])

    assert len(tables) == 2  # Each provider in separate table

    assert tables[0].title == 'core-teacup'
    assert list(tables[0].rows()) == [
        ['', 'general', 'heavy'],
        ['total', '0 of 100', '0 of 10']
    ]
    assert tables[1].title == 'core-teaspoon'
    assert list(tables[1].rows()) == [
        ['', 'general'],
        ['total', '0 of 25']
    ]


def test_client_without_issued_quotas() -> None:
    with pytest.raises(Exception, match=r'Not found client with ABC slug: random-abc-slug'):
        invoke_cli_command(['quota', 'info'], abc_slug='random-abc-slug')


def test_info_with_accounts(table_absorber: tp.ContextManager[tp.List[Table]],
                            auth_request: tp.ContextManager[None],
                            provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'stable'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'closed'])
        invoke_cli_command(['account', 'close', 'closed'])

    # quota info filtered by provider teacup
    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info', '--provider', 'core-teacup'])
    assert len(tables) == 1

    quotas_table = tables[0]
    assert quotas_table.title == 'core-teacup'
    assert list(quotas_table.rows()) == [
        ['', 'general', 'heavy'],
        ['stable', '0', '0'],
        ['testing', '0', '0'],
        ['total', '0 of 100', '0 of 10']
    ]

    # quota info for single provider teaspoon
    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info', '--provider', 'core-teaspoon'])
    assert len(tables) == 1

    quotas_table = tables[0]
    assert quotas_table.title == 'core-teaspoon'
    assert list(quotas_table.rows()) == [
        ['', 'general'],
        ['total', '0 of 25']
    ]


def test_set_absolute(table_absorber: tp.ContextManager[tp.List[Table]],
                      auth_request: tp.ContextManager[None],
                      provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:50'])

        with pytest.raises(Exception, match=r' No core-teacup:invalid-resource quota issued'):
            invoke_cli_command(['quota', 'set', 'testing', 'invalid-resource:10'])

    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info'])
    assert len(tables) == 2

    teacup_quotas_table, teaspoon_quotas_table = tables

    assert teacup_quotas_table.title == 'core-teacup'
    assert list(teacup_quotas_table.rows()) == [
        ['', 'general', 'heavy'],
        ['testing', '50', '0'],
        ['total', '50 of 100', '0 of 10']
    ]
    assert teaspoon_quotas_table.title == 'core-teaspoon'
    assert list(teaspoon_quotas_table.rows()) == [
        ['', 'general'],
        ['total', '0 of 25']
    ]


def test_set_relative_increase(table_absorber: tp.ContextManager[tp.List[Table]],
                               auth_request: tp.ContextManager[None],
                               provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:50'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:+5'])

    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info', '--provider', 'core-teacup'])
    quotas_table = tables[0]

    assert quotas_table.title == 'core-teacup'
    assert list(quotas_table.rows()) == [
        ['', 'general', 'heavy'],
        ['testing', '55', '0'],
        ['total', '55 of 100', '0 of 10']
    ]


def test_set_relative_decrease(table_absorber: tp.ContextManager[tp.List[Table]],
                               auth_request: tp.ContextManager[None],
                               provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:50'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:-5'])

    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info', '--provider', 'core-teacup'])
    quotas_table = tables[0]

    assert quotas_table.title == 'core-teacup'
    assert list(quotas_table.rows()) == [
        ['', 'general', 'heavy'],
        ['testing', '45', '0'],
        ['total', '45 of 100', '0 of 10']
    ]


def test_set_absolute_overflow(auth_request: tp.ContextManager[None],
                               provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        with pytest.raises(Exception,
                           match=r'Invalid quota specified: Not allowed to exceed total quota for general'):
            invoke_cli_command(['quota', 'set', 'testing', 'general:120'])


def test_set_relative_overflow(auth_request: tp.ContextManager[None],
                               provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:80'])
        with pytest.raises(Exception,
                           match=r'Invalid quota specified: Not allowed to exceed total quota for general'):
            invoke_cli_command(['quota', 'set', 'testing', 'general:+50'])


def test_set_relative_underflow(auth_request: tp.ContextManager[None],
                                provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:80'])
        with pytest.raises(Exception, match=r'Invalid quota specified: trying to set limit < 0 for testing'):
            invoke_cli_command(['quota', 'set', 'testing', 'general:-100'])


def test_move(table_absorber: tp.ContextManager[tp.List[Table]],
              auth_request: tp.ContextManager[None],
              provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'stable'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:100'])
        invoke_cli_command(['quota', 'move', 'testing', 'stable', 'general:50'])

    with table_absorber() as tables:
        invoke_cli_command(['quota', 'info'])

    assert len(tables) == 2  # Separate tables for teacup and teaspoon providers
    teacup_quotas_table, teaspoon_quotas_table = tables

    assert teacup_quotas_table.title == 'core-teacup'
    assert list(teacup_quotas_table.rows()) == [
        ['', 'general', 'heavy'],
        ['stable', '50', '0'],
        ['testing', '50', '0'],
        ['total', '100 of 100', '0 of 10']
    ]
    assert teaspoon_quotas_table.title == 'core-teaspoon'
    assert list(teaspoon_quotas_table.rows()) == [
        ['', 'general'],
        ['total', '0 of 25']
    ]


def test_move_underflow(auth_request: tp.ContextManager[None],
                        provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'stable'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:10'])
        with pytest.raises(Exception, match=r'Invalid quota specified: trying to set limit < 0 for testing'):
            invoke_cli_command(['quota', 'move', 'testing', 'stable', 'general:20'])


def test_unknown_account(auth_request: tp.ContextManager[None],
                         provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        with pytest.raises(Exception, match=r'No such account: testing'):
            invoke_cli_command(['quota', 'set', 'testing', 'general:50'])
        with pytest.raises(Exception, match=r'No such account: testing'):
            invoke_cli_command(['quota', 'move', 'testing', 'stable', 'general:10'])


def test_closed_account(auth_request: tp.ContextManager[None],
                        provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:100'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'closed'])
        invoke_cli_command(['account', 'close', 'closed'])
        with pytest.raises(Exception, match=r'Unable to change quotas for closed account: closed'):
            invoke_cli_command(['quota', 'set', 'closed', 'general:50'])
        with pytest.raises(Exception, match=r'Unable to change quotas for closed account: closed'):
            invoke_cli_command(['quota', 'move', 'closed', 'testing', 'general:10'])


def test_move_to_the_same_account(auth_request: tp.ContextManager[None],
                                  provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['quota', 'set', 'testing', 'general:10'])
        with pytest.raises(Exception, match=r'Unable to move quota to the same account testing'):
            invoke_cli_command(['quota', 'move', 'testing', 'testing', 'general:10'])


def test_move_inconsistent_accounts(table_absorber: tp.ContextManager[tp.List[Table]],
                                    auth_request: tp.ContextManager[None],
                                    provider_admin_uid: int) -> None:
    with auth_request(user_id=provider_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'aaa'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teaspoon', 'bbb'])
        invoke_cli_command(['quota', 'set', 'aaa', 'general:10'])
        invoke_cli_command(['quota', 'set', 'bbb', 'general:10'])

        # Can't move between accounts of different providers
        with pytest.raises(Exception, match=r'Not allowed accounts for different providers'):
            invoke_cli_command(['quota', 'move', 'aaa', 'bbb', 'general:5'])
        with pytest.raises(Exception, match=r'Not allowed accounts for different providers'):
            invoke_cli_command(['quota', 'move', 'aaa', 'bbb', 'general:5'])
