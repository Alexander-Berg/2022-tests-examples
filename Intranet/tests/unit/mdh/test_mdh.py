import pretend
import pytest
from django.core.management import call_command

from plan.denormalization.check import check_obj_with_denormalized_fields
from plan.mdh.tasks import export_to_mdh
from plan.mdh.utils import pick_services, ExportParams
from plan.periodic.models import RunResult
from plan.services.models import Service
from common import factories


class MockLogbrokerClient:

    def __init__(self, *args, **kwargs):
        self.written = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def __call__(self, *args, **kwargs):
        return self

    def get_producer(self, *args, **kwargs):
        return self

    def write(self, msg, *args, **kwargs):
        self.written.append(msg)


@pytest.fixture
def mock_lb_client(monkeypatch):
    mock = MockLogbrokerClient()
    monkeypatch.setattr('plan.mdh.utils.LogbrokerClient', mock)
    return mock


@pytest.fixture
def mock_services():

    root = factories.ServiceFactory()
    leaf_1 = factories.ServiceFactory(parent=root)
    non_leaf = factories.ServiceFactory(parent=root)
    leaf_2 = factories.ServiceFactory(parent=non_leaf)

    for service in (root, leaf_1, non_leaf, leaf_2):
        check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)

    mocked = pretend.stub(
        root=root,
        leaf_1=leaf_1,
        non_leaf=non_leaf,
        leaf_2=leaf_2,
    )

    return mocked


def test_pick_services(mock_services):

    assert pick_services(ExportParams()).count() == 2
    assert pick_services(ExportParams(slugs=[mock_services.root.slug])).count() == 1


def test_export_mdh(mock_lb_client, django_assert_num_queries, mock_services):
    assert not RunResult.objects.count()

    with django_assert_num_queries(12):
        export_to_mdh()

    assert len(mock_lb_client.written) == 2  # leaf_1, leaf_2

    results = list(RunResult.objects.all())
    assert len(results) == 1
    assert results[0].data['since']


def test_reexport(mock_lb_client, mock_services):

    # Без фильтра.
    call_command('mdh_reexport', verbosity=0, interactive=False)

    assert len(mock_lb_client.written) == 2
    mock_lb_client.written.clear()

    # Фильтр по псевдониму.
    call_command(
        'mdh_reexport',
        slugs='%s,xxx' % mock_services.root.slug,
        verbosity=0, interactive=False)

    assert len(mock_lb_client.written) == 1
