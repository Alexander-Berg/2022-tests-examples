from mock import patch, call
from datetime import timedelta
from freezegun import freeze_time

from django.utils import timezone
from django.db import connection

from idm.core.models import BatchRequest, DelayedRoleRequest
from idm.core.tasks.roles import ExecuteDelayedRoleRequests


def test_execute_delayed_role_requests(arda_users):
    with freeze_time(timezone.now() - timezone.timedelta(hours=2)):
        batch_request = BatchRequest.objects.create(requester={})
    
    # не отправляем - готов
    DelayedRoleRequest.objects.create(is_done=True, batch_request=batch_request)
    
    # отправим - так как прошло больше часа после того как отправили в первый раз
    delayed_request = DelayedRoleRequest.objects.create(
        is_send=True, 
        batch_request=batch_request,
    )

    # не отправим - так как прошло меньше часа после того как отправили в первый раз
    with freeze_time(timezone.now() - timedelta(minutes=40)):
        batch_request_1 = BatchRequest.objects.create(requester={},)
    DelayedRoleRequest.objects.create(
        is_send=True,
        batch_request=batch_request_1,
    )

    # отправим - так как еще не отправляли и прошло больше 5 минут
    with freeze_time(timezone.now() - timedelta(minutes=20)):
        batch_request_2 = BatchRequest.objects.create(requester={},)
    delayed_request_2 = DelayedRoleRequest.objects.create(
        batch_request=batch_request_2,
    )

    # не отправим - так как еще не отправляли и прошло меньше 5 минут
    with freeze_time(timezone.now() - timedelta(minutes=2)):
        batch_request_3 = BatchRequest.objects.create(requester={}, )
    DelayedRoleRequest.objects.create(batch_request=batch_request_3)

    with patch.object(ExecuteDelayedRoleRequests, 'delay') as execute_delay_mock:
        ExecuteDelayedRoleRequests()

    execute_delay_mock.assert_has_calls(
        [
            call(delayed_request_id=delayed_request.id, step='request_role'),
            call(delayed_request_id=delayed_request_2.id, step='request_role'),
        ], any_order=True
    )

    delayed_request_2.refresh_from_db()
    assert delayed_request_2.is_send is True
