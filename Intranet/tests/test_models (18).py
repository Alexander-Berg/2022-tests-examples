from builtins import object, str

import pytest
from future.moves.itertools import zip_longest
from mock.mock import MagicMock, call

from django.conf import settings
from django.contrib.auth import get_user_model

from kelvin.courses.models import Course
from kelvin.mail.models import EmailTemplate, send_mails

User = get_user_model()


@pytest.mark.skip
class TestEmailTemplateModel(object):
    """
    Тесты модели EmailTemplate
    """
    def test_unicode(self):
        """
        Тест метода EmailTemplate.__str__
        """
        template = EmailTemplate(name='test', template='template')

        assert str(template) == 'test'

    def test_render(self, mocker):
        """
        Тест метода EmailTemplate.render

        Проверяем, что создаются ожидаемые джанговые Template и Context,
        вызывается render у Template с ожидаемыми аргументами и отрендеренное
        письмо проходит через премейлер
        """
        mocked_template = mocker.patch('kelvin.mail.models.Template')
        mocked_context = mocker.patch('kelvin.mail.models.Context')
        mocked_premailer = mocker.patch('kelvin.mail.models.transform')

        template = EmailTemplate(name='test', template='some_template')
        rendered = template.render({'some': 'context'})

        assert mocked_template.called_once_with('some_template')
        assert mocked_context.called_once_with({'some': 'context'})
        assert mocked_premailer.called_once_with(
            mocked_template.return_value.render.return_value)
        assert mocked_template.return_value.render.called_once_with(
            mocked_context.return_value), (
            'Template.render called with wrong arguments')
        assert rendered == mocked_premailer.return_value, (
            'should return result of built-in Template.render after premailer')


send_mails_data = (
    # отправка письма ученикам курса
    (
        None,
        'html message',
        MagicMock(
            id=1,
            copy_of=Course(id=2),
            students=MagicMock(**{
                "all.return_value": [
                    MagicMock(email='student1@1.com', unsubscribed=False),
                    MagicMock(email='student2@2.com', unsubscribed=True),
                    MagicMock(email='student3@3.com', unsubscribed=False),
                ],
            }),
        ),
        u'Тема',
        '1@1.com\n2@2.com',
        False,
        u'Тема',
        {'2@2.com', '1@1.com'},
        ['student1@1.com', 'student3@3.com'],
        'html message',
    ),
    # отправка письма отписанным
    (
        None,
        'html message',
        MagicMock(
            id=1,
            copy_of=Course(id=2),
            students=MagicMock(**{
                "all.return_value": [
                    MagicMock(email='student1@1.com', unsubscribed=False),
                    MagicMock(email='student2@2.com', unsubscribed=True),
                    MagicMock(email='student3@3.com', unsubscribed=False),
                ],
            }),
        ),
        u'Тема',
        '1@1.com\n2@2.com',
        True,
        u'Тема',
        {'2@2.com', '1@1.com'},
        ['student1@1.com', 'student2@2.com', 'student3@3.com'],
        'html message',
    ),
    # отправка письма группе пользователей
    (
        EmailTemplate(
            name=u'Тема из шаблона',
            template=u'Письмецо {{ inner }}'
        ),
        'html message',
        MagicMock(
            id=1,
            copy_of=Course(id=2),
        ),
        '',
        '',
        False,
        u'Тема из шаблона',
        [],
        {},
        u'<html>\n<head></head>\n<body><p>Письмецо html message</p></body>\n'
        u'</html>\n',
    ),
    # отправка письма ученикам копий курса
    (
        None,
        'html message',
        MagicMock(
            id=1,
            copy_of=None,
        ),
        u'Тема',
        '1@1.com\n2@2.com',
        False,
        u'Тема',
        {'2@2.com', '1@1.com'},
        ['student1', 'student2'],
        'html message',
    ),
)


@pytest.mark.skip()
@pytest.mark.parametrize('template,html_text,_course,subject,email_list,'
                         'force,expected_subject,expected_generic_recipients,'
                         'expected_personal_recipients,expected_message',
                         send_mails_data)
def test_send_mails(mocker, template, html_text, _course, subject, email_list,
                    force, expected_subject,
                    expected_generic_recipients, expected_personal_recipients,
                    expected_message):
    """Тест определения кому и что отправляется в `send_mails`"""
    mocked_course_objects = mocker.patch.object(Course, 'objects')
    mocked_course_objects.filter.return_value.prefetch_related.return_value = [
        MagicMock(**{
            'students.all.return_value': [
                MagicMock(email='student1', unsubscribed=False)],
        }),
        MagicMock(**{
            'students.all.return_value': [
                MagicMock(email='student2', unsubscribed=False)],
        }),
    ]
    mocked_email = mocker.patch('kelvin.mail.models.EmailMultiAlternatives')

    mocked_user_objects = mocker.patch.object(User, 'objects')
    mocked_user_objects.all.return_value = []

    assert send_mails(template, html_text, _course, subject, email_list,
                      force) == expected_subject

    # Проверяем вызовы отправки почты
    email_calls = mocked_email.mock_calls

    if expected_generic_recipients:
        assert email_calls[:3] == [
            call(bcc=list(expected_generic_recipients),
                 from_email=settings.DEFAULT_FROM_EMAIL,
                 subject=expected_subject),
            call().attach_alternative(expected_message, 'text/html'),
            call().send(),
        ]
        email_calls = email_calls[3:]

    personal_recipient_set = set(expected_personal_recipients)

    # Из-за использования множеств внутри вызовы создания отдельного письма
    # не упорядочены. Но есть паттерн создание-аттач-отправка
    def grouper(iterable, n, fillvalue=None):
        "Collect data into fixed-length chunks or blocks"
        # grouper('ABCDEFG', 3, 'x') --> ABC DEF Gxx"
        args = [iter(iterable)] * n
        return zip_longest(*args, fillvalue=fillvalue)

    for mail_call, attach_call, send_call in grouper(
            email_calls, 3):
        # кварги лежат в call[2]
        assert len(mail_call[2]['bcc']) == 1, u'Письмо создается индивидуально'
        assert mail_call[2]['bcc'][0] in personal_recipient_set
        personal_recipient_set.remove(mail_call[2]['bcc'][0])
        assert attach_call == call().attach_alternative(
            expected_message, 'text/html')
        assert send_call == call().send()

    assert len(personal_recipient_set) == 0, u'Не всем было отправлено письмо'
