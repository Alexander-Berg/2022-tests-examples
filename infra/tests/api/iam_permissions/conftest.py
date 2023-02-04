from unittest import mock

import pytest
import yc_as_client

from infra.walle.server.tests.lib.util import TestCase
from tests.api.iam_permissions import mocks
from walle.authorization.types import AuthorizationType
from walle.clients import iam as iam_client


@pytest.fixture
def iam(mp):
    mp.config("authorization.type", AuthorizationType.iam)


@pytest.fixture(autouse=True)
def clear_iam_user_cache():
    iam_client._cached_get_user_login.cache_clear()
    iam_client._cached_get_sa_name.cache_clear()


@pytest.yield_fixture
def project_idm_push_called(enable_idm_push, project_idm_add_node_mock, project_idm_request_ownership_mock):
    yield
    assert project_idm_add_node_mock.called
    assert project_idm_request_ownership_mock.called


@pytest.yield_fixture
def mock_iam_as_authorize():
    mocked_subject = yc_as_client.entities.UserAccountSubject(mocks.MOCKED_USER_ID)
    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authorize",
        return_value=lambda *args, **kwargs: mocked_subject,
        spec=yc_as_client.YCAccessServiceClient.authorize,
    ) as mocked_authorize:
        yield mocked_authorize


@pytest.yield_fixture
def mock_iam_sa_as_authorize():
    mocked_subject = yc_as_client.entities.ServiceAccountSubject(mocks.MOCKED_SA_ID)
    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authorize",
        return_value=lambda *args, **kwargs: mocked_subject,
        spec=yc_as_client.YCAccessServiceClient.authorize,
    ) as mocked_authorize:
        yield mocked_authorize


@pytest.yield_fixture
def mock_iam_as_authenticate():
    mocked_subject = yc_as_client.entities.UserAccountSubject(mocks.MOCKED_USER_ID)
    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authenticate",
        return_value=lambda *args, **kwargs: mocked_subject,
        spec=yc_as_client.YCAccessServiceClient.authenticate,
    ) as mocked_authenticate:
        yield mocked_authenticate


@pytest.yield_fixture
def mock_iam_sa_as_authenticate():
    mocked_subject = yc_as_client.entities.ServiceAccountSubject(mocks.MOCKED_SA_ID)
    with mock.patch.object(
        yc_as_client.YCAccessServiceClient,
        "authenticate",
        return_value=lambda *args, **kwargs: mocked_subject,
        spec=yc_as_client.YCAccessServiceClient.authenticate,
    ) as mocked_authenticate:
        yield mocked_authenticate


@pytest.yield_fixture
def mock_iam_get_user_login():
    with mock.patch.object(
        iam_client.IamClient, "get_user_login", return_value=mocks.MOCKED_USER_LOGIN
    ) as get_user_login_mock:
        yield get_user_login_mock


@pytest.yield_fixture
def mock_iam_get_sa_name():
    with mock.patch.object(
        iam_client.IamClient, "get_sa_name", return_value=mocks.MOCKED_USER_LOGIN
    ) as get_sa_name_mock:
        yield get_sa_name_mock


@pytest.fixture
def mock_yc_binded_automation_plot():
    return {
        "id": "plot-id",
        "name": "plot name",
        "yc_iam_folder_id": mocks.AUTOMATION_PLOT_FOLDER_ID,
        "owners": [TestCase.api_user],
        "checks": [
            {
                "name": "ssh",
                "reboot": True,
            }
        ],
    }
