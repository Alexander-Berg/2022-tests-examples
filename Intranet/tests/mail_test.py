import pytest
from django.conf import settings
from mock import patch

from staff.django_intranet_notifications.mail import HTMLEmailMessage
from staff.django_intranet_notifications.transports import Transport


@patch('staff.django_intranet_notifications.mail.HTMLEmailMessage.prepare_headers')
@patch('staff.django_intranet_notifications.mail.HTMLEmailMessage.get_message_id')
@patch('staff.django_intranet_notifications.mail.HTMLEmailMessage.get_reply_id')
@patch('staff.django_intranet_notifications.mail.HTMLEmailMessage.get_template_path')
@patch('staff.django_intranet_notifications.mail.HTMLEmailMessage.render')
@patch('staff.django_intranet_notifications.mail.MessageWrapper')
def test_send(ph, gms, gri, gtp, ren, MW):
    HTMLEmailMessage().send()


@pytest.mark.parametrize(
    'params_in,params_out', [
        (
            {'assignee': 'asd'},
            {'assignee': 'asd'},
        ),
        (
            {'assignee': 'volozh'},
            {'access': ['volozh']},
        ),
        (
            {'followers': ['abovsky', 'asd']},
            {'access': ['abovsky'], 'followers': ['asd']},
        ),
        (
            {'recipients': ['volozh@yandex-team.ru', 'asd@yandex-team.ru']},
            {'recipients': ['asd@yandex-team.ru', 'tops-gaps@yandex-team.ru']},
        )
    ]
)
def test_replace_tops(params_in, params_out):
    settings.NOTIFICATIONS_FORBIDDEN_TO_NOTIFY = ['volozh', 'abovsky']
    assert Transport.replace_forbidden_to_notify(params_in) == params_out
