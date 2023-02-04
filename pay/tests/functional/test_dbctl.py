import pytest
from unittest import mock

import pytest_mock.plugin

try:
    from paysys.sre.oracle.dbctl import dbctl
except ImportError:
    import sys
    sys.path.append('../..')
    import dbctl


class TestDbctl:
    @pytest.fixture
    def datacenters(self) -> list:
        return ['sas', 'myt']

    @pytest.fixture
    def step_id(self) -> any:
        return None

    @pytest.fixture
    def env(self) -> str:
        return ''

    @pytest.fixture
    def prj(self) -> str:
        return ''

    @pytest.fixture
    def dry_run(self) -> bool:
        return False

    @pytest.fixture
    def manual(self) -> bool:
        return False

    @pytest.fixture
    def steps(self) -> list:
        return [
            {
                "id": 1,
                "step": "some",
                "command": "some",
                "role": "some",
                "enabled": "true",
                "description": "some"
            }
        ]

    @pytest.fixture
    def projects(self) -> list:
        return [
            {
                "id": 1,
                "fqdn": "test-node",
                "oracle_home": "/opt/oracle/product/database/12.2.0.1",
                "project": "test_prj",
                "dgmgrl_db": "test_dbctl",
                "srvctl_srv": "test_srv",
                "sid": "test_sid",
                "container": "container",
                "tns_port": 1521,
                "dc": "sas",
                "env": "test",
                "steps": [1]
            },
            {
                "id": 2,
                "fqdn": "test-node",
                "oracle_home": "/opt/oracle/product/database/12.2.0.1",
                "project": "test_prj",
                "dgmgrl_db": "test_dbctl",
                "srvctl_srv": "test_srv",
                "sid": "test_sid",
                "container": "container",
                "tns_port": 1521,
                "dc": "myt",
                "env": "test",
                "steps": [1]
            }
        ]

    @pytest.fixture
    def load_steps_mock(self, mocker:  pytest_mock.plugin.MockerFixture, steps: list) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.load_steps')
        mocker_.return_value = steps
        return mocker_

    @pytest.fixture
    def load_projects_mock(self, mocker: pytest_mock.plugin.MockerFixture, projects: list) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.load_projects')
        mocker_.return_value = projects
        return mocker_

    @pytest.fixture
    def checking_db_role_mock(self, mocker: pytest_mock.plugin.MockerFixture) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.checking_db_role')
        mocker_.return_value = 'PRIMARY'
        return mocker_

    @pytest.fixture
    def preparing_command_mock(self, mocker: pytest_mock.plugin.MockerFixture) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.preparing_command')
        mocker_.return_value = [
            {'fqdn': 'fqdn',
             'oracle_home': 'oracle_home',
             'sid': 'sid',
             'command': 'command',
             'id': 1,
             'container': 'container',
             'tns_port': 1521
             }
        ]
        return mocker_

    @pytest.fixture
    def performing_command_mock(self, mocker: pytest_mock.plugin.MockerFixture) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.performing_command')
        mocker_.return_value = ''
        return mocker_

    @pytest.fixture
    def is_in_mock(self, mocker: pytest_mock.plugin.MockerFixture) -> pytest_mock.plugin.MockerFixture:
        mocker_ = mocker.patch('dbctl.is_in')
        mocker_.return_value = False
        return mocker_

    class TestMain:
        @pytest.fixture
        def argparse_mock(self, mocker: pytest_mock.plugin.MockerFixture) -> pytest_mock.plugin.MockerFixture:
            parser_ = mocker.patch('dbctl.parser')
            parser_.add_argument('-show_steps', help='Showing the all steps', action='store_true')
            return parser_.parse_args()

        @pytest.mark.usefixtures('argparse_mock')
        @pytest.mark.usefixtures('load_steps_mock')
        def test_show_steps_call(self) -> None:
            with pytest.raises(SystemExit) as se:
                dbctl.show_steps()
                assert se.value == SystemExit

        def test_switchover_call(self, datacenters: list, step_id: int, env: str, prj: str) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, False, False) is None

    @pytest.mark.parametrize('env', ['test'])
    @pytest.mark.parametrize('prj', ['test_prj'])
    @pytest.mark.usefixtures('load_projects_mock')
    @pytest.mark.usefixtures('load_steps_mock')
    @pytest.mark.usefixtures('checking_db_role_mock')
    @pytest.mark.usefixtures('preparing_command_mock')
    @pytest.mark.usefixtures('performing_command_mock')
    @pytest.mark.usefixtures('yes_no_mock')
    @pytest.mark.usefixtures('is_in_mock')
    class TestSwitchover:
        @pytest.fixture
        def y_n(self) -> bool:
            return False

        @pytest.fixture
        def yes_no_mock(self, mocker: pytest_mock.plugin.MockerFixture, y_n: bool) -> pytest_mock.plugin.MockerFixture:
            mocker_ = mocker.patch('dbctl.yes_no')
            mocker_.return_value = y_n
            return mocker_

        @pytest.mark.parametrize('dry_run', [True])
        def test_dry_run(self, datacenters: list, step_id: int, env: str, prj: str, dry_run, manual) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, dry_run, manual) is None

        @pytest.mark.parametrize('y_n', [True])
        @pytest.mark.parametrize('manual', [True])
        def test_manual_yes(self, datacenters: list, step_id: int, env: str, prj: str, y_n: bool, dry_run, manual) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, dry_run, manual) is None

        @pytest.mark.parametrize('manual', [True])
        def test_manual_no(self, datacenters: list, step_id: int, env: str, prj: str, dry_run, manual) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, dry_run, manual) is None

        @pytest.mark.parametrize('step_id', [1])
        def test_step_id(self, datacenters: list, step_id: int, env: str, prj: str, dry_run, manual) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, dry_run, manual) is None

        def test_automatic(self, datacenters: list, step_id: int, env: str, prj: str, dry_run, manual) -> None:
            assert dbctl.switchover(datacenters, step_id, env, prj, dry_run, manual) is None

    class TestPreparingHosts:
        def test_hosts_involved(self) -> None:
            pass

        def test_hosts_empty(self) -> None:
            pass

        def test_projects_empty(self) -> None:
            pass

        def test_project_hosts(self) -> None:
            pass

    class TestPreparingSteps:
        def test_steps_involved(self) -> None:
            pass

        def test_steps_empty(self) -> None:
            pass

        def test_step_disabled(self) -> None:
            pass

        def test_step_enabled(self) -> None:
            pass

    class TestLoadSteps:
        def test_load_steps(self) -> None:
            pass

        def test_load_steps_empty(self) -> None:
            pass

    class TestLoadProjects:
        def test_load_projects(self) -> None:
            pass

        def test_load_projects_empty(self) -> None:
            pass

    class TestYesNo:
        def test_yes(self) -> None:
            pass

        def test_no(self) -> None:
            pass

        def test_value_error(self) -> None:
            pass

    class TestIsIn:
        def test_found_pattern(self) -> None:
            pass

        def test_notfound_pattern(self) -> None:
            pass

    class TestReadOraPassFile:
        def test_pass_exists(self) -> None:
            pass

        def test_pass_not_exists(self) -> None:
            pass

    class TestConnection:
        def test_connect_ok(self) -> None:
            pass

        def test_connect_exception(self) -> None:
            pass

    class TestCheckDbRole:
        def test_db_role_ok(self) -> None:
            pass

        def test_db_role_exception(self) -> None:
            pass

    class TestPreparingCommand:
        def test_ora_pass_empty(self) -> None:
            pass

        def test_result_hosts_empty(self) -> None:
            pass

        def test_result_hosts(self) -> None:
            pass

    class TestExecSql:
        def test_container(self) -> None:
            pass

        def test_not_container(self) -> None:
            pass

        def test_sql(self) -> None:
            pass

        def test_ddl(self) -> None:
            pass

    class TestExecSsh:
        def test_ssh_ok(self) -> None:
            pass

        def test_ssh_exception(self) -> None:
            pass

    class TestPerformingCommand:
        def test_sql_template(self) -> None:
            pass

        def test_ora_template(self) -> None:
            pass

        def test_os_template(self) -> None:
            pass

        def test_template_notfound(self) -> None:
            pass
