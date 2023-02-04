from typing import List

import pytest

from intranet.domenator.src.db.domain.models import Domain
from intranet.domenator.src.logic.domain import (
    sync_domain_state,
)
from intranet.domenator.src.logic.mail_user_domain import (
    get_valid_domain,
    get_valid_login,
)

pytestmark = pytest.mark.asyncio


@pytest.fixture
async def domains(db_bind):
    async with db_bind as engine:
        return [
            await Domain.create(
                org_id='1',
                name='domain.com',
                admin_id='1',
                master=True,
                owned=True,
                bind=engine,
            ),
            await Domain.create(
                org_id='2',
                name='domain.com',
                admin_id='2',
                master=False,
                owned=False,
                bind=engine,
            )
        ]


async def test_sync_domain_state(client, domains: List[Domain], test_vcr):
    with test_vcr.use_cassette('test_sync_domain_state_success.yaml'):
        verified_domain = domains[1]
        registrar_id = 123
        await sync_domain_state(verified_domain.org_id, verified_domain.name, verified_domain.admin_id, registrar_id)


async def test_get_valid_domain():
    assert get_valid_domain('pirogi') == 'pirogi.ru'
    assert get_valid_domain('pir----ogi') == 'pir-ogi.ru'
    assert get_valid_domain('---..--pir----ogi') == 'pir-ogi.ru'
    assert get_valid_domain('---..--pir----ogi-') == 'pir-ogi.ru'
    assert get_valid_domain('---..--pir----ogi-----') == 'pir-ogi.ru'
    assert get_valid_domain('pir-{/}-ogi-----') == 'pir-ogi.ru'
    assert get_valid_domain(' pir  ogi  ') == 'pirogi.ru'
    assert get_valid_domain('пироги') == 'pirogi.ru'
    assert get_valid_domain('---------....') is None
    assert get_valid_domain('x') is None
    assert get_valid_domain('xx') is None


@pytest.mark.parametrize(
    'login,expected', [
        ('', None),
        (None, None),
        ('ABC123', 'ABC123'),
        ('пироги', 'pirogi'),
        ('yndx-pirogi', 'pirogi'),
        ('x' * 50, 'x' * 30),
        ('pirogi^^&&**', 'pirogi'),
        ('yambot-', ''),
        ('-..-123yambot-', 'yambot'),
        ('-----..123pirogi--ru123-..', 'pirogi-ru123'),
        ('Hello there!', 'Hellothere'),
        ('uid...there', 'there')
    ])
async def test_get_valid_login(login, expected):
    assert get_valid_login(login) == expected
