import pytest

from intranet.search.core.models import PushInstance, PushRecord
from intranet.search.core.storages.push import PushStorage
from intranet.search.tests.helpers import models_helpers as mh

pytestmark = pytest.mark.django_db(transaction=False)


@pytest.mark.parametrize('status', [
    PushInstance.STATUS_FAIL,
    PushInstance.STATUS_DONE,
    PushInstance.STATUS_CANCEL,
    PushInstance.STATUS_NEW
])
def test_push_status_update_by_instance_status(status):
    storage = PushStorage()
    push = mh.create_push(PushInstance.STATUS_NEW)
    mh.create_push_instance(push, status)

    storage.update_statuses()
    push.refresh_from_db()

    assert push.status == status


@pytest.mark.parametrize('status', [
    PushInstance.STATUS_DONE,
    PushInstance.STATUS_CANCEL
])
def test_push_failed_by_any_instance(status):
    storage = PushStorage()
    push = mh.create_push(PushInstance.STATUS_NEW)
    mh.create_push_instance(push, status)
    mh.create_push_instance(push, PushRecord.STATUS_FAIL)

    storage.update_statuses()
    push.refresh_from_db()

    assert push.status == PushRecord.STATUS_FAIL


@pytest.mark.parametrize('status', [
    PushInstance.STATUS_FAIL,
    PushInstance.STATUS_DONE,
    PushInstance.STATUS_CANCEL
])
@pytest.mark.parametrize('deleted', [True, False])
def test_push_new_by_any_instance(status, deleted):
    organization = mh.Organization(deleted=deleted)
    storage = PushStorage()

    push = mh.create_push(PushInstance.STATUS_NEW, organization=organization)
    mh.create_push_instance(push, PushInstance.STATUS_NEW)
    mh.create_push_instance(push, status)

    storage.update_statuses()

    push.refresh_from_db()

    if deleted:
        assert push.status == PushRecord.STATUS_KNOWN_FAIL
    else:
        assert push.status == PushRecord.STATUS_NEW


@pytest.mark.parametrize('status', [
    PushInstance.STATUS_FAIL,
    PushInstance.STATUS_DONE,
    PushInstance.STATUS_CANCEL
])
def test_status_fail_with_deleted_org(status):
    organization = mh.Organization(deleted=True)
    storage = PushStorage()
    push = mh.create_push(PushInstance.STATUS_NEW, organization=organization)
    mh.create_push_instance(push, PushRecord.STATUS_FAIL)
    mh.create_push_instance(push, status)

    storage.update_statuses()

    push.refresh_from_db()

    assert push.status == PushRecord.STATUS_KNOWN_FAIL
