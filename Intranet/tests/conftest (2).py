# coding: utf-8

from django.conf import settings

from at.aux_.models import Person

from tests.test_fixtures.fake_blackbox import mock_blackbox
from tests.test_fixtures.common_models import *
from tests.test_fixtures.aux_models import *
from tests.test_fixtures.mocking import *


mock_blackbox(settings.YAUTH_TEST_USER)


@pytest.fixture(scope='session')
def django_db_setup(django_db_setup, django_db_blocker):
    with django_db_blocker.unblock():
        test_club = Person(login=settings.AT_TEST_CLUB['name'],
                           person_id=settings.AT_TEST_CLUB['feed_id'],
                           has_access=True,
                           community_type='OPENED_COMMUNITY',
                           user_type='community'
                           )
        test_user = Person(login=settings.YAUTH_TEST_USER['login'],
                           person_id=settings.YAUTH_TEST_USER['uid'],
                           has_access=True,
                           community_type='NONE',
                           user_type='profile'
                           )
        test_club.save()
        test_user.save()

        for fake_user in FAKE_USERS:
            Person.objects.update_or_create(**fake_user)


