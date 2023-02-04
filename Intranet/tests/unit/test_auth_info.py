# coding: utf-8
import pytest
from django.db.models import Q

from at.aux_.models import Person
from tests.test_fixtures import fake_blackbox

from at.common.utils import getAuthInfo
from at.aux_ import models

pytestmark = pytest.mark.django_db

USER_UID = 1120000000000227
USER_LOGIN = 'kukutz'
DUMMY_UID = 2345
DUMMY_LOGIN = 'login-from-bb'


@pytest.fixture
def create_persons():
    Person.objects.filter(Q(person_id__in=[USER_UID, DUMMY_UID])
                          | Q(login__in=[USER_LOGIN, DUMMY_LOGIN])).delete()
    Person.objects.create(login=USER_LOGIN, person_id=USER_UID, community_type='NONE')
    Person.objects.create(login=DUMMY_LOGIN, person_id=DUMMY_UID, community_type='NONE')


@pytest.mark.usefixtures('create_persons')
def testShouldGetUidFromDatabaseByLoginExplicit():
    ai = getAuthInfo(login=USER_LOGIN)
    model = models.Person.get_by(login=USER_LOGIN)
    assert ai.uid == model.person_id


@pytest.mark.usefixtures('create_persons')
def testShouldGetLoginFromDatabaseByUidExplicit():
    ai = getAuthInfo(uid=USER_UID)
    model = models.Person.get_by(person_id=USER_UID)
    assert ai.login == model.login


@pytest.mark.usefixtures('create_persons')
def testShouldGetUidFromDatabaseByUidAny():
    ai = getAuthInfo(any=USER_UID)
    model = models.Person.get_by(person_id=USER_UID)
    assert ai.uid == model.person_id


@pytest.mark.usefixtures('create_persons')
def testShouldGetLoginFromDatabaseByLoginAny():
    ai = getAuthInfo(any=USER_UID)
    model = models.Person.get_by(login=USER_LOGIN)
    assert ai.uid == model.person_id


@pytest.mark.usefixtures('create_persons')
def testShouldCallBlackboxIfNoLoginInDatabase():
    fake_blackbox.mock_blackbox({DUMMY_LOGIN: DUMMY_UID})
    ai = getAuthInfo(login=DUMMY_LOGIN)
    assert ai.uid == DUMMY_UID


@pytest.mark.usefixtures('create_persons')
def testShouldCallBlackboxIfNoUidInDatabase():
    fake_blackbox.mock_blackbox({DUMMY_LOGIN: DUMMY_UID})
    ai = getAuthInfo(uid=DUMMY_UID)
    assert ai.login == DUMMY_LOGIN


@pytest.mark.usefixtures('create_persons')
def testShouldCallNothingIfBothParamsProvided():
    login = USER_LOGIN
    uid = USER_UID + 1
    ai = getAuthInfo(login=login, uid=uid)
    assert ai.login == login
    assert ai.uid != USER_UID
