from mock import MagicMock, call

from kelvin.mail.models import EmailHistory, ScheduledEmail
from kelvin.mail.tasks import check_scheduled_email


def test_check_scheduled_email(mocker):
    """Тест отправки рассылок по расписанию"""
    mocked_datetime = mocker.patch('kelvin.mail.tasks.datetime')
    mocked_datetime.now.return_value = 10
    strptime_args = []

    def mocked_strptime(*args):
        strptime_args.append(args)
        return args[0]
    mocked_datetime.strptime = mocked_strptime
    mocked_history_objects = mocker.patch.object(EmailHistory, 'objects')
    mocked_history_objects.create.return_value = EmailHistory(id=123)
    mocked_schedule_objects = mocker.patch.object(ScheduledEmail, 'objects')
    mocked_schedule_objects.all.return_value = [
        MagicMock(
            id=1,
            template='template',
            html_text='text',
            course='course',
            subject='subject',
            email_list='email_list',
            schedule=[
                {
                    'date': 1,
                    'sent': True,
                },
                {
                    'date': 2,
                    'sent': False,
                },
                {
                    'date': 3,
                    'sent': False,
                },
            ],
        ),
        MagicMock(
            id=2,
            template='template2',
            html_text='text2',
            course='course',
            subject='subject',
            email_list='email_list',
            schedule=[
                {
                    'date': 20,
                    'sent': False,
                },
            ],
        ),
    ]
    mocked_send_mails = mocker.patch('kelvin.mail.tasks.send_mails')
    mocked_send_mails.return_value = 'some subject'

    assert check_scheduled_email() is None
    assert mocked_send_mails.mock_calls == [
        call('template', 'text', 'course', 'subject', 'email_list',
             history_id=123),
        call('template', 'text', 'course', 'subject', 'email_list',
             history_id=123),
    ]
    assert strptime_args == [
        (2, '%Y-%m-%dT%H:%M:%SZ'),
        (3, '%Y-%m-%dT%H:%M:%SZ'),
        (20, '%Y-%m-%dT%H:%M:%SZ'),
    ]
    assert mocked_history_objects.mock_calls == [
        call.create(course='course', email_list='email_list',
                    html_text='text', subject='subject', template='template'),
        call.create(course='course', email_list='email_list',
                    html_text='text', subject='subject', template='template'),
    ]
