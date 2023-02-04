import datetime

import pytest
from django.utils import timezone
from django.utils.encoding import force_bytes
from rest_framework import status

from intranet.crt.constants import TASK_TYPE
from intranet.crt.core.models import TaskTimestamp
from intranet.crt.exceptions import CrtError, CrtTimestampError
from intranet.crt.utils.timestamp import TimestampContext

pytestmark = pytest.mark.django_db


def make_timestamp(finish, is_success=False, task_type=TASK_TYPE.SYNC_TAGS, traceback=None):
    timestamp = TaskTimestamp(
        type=task_type,
        finish=finish,
        is_success=is_success,
        traceback=traceback,
    )
    timestamp.save()

    return timestamp


@pytest.fixture()
def timestamps():
    now = timezone.now()
    new_timestamps = {
        '3h': make_timestamp(
            finish=now - datetime.timedelta(hours=3),
            is_success=True,
        ),
        '2h': make_timestamp(
            finish=now - datetime.timedelta(hours=2),
            is_success=True,
        ),
        '30m': make_timestamp(
            finish=now - datetime.timedelta(minutes=30),
            is_success=True,
        ),
        '8d': make_timestamp(
            finish=now - datetime.timedelta(days=8),
            is_success=True,
        ),
        '1m_exp': make_timestamp(
            finish=now - datetime.timedelta(minutes=1),
            is_success=False,
            traceback='traceback',
        ),
        '200d_exp': make_timestamp(
            finish=now - datetime.timedelta(days=200),
            is_success=False,
            traceback='traceback',
        ),
        '500d_users': make_timestamp(
            finish=now - datetime.timedelta(days=500),
            is_success=True,
            task_type=TASK_TYPE.SYNC_USERS,
        ),
    }

    return new_timestamps


def test_timestamp_get_last(timestamps):
    timestamp = TaskTimestamp.objects.get_last(TASK_TYPE.SYNC_TAGS)

    assert timestamp.id == timestamps['30m'].id


def test_check_timestamps_ok(timestamps, crt_client):
    response = crt_client.get('/monitorings/sync-tags/')
    assert response.status_code == status.HTTP_200_OK
    assert response.content == b'ok'


def test_check_timestamps_never_finished(timestamps, crt_client):
    response = crt_client.get('/monitorings/cvs-upload/')
    assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
    assert force_bytes('{} never finished'.format(TASK_TYPE.SYNC_CVS_TAGS)) in response.content


def test_check_timestamps_last_finish(timestamps, crt_client):
    response = crt_client.get('/monitorings/sync-users/')
    assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
    assert force_bytes('{} last finish on'.format(TASK_TYPE.SYNC_USERS)) in response.content


def test_timestamp_context():
    with TimestampContext(TASK_TYPE.SYNC_TAGS):
        pass

    timestamp = TaskTimestamp.objects.get_last(TASK_TYPE.SYNC_TAGS)

    assert timestamp is not None
    assert timestamp.start is not None
    assert timestamp.finish is not None
    assert timestamp.is_success is True
    assert timestamp.traceback is None


def test_timestamp_context_exception():
    with pytest.raises(CrtError):
        with TimestampContext(TASK_TYPE.SYNC_TAGS):
            raise CrtError()

    timestamp = TaskTimestamp.objects.filter(type=TASK_TYPE.SYNC_TAGS).last()

    assert timestamp is not None
    assert timestamp.start is not None
    assert timestamp.finish is not None
    assert timestamp.is_success is False
    assert timestamp.traceback is not None


def test_timestamp_without_traceback():
    error_message = 'error message'
    with pytest.raises(CrtTimestampError):
        with TimestampContext(TASK_TYPE.SYNC_TAGS):
            raise CrtTimestampError(error_message)

    timestamp = TaskTimestamp.objects.filter(type=TASK_TYPE.SYNC_TAGS).last()

    assert timestamp is not None
    assert timestamp.start is not None
    assert timestamp.finish is not None
    assert timestamp.is_success is False
    assert timestamp.traceback == error_message
