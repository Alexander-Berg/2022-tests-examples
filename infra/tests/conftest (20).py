import pytest
import mock

from infra.swatlib.rpc.authentication import AuthSubject
from sepelib.core import config as sconfig


def load_config():
    sconfig.load()


@pytest.fixture()
def config():
    load_config()
    return sconfig


@pytest.fixture()
def yp_client_mock():  # type: () -> yp_client.YpClient
    return mock.Mock()


@pytest.fixture()
def mail_client_mock():  # type: () -> mail_client.MailClient
    return mock.Mock()


@pytest.fixture()
def staff_client_mock():  # type: () -> staff_client.StaffClient
    return mock.Mock()


@pytest.fixture()
def abc_client_mock():  # type: () -> abc_client.AbcClient
    return mock.Mock()


@pytest.fixture()
def pod_client_mock():  # type: () -> pod_controller.PodController
    return mock.Mock()


@pytest.fixture()
def vmproxy_client_mock():  # type: () -> pod_controller.PodController
    return mock.Mock()


@pytest.fixture()
def qdm_client_mock():  # type: () -> QDMClient
    return mock.Mock()


@pytest.fixture()
def ctx_mock(yp_client_mock, mail_client_mock, staff_client_mock, abc_client_mock, pod_client_mock,
             vmproxy_client_mock, qdm_client_mock):
    ctx = mock.Mock()
    ctx.yp_client_list = yp_client_mock
    ctx.mail_client = mail_client_mock
    ctx.staff_client = staff_client_mock
    ctx.abc_client = abc_client_mock
    ctx.pod_ctl_map = {}
    ctx.vmproxy_client = vmproxy_client_mock
    ctx.qdm_client = qdm_client_mock
    ctx.value_stream_quotas = {}
    return ctx


@pytest.fixture()
def call():
    def _call(method, req, auth_subject=AuthSubject('anonymous')):
        return method.handler(req, auth_subject)

    return _call
