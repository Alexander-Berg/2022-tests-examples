import pytest

from collections import namedtuple
from unittest.mock import patch, Mock, call

from ok.notifications import approvements as notifications

from tests import factories as f
from tests.utils.assertions import assert_not_raises


pytestmark = pytest.mark.django_db


FakeStage = namedtuple('FakeStage', ['approver'])


def _has_app_notification(notification_class):
    return (
        issubclass(notification_class, notifications.BaseNotificationSender)
        and any(i.transport == 'xiva' for i in notification_class.nested_notification_classes)
    )


@pytest.mark.parametrize('notification_class,extra_data', (
    (notifications.ApprovementRequiredNotification, {'current_stages': [FakeStage('approver')]}),
    (notifications.ApprovementApprovedByResponsibleNotification, {'receivers': ['receiver']}),
    (notifications.ApprovementFinishedNotification, {}),
    (notifications.ApprovementCancelledNotification, {'current_stages': [FakeStage('approver')]}),
    (notifications.ApprovementQuestionNotification, {'comment': 'text'}),
    (notifications.ApprovementSuspendedNotification, {'current_stages': [FakeStage('approver')]}),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.push_notification_to_app_task.delay')
@patch('ok.notifications.send_email.delay')
def test_approvement_notification(mocked_email, mocked_app, notification_class, extra_data):
    f.create_waffle_switch('enable_mobile_app_notification', True)
    instance = f.create_approvement(author=f.UserFactory(username='author').username)
    f.UserFactory(username='approver')
    f.UserFactory(username='receiver')
    initiator = instance.author

    with assert_not_raises():
        notification = notification_class(instance, initiator, **extra_data)
        notification.send()
    assert mocked_email.called
    assert mocked_app.called is _has_app_notification(notification_class)


@pytest.mark.parametrize('notification_class, extra_data', (
    (notifications.ApprovementReminderNotification, {'approvements': []}),
    (notifications.ApprovementOverdueNotification, {'approvement_to_current_stages': {}}),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.push_notification_to_app_task.delay')
@patch('ok.notifications.send_email.delay')
def test_approvement_bulk_notification(mocked_email, mocked_app, notification_class, extra_data):
    f.create_waffle_switch('enable_mobile_app_notification', True)
    receiver = f.UserFactory(username='receiver')
    with assert_not_raises():
        notification_class(receiver=receiver.username, **extra_data).send()
    assert mocked_email.called
    assert mocked_app.called is _has_app_notification(notification_class)


@pytest.mark.parametrize('approvement_text, defined_languages, expected_languages', (
    # Первые 2 языка – языки для пользователей, 3й язык – язык для рассылок
    ('Approvement in English', ('', ''), ('en', 'en', 'en')),
    ('Согласование на русском', ('', ''), ('ru', 'ru', 'ru')),
    ('Approvement in English', ('ru', 'en'), ('ru', 'en', 'en')),
    ('Согласование на русском', ('en', 'ru'), ('en', 'ru', 'ru')),
    ('Согласование на русском', ('en', 'en'), ('en', 'en', 'ru')),
))
def test_get_language_by_receiver_email(approvement_text, defined_languages, expected_languages):
    """
    Тест проверяет, как работает метод get_language_by_receiver_email у нотификаций.
    Ожидается, что метод отдаёт словарь вида {email: language} для всех email-ов получателей.
    Важно, чтобы email получателя был в словаре, даже если невозможно получить для него язык
    – например, если получатель указан через info_mails_to и является рассылкой, а не пользователем.
    Если язык получателя определить нельзя, то берём язык, указанный по умолчанию.
    В отбивках по согласованиям, если в тексте согласования есть кириллица,
    по умолчанию используется русский язык, в противном случае — английский
    """
    approver = f.UserFactory(username='approver', language=defined_languages[0])
    known_user = f.UserFactory(username='known-user', language=defined_languages[1])
    instance = f.create_approvement(
        approvers=[approver.username],
        info_mails_to=[known_user.email, 'mail-list@yandex-team.ru'],
        text=approvement_text,
    )
    notification = notifications.ApprovementSuspendedEmailNotification(
        instance=instance,
        initiator=instance.author,
        current_stages=instance.stages.current().leaves(),
    )

    result = notification.get_language_by_receiver_email()

    expected = {
        'approver@yandex-team.ru': expected_languages[0],
        'known-user@yandex-team.ru': expected_languages[1],
        'mail-list@yandex-team.ru': expected_languages[2],
    }
    assert result == expected


@patch('ok.approvements.tracker.client')
@patch('ok.notifications.push_notification_to_app_task.delay')
def test_app_notification(task_mock, tracker_client_mock):
    f.create_waffle_switch('enable_mobile_app_notification', True)
    ru_approver = f.UserFactory(username='ru_approver', language='ru')
    en_approver = f.UserFactory(username='en_approver', language='en')

    instance = f.create_approvement(
        approvers=[ru_approver.username, en_approver.username],
        is_parallel=True,
    )
    summary = 'Some text'
    tracker_client_mock.issues.find.return_value = [Mock(key=instance.object_id, summary=summary)]
    notification = notifications.ApprovementRequiredAppNotification(
        instance=instance,
        initiator=instance.author,
        current_stages=instance.stages.current().leaves(),
    )
    notification.send()

    calls = [
        call(
            f'({instance.object_id}) {summary}',
            user_uids=[ru_approver.uid],
            title='Требуется согласование',
            url=instance.url,
            event='ApprovementRequiredAppNotification',
        ),
        call(
            f'({instance.object_id}) {summary}',
            user_uids=[en_approver.uid],
            title='Approval action required',
            url=instance.url,
            event='ApprovementRequiredAppNotification',
        ),
    ]
    task_mock.assert_has_calls(calls)
