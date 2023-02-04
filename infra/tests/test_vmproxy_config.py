import os
import pytest
import yatest

from sepelib.core import config as sepelibconfig


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


@pytest.fixture()
def config():
    return None


def load_config(**config_context):
    config_path = source_path("infra/qyp/vmproxy/cfg_default.yml")
    sepelibconfig.load(config_path, config_context=config_context)
    return sepelibconfig


def test_load_config_with_context():
    # port
    config = load_config()
    assert not config.get_value('web.http.port')

    config = load_config(port=9000)
    assert config.get_value('web.http.port') == 9000

    config = load_config(debug='1')
    assert config.get_value('run.debug')

    # debug
    config = load_config()
    assert not config.get_value('run.debug')

    config = load_config(debug='1')
    assert config.get_value('run.debug')

    # disable_auth
    config = load_config()
    assert config.get_value('run.auth')

    config = load_config(disable_auth='1')
    assert not config.get_value('run.auth')

    # force_return_user
    config = load_config()
    assert not config.get_value('run.force_return_user')

    config = load_config(force_return_user='test')
    assert config.get_value('run.force_return_user') == 'test'

    # blackbox_testing_enabled
    config = load_config()
    assert config.get_value('passport.blackbox_url') == 'http://blackbox.yandex-team.ru/blackbox'
    assert config.get_value('passport.blackbox_auth_url') == 'http://passport.yandex-team.ru/passport?retpath={}'

    config = load_config(blackbox_testing_enabled='1')
    assert config.get_value('passport.blackbox_url') == 'http://pass-test.yandex.ru/blackbox'
    assert config.get_value('passport.blackbox_auth_url') == 'http://passport-test.yandex.ru/passport?retpath={}'

    # base_logs_dir
    config = load_config(port=9000)
    assert config.get_value('log.filepath') == '/logs/vmproxy.log'
    assert config.get_value('web.access_log.filepath') == '/logs/vmproxy_9000_access.log'

    config = load_config(base_logs_dir='/tmp', port=9000)
    assert config.get_value('log.filepath') == '/tmp/vmproxy.log'
    assert config.get_value('web.access_log.filepath') == '/tmp/vmproxy_9000_access.log'


def test_load_config_with_environment():
    # TOKENS
    config = load_config(
        YP_API_TOKEN='YP_API_TOKEN',
        NANNY_TOKEN='NANNY_TOKEN',
        STAFF_OAUTH_TOKEN='STAFF_OAUTH_TOKEN',
        ABC_OAUTH_TOKEN='ABC_OAUTH_TOKEN',
    )
    assert config.get_value('yp.robot_token') == 'YP_API_TOKEN'
    assert config.get_value('nanny.token') == 'NANNY_TOKEN'
    assert config.get_value('staff.oauth_token') == 'STAFF_OAUTH_TOKEN'
    assert config.get_value('abc.oauth_token') == 'ABC_OAUTH_TOKEN'

    # TVM_CONTEXT_SECRET_FILE_PATH(default: secret / rtc - vmproxy - secret)
    config = load_config()
    assert config.get_value('tvm_context.secret_file') == 'secret/rtc-vmproxy-secret'
    config = load_config(TVM_CONTEXT_SECRET_FILE_PATH='test')
    assert config.get_value('tvm_context.secret_file') == 'test'

    # DEFAULT_CLUSTER(default: TEST_SAS)
    config = load_config()
    assert config.get_value('yp.default_cluster') == 'TEST_SAS'
    assert config.get_value(
        'vmproxy.check_access_url') == 'http://dev-vmproxy.n.yandex-team.ru/api/CheckVmAccess/'

    config = load_config(DEFAULT_CLUSTER='TEST_SAS')
    assert config.get_value(
        'vmproxy.check_access_url') == 'http://dev-vmproxy.n.yandex-team.ru/api/CheckVmAccess/'

    config = load_config(DEFAULT_CLUSTER='SAS')
    assert config.get_value(
        'vmproxy.check_access_url') == 'https://vmproxy.sas-swat.yandex-team.ru/api/CheckVmAccess/'
