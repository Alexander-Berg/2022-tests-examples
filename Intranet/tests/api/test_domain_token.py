import pytest

from sqlalchemy import and_

from intranet.domenator.src.db import DomainToken
from intranet.domenator.src.logic.cryptography import (
    PlainToken,
    CryptToken,
)

pytestmark = pytest.mark.asyncio


@pytest.fixture
async def domain_token_old(db_bind):
    async with db_bind as engine:
        return await DomainToken.create(
            pdd_version='old',
            admin_id='123',
            domain='test.com',
            token=PlainToken().serialize_token('token_old'),
            bind=engine
        )


@pytest.fixture
async def domain_token_new(db_bind):
    async with db_bind as engine:
        return await DomainToken.create(
            pdd_version='new',
            admin_id='123',
            domain='test1.com',
            token=CryptToken().serialize_token('t' * 52),
            bind=engine
        )


async def test_get_token_data_old_version(client, domain_token_old: DomainToken):
    pdd_version = 'old'
    token = 'token_old'

    response = await client.get(
        f'api/domain-token/{token}/{pdd_version}/'
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data['domain'] == domain_token_old.domain
    assert data['uid'] == int(domain_token_old.admin_id)


async def test_get_token_data_new_version(client, domain_token_new):
    pdd_version = 'new'
    token = 't' * 52

    response = await client.get(
        f'api/domain-token/{token}/{pdd_version}/'
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data['domain'] == domain_token_new.domain


async def test_get_404_on_invalid_token_new(client):
    token = 'incorrecttoken'
    pdd_version = 'new'
    response = await client.get(
        f'api/domain-token/{token}/{pdd_version}/'
    )
    assert response.status_code == 404, response.text


async def test_get_404_on_invalid_token_old(client):
    token = 'incorrecttoken'
    pdd_version = 'old'
    response = await client.get(
        f'api/domain-token/{token}/{pdd_version}/'
    )
    assert response.status_code == 404, response.text


async def test_gen_domain_token_with_existing_old_pdd(client, domain_token_old):
    admin_id = '123'
    domain = 'test.com'
    pdd_version = 'old'

    response = await client.post(
        f'api/domain-token/{admin_id}/{domain}/{pdd_version}/'
    )
    assert response.status_code == 201, response.text
    token = response.json()['token']
    serialized_token = PlainToken().serialize_token(token)

    domain_token = await DomainToken.query.where(
        and_(
            DomainToken.pdd_version == pdd_version,
            DomainToken.token == serialized_token,
        )
    ).gino.first()
    assert domain_token.id == domain_token_old.id


async def test_gen_domain_token_old_pdd(client):
    admin_id = '123'
    domain = 'test_get.com'
    pdd_version = 'old'

    response = await client.post(
        f'api/domain-token/{admin_id}/{domain}/{pdd_version}/'
    )
    assert response.status_code == 201, response.text
    token = response.json()['token']
    serialized_token = PlainToken().serialize_token(token)

    domain_token = await DomainToken.query.where(
        and_(
            DomainToken.pdd_version == pdd_version,
            DomainToken.token == serialized_token,
        )
    ).gino.first()
    assert domain_token.domain == domain


async def test_gen_domain_token_new_pdd(client):
    admin_id = '123'
    domain = 'test_get.com'
    pdd_version = 'new'

    response = await client.post(
        f'api/domain-token/{admin_id}/{domain}/{pdd_version}/'
    )
    assert response.status_code == 201, response.text
    token = response.json()['token']
    serialized_token = CryptToken().serialize_token(token)

    domain_token = await DomainToken.query.where(
        and_(
            DomainToken.pdd_version == pdd_version,
            DomainToken.token == serialized_token,
        )
    ).gino.first()
    assert domain_token.domain == domain


async def test_delete_token(client, domain_token_old):
    pdd_version = 'old'
    response = await client.delete(
        f'api/domain-token/{domain_token_old.admin_id}/{domain_token_old.domain}/{pdd_version}/'
    )
    assert response.status_code == 204, response.text

    domain_token = await DomainToken.query.where(
        and_(
            DomainToken.pdd_version == pdd_version,
            DomainToken.domain == domain_token_old.domain,
            DomainToken.admin_id == domain_token_old.admin_id,
        )
    ).gino.first()
    assert domain_token is None


async def test_delete_not_existing_token(client):
    pdd_version = 'old'
    admin_id = '1234'
    domain = 'test.com'
    response = await client.delete(
        f'api/domain-token/{admin_id}/{domain}/{pdd_version}/'
    )
    assert response.status_code == 404, response.text
