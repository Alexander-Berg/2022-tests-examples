from datetime import timedelta

from mock import patch, MagicMock
import pytest

from django.utils import timezone

from wiki.uploads.consts import UploadSessionStatusType
from wiki.uploads.models import UploadSession
from wiki.uploads.tasks.clear_upload_sessions import clear_upload_sessions, S3_CLIENT

pytestmark = [pytest.mark.django_db]


@patch.object(S3_CLIENT, 'delete_objects')
@patch.object(S3_CLIENT, 'abort_multipart_upload')
def test_clear_in_progress(s3_abort: MagicMock, s3_delete: MagicMock, wiki_users, upload_sessions):
    date = timezone.now()
    status = UploadSessionStatusType.IN_PROGRESS

    session_yet = upload_sessions['9b18deaa-b969-4caa-a4f0-b13e455b610b']
    session_old = upload_sessions['a7901b58-d316-4b86-ac85-002a3bf68d88']

    for hours, session in zip([1, 13], [session_yet, session_old]):
        session.status = status
        session.created_at = date - timedelta(hours=hours)
        session.save()

    assert set(s.pk for s in UploadSession.objects.filter(status=status)) == {session_yet.pk, session_old.pk}

    clear_upload_sessions(date)
    assert set(s.pk for s in UploadSession.objects.filter(status=status)) == {session_yet.pk}

    s3_abort.assert_called_once()
    s3_delete.assert_not_called()


@patch.object(S3_CLIENT, 'delete_objects')
@patch.object(S3_CLIENT, 'abort_multipart_upload')
def test_clear_marked_for_cleanup_and_used(s3_abort: MagicMock, s3_delete: MagicMock, wiki_users, upload_sessions):
    date = timezone.now()
    statuses = [UploadSessionStatusType.MARKED_FOR_CLEANUP, UploadSessionStatusType.USED]

    session_cleanup = upload_sessions['9b18deaa-b969-4caa-a4f0-b13e455b610b']
    session_used = upload_sessions['a7901b58-d316-4b86-ac85-002a3bf68d88']

    for status, session in zip(statuses, [session_cleanup, session_used]):
        session.status = status
        session.save()

    assert set(s.pk for s in UploadSession.objects.filter(status__in=statuses)) == {session_cleanup.pk, session_used.pk}

    clear_upload_sessions(date)
    assert set(s.pk for s in UploadSession.objects.filter(status__in=statuses)) == set()

    s3_abort.assert_not_called()
    s3_delete.assert_called_once_with([session_cleanup.storage_key])  # no used


@patch.object(S3_CLIENT, 'delete_objects')
@patch.object(S3_CLIENT, 'abort_multipart_upload')
def test_clear_finished(s3_abort: MagicMock, s3_delete: MagicMock, wiki_users, upload_sessions):
    date = timezone.now()
    status = UploadSessionStatusType.FINISHED

    session_yet = upload_sessions['9b18deaa-b969-4caa-a4f0-b13e455b610b']
    session_old = upload_sessions['a7901b58-d316-4b86-ac85-002a3bf68d88']

    for hours, session in zip([12, 25], [session_yet, session_old]):
        session.status = status
        session.finished_at = date - timedelta(hours=hours)
        session.save()

    assert set(s.pk for s in UploadSession.objects.filter(status=status)) == {session_yet.pk, session_old.pk}

    clear_upload_sessions(date)
    assert set(s.pk for s in UploadSession.objects.filter(status=status)) == {session_yet.pk}

    s3_abort.assert_not_called()
    s3_delete.assert_called_once_with([session_old.storage_key])


@patch.object(S3_CLIENT, 'delete_objects')
@patch.object(S3_CLIENT, 'abort_multipart_upload')
def test_clear_not_started_and_aborted(s3_abort: MagicMock, s3_delete: MagicMock, wiki_users, upload_sessions):
    date = timezone.now()
    statuses = [UploadSessionStatusType.NOT_STARTED, UploadSessionStatusType.ABORTED]

    session_not_st = upload_sessions['9b18deaa-b969-4caa-a4f0-b13e455b610b']
    session_abort = upload_sessions['a7901b58-d316-4b86-ac85-002a3bf68d88']

    for status, session in zip(statuses, [session_not_st, session_abort]):
        session.status = status
        session.created_at = date - timedelta(hours=12)
        session.save()

    # less 1 day - no delete
    clear_upload_sessions(date)
    assert UploadSession.objects.filter(pk=session_not_st.pk).exists()
    assert UploadSession.objects.filter(pk=session_abort.pk).exists()

    # set more than 1 day
    for session in [session_not_st, session_abort]:
        session.created_at = date - timedelta(days=2)
        session.save()

    clear_upload_sessions(date)
    assert UploadSession.objects.filter(pk=session_not_st.pk).exists() is False
    assert UploadSession.objects.filter(pk=session_abort.pk).exists() is False

    s3_abort.assert_not_called()
    s3_delete.assert_not_called()
