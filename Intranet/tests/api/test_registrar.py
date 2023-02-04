import pytest

from intranet.domenator.src.db.registrar.models import Registrar
from intranet.domenator.src.logic.registrar.password import encrypt_registrar_password

pytestmark = pytest.mark.asyncio


@pytest.fixture
async def registrar_new(db_bind):
    async with db_bind as engine:
        registrar = Registrar(
            name='registrar-1',
            admin_id='1',
            pdd_version='new',
            oauth_client_id='123',
            validate_domain_url='validate_domain_url',
            domain_added_callback_url='domain_added_callback_url',
            domain_verified_callback_url='domain_verified_callback_url',
            domain_deleted_callback_url='domain_deleted_callback_url',
        )
        registrar.password, registrar.iv, registrar.plength = encrypt_registrar_password(registrar.pdd_version, 'qwerty')
        await registrar.create(bind=engine)

        return registrar


@pytest.fixture
async def registrar_old(db_bind):
    async with db_bind as engine:
        registrar = Registrar(
            name='registrar-2',
            admin_id='1',
            pdd_id=123456,
            pdd_version='old',
            oauth_client_id='123',
            validate_domain_url='validate_domain_url',
            domain_added_callback_url='domain_added_callback_url',
            domain_verified_callback_url='domain_verified_callback_url',
            domain_deleted_callback_url='domain_deleted_callback_url',
        )
        registrar.password, registrar.iv, registrar.plength = encrypt_registrar_password(registrar.pdd_version, 'qwerty')
        await registrar.create(bind=engine)

        return registrar


async def test_get_non_existent_registrar(client):
    registrar_id = 99999
    response = await client.get(
        f'api/registrar/{registrar_id}/'
    )
    assert response.status_code == 404, response.text


async def test_get_registrar(client, registrar_new: Registrar):
    registrar_id = registrar_new.id
    response = await client.get(
        f'api/registrar/{registrar_id}/'
    )
    assert response.status_code == 200, response.text

    data = response.json()
    assert registrar_new.id == data['id']
    assert 'qwerty' == data['password']
    assert registrar_new.name == data['name']
    assert int(registrar_new.admin_id) == data['admin_id']
    assert registrar_new.pdd_version == data['pdd_version']
    assert registrar_new.oauth_client_id == data['oauth_client_id']
    assert registrar_new.validate_domain_url == data['validate_domain_url']
    assert registrar_new.domain_added_callback_url == data['domain_added_callback_url']
    assert registrar_new.domain_verified_callback_url == data['domain_verified_callback_url']
    assert registrar_new.domain_deleted_callback_url == data['domain_deleted_callback_url']
    assert registrar_new.validate_domain_url == data['payed_url']
    assert registrar_new.domain_added_callback_url == data['added_init']
    assert registrar_new.domain_verified_callback_url == data['added']
    assert registrar_new.domain_deleted_callback_url == data['delete_url']


async def test_get_registrar_by_pdd_id(client, registrar_old: Registrar):
    pdd_version = registrar_old.pdd_version
    pdd_id = registrar_old.pdd_id
    registrar_id = f'{pdd_version}:{pdd_id}'
    response = await client.get(
        f'api/registrar/{registrar_id}/'
    )
    assert response.status_code == 200, response.text

    data = response.json()
    assert registrar_old.id == data['id']
    assert 'qwerty' == data['password']
    assert registrar_old.name == data['name']
    assert int(registrar_old.admin_id) == data['admin_id']
    assert registrar_old.pdd_version == data['pdd_version']
    assert registrar_old.oauth_client_id == data['oauth_client_id']
    assert registrar_old.validate_domain_url == data['validate_domain_url']
    assert registrar_old.domain_added_callback_url == data['domain_added_callback_url']
    assert registrar_old.domain_verified_callback_url == data['domain_verified_callback_url']
    assert registrar_old.domain_deleted_callback_url == data['domain_deleted_callback_url']
    assert registrar_old.validate_domain_url == data['payed_url']
    assert registrar_old.domain_added_callback_url == data['added_init']
    assert registrar_old.domain_verified_callback_url == data['added']
    assert registrar_old.domain_deleted_callback_url == data['delete_url']


async def test_patch_registrar(client, registrar_new: Registrar):
    url = f'api/registrar/{registrar_new.id}/'
    data_for_patch = {
        'pdd_id': 101,
        'pdd_version': 'new',
        'name': 'new_name',
        'admin_id': 102,
        'password': 'new_password',
        'oauth_client_id': 'new_oauth_client_id',
        'validate_domain_url': 'new_validate_domain_url',
        'domain_added_callback_url': 'new_domain_added_callback_url',
        'domain_verified_callback_url': 'new_domain_verified_callback_url',
        'domain_deleted_callback_url': 'new_domain_deleted_callback_url',
    }
    patch_response = await client.patch(url, json=data_for_patch)
    assert patch_response.status_code == 200, patch_response.text

    response = await client.get(url)
    data = response.json()

    for key in data_for_patch:
        assert data_for_patch[key] == data[key]


async def test_patch_registrar_with_aliases(client, registrar_new):
    url = f'api/registrar/{registrar_new.id}/'
    data_for_patch = {
        'pdd_id': 101,
        'pdd_version': 'new',
        'name': 'new_name',
        'admin_id': 102,
        'password': 'new_password',
        'oauth_client_id': 'new_oauth_client_id',
        'payed_url': 'new_validate_domain_url',
        'added_init': 'new_domain_added_callback_url',
        'added': 'new_domain_verified_callback_url',
        'delete_url': 'new_domain_deleted_callback_url',
    }

    patch_response = await client.patch(url, json=data_for_patch)
    assert patch_response.status_code == 200, patch_response.text

    response = await client.get(url)
    data = response.json()

    for key in data_for_patch:
        assert data_for_patch[key] == data[key]

    assert data_for_patch['payed_url'] == data['validate_domain_url']
    assert data_for_patch['added_init'] == data['domain_added_callback_url']
    assert data_for_patch['added'] == data['domain_verified_callback_url']
    assert data_for_patch['delete_url'] == data['domain_deleted_callback_url']


async def test_get_token_for_registrar_with_old_pdd(client, registrar_old: Registrar):
    token_response = await client.post(f'api/registrar/{registrar_old.id}/token')
    assert token_response.status_code == 422, token_response.text


async def test_get_registrar_by_token(client, registrar_new: Registrar):
    token_response = await client.post(f'api/registrar/{registrar_new.id}/token')
    assert token_response.status_code == 200

    token = token_response.json()['token']

    response = await client.get(f'api/registrar/token/v2/{token}')
    assert response.status_code == 200, response.text

    data = response.json()
    assert registrar_new.id == data['id']
    assert int(registrar_new.admin_id) == data['admin_id']
    assert registrar_new.pdd_id == data['pdd_id']


async def test_get_registrar_by_non_existent_token(client, registrar_new):
    token = 'ECKMM2PXB6BTNJL3MMXLLN2UA2KDOZ63OBSBWYAQL6LMMRPWAERQ'
    response = await client.get(f'api/registrar/token/v2/{token}')
    assert response.status_code == 404, response.text


async def test_get_registrar_by_uid(client, registrar_new: Registrar):
    uid = registrar_new.admin_id
    response = await client.get(f'api/registrar/uid/v2/{uid}')
    assert response.status_code == 200, response.text

    data = response.json()
    assert registrar_new.name == data['name']
    assert int(registrar_new.admin_id) == data['admin_id']
    assert registrar_new.pdd_version == data['pdd_version']
    assert registrar_new.oauth_client_id == data['oauth_client_id']
    assert registrar_new.validate_domain_url == data['validate_domain_url']
    assert registrar_new.domain_added_callback_url == data['domain_added_callback_url']
    assert registrar_new.domain_verified_callback_url == data['domain_verified_callback_url']
    assert registrar_new.domain_deleted_callback_url == data['domain_deleted_callback_url']
    assert registrar_new.validate_domain_url == data['payed_url']
    assert registrar_new.domain_added_callback_url == data['added_init']
    assert registrar_new.domain_verified_callback_url == data['added']
    assert registrar_new.domain_deleted_callback_url == data['delete_url']


async def test_get_registrar_by_non_existent_uid(client, registrar_new: Registrar):
    uid = 999
    response = await client.get(f'api/registrar/uid/v2/{uid}')
    assert response.status_code == 404, response.text


async def test_delete_registrar_token(client, registrar_new: Registrar):
    token_response = await client.post(f'api/registrar/{registrar_new.id}/token')
    assert token_response.status_code == 200, token_response.text
    token = token_response.json()['token']

    get_by_token_response = await client.get(f'api/registrar/token/v2/{token}')
    assert get_by_token_response.status_code == 200, get_by_token_response.text

    delete_token_response = await client.delete(f'api/registrar/{registrar_new.id}/token')
    assert delete_token_response.status_code == 200, delete_token_response.text

    get_by_token_response = await client.get(f'api/registrar/token/v2/{token}')
    assert get_by_token_response.status_code == 404, get_by_token_response.text
