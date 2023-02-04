# coding: utf-8

import pytest
from django.db.models import F, FilteredRelation, Q

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]

NOTE = {
    'text': 'no-no-no-note',
    'updated_at': '2018-05-29T20:00:02.103000+03:00',
    'created_at': '2018-05-25T14:42:22.195000+03:00',
}

EMPTY_NOTE = {'text': '', 'updated_at': None, 'created_at': None}


@pytest.mark.parametrize(
    'enquiry_id,username,ref',
    (
        (1, 'robot-procu', NOTE),
        (1, 'robot-procu-test', EMPTY_NOTE),
        (2, 'robot-procu', EMPTY_NOTE),
    ),
)
def test_get_enquiry_note(clients, enquiry_id, username, ref):

    client = clients['internal']
    prepare_user(client, username=username, roles=['admin'])

    resp = client.get(f'/api/enquiries/{enquiry_id}/note')
    assert_status(resp, 200)

    assert resp.json() == ref


def test_update_enquiry_note(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.patch('/api/enquiries/1/note', data={'text': 'foo bar baz'})
    assert_status(resp, 200)

    obj = (
        models.Enquiry.objects.annotate(
            enquiry_note=FilteredRelation(
                'notes', condition=Q(notes__user_id=1)
            )
        )
        .annotate(note=F('enquiry_note__text'))
        .get(id=1)
    )

    assert obj.note == 'foo bar baz'
