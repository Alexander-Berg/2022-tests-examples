# coding: utf-8



import pytest

import at.common.models
from at.common import const
from at.aux_ import Profile


PERSON_DATA = {
    'uid': 100500,
    'login': 'employee',
    'name': {
        'first': {
            'ru': 'Иван',
            'en': 'Ivan',
        },
        'last': {
            'ru': 'Иванов',
            'en': 'Ivanov',
        },
    },
    'work_email': 'employee@yandex-team.ru',
    'memorial': False,
    'official': {
        'is_dismissed': False,
    },
    'personal': {
        'birthday': '2015-01-01',
        'gender': 'male',
    },

}


@pytest.mark.skip
@pytest.mark.django_db(transaction=True)
def test_create_blog_with_mail_settings():
    Profile.maybe_create_blog(
        uid=PERSON_DATA['uid'],
        data=PERSON_DATA,
    )
    assert at.common.models.MailSettings.objects.filter(
        person_id=PERSON_DATA['uid'],
        digest_mode=const.DIGEST_MODES.DISABLED,
    ).exists()
