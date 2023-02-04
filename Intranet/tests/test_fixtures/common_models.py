# coding: utf-8



import pytest

from at.common import models
from at.common import const


@pytest.fixture
def mail_settings_builder(db, person_builder):
    def builder(**kwargs):
        params = {
            'comment_mode': const.COMMENT_MODES.ALL,
            'digest_mode': const.DIGEST_MODES.POPULAR,
            'notify_summons': True,
            'notify_invites': True,
            'notify_self': False,
            'notify_moderation': True,
        }
        if 'person' not in params:
            params['person'] = person_builder()
        params.update(kwargs)
        person = models.MailSettings.objects.create(**params)
        return person
    return builder


@pytest.fixture
def mail_settings(db, mail_settings_builder):
    return mail_settings_builder()
