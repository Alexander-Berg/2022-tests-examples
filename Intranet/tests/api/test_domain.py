from datetime import datetime, timezone
import pytest
from asynctest import patch
from sqlalchemy import and_
from typing import List

from intranet.domenator.src.api.schemas.domain import DomainOutSchema
from intranet.domenator.src.db import db, Domain, DomainHistory, MailUserDomain, MailUserDomainStatus
from intranet.domenator.src.db.domain.models import DomainAction
from intranet.domenator.src.logic.domain import to_punycode

pytestmark = pytest.mark.asyncio


@pytest.fixture
async def domain_owned(db_bind):
    async with db_bind as engine:
        return await Domain.create(
            name='test.ru',
            admin_id='123',
            org_id='123',
            owned=True,
            bind=engine,
        )


@pytest.fixture
async def domain_not_owned(db_bind):
    async with db_bind as engine:
        return await Domain.create(
            name='test.ru',
            admin_id='123',
            org_id='1234',
            bind=engine,
        )


@pytest.fixture()
async def domains(db_bind):
    async with db_bind as engine:
        return [
            await Domain.create(
                name='test1.ru',
                admin_id='1',
                org_id='1',
                bind=engine,
                owned=True,
                master=True,
            ),
            await Domain.create(
                name='test2.ru',
                admin_id='2',
                org_id='2',
                bind=engine,
            ),
            await Domain.create(
                name='test3.ru',
                admin_id='3',
                org_id='3',
                bind=engine,
            ),
            await Domain.create(
                name='test2-master.ru',
                admin_id='2',
                org_id='2',
                bind=engine,
                master=True,
            ),
        ]


@pytest.fixture()
async def domain_history(db_bind):
    async with db_bind as engine:
        return [
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_added,
                timestamp=datetime(2020, 1, 1, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_occupied,
                timestamp=datetime(2020, 1, 2, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_deleted,
                timestamp=datetime(2020, 1, 3, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),

            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_added,
                timestamp=datetime(2020, 2, 1, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_occupied,
                timestamp=datetime(2020, 2, 2, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_deleted,
                timestamp=datetime(2020, 2, 3, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),

            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_added,
                timestamp=datetime(2020, 2, 3, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_occupied,
                timestamp=datetime(2020, 3, 2, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),
            await DomainHistory.create(
                org_id='1',
                name='test1.ru',
                action=DomainAction.domain_deleted,
                timestamp=datetime(2020, 3, 3, tzinfo=timezone.utc),
                author_id='11',
                bind=engine,
            ),

            await DomainHistory.create(
                org_id='1',
                name='test2.ru',
                action=DomainAction.domain_added,
                timestamp=datetime(2020, 1, 1, tzinfo=timezone.utc),
                author_id='22',
                bind=engine,
            ),
        ]


async def test_who_is_success(client, domain_owned: Domain):
    response = await client.get(
        f'api/domains/who-is/?domain={domain_owned.name}'
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {
        'org_id': int(domain_owned.org_id),
        'type': 'domain',
        'object_id': domain_owned.name,
    }


async def test_who_is_fail_not_own(client, domain_not_owned: Domain):
    response = await client.get(
        f'api/domains/who-is/?domain={domain_not_owned.name}'
    )
    assert response.status_code == 404, response.text


async def test_who_is_fail_no_obj(client):
    response = await client.get(
        'api/domains/who-is/?domain=test.ru'
    )
    assert response.status_code == 404, response.text


async def test_ownership_info_owned(client, domain_owned: Domain):
    response = await client.get(
        f'api/domains/{domain_owned.org_id}/{domain_owned.name}/ownership-info/'
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {
        'domain': 'test.ru',
        'last_check': None,
        'methods': None,
        'preferred_host': 'test.ru',
        'status': 'owned',
    }


async def test_ownership_info_webmaster_error(client, domain_not_owned: Domain, test_vcr):
    with test_vcr.use_cassette('test_ownership_info_webmaster_error.yaml'):
        response = await client.get(
            f'api/domains/{domain_not_owned.org_id}/{domain_not_owned.name}/ownership-info/'
        )
    assert response.status_code == 422, response.text


async def test_ownership_info_not_owned(client, domain_not_owned: Domain, test_vcr):
    with patch('intranet.domenator.src.api.routes.domain.sync_domain_state') as sync_domain:
        with test_vcr.use_cassette('test_ownership_info_not_owned.yaml'):
            response = await client.get(
                f'api/domains/{domain_not_owned.org_id}/{domain_not_owned.name}/ownership-info/'
            )
        sync_domain.assert_called_once()
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {
        'domain': 'test.ru',
        'last_check': None,
        'methods': [{'code': 123, 'method': 'webmaster.dns', 'weight': 0}],
        'preferred_host': 'www.aite-dev1.yaconnect.com',
        'status': 'in-progress',
    }


async def test_check_ownership(client, domain_not_owned: Domain, test_vcr):
    with patch('intranet.domenator.src.api.routes.domain.sync_domain_state') as sync_domain:
        with test_vcr.use_cassette('test_check_ownership.yaml'):
            response = await client.post(
                f'api/domains/{domain_not_owned.org_id}/{domain_not_owned.name}/check-ownership/',
                json={'verification_type': 'webmaster.DNS'},
            )
        sync_domain.assert_called_once()
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {
        'domain': 'test.ru',
        'owned': False,
    }


async def test_check_ownership_bad_request(client, domain_not_owned: Domain, test_vcr):
    with patch('intranet.domenator.src.api.routes.domain.sync_domain_state') as sync_domain:
        response = await client.post(
            f'api/domains/{domain_not_owned.org_id}/{domain_not_owned.name}/check-ownership/',
            json={'verification_type': 'webmaster.SMTH'},
        )
        sync_domain.assert_not_called()
    assert response.status_code == 400, response.text


async def test_get_domains(client, domains: list):
    response = await client.get('api/domains/?org_ids=1,2')
    assert response.status_code == 200, response.text

    result_domains = [
        DomainOutSchema().from_orm(domain).dict(include={'name'})
        for domain in domains
        if domain.org_id in ('1', '2',)
    ]
    assert response.json() == result_domains


async def test_get_cyrillic_domains(client, db_bind):
    domain_name = 'тест.рф'
    async with db_bind as engine:
        await Domain.create(
            org_id='1',
            admin_id='1',
            name=to_punycode(domain_name),
            bind=engine,
        )

    response = await client.get('api/domains/?org_ids=1')
    assert response.status_code == 200, response.text
    assert response.json()[0]['name'] == domain_name


async def test_get_domains_with_fields(client, domains: list, test_vcr):
    fields = {'org_id', 'name', 'owned', 'master', 'tech', 'mx', 'delegated', }
    with test_vcr.use_cassette('test_get_domains_with_fields.yaml'):
        fields_str = ','.join(fields)
        response = await client.get(f'api/domains/?org_ids=1,2&name=test1.ru&fields={fields_str}')

    assert response.status_code == 200, response.text
    data = response.json()[0]

    assert data == {
        'org_id': int('1'),
        'name': 'test1.ru',
        'owned': True,
        'master': True,
        'tech': False,
        'mx': True,
        'delegated': True,
    }


async def test_get_domains_without_passed_org_ids(client, domains: list, test_vcr):
    response = await client.get('api/domains/')
    assert response.status_code == 422, response.text


async def test_delete_master_domain(client, domains: List[Domain]):
    domain_name = domains[0].name
    org_id = domains[0].org_id
    admin_uid = domains[0].admin_id
    author_uid = 111

    response = await client.delete(f'api/domains/{domain_name}', params={
        'org_id': org_id,
        'admin_uid': admin_uid,
        'author_uid': author_uid,
    })
    assert response.status_code == 422, response.text


async def test_delete_domain_success(client, domains: List[Domain], test_vcr):
    domain_name = domains[1].name
    org_id = domains[1].org_id
    admin_uid = domains[1].admin_id
    author_id = '111'

    with test_vcr.use_cassette('test_domains_delete_domain_success.yaml'):
        response = await client.delete(f'api/domains/{domain_name}', params={
            'org_id': org_id,
            'admin_uid': admin_uid,
            'author_id': author_id,
        })
    assert response.status_code == 200, response.text


async def test_delete_domain_blackbox_500(client, domains: List[Domain], test_vcr):
    domain_name = domains[1].name
    org_id = domains[1].org_id
    admin_uid = domains[1].admin_id
    author_id = '111'

    with test_vcr.use_cassette('test_domains_delete_domain_blackbox_500.yaml'):
        response = await client.delete(f'api/domains/{domain_name}', params={
            'org_id': org_id,
            'admin_uid': admin_uid,
            'author_id': author_id,
        })
    assert response.status_code == 424, response.text


async def test_delete_domain_passport_500(client, domains: List[Domain], test_vcr):
    domain_name = domains[1].name
    org_id = domains[1].org_id
    admin_uid = domains[1].admin_id
    author_id = '111'

    with test_vcr.use_cassette('test_domains_delete_domain_passport_500.yaml'):
        response = await client.delete(f'api/domains/{domain_name}', params={
            'org_id': org_id,
            'admin_uid': admin_uid,
            'author_id': author_id,
        })
    assert response.status_code == 424, response.text


async def test_domain_history_saved_when_deleting_domain(client, domains: List[Domain], test_vcr):
    domain_name = domains[1].name
    org_id = domains[1].org_id
    admin_uid = domains[1].admin_id
    author_id = '111'

    with test_vcr.use_cassette('test_domain_history_saved_when_deleting_domain.yaml'):
        await client.delete(f'api/domains/{domain_name}', params={
            'org_id': org_id,
            'admin_uid': admin_uid,
            'author_id': author_id,
        })

    domain_history = await DomainHistory.query.where(
        and_(
            DomainHistory.org_id == org_id,
            DomainHistory.name == domain_name,
            DomainHistory.action == DomainAction.domain_deleted,
            DomainHistory.author_id == author_id,
        )
    ).gino.first()

    assert domain_history is not None


async def test_get_domain_history_with_pagination(client, domain_history):
    domain_name = 'test1.ru'
    expected_data = [
        [
            {"org_id": '1', "name": "test1.ru", "action": "domain_deleted", "author_id": "11",
             "timestamp": "2020-03-03T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_occupied", "author_id": "11",
             "timestamp": "2020-03-02T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_added", "author_id": "11",
             "timestamp": "2020-02-03T00:00:00+00:00"}
        ],
        [
            {"org_id": '1', "name": "test1.ru", "action": "domain_deleted", "author_id": "11",
             "timestamp": "2020-02-03T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_occupied", "author_id": "11",
             "timestamp": "2020-02-02T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_added", "author_id": "11",
             "timestamp": "2020-02-01T00:00:00+00:00"}
        ],
        [
            {"org_id": '1', "name": "test1.ru", "action": "domain_deleted", "author_id": "11",
             "timestamp": "2020-01-03T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_occupied", "author_id": "11",
             "timestamp": "2020-01-02T00:00:00+00:00"},
            {"org_id": '1', "name": "test1.ru", "action": "domain_added", "author_id": "11",
             "timestamp": "2020-01-01T00:00:00+00:00"}
        ],
    ]

    response = await client.get(f'api/domains/{domain_name}/history?limit=3')
    assert response.status_code == 200, response.text
    response_json = response.json()
    assert all([
        expected.items() <= actual.items()
        for expected, actual in zip(expected_data[0], response_json['result'])
    ])

    next_url = response_json['next']
    response = await client.get(next_url)
    assert response.status_code == 200, response.text
    response_json = response.json()
    assert all([
        expected.items() <= actual.items()
        for expected, actual in zip(expected_data[1], response_json['result'])
    ])

    next_url = response_json['next']
    response = await client.get(next_url)
    assert response.status_code == 200, response.text
    response_json = response.json()
    assert all([
        expected.items() <= actual.items()
        for expected, actual in zip(expected_data[2], response_json['result'])
    ])
    assert response_json['next'] is None

    prev_url = response_json['prev']
    response = await client.get(prev_url)
    assert response.status_code == 200, response.text
    response_json = response.json()
    assert all([
        expected.items() <= actual.items()
        for expected, actual in zip(expected_data[1], response_json['result'])
    ])

    prev_url = response_json['prev']
    response = await client.get(prev_url)
    assert response.status_code == 200, response.text
    response_json = response.json()
    assert all([
        expected.items() <= actual.items()
        for expected, actual in zip(expected_data[0], response_json['result'])
    ])
    assert response_json['prev'] is None


async def test_add_domain_success(client, test_vcr):
    domain_name = 'тест.рф'
    with test_vcr.use_cassette('test_add_domain_success.yaml'):
        response = await client.post('api/domains', json={
            'org_id': '1',
            'domain': domain_name,
            'admin_uid': '2',
        })
        assert response.status_code == 200, response.text


async def test_add_domain_invalid_domain(client):
    domain_name = 'тестрф'
    response = await client.post('api/domains', json={
        'org_id': '1',
        'domain': domain_name,
        'admin_uid': '2',
    })
    assert response.status_code == 422, response.text
    assert response.json()['error'] == 'invalid_domain'


async def test_add_domain_already_existent_in_same_org(client, db_bind):
    org_id = '1'
    domain_name = 'тест.рф'
    async with db_bind as engine:
        await Domain.create(
            org_id=org_id,
            admin_id='2',
            name=to_punycode(domain_name),
            bind=engine,
        )

    response = await client.post('api/domains', json={
        'org_id': org_id,
        'domain': domain_name,
        'admin_uid': '3',
    })

    assert response.status_code == 422, response.text
    response_data = response.json()
    assert response_data['error'] == 'duplicate_domain'
    assert response_data['context']['conflicting_org_id'] == '1'


async def test_add_domain_already_linked_with_same_admin(client, db_bind):
    admin_uid = '3'
    domain_name = 'тест.рф'
    async with db_bind as engine:
        await Domain.create(
            org_id='1',
            admin_id=admin_uid,
            name=to_punycode(domain_name),
            bind=engine,
        )

    response = await client.post('api/domains', json={
        'org_id': '2',
        'domain': domain_name,
        'admin_uid': admin_uid,
    })

    assert response.status_code == 422, response.text
    response_data = response.json()
    assert response_data['error'] == 'duplicate_domain'
    assert response_data['context']['conflicting_org_id'] == '1'


async def test_sync_with_connect(client, test_vcr):
    assert await db.func.count(Domain.org_id).gino.scalar() == 0
    with test_vcr.use_cassette('test_sync_with_connect.yaml'):
        response = await client.post('api/domains/sync-connect', json={
            'org_id': '1',
            'admin_id': '2',
        })
        assert response.status_code == 200, response.text

    assert await db.func.count(Domain.org_id).gino.scalar() == 1
    domain = await Domain.query.where(
        and_(
            Domain.name == 'test.ru',
        )
    ).gino.first()
    assert domain.org_id == '1'
    assert domain.admin_id == '2'
    assert domain.validated is True
    assert domain.master is False


async def test_update_admin(db_bind, client, test_vcr):
    async with db_bind as engine:
        await Domain.create(
            org_id='1',
            admin_id='1',
            name='domain-1.com',
            owned=True,
            bind=engine,
        )
        await Domain.create(
            org_id='1',
            admin_id='1',
            name='domain-2.com',
            owned=True,
            master=True,
            bind=engine,
        )

    with test_vcr.use_cassette('test_update_admin_success.yaml'):
        response = await client.post('api/domains/1/update-admin', json={
            'admin_id': '2',
        })
    assert response.status_code == 200, response.text

    domains = await Domain.query.where(
        Domain.org_id == '1',
    ).gino.all()
    for domain in domains:
        assert domain.admin_id == '2'


async def test_add_owned_domain(client, test_vcr):
    with test_vcr.use_cassette('test_add_owned_domain_success.yaml'):
        response = await client.post('api/domains/add-owned', json={
            'org_id': '1',
            'admin_id': '2',
            'domain': 'domain.com',
        })
    assert response.status_code == 200, response.text

    domain = await Domain.query.where(
        and_(
            Domain.org_id == '1',
            Domain.name == 'domain.com',
        )
    ).gino.first()
    assert domain.owned


async def test_suggest_domain(client, test_vcr):
    with test_vcr.use_cassette('test_suggest_domain.yaml'), mock_cached_domains():
        response = await client.get(
            'api/domains/suggest/287510037?ip=0.0.0.0',
        )

    data = response.json()

    assert 'suggested_domains' in data
    assert {'login': 'me',
            'name': 'iegit.ru'} not in data['suggested_domains']
    assert {'login': 'i',
            'name': 'iiegit.ru'} in data['suggested_domains']


async def test_suggest_domain_filter_in_blackbox(client, test_vcr):
    with test_vcr.use_cassette('test_suggest_domain_filter_blackbox.yaml'), mock_cached_domains(), \
         patch('intranet.domenator.src.settings.config.connect_check_domain_in_bb_percent', new=100):
        response = await client.get(
            'api/domains/suggest/287510037?ip=0.0.0.0',
        )

    data = response.json()

    assert 'suggested_domains' in data
    assert {'login': 'me',
            'name': 'iegit.ru'} not in data['suggested_domains']
    assert {'login': 'i',
            'name': 'iiegit.ru'} not in data['suggested_domains']


async def test_suggest_domain_with_domain_base(client, test_vcr):
    with test_vcr.use_cassette('test_suggest_domain_with_domain_base.yaml'), mock_cached_domains():
        response = await client.get(
            'api/domains/suggest/287510037?ip=0.0.0.0&domain_base=yandex',
        )

    data = response.json()
    assert 'suggested_domains' in data
    assert {'login': 'me',
            'name': 'yandex-iegit.ru'} in data['suggested_domains']
    assert {'login': 'me',
            'name': 'yandex.ru'} not in data['suggested_domains']
    assert data['domain_status'] == 'occupied'


async def test_suggest_domain_no_birthdate(client, test_vcr):
    with test_vcr.use_cassette('test_suggest_domain_no_birthdate.yaml'), mock_cached_domains():
        response = await client.get(
            'api/domains/suggest/287510037?ip=0.0.0.0',
        )

    data = response.json()
    assert 'suggested_domains' in data
    assert {'login': 'iegit11',
            'name': 'iegit.ru'} not in data['suggested_domains']
    assert {'login': 'iegit11',
            'name': 'iegit94.ru'} not in data['suggested_domains']


@pytest.mark.parametrize("limit", [1, 20, 100])
async def test_suggest_domain_with_limit(client, test_vcr, limit):
    with test_vcr.use_cassette('test_suggest_domain.yaml'), mock_cached_domains():
        response = await client.get(
            f'api/domains/suggest/287510037?ip=0.0.0.0&limit={limit}',
        )

    data = response.json()
    assert 'suggested_domains' in data
    assert len(data['suggested_domains']) == limit


async def test_suggest_domain_with_limit_and_domain_base(client, test_vcr):
    limit = 5
    domain_base = 'yandex'
    with test_vcr.use_cassette('test_suggest_domain.yaml'), mock_cached_domains():
        response = await client.get(
            f'api/domains/suggest/287510037?ip=0.0.0.0&limit={limit}&domain_base={domain_base}',
        )

    data = response.json()
    assert 'suggested_domains' in data
    assert len(data['suggested_domains']) == limit
    assert {'login': 'iegit11', 'name': f'{domain_base}.ru'} not in data['suggested_domains']
    assert data['domain_status'] == 'occupied'


@pytest.mark.parametrize("input_login,expected", [
    ("Zero-ger", "Zero-ger"),
    ("!№%:,;;)", "me"),
    ("буква1..--zero", "bukva1-zero"),
    ("yandex-team-zero-ger", "zero-ger")
])
async def test_suggest_custom_login(client, test_vcr, input_login, expected):
    with test_vcr.use_cassette('test_suggest_domain.yaml'), mock_cached_domains():
        response = await client.get(
            f'api/domains/suggest/287510037?ip=0.0.0.0&login={input_login}',
        )

    data = response.json()
    assert 'suggested_domains' in data
    suggested_logins = set([row["login"] for row in data['suggested_domains']])
    assert expected in suggested_logins


async def test_register_domain_ok(client, test_vcr):
    with test_vcr.use_cassette('test_register_domain_ok.yaml'), mock_date_now():
        response = await client.post(
            'api/domains/register/287510037?login=test_login&ip=0.0.0.0&domain=not-registered.ru',
        )

    data = response.json()
    assert data == {'result': 'success'}

    domain_record = await MailUserDomain.query.where(
        and_(
            MailUserDomain.domain == 'not-registered.ru',
            MailUserDomain.uid == '287510037',
        )
    ).gino.all()

    assert domain_record[-1].status == 'wait_dns_entries'
    assert domain_record[-1].service_id == '12345'
    assert domain_record[-1].expired_at is None


async def test_register_domain_occupied(client, test_vcr):
    with test_vcr.use_cassette('test_register_domain_occupied.yaml'):
        response = await client.post(
            'api/domains/register/287510037?login=test_login&ip=0.0.0.0&domain=yandex.ru',
        )

    data = response.json()
    assert data == {'error': 'DOMAIN_ALREADY_EXISTS', 'result': 'error'}

    domain_record = await MailUserDomain.query.where(
        and_(
            MailUserDomain.domain == 'yandex.ru',
            MailUserDomain.uid == '287510037',
        )
    ).gino.all()

    assert domain_record[-1].status == 'failed'


def mock_cached_domains():
    return patch('intranet.domenator.src.settings.config.domain_cache', new={'iegit.ru', 'yandex.ru'})


async def test_status_domain_ok(client, test_vcr):

    with test_vcr.use_cassette('test_status_domain_ok.yaml'), mock_date_now():
        await client.post(
            'api/domains/register/287510037?login=testlogin&ip=0.0.0.0&domain=not-registered.ru',
        )
        response = await client.get(
            'api/domains/status/287510037?ip=0.0.0.0',
        )
        assert {'domain': 'not-registered.ru',
                'login': 'testlogin',
                'status': 'wait_dns_entries',
                'register_allowed': False,
                'register_allowed_ts': EXPECTED_REGISTERED_TS

                } == response.json()


async def test_status_domain_not_found(client, test_vcr):
    with test_vcr.use_cassette('test_status_domain_not_found.yaml'), mock_date_now():
        response = await client.get(
            'api/domains/status/12332456?ip=0.0.0.0',
        )

        assert response.status_code == 404

        await client.post(
            'api/domains/register/287510017?login=test_login&ip=0.0.0.0&domain=not-registered.ru',
        )
        response = await client.get(
            'api/domains/status/287510017?ip=0.0.0.0&domain=not-registered-other.ru',
        )
        assert response.status_code == 404


MOCKED_DATETIME_NOW = datetime(year=2021, month=1, day=1)
EXPECTED_REGISTERED_TS = '2021-06-30T00:00:00'


def mock_date_now():
    return patch('intranet.domenator.src.api.routes.domain.get_now', return_value=MOCKED_DATETIME_NOW)


async def test_cancel_domain_subscription(client, test_vcr):
    with test_vcr.use_cassette('test_cancel_domain_subscription.yaml'), mock_date_now():
        await client.post(
            'api/domains/register/287510037?login=testlogin&ip=0.0.0.0&domain=not-registered.ru',
        )
        response = await client.get(
            'api/domains/status/287510037?ip=0.0.0.0',
        )
        assert {'domain': 'not-registered.ru',
                'login': 'testlogin',
                'status': 'wait_dns_entries',
                'register_allowed': False,
                'register_allowed_ts': EXPECTED_REGISTERED_TS
                } == response.json()

        response = await client.post(
            'api/domains/cancel_subscription/287510037',
        )

        data = response.json()
        assert data == {'result': 'success'}


async def test_fail_second_domain_register(client, test_vcr):
    with test_vcr.use_cassette('test_fail_second_domain_register.yaml'), mock_date_now():
        expected_domain = 'not-registered-123.ru'
        await client.post(
            f'api/domains/register/287510037?login=testlogin&ip=0.0.0.0&domain={expected_domain}',
        )
        response = await client.get(
            'api/domains/status/287510037?ip=0.0.0.0',
        )
        assert {'domain': expected_domain,
                'login': 'testlogin',
                'status': 'wait_dns_entries',
                'register_allowed': False,
                'register_allowed_ts': EXPECTED_REGISTERED_TS
                } == response.json()

        record = await MailUserDomain.query.where(
            MailUserDomain.domain == expected_domain
        ).gino.first()

        await record.update(status=MailUserDomainStatus.registered.value).apply()

        response = await client.post(
            'api/domains/register/287510037?login=test_login&ip=0.0.0.0&domain=not-registered2.ru',
        )

        data = response.json()

        assert data == {
            'result': 'error',
            'error': 'multiple_domains_not_allowed'
        }


async def test_change_login_domain_not_found(client):
    response = await client.post(
        'api/domains/change_login/287510033?ip=0.0.0.0&login=foo',
    )

    assert response.status_code == 404, response.text
