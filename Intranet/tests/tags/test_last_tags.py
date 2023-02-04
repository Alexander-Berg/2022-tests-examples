import datetime

import pytest
from django.utils import timezone
from rest_framework import status

from intranet.crt.constants import TASK_TYPE
from intranet.crt.core.models import TaskTimestamp
from intranet.crt.utils.time import aware_to_timestamp

pytestmark = pytest.mark.django_db


def create_timestamp(now):
    start = now - datetime.timedelta(days=1, hours=1)
    timestamp = TaskTimestamp.objects.create(
        type=TASK_TYPE.SYNC_CVS_TAGS,
        finish=start + datetime.timedelta(hours=1),
        is_success=True,
    )

    timestamp.start = start
    timestamp.save()


def test_last_tags(crt_client, new_noc_certificates, users):
    create_timestamp(timezone.now())

    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    response = crt_client.json.get('/api/last-tags/')

    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()
    assert {'meta', 'certificates'} <= set(response_data.keys())
    assert len(response_data['certificates']) == 2

    cert2 = response_data['certificates'][0]
    assert cert2['serial_number'] == 'a2'
    assert cert2['tags'] == ['tag3', 'tag2', 'tag1']

    cert1 = response_data['certificates'][1]
    assert cert1['serial_number'] == 'a1'
    assert cert1['tags'] == ['tag3', 'tag2', 'tag1']


def test_last_tags_since(crt_client, new_noc_certificates, users):
    now = timezone.now()
    create_timestamp(now)

    normal_user = users['normal_user']
    crt_client.login(normal_user.username)
    data = {
        'since': aware_to_timestamp(now - datetime.timedelta(hours=5)),
    }

    response = crt_client.json.get('/api/last-tags/', data=data)

    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()
    assert {'meta', 'certificates'} <= set(response_data.keys())
    assert len(response_data['certificates']) == 1

    cert1 = response_data['certificates'][0]
    assert cert1['serial_number'] == 'a1'
    assert cert1['tags'] == ['tag3', 'tag2', 'tag1']
