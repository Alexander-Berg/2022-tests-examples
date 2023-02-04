import os
import random
import factory
import json
import pytest

from mock import patch, Mock

from datetime import date

from django.conf import settings
from django.core.urlresolvers import reverse
from staff.rfid.views import ShowNewCandidate

from staff.preprofile.tests.utils import PreprofileFactory
from staff.person.models import AFFILIATION, PHONE_PROTOCOLS

from staff.lib.testing import (
    ApiMethodTester,
    StaffFactory,
    StaffPhoneFactory,
    OfficeFactory,
    CityFactory,
    CountryFactory,
    DepartmentFactory
)

from staff.rfid.exceptions import RfidCodeCannotBeUsedError
from staff.rfid.constants import STATE, OWNER
from staff.rfid.controllers import Badges, Reserves, Anonym
from staff.rfid.models import ContractorFirm, Rfid

import logging
logger = logging.getLogger(__name__)


@pytest.fixture
def yandex_person(db, settings):
    russian_office = OfficeFactory(
        city=CityFactory(country=CountryFactory())
    )
    settings.RUSSIA_ID = russian_office.city.country_id
    return StaffFactory(
        affiliation=AFFILIATION.YANDEX,
        office=russian_office,
    )


class ContractorFactory(factory.DjangoModelFactory):
    class Meta:
        model = ContractorFirm


class RfidFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'rfid.Rfid'

    code = factory.Sequence(lambda x: str(x))


class AnonymFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'rfid.Badge'

    owner = OWNER.ANONYM
    rfid = factory.SubFactory(RfidFactory)
    first_name = factory.Sequence(lambda x: 'anon_first_name{}'.format(x))
    last_name = factory.Sequence(lambda x: 'anon_last_name{}'.format(x))


class CandidateFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'rfid.Badge'

    owner = OWNER.CANDIDATE
    rfid = factory.SubFactory(RfidFactory)
    first_name = factory.Sequence(lambda x: 'cand_first_name{}'.format(x))
    last_name = factory.Sequence(lambda x: 'cand_last_name{}'.format(x))


class EmployeeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'rfid.Badge'

    owner = OWNER.EMPLOYEE
    rfid = factory.SubFactory(RfidFactory)
    person = factory.SubFactory(StaffFactory)


class FakeAvatarStorage(object):
    def __init__(self, scope):
        pass

    def upload_by_file(self, id_, photo_file):
        pass

    def upload_by_url(self, id_, url):
        pass

    def delete(self, url):
        pass


PHOTO_PATH = os.path.join(settings.PROJECT_PATH, 'api', '1px.png')


# Sets of permissions used to test authorization on badge-editing methods
edit_permlists = (
    (),
    ('rfid.edit_employees',),
    ('rfid.edit_candidates',),
    ('rfid.edit_reserve',),
    ('rfid.list_anonymous',),
)


view_permlists = (
    (),
    ('rfid.list_employees',),
    ('rfid.list_candidates',),
    ('rfid.list_reserve',),
    ('rfid.list_anonymous',),
)


@pytest.mark.django_db
def test_meta(client):
    url = '/rfid-api/meta.json'
    tester = ApiMethodTester(client.get, url=url)
    ok = tester.with_perms()
    ok.assert_status(200)

    def permission_set(data):
        container = data['content']['permissions']
        return [k for (k, v) in container.items() if v]

    data = ok.call().json()

    content = data['content']
    for key in ('permissions', 'ownertypes', 'statuses', 'offices'):
        assert key in content

    assert permission_set(data) == []


@pytest.mark.django_db
def test_organisations(client):
    url = '/rfid-api/orgs.json'
    tester = ApiMethodTester(client.get, url)

    ContractorFirm.objects.create(name="Procter")
    ContractorFirm.objects.create(name="Gamble")

    ok = tester.with_perms().call()
    ok.assert_status(200)

    data = ok.json()
    assert len(data['content']) == 2
    names = [x['name'] for x in data['content']]
    assert sorted(names) == ['Gamble', 'Procter']


@pytest.mark.django_db
def test_check_code(client):
    url = "/rfid-api/check_code.json"
    tester = ApiMethodTester(client.get, url=url)

    tester = tester.with_perms()

    invalid = tester.call(code='aaa')
    invalid.assert_status(200)

    assert invalid.json()['errors'][0]['code'] == 'INVALID_PARAMS'
    assert 'code' in invalid.json()['errors'][0]

    ok = tester.call({'code': '123'})
    ok.assert_status(200)
    assert ok.json()['content']['status'] == 'new'

    Reserves().create(code=123)

    assert tester.call({'code': '123'}).json()['content']['status'] == 'in_reserve'

    rfid = Rfid.objects.get(code=123)
    Anonym(
        AnonymFactory.create(rfid=rfid),
        avatar_storage=FakeAvatarStorage('rfid')
    )

    status_ = tester.call({'code': '123'}).json()['content']['status']
    assert status_ == 'used'


@pytest.mark.django_db
def test_list_reserves(client):
    url = '/rfid-api/reserves/list.json'
    tester = ApiMethodTester(client.get, url=url)

    ok = tester.with_perms('rfid.list_reserve')
    ok.call().assert_status(200)

    content_ = ok.call().json()['content']
    assert len(content_['reserves']) == 0
    assert content_['count'] == 0
    assert content_['more'] is False

    Reserves().create(code=123)
    Reserves().create(code=456)
    Reserves().create(code=789)

    content_ = ok.call().json()['content']
    assert len(content_['reserves']) == 3
    assert content_['count'] == 3
    assert content_['more'] is False


@pytest.mark.django_db
def test_create_reserve(client):
    url = '/rfid-api/reserves/add.json'
    tester = ApiMethodTester(
        client.post,
        url=url,
    )

    for permset in [(), ('rfid.list_reserve',)]:
        tester.with_perms(*permset).assert_status(403)

    allowed = tester.with_perms('rfid.edit_reserve')

    response = allowed.call({'code': '123'}, content_type='application/json').result
    assert response.status_code == 200

    resp = allowed.call({'code': '123'}, content_type='application/json',).json()  # already used
    assert resp['content']['code'][0]['code'] == 'RfidCodeBusyError'

    invalid = allowed.call({'code': 'abyrvalg'})
    invalid.assert_status(200)
    assert invalid.json()['errors'][0]['code'] == 'INVALID_PARAMS'


@pytest.mark.django_db
def test_delete_reserve(client):
    url = '/rfid-api/reserves/delete.json'
    tester = ApiMethodTester(client.post, url=url)

    for permset in [(), ('rfid.list_reserve',)]:
        tester.with_perms(*permset).assert_status(403)

    ok = tester.with_perms('rfid.edit_reserve')

    Reserves().create(code=123)

    ok.call({'code': '123'}, content_type='application/json').assert_status(200)
    ok.call({'code': '123'}, content_type='application/json').assert_status(404)  # already deleted

    invalid = ok.call(code='abyrvalg')
    invalid.assert_status(200)
    assert invalid.json()['errors'][0]['code'] == 'INVALID_PARAMS'


@pytest.mark.django_db
def test_list_badges_rights(client, yandex_person):
    url = '/rfid-api/badges/list.json'
    tester = ApiMethodTester(client.get, url=url)

    anon = Anonym(AnonymFactory(), avatar_storage=FakeAvatarStorage('rfid'))
    emp = Badges().create_employee(person=yandex_person)

    empty = tester.with_perms().call({'limit': 100})
    empty.assert_status(200)
    assert len(empty.json()['content']['badges']) == 0

    # Only anons
    anons = tester.with_perms('rfid.list_anonymous').call({'limit': 100})
    anons.assert_status(200)
    badges = anons.json()['content']['badges']
    assert len(badges) == 1
    assert badges[0]['id'] == anon.id

    # Anons + employees
    anons_emps = tester.with_perms(
        'rfid.list_anonymous', 'rfid.list_employees').call({'limit': 100})
    anons_emps.assert_status(200)
    badges = anons_emps.json()['content']['badges']
    assert len(badges) == 2
    assert badges[0]['id'] == anon.id
    assert badges[1]['id'] == emp.id

    # Candidates are not listed by this method
    candidates = tester.with_perms('rfid.list_candidates').call({'limit': 100})

    candidates.assert_status(200)
    assert len(candidates.json()['content']['badges']) == 0


@pytest.mark.django_db
def test_list_badges_filters(client):
    # Too lazy to test filters, will do that if any bugs are found.
    pass


@pytest.mark.django_db
def test_deactivate_badge(client, yandex_person):
    url = '/rfid-api/badges/deactivate.json'
    tester = ApiMethodTester(client.post, url=url,)

    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=StaffFactory(login='recruiter'),
        department=DepartmentFactory(),
    )

    # Missing code
    tester.with_perms().call().assert_status(200)
    with open(PHOTO_PATH) as photo_file:
        anon = Badges().create_anonym(
            first_name='Charmander',
            last_name='Charizard',
            contractor=ContractorFactory(),
            avatar_storage=FakeAvatarStorage('rfid'),
            photo_file=photo_file
        )
    emp = Badges().create_employee(code=456, person=yandex_person)
    cand = Badges().create_candidate(code=789, preprofile_id=preprofile.id)

    def check_badge_type(badge, required_permission, state):
        # NO
        for permlist in edit_permlists:
            if required_permission in permlist:
                continue
            tester.with_perms(*permlist).call(
                {'id': badge.id},
                content_type='application/json',
            ).assert_status(403)

        # YES
        tester.with_perms(required_permission).call(
            {'id': badge.id},
            content_type='application/json',
        ).assert_status(200)

        badge = Badges().get(id=badge.id)
        assert badge.state == state

    check_badge_type(anon, 'rfid.edit_anonymous', STATE.INACTIVE)
    check_badge_type(emp, 'rfid.edit_employees', STATE.LOST)
    check_badge_type(cand, 'rfid.edit_candidates', STATE.INACTIVE)


BADGE_AND_PERMISSIONS = (
    ('anon', 'rfid.edit_anonymous'),
    ('emp', 'rfid.edit_employees'),
    ('cand', 'rfid.edit_candidates'),
)


@pytest.mark.django_db
@pytest.mark.parametrize('badge_and_perm', BADGE_AND_PERMISSIONS)
def test_reactivate_badge(client, yandex_person, badge_and_perm):
    url = '/rfid-api/badges/activate.json'
    tester = ApiMethodTester(client.post, url=url,)

    badges = {}

    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=StaffFactory(login='recruiter'),
        department=DepartmentFactory(),
    )

    # Missing code
    with open(PHOTO_PATH) as photo_file:
        badges['anon'] = Badges().create_anonym(
            first_name='Charmander',
            last_name='Charizard',
            contractor=ContractorFactory(),
            avatar_storage=FakeAvatarStorage('rfid'),
            photo_file=photo_file
        )
    badges['emp'] = Badges().create_employee(code=456, person=yandex_person)
    badges['cand'] = Badges().create_candidate(code=789, preprofile_id=preprofile.id)

    badge_name, edit_permission = badge_and_perm
    badge = badges[badge_name]

    tester.with_perms().call({'id': badge.id}).assert_status(403)
    response = tester.with_perms(edit_permission).call({'id': badge.id})
    assert response.result.status_code == 200
    response = json.loads(response.result.content)
    assert response['errors'][0]['code'] == 'activate_error'
    assert response['content']['id']['code'] == 'RfidUnavailableStateTransition'
    badge.deactivate()
    badge.block()
    response = tester.with_perms(edit_permission).call({'id': badge.id})
    assert response.result.status_code == 200
    response = json.loads(response.result.content)
    if badge_name == 'cand':
        # https://st.yandex-team.ru/STAFF-6041/
        assert response['errors'][0]['code'] == 'activate_error'
        assert response['content']['id']['code'] == 'RfidUnavailableStateTransition'
        assert Badges().get(id=badge.id).state == STATE.BLOCKED
    else:
        assert response['content']['state'] == STATE.ACTIVE
        assert Badges().get(id=badge.id).state == STATE.ACTIVE


@pytest.mark.django_db
def test_delete_badge(client, yandex_person):
    url = '/rfid-api/badges/delete.json'
    tester = ApiMethodTester(client.post, url=url,)

    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=StaffFactory(login='recruiter'),
        department=DepartmentFactory(),
    )

    # Missing code
    with open(PHOTO_PATH) as photo_file:
        anon = Badges().create_anonym(
            first_name='Charmander',
            last_name='Charizard',
            contractor=ContractorFactory(),
            avatar_storage=FakeAvatarStorage('rfid'),
            photo_file=photo_file
        )
    emp = Badges().create_employee(person=yandex_person)
    cand = Badges().create_candidate(preprofile_id=preprofile.id)

    badge_and_permissions = (
        (anon, 'rfid.edit_anonymous'),
        (emp, 'rfid.edit_employees'),
        (cand, 'rfid.edit_candidates')
    )

    for badge, edit_permission in badge_and_permissions:
        tester.with_perms().call({'id': badge.id}).assert_status(403)
        assert badge.state == STATE.NOCODE
        response = tester.with_perms(edit_permission).call({'id': badge.id})
        assert response.result.status_code == 200
        assert json.loads(response.result.content) == {'content': []}
        assert list(Badges().filter(id=badge.id)) == []


@pytest.mark.django_db
def test_view_badge(client, yandex_person):
    url = '/rfid-api/badges/show.json'
    tester = ApiMethodTester(client.get, url=url)

    bad = tester.with_perms().assert_status(200)  # No params
    assert 'errors' in bad.json()

    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=StaffFactory(login='recruiter'),
        department=DepartmentFactory(),
    )

    anon = Anonym(AnonymFactory(), avatar_storage=FakeAvatarStorage('rfid'))
    emp = Badges().create_employee(code=456, person=yandex_person)
    cand = Badges().create_candidate(code=789, preprofile_id=preprofile.id)

    def check_badge_type(badge, required_permission):
        # NO
        for permlist in view_permlists:
            if required_permission in permlist:
                continue
            tester.with_perms(*permlist).call(
                {'id': badge.id}
            ).assert_status(403)

        # YES
        ok = tester.with_perms(required_permission).call({'id': badge.id})
        ok.assert_status(200)
        logger.info('{}'.format(ok.json()))
        assert ok.json()['content']['id'] == badge.id

    check_badge_type(anon, 'rfid.list_anonymous')
    check_badge_type(emp, 'rfid.list_employees')
    check_badge_type(cand, 'rfid.list_candidates')


@pytest.mark.django_db
def test_new_employee(client):
    url = '/rfid-api/badges/employee.json'
    tester = ApiMethodTester(client.post, url=url)

    ok = tester.with_perms('rfid.edit_employees')

    person = StaffFactory(login='vovovolozh')

    ok.call({'login': person.login, 'code': '1234'}, content_type='application/json').assert_status(200)
    bad = ok.call(login=person.login).assert_status(200)  # Already has a badge
    assert 'errors' in bad.json()


@pytest.mark.django_db
def test_anonymous(client):
    tester = ApiMethodTester(
        client.post,
        '/rfid-api/badges/anonymous.json',
    )

    tester.with_perms().assert_status(403)

    permitted = tester.with_perms('rfid.edit_anonymous')

    assert len(list(Badges().filter(owner=OWNER.ANONYM))) == 0

    Anonym.avatar_storage_class = FakeAvatarStorage
    with open(PHOTO_PATH, 'rb') as photo_file:
        response = permitted.call(
            first_name='Harrison',
            last_name='Ford',
            contractor=ContractorFactory(name="Jabba's Cartel").id,
            photo_file=photo_file,
            anonym_food_allowed='true'
        ).result
        assert response.status_code == 200
        assert 'error' not in response.content.decode('utf-8')  # '{"content": {"id": 1}}'

    han = Badges().get(last_name="Ford")

    assert han.full_name == "Harrison Ford"
    assert not han.anonym_food_allowed

    # Trying to change food_allowance without rights
    response = permitted.call(
        id=han.id,
        first_name=han.first_name,
        last_name=han.last_name,
        contractor=han.contractor.id,
        anonym_food_allowed='true',
    ).result
    assert response.status_code == 200

    han = Badges().get(id=han.id)
    assert not han.anonym_food_allowed

    # Trying to change food_allowance with rights
    response = permitted.with_perms(
        'rfid.edit_anonymous',
        'rfid.change_anonym_food').call(
            id=han.id,
            first_name=han.first_name,
            last_name=han.last_name,
            contractor=han.contractor.id,
            anonym_food_allowed='true',
    ).result
    assert response.status_code == 200

    han = Badges().get(id=han.id)
    assert han.anonym_food_allowed

    # changing existed anonym badge
    assert han.contractor_name == "Jabba's Cartel"
    with open(PHOTO_PATH, 'rb') as photo_file:
        leia = permitted.call(
            id=han.id,
            contractor=ContractorFactory(name="The Rebels").id,
            first_name='Han',
            last_name='Salo',
            code='1234',
            photo_file=photo_file,
        )
    leia.assert_status(200)

    han = Badges().get(id=han.id)
    assert han.contractor_name == 'The Rebels'
    assert han.full_name == 'Han Salo'


@pytest.mark.django_db
def test_create_candidate(client, yandex_person, monkeypatch):
    tester = ApiMethodTester(
        client.post,
        '/rfid-api/badges/candidate.json',
    )
    recruiter = StaffFactory(login='recruiter')
    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        department=DepartmentFactory()
    )

    tester.with_perms().assert_status(403)

    ok = tester.with_perms('rfid.edit_candidates', 'preprofile.add_personadoptapplication')
    response = ok.call(
        {
            'preprofile_id': str(preprofile.id),
            'code': '4321',
        },
        content_type='application/json'
    ).result
    assert response.status_code == 200
    assert len(list(Badges().filter(owner=OWNER.CANDIDATE))) == 1


@pytest.mark.django_db
def test_edit_candidate(client):
    tester = ApiMethodTester(
        client.post,
        '/rfid-api/badges/candidate.json',
    )
    ok = tester.with_perms('rfid.edit_candidates')

    cand = Badges().create_candidate(
        last_name="Ford", first_name="Harrison")
    response = ok.call(
        {
            'first_name_en': 'Han',
            'last_name_en': 'Solo',
            'id': cand.id,
            'code': '111',
        },
        content_type='application/json',
    ).result
    assert response.status_code == 200

    han = Badges().get(id=cand.id)

    assert han.full_name == "Harrison Ford"

    leia = ok.call(id=han.id)
    leia.assert_status(200)


@pytest.mark.django_db
def test_list_candidates(client):
    tester = ApiMethodTester(client.get, '/rfid-api/newcandidates/list.json')

    recruiter = StaffFactory()
    PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonuychka',
        recruiter=recruiter,
        department=DepartmentFactory(),
    )
    PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonuychka',
        recruiter=recruiter,
        department=DepartmentFactory(),
    )

    tester.with_perms().assert_status(403)

    ok = tester.with_perms(
        'rfid.list_candidates',
        'preprofile.add_personadoptapplication',
    )
    ok.call(limit=20)
    ok.assert_status(200)

    Badges().create_candidate(  # бейджик, не ссылающийся ни на один препрофайл
        code=123,
        first_name='Not this name',
        last_name='This is overwritten',
        join_at=date.today(),
    )

    data = ok.call({'limit': 10}).json()

    assert data['content']['count'] == 2
    for candidate_data in data['content']['candidates']:
        assert 'first_name' in candidate_data
        assert 'last_name' in candidate_data


@pytest.mark.django_db
def test_single_candidate_has_login(client):
    tester = ApiMethodTester(client.get, '/rfid-api/newcandidates/show.json')
    login = 'some_login'
    recruiter = StaffFactory()
    preprofile = PreprofileFactory(
        first_name='Koluychka',
        last_name='Vonuychka',
        recruiter=recruiter,
        login=login,
        department=DepartmentFactory(),
    )

    response = tester.with_perms(
        'preprofile.can_view_all_preprofiles',
        'rfid.edit_candidates',
    ).call(
        data={'preprofile_id': preprofile.id},
    )
    received = json.loads(response.result.content)
    assert received['content']['login'] == login


@pytest.mark.django_db
def test_employee_using_reserve(yandex_person):
    RESERVE_CODE = '123'
    Reserves().create(code=RESERVE_CODE)

    non_russian_office = OfficeFactory(
        city=CityFactory(country=CountryFactory())
    )

    non_yandex_person = StaffFactory(
        affiliation=AFFILIATION.YAMONEY, office=yandex_person.office
    )
    non_russian_person = StaffFactory(
        affiliation=AFFILIATION.YANDEX, office=non_russian_office)

    Badges().create_employee(person=yandex_person)
    assert len(list(
        Badges().filter(person=yandex_person, owner=OWNER.EMPLOYEE)
    )) == 1
    with pytest.raises(RfidCodeCannotBeUsedError):
        Badges().create_employee(person=non_yandex_person, code=RESERVE_CODE)
        Badges().create_employee(person=non_russian_person, code=RESERVE_CODE)


@pytest.mark.django_db
def test_sexport(client):
    tester = ApiMethodTester(client.get, reverse('rfid-api:export'))
    tester.with_perms().assert_status(403)

    permitted = tester.with_perms('rfid.use_list_handle')
    response = permitted.call().result
    assert response.status_code == 200
    assert response.content == b''

    anonymous_without_food = AnonymFactory(state=STATE.ACTIVE)
    anonymous_with_food = AnonymFactory(
        state=STATE.ACTIVE, anonym_food_allowed=True)

    employee = EmployeeFactory(state=STATE.ACTIVE)
    candidate = CandidateFactory(state=STATE.ACTIVE, person=StaffFactory())

    Reserves().create(code=32254)

    response = permitted.call().result
    assert response.status_code == 200

    anonymous_with_food_substr = '{fn} ; {ln} ; anonym ; 2'.format(
        fn=anonymous_with_food.first_name,
        ln=anonymous_with_food.last_name,
    )
    result = response.content.decode('utf-8')
    assert anonymous_with_food_substr in result
    assert anonymous_without_food.first_name not in result
    assert anonymous_without_food.last_name not in result
    assert employee.person.login in result
    assert candidate.first_name in result


@pytest.mark.django_db
def test_sexport_login(client, yandex_person):
    tester = ApiMethodTester(client.get, reverse('rfid-api:export-login', kwargs={'login': 'tester'}))
    tester.with_perms().assert_status(403)

    permitted = tester.with_perms('rfid.use_key_handle')
    response = permitted.call().result
    assert response.status_code == 404

    tester = ApiMethodTester(client.get, reverse('rfid-api:export-login', kwargs={'login': yandex_person.login}))
    response = tester.with_perms('rfid.use_key_handle').call().result
    assert response.status_code == 404

    EmployeeFactory(person=yandex_person)
    response = tester.with_perms('rfid.use_key_handle').call().result
    assert response.status_code == 404

    Badges().create_employee(code=123, person=yandex_person)
    response = tester.with_perms('rfid.use_key_handle').call().result
    response = json.loads(response.content)

    assert response['code']
    assert response['login'] == yandex_person.login


@pytest.mark.django_db
def test_newcandidates_show(client):
    preprofile = PreprofileFactory(
        department=DepartmentFactory(),
        first_name='Родион',
        last_name='Раскольников',
        first_name_en='Mata',
        last_name_en='Hari',
    )
    url = '/rfid-api/{url}?preprofile_id={preprofile_id}'.format(
        url=ShowNewCandidate.url.format(format='json'),
        preprofile_id=preprofile.id,
    )
    tester = ApiMethodTester(client.get, url)
    response = tester.with_perms(
        'preprofile.can_view_all_preprofiles',
        'rfid.edit_candidates',
    ).call().result

    response = json.loads(response.content)
    for field_name in ['first_name', 'last_name', 'first_name_en', 'last_name_en']:
        assert response['content'][field_name] == getattr(preprofile, field_name)


@pytest.mark.django_db
def test_newcandidates_show_unknown_pre_profile(client):
    url = '/rfid-api/{url}?preprofile_id={preprofile_id}'.format(
        url=ShowNewCandidate.url.format(format='json'),
        preprofile_id=random.randint(1, 10000),
    )
    tester = ApiMethodTester(client.get, url)

    response = tester.with_perms(
        'preprofile.can_view_all_preprofiles',
        'rfid.edit_candidates',
    ).call().result

    assert response.status_code == 404


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    response = client.get(url)
    assert response.status_code == 200
    assert response.content.decode('utf-8') == '[]'

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34')
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)
    EmployeeFactory(state=STATE.LOST, person=tester)

    employee_person = StaffFactory()
    employee = EmployeeFactory(state=STATE.ACTIVE, person=employee_person)
    CandidateFactory(state=STATE.ACTIVE, person=StaffFactory())
    AnonymFactory(state=STATE.ACTIVE)
    anonymous_with_food = AnonymFactory(state=STATE.ACTIVE, anonym_food_allowed=True)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': '+79999871234',
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
        {
            'type': 'Guest',
            'id': int(anonymous_with_food.id),
            'badges': [{'status': 'active', 'rfid': int(anonymous_with_food.rfid.code)}]
        },
        {
            'type': 'Employee',
            'firstName': employee_person.first_name,
            'lastName': employee_person.last_name,
            'middleName': '',
            'phone': None,
            'login': employee.person.login,
            'badges': [{'status': 'active', 'rfid': int(employee.rfid.code)}]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers_only_all_phones(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34', protocol=PHONE_PROTOCOLS.ALL, position=0)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-35', protocol=PHONE_PROTOCOLS.ALL, position=1)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-36', protocol=PHONE_PROTOCOLS.ALL, position=2)
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': '+79999871234',
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers_only_sms_phones(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34', protocol=PHONE_PROTOCOLS.SMS, position=0)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-35', protocol=PHONE_PROTOCOLS.SMS, position=1)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-36', protocol=PHONE_PROTOCOLS.SMS, position=2)
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': '+79999871234',
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers_only_voice_phones(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34', protocol=PHONE_PROTOCOLS.VOICE, position=0)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-35', protocol=PHONE_PROTOCOLS.VOICE, position=1)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-36', protocol=PHONE_PROTOCOLS.VOICE, position=2)
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': None,
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers_various_phones(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34', protocol=PHONE_PROTOCOLS.SMS, position=0)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-35', protocol=PHONE_PROTOCOLS.ALL, position=1)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-36', protocol=PHONE_PROTOCOLS.VOICE, position=2)
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': '+79999871234',
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)


@patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
@pytest.mark.django_db
def test_export_payers_invalid_sms_phones(client, rfid_cmp):
    url = reverse('rfid-api:export-payers')

    tester = StaffFactory(login='tester1', first_name='first', last_name='last', middle_name='middle')
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-34', protocol=PHONE_PROTOCOLS.VOICE, position=0)
    StaffPhoneFactory(staff=tester, number='+7 999 987-12-35', protocol=PHONE_PROTOCOLS.ALL, position=1)
    StaffPhoneFactory(staff=tester, number='invalid_number', protocol=PHONE_PROTOCOLS.SMS, position=2)
    test_active_rfid = EmployeeFactory(state=STATE.ACTIVE, person=tester)

    response = client.get(url)
    assert response.status_code == 200

    answer = [
        {
            'type': 'Employee',
            'firstName': 'first',
            'lastName': 'last',
            'middleName': 'middle',
            'phone': '+79999871235',
            'login': tester.login,
            'badges': [
                {'status': 'active', 'rfid': int(test_active_rfid.rfid.code)},
            ]
        },
    ]

    assert sorted(json.loads(response.content), key=rfid_cmp) == sorted(answer, key=rfid_cmp)
