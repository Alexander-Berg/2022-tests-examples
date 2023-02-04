import logging
import typing as tp

import pytest
from click.testing import CliRunner

from maps.infra.quotateka.cli import cli
from maps.pylibs.terminal_utils.table import Table
from maps.pylibs.terminal_utils.utils import yellow, red


logger = logging.getLogger(__name__)


def invoke_cli_command(args: tp.List[str], abc_slug: str = 'maps-core-infra-teapot') -> str:
    runner = CliRunner()
    result = runner.invoke(cli, ['--abc', abc_slug] + args)
    if result.exception:
        raise result.exception
    logger.info(result.output)
    assert result.exit_code == 0
    return result.output


class StartsWith:
    def __init__(self, entry: str) -> None:
        self.entry = entry

    def __eq__(self, other) -> bool:
        return other.startswith(self.entry)

    def __repr__(self) -> str:
        return f'StartsWith(\'{self.entry}\')'


class EndsWith:
    def __init__(self, entry: str) -> None:
        self.entry = entry

    def __eq__(self, other) -> bool:
        return other.endswith(self.entry)

    def __repr__(self) -> str:
        return f'EndsWith(\'{self.entry}\')'


def test_empty_info(table_absorber: tp.ContextManager[tp.List[Table]]) -> None:
    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert accounts_table.title == 'No accounts created for maps-core-infra-teapot'
    assert list(accounts_table.rows()) == []


def test_client_without_issued_quotas() -> None:
    with pytest.raises(Exception, match=r'Not found client with ABC slug: random-abc-slug'):
        invoke_cli_command(['account', 'info'], abc_slug='random-abc-slug')


def test_create_with_conflict(auth_request: tp.ContextManager[None],
                              client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        with pytest.raises(Exception, match=r'Account testing already exists'):
            invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])


def test_info_with_accounts(table_absorber: tp.ContextManager[tp.List[Table]],
                            auth_request: tp.ContextManager[None],
                            client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'testing'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'stable'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert accounts_table.title is None
    assert list(accounts_table.rows()) == [
        [StartsWith(f'{yellow("Account stable")}\n'
                    'Description: -\n')],
        [StartsWith(f'{yellow("Account testing")}\n'
                    'Description: -\n')],
    ]


def test_account_rename(table_absorber: tp.ContextManager[tp.List[Table]],
                        auth_request: tp.ContextManager[None],
                        client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'sample',
                            '--name', 'sample name',
                            '--description', 'sample description'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == [[StartsWith(
        f'{yellow("Account sample")}\n'
        'Name: sample name\n'
        'Description: sample description\n'
    )]]

    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'rename', 'sample',
                            '--name', 'another name',
                            '--description', 'another description'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == [[StartsWith(
        f'{yellow("Account sample")}\n'
        'Name: another name\n'
        'Description: another description\n'
    )]]


def test_account_close(table_absorber: tp.ContextManager[tp.List[Table]],
                       auth_request: tp.ContextManager[None],
                       client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'sample'])
        invoke_cli_command(['account', 'close', 'sample'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == []

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info', '--show-closed'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == [
        [StartsWith(f'{yellow("Account sample")} [{red("CLOSED")}]\n')]
    ]


def test_account_reopen(table_absorber: tp.ContextManager[tp.List[Table]],
                        auth_request: tp.ContextManager[None],
                        client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'sample'])
        invoke_cli_command(['account', 'close', 'sample'])
        invoke_cli_command(['account', 'reopen', 'sample'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == [
        [StartsWith(f'{yellow("Account sample")}\n')]
    ]


def test_account_add_tvm(table_absorber: tp.ContextManager[tp.List[Table]],
                         auth_request: tp.ContextManager[None],
                         client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'one'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'two'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teaspoon', 'three'])
        invoke_cli_command(['account', 'tvm', 'add', 'one', '--tvm', '12345', '--name', 'example-tvm'])
        # Failed add to another account of the same provider
        with pytest.raises(Exception, match='.*tvm:12345 already assigned to account one \\(owned by maps-core-infra-teapot\\)'
                                            ' of the provider maps-core-teacup.*'):
            invoke_cli_command(args=['account', 'tvm', 'add', 'two', '--tvm', '12345', '--name', 'example-tvm'])
        # Successful add to the same account
        invoke_cli_command(['account', 'tvm', 'add', 'one', '--tvm', '12345', '--name', 'cup-tvm'])
        # Successful add to account of another provider
        invoke_cli_command(['account', 'tvm', 'add', 'three', '--tvm', '12345', '--name', 'spoon-tvm'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table_rows = list(tables[0].rows())

    assert accounts_table_rows[0] == [StartsWith(f'{yellow("Account one")}')]
    assert accounts_table_rows[0] == [EndsWith('TVM:\n  cup-tvm: 12345')]

    assert accounts_table_rows[1] == [StartsWith(f'{yellow("Account three")}')]
    assert accounts_table_rows[1] == [EndsWith('TVM:\n  spoon-tvm: 12345')]

    assert accounts_table_rows[2] == [StartsWith(f'{yellow("Account two")}')]
    assert accounts_table_rows[2] == [EndsWith('TVM: -')]


def test_account_move_tvm(table_absorber: tp.ContextManager[tp.List[Table]],
                          auth_request: tp.ContextManager[None],
                          client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'one'])
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'two'])
        invoke_cli_command(['account', 'tvm', 'add', 'one', '--tvm', '12345', '--name' , 'example-tvm'])
        # Move tvm from account 'one' into 'two'
        invoke_cli_command(['account', 'tvm', 'move', 'one', 'two', '--tvm', '12345'])
        # Fail - already moved
        with pytest.raises(Exception, match=r'Requested object not found'):
            invoke_cli_command(['account', 'tvm', 'move', 'one', 'two', '--tvm', '12345'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table_rows = list(tables[0].rows())

    assert accounts_table_rows[0] == [StartsWith(f'{yellow("Account one")}')]
    assert accounts_table_rows[0] == [EndsWith('TVM: -')]
    assert accounts_table_rows[1] == [StartsWith(f'{yellow("Account two")}')]
    assert accounts_table_rows[1] == [EndsWith('TVM:\n  example-tvm: 12345')]


def test_account_delete_tvm(table_absorber: tp.ContextManager[tp.List[Table]],
                            auth_request: tp.ContextManager[None],
                            client_admin_uid: int) -> None:
    with auth_request(user_id=client_admin_uid):
        invoke_cli_command(['account', 'create', '--provider', 'core-teacup', 'sample'])
        invoke_cli_command(['account', 'tvm', 'add', 'sample', '--tvm', '12345', '--name', 'sample-tvm'])
        invoke_cli_command(['account', 'tvm', 'del', 'sample', '--tvm', '12345'])

    with table_absorber() as tables:
        invoke_cli_command(['account', 'info'])
    accounts_table = tables[0]

    assert list(accounts_table.rows()) == [
        [EndsWith('TVM: -')]
    ]
