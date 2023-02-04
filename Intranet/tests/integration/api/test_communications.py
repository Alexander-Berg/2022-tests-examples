import pytest

from copy import deepcopy
from unittest.mock import patch

from django.urls.base import reverse

from intranet.femida.src.communications.choices import (
    MESSAGE_TYPES,
    EXTERNAL_MESSAGE_TYPES,
    MESSAGE_STATUSES,
    MESSAGE_TEMPLATE_TYPES,
)
from intranet.femida.src.communications.models import Reminder
from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import dt_to_str, shifted_now_str


pytestmark = pytest.mark.django_db

EXTERNAL_MESSAGE_REQUIRED_DATA = (
    {
        'email': 'email@email.com',
        'subject': 'subject',
        'text': 'text',
    },
)


@pytest.mark.parametrize('timeshift, status_code, is_sending', (
    ({}, 201, True),
    ({'days': 1}, 201, False),
    ({'days': -1}, 400, False),
))
@pytest.mark.parametrize('data', deepcopy(EXTERNAL_MESSAGE_REQUIRED_DATA))
@patch('intranet.femida.src.communications.controllers.send_email_to_candidate')
def test_external_message_create(send_email_mock, su_client, data, timeshift, status_code,
                                 is_sending):
    candidate = f.CandidateFactory.create()
    url = reverse(
        viewname='api:communications:candidate-message-list',
        kwargs={'candidate_id': candidate.id},
    )
    if timeshift:
        data['schedule_time'] = shifted_now_str(**timeshift)
    response = su_client.post(url, data)
    assert response.status_code == status_code
    assert send_email_mock.called == is_sending


@pytest.mark.parametrize('type_, status, timeshift, status_code', (
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.scheduled, {'days': 1}, 200),
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.scheduled, {'days': -1}, 400),
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.sending, {}, 403),
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.sent, {}, 403),
    (EXTERNAL_MESSAGE_TYPES.incoming, MESSAGE_STATUSES.sent, {}, 403),
))
@pytest.mark.parametrize('data', deepcopy(EXTERNAL_MESSAGE_REQUIRED_DATA))
@patch('intranet.femida.src.communications.controllers.send_email_to_candidate')
def test_external_message_update(send_email_mock, su_client, data, type_, status, timeshift,
                                 status_code):
    # Редактировать можно только свое сообщение.
    # Мы отправляем запрос через su_client, значит автором сообщения должен быть суперпользователь.
    user = f.get_superuser()
    schedule_time = shifted_now(**timeshift) if timeshift else None
    message = f.create_message(
        type=type_,
        status=status,
        author=user,
        schedule_time=schedule_time,
    )

    if schedule_time:
        data['schedule_time'] = dt_to_str(schedule_time)
    url = reverse(
        'api:communications:candidate-message-detail',
        kwargs={
            'candidate_id': message.candidate_id,
            'pk': message.id,
        },
    )
    response = su_client.put(url, data)
    assert response.status_code == status_code
    assert not send_email_mock.called


@pytest.mark.parametrize('data', deepcopy(EXTERNAL_MESSAGE_REQUIRED_DATA))
def test_external_message_attachments_update(su_client, data):
    user = f.get_superuser()
    message = f.create_scheduled_message(author=user)
    attachment_to_keep = f.MessageAttachmentFactory(message=message).attachment
    f.MessageAttachmentFactory(message=message)  # attachment_to_delete

    attachment_to_insert = f.AttachmentFactory()
    data['attachments'] = [attachment_to_keep.id, attachment_to_insert.id]
    url = reverse(
        'api:communications:candidate-message-detail',
        kwargs={
            'candidate_id': message.candidate_id,
            'pk': message.id,
        },
    )

    response = su_client.put(url, data)
    assert response.status_code == 200
    response_attachments_ids = {a['id'] for a in response.data['attachments']}
    assert response_attachments_ids == {attachment_to_keep.id, attachment_to_insert.id}


@pytest.mark.parametrize('timeshift, status_code, is_sending', (
    ({'days': 2}, 200, False),  # отдалить время отправки
    ({'hours': 1}, 200, False),  # приблизить время отправки
    ({'days': -1}, 400, False),  # не смочь отправлять задним числом
    ({}, 200, True),  # удалить таймер
))
@pytest.mark.parametrize('data', deepcopy(EXTERNAL_MESSAGE_REQUIRED_DATA))
@patch('intranet.femida.src.communications.controllers.send_email_to_candidate')
def test_external_message_schedule_time_update(send_email_mock, su_client, data, timeshift,
                                               status_code, is_sending):
    user = f.get_superuser()
    message = f.create_scheduled_message(author=user)
    url = reverse(
        'api:communications:candidate-message-detail',
        kwargs={
            'candidate_id': message.candidate_id,
            'pk': message.id,
        },
    )
    data['schedule_time'] = shifted_now_str(**timeshift) if timeshift else None
    response = su_client.put(url, data)
    assert response.status_code == status_code
    assert send_email_mock.called == is_sending


@pytest.mark.parametrize('type_, status, status_code', (
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.scheduled, 204),
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.sending, 403),
    (EXTERNAL_MESSAGE_TYPES.outcoming, MESSAGE_STATUSES.sent, 403),
    (EXTERNAL_MESSAGE_TYPES.incoming, MESSAGE_STATUSES.sent, 403),
))
@patch('intranet.femida.src.communications.controllers.send_email_to_candidate')
def test_external_message_delete(send_email_mock, su_client, type_, status, status_code):
    user = f.get_superuser()
    message = (
        f.create_scheduled_message(author=user)
        if status == MESSAGE_STATUSES.scheduled
        else f.create_message(
            type=type_,
            status=status,
            author=user,
        )
    )
    url = reverse(
        'api:communications:candidate-message-detail',
        kwargs={
            'candidate_id': message.candidate_id,
            'pk': message.id,
        },
    )
    response = su_client.delete(url)
    assert response.status_code == status_code, response.content
    assert not send_email_mock.called


# TODO: FEMIDA-5739: удалить
def test_message_template_list_old(su_client):
    f.MessageTemplateFactory.create(text=(
        'Dear {{ candidate_name }},\n'
        'See you at {interview_datetime} in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
    ))
    url = reverse('api:communications:templates-list')
    response = su_client.get(url)
    assert response.status_code == 200, response.content
    template = response.json()['results'][0]
    assert template['text'] == (
        'Dear [ИМЯ_КАНДИДАТА],\n'
        'See you at [ДАТА/ВРЕМЯ] in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
    )


def test_message_template_list(su_client):
    candidate = f.CandidateFactory(first_name='Name')
    f.MessageTemplateFactory.create(
        type=MESSAGE_TEMPLATE_TYPES.communication,
        category_name='Category 0',
        name='Template 0',
    )
    f.MessageTemplateFactory.create(
        type=MESSAGE_TEMPLATE_TYPES.interview,
        name='Template 1',
        category_name='Category 1',
        text=(
            'Dear {{ candidate_name }},\n'
            'See you at {interview_datetime} in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
        )
    )
    f.MessageTemplateFactory(type=MESSAGE_TEMPLATE_TYPES.signature)  # неподходящий

    url = reverse(
        viewname='api:communications:candidate-message-template-list',
        kwargs={'candidate_id': candidate.id},
    )
    response = su_client.get(url)

    assert response.status_code == 200, response.content
    templates = response.json()['results']
    assert len(templates) == 2
    assert templates[1]['text'] == (
        'Dear Name,\n'
        'See you at [ДАТА/ВРЕМЯ] in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
    )


def test_candidate_message_list_external(su_client):
    candidate = f.create_candidate_with_consideration()
    user = f.get_superuser()
    f.create_message(
        author=user,
        candidate=candidate,
        type=MESSAGE_TYPES.outcoming,
    )
    url = reverse(
        'api:communications:candidate-message-list',
        kwargs={'candidate_id': candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_candidate_message_create_form(su_client):
    candidate = f.CandidateFactory()
    email = f.CandidateContactFactory(
        candidate=candidate,
        type=CONTACT_TYPES.email,
    )
    url = reverse(
        viewname='api:communications:candidate-message-create-form',
        kwargs={'candidate_id': candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200

    email_prefilled = response.data['structure']['email']['value']
    assert email_prefilled == email.normalized_account_id


###########
# Заметки #
###########


def test_note_list(su_client):
    message = f.create_message(type=MESSAGE_TYPES.note)
    url = reverse(
        'api:candidates:note-list',
        kwargs={'candidate_id': message.candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_note_create(su_client):
    candidate = f.CandidateFactory.create()
    data = {
        'text': 'text',
    }
    url = reverse(
        viewname='api:candidates:note-list',
        kwargs={'candidate_id': candidate.id},
    )
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_note_update(su_client):
    user = f.get_superuser()
    message = f.create_message(type=MESSAGE_TYPES.note, author=user)
    attachment_to_keep = f.MessageAttachmentFactory(message=message).attachment
    f.MessageAttachmentFactory(message=message)  # attachment_to_delete
    attachment_to_insert = f.AttachmentFactory()

    url = reverse(
        'api:candidates:note-detail',
        kwargs={
            'candidate_id': message.candidate.id,
            'pk': message.id,
        },
    )
    data = {
        'text': 'text',
        'attachments': [attachment_to_keep.id, attachment_to_insert.id],
    }
    response = su_client.put(url, data)
    assert response.status_code == 200
    response_attachments_ids = {a['id'] for a in response.data['attachments']}
    assert response_attachments_ids == {attachment_to_keep.id, attachment_to_insert.id}


def test_note_delete(su_client):
    user = f.get_superuser()
    message = f.create_message(type=MESSAGE_TYPES.note, author=user)
    url = reverse(
        'api:candidates:note-detail',
        kwargs={
            'candidate_id': message.candidate.id,
            'pk': message.id,
        },
    )
    response = su_client.delete(url)
    assert response.status_code == 204
    message.refresh_from_db()
    assert message.status == MESSAGE_STATUSES.deleted


def test_note_create_form(su_client):
    candidate = f.CandidateFactory()
    url = reverse(
        viewname='api:candidates:note-create-form',
        kwargs={'candidate_id': candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_note_update_form(su_client):
    message = f.create_message()
    url = reverse(
        'api:candidates:note-update-form',
        kwargs={
            'candidate_id': message.candidate.id,
            'pk': message.id
        }
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_note_relevant_reminders(su_client, django_assert_num_queries):
    user = f.get_superuser()
    message = f.create_note()
    f.ReminderFactory(message=message, user=user)
    f.ReminderFactory(message=message, user=f.create_user())
    url = reverse(
        'api:candidates:note-list',
        kwargs={'candidate_id': message.candidate.id},
    )
    with django_assert_num_queries(6):
        response = su_client.get(url)
    assert response.status_code == 200
    reminders_list = response.data['results'][0]['reminders']
    assert len(reminders_list) == 1
    assert reminders_list[0]['user']['username'] == user.username


def test_note_reminders_delete(su_client):
    user = f.get_superuser()
    message = f.create_note(author=user)
    f.ReminderFactory(message=message, user=user)
    f.ReminderFactory(message=message, user=f.create_user())
    url = reverse(
        'api:candidates:note-detail',
        kwargs={
            'candidate_id': message.candidate.id,
            'pk': message.id,
        },
    )
    # Проверяем, что вместе с заметкой удаляется напоминание автора заметки,
    # но не напоминания других пользователей
    assert Reminder.objects.count() == 2
    su_client.delete(url)
    assert Reminder.objects.count() == 1


def test_deleted_note_update_restriction(su_client):
    message = f.create_note(author=f.get_superuser(), status=MESSAGE_STATUSES.deleted)
    url = reverse(
        'api:candidates:note-detail',
        kwargs={
            'candidate_id': message.candidate.id,
            'pk': message.id
        }
    )
    data = {'text': 'text'}
    response = su_client.put(url, data)
    assert response.status_code == 404
