import pytest

from intranet.femida.tests.factories import UserFactory
from intranet.femida.src.permissions.context import context
from intranet.femida.tests.clients import APIClient, DjangoSimpleClient
from intranet.femida.tests.utils import patch_service_permissions


@pytest.fixture
def client():
    UserFactory.create(username='user', is_superuser=False)
    client = APIClient()
    client.login(login='user')
    return client


@pytest.fixture
def su_client():
    UserFactory.create(username='superuser', is_superuser=True)
    client = APIClient()
    client.login(login='superuser')
    return client


@pytest.fixture
def tvm_jobs_client_without_permissions():
    UserFactory.create(username='user', is_superuser=False)
    client = APIClient()
    client.login(
        login='user',
        mechanism_name='tvm',
        tvm_client_id=112233,
    )
    yield client


@pytest.fixture
def tvm_jobs_client(tvm_jobs_client_without_permissions):
    tvm_id = tvm_jobs_client_without_permissions._tvm_client_id
    with patch_service_permissions({tvm_id: ['permissions.can_view_external_publications']}):
        yield tvm_jobs_client_without_permissions


@pytest.fixture
def tvm_certification_client(tvm_jobs_client_without_permissions):
    tvm_id = tvm_jobs_client_without_permissions._tvm_client_id
    with patch_service_permissions({tvm_id: ['permissions.can_access_certification_data']}):
        yield tvm_jobs_client_without_permissions


@pytest.fixture
def django_client():
    return DjangoSimpleClient()


@pytest.fixture
def django_su_client(django_client):
    user = UserFactory.create(
        username='superuser',
        is_superuser=True,
    )
    context.init(user)
    django_client.authenticate(user)
    return django_client


@pytest.fixture(scope='module')
def module_db(django_db_setup, django_db_blocker):
    """
    Фикстура для использования БД в фикстурах со scope = module.
    Важно понимать, что данные, записанные в БД, останутся там до конца сессии
    """
    with django_db_blocker.unblock():
        yield
