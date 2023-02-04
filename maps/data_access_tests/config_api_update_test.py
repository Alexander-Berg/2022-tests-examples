import asyncio
import typing as tp
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import ServiceConfigFactory, MongoFixture
from maps.infra.sedem.machine.lib.config_api import ServiceConfigApi, ServiceConfig

from maps.infra.sedem.machine.lib.exceptions import OldConfigRevisionError


@pytest.mark.asyncio
async def test_insert_new_config(mongo: MongoFixture) -> None:
    async with await mongo.async_client.start_session() as session:
        await ServiceConfigApi(session).update_config(ServiceConfig(
            revision=1,
            name='maps-fake-service',
            path='maps/service/dir',
            abc_slug='maps-fake-abc',
            release_type='NANNY',
            stages=[
                ServiceConfig.Stage(name='testing', deploy_units=['testing', 'load']),
                ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
            acceptance=[],
            sox=True,
        ))

        config = await ServiceConfigApi(session).load_config(name='maps-fake-service')

    assert config.revision == 1
    assert config.name == 'maps-fake-service'
    assert config.path == 'maps/service/dir'


@pytest.mark.asyncio
async def test_update_config(mongo: MongoFixture,
                             service_config_factory: ServiceConfigFactory) -> None:
    await service_config_factory(
        revision=1,
        name='maps-fake-service',
        path='maps/service/dir',
    )

    async with await mongo.async_client.start_session() as session:
        await ServiceConfigApi(session).update_config(ServiceConfig(
            revision=2,
            name='maps-fake-service',
            path='maps/service/dir',
            abc_slug='maps-fake-abc',
            release_type='GARDEN',
            stages=[
                ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
            acceptance=[],
            sox=True,
        ))

        config = await ServiceConfigApi(session).load_config(name='maps-fake-service')

    assert config.revision == 2
    assert config.abc_slug == 'maps-fake-abc'
    assert len(config.stages) == 2
    assert config.sox is True


@pytest.mark.asyncio
async def test_downgrade_config(mongo: MongoFixture,
                                service_config_factory: ServiceConfigFactory) -> None:
    await service_config_factory(
        revision=2,
        name='maps-fake-service',
        path='maps/service/dir',
    )
    with pytest.raises(OldConfigRevisionError):
        async with await mongo.async_client.start_session() as session:
            await ServiceConfigApi(session).update_config(ServiceConfig(
                revision=1,
                name='maps-fake-service',
                path='maps/service/dir',
                abc_slug='maps-fake-abc',
                release_type='GARDEN',
                stages=[],
                acceptance=[],
                sox=True,
            ))


@pytest.mark.asyncio
async def test_move_service(mongo: MongoFixture,
                            service_config_factory: ServiceConfigFactory) -> None:
    await service_config_factory(
        revision=1,
        name='maps-fake-service1',
        path='maps/dir2',
        abc_slug='slug1',
        release_type='NANNY',
        stages=[
            ServiceConfig.Stage(name='testing', deploy_units=['testing']),
            ServiceConfig.Stage(name='stable', deploy_units=['stable']),
        ],
        sox=False,
    )

    async with await mongo.async_client.start_session() as session:
        await ServiceConfigApi(session).update_config(ServiceConfig(
            revision=2,
            name='maps-fake-service1',
            path='maps/dir2',
            abc_slug='slug1',
            release_type='NANNY',
            stages=[
                ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
            acceptance=[],
            sox=False,
        ))

        config = await ServiceConfigApi(session).load_config(name='maps-fake-service1')

    assert config.revision == 2
    assert len(config.stages) == 2
    assert config.path == 'maps/dir2'


@dataclass
class ConcurrentUpdateTest:
    @dataclass
    class Params:
        revision: int
        name: str
        path: str
        abc_slug: str

    name: str
    first: 'Params'
    second: 'Params'
    expected_revisions: tp.Optional[dict[str, int]]
    expected_error: tp.Optional[str]

    def __str__(self) -> str:
        return self.name


CONCURRENT_TEST_CASES = [
    ConcurrentUpdateTest(
        name='retry',
        first=ConcurrentUpdateTest.Params(
            revision=1,
            name='maps-fake-service1',
            path='maps/dir1',
            abc_slug='slug1',
        ),
        second=ConcurrentUpdateTest.Params(
            revision=1,
            name='maps-fake-service1',
            path='maps/dir1',
            abc_slug='slug1',
        ),
        expected_revisions={
            'maps-fake-service1': 1,
        },
        expected_error=None,
    ),
    ConcurrentUpdateTest(
        name='rename_service',
        first=ConcurrentUpdateTest.Params(
            revision=1,
            name='maps-fake-service1',
            path='maps/dir1',
            abc_slug='slug1',
        ),
        second=ConcurrentUpdateTest.Params(
            revision=2,
            name='maps-fake-service2',
            path='maps/dir1',
            abc_slug='slug1',
        ),
        expected_revisions=None,
        expected_error=r'Another service [^\s]+ is already located at arcadia:[^\s]+',
    ),
    ConcurrentUpdateTest(
        name='different_services',
        first=ConcurrentUpdateTest.Params(
            revision=1,
            name='maps-fake-service1',
            path='maps/dir1',
            abc_slug='slug1',
        ),
        second=ConcurrentUpdateTest.Params(
            revision=2,
            name='maps-fake-service2',
            path='maps/dir2',
            abc_slug='slug2',
        ),
        expected_revisions={
            'maps-fake-service1': 1,
            'maps-fake-service2': 2,
        },
        expected_error=None,
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CONCURRENT_TEST_CASES, ids=str)
async def test_concurrent_update_config(mongo: MongoFixture,
                                        case: ConcurrentUpdateTest) -> None:
    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            results = await asyncio.gather(
                ServiceConfigApi(session1).update_config(ServiceConfig(
                    revision=case.first.revision,
                    name=case.first.name,
                    path=case.first.path,
                    abc_slug=case.first.abc_slug,
                    release_type='NANNY',
                    stages=[
                        ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                        ServiceConfig.Stage(name='stable', deploy_units=['stable']),
                    ],
                    acceptance=[],
                    sox=False,
                )),
                ServiceConfigApi(session2).update_config(ServiceConfig(
                    revision=case.second.revision,
                    name=case.second.name,
                    path=case.second.path,
                    abc_slug=case.second.abc_slug,
                    release_type='NANNY',
                    stages=[
                        ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                        ServiceConfig.Stage(name='stable', deploy_units=['stable']),
                    ],
                    acceptance=[],
                    sox=False,
                )),
                return_exceptions=True
            )

    errors = [result for result in results if isinstance(result, Exception)]
    if case.expected_error is not None:
        assert len(errors)  # expected error not found
        with pytest.raises(Exception, match=case.expected_error):
            raise errors.pop()
    if errors:
        raise errors.pop()  # unexpected error

    if case.expected_revisions is not None:
        async with await mongo.async_client.start_session() as session:
            for service_name, expected_revision in case.expected_revisions.items():
                config = await ServiceConfigApi(session).load_config(name=service_name)
                assert config.revision == expected_revision

            collection = session.client.get_database().service_config
            assert len(case.expected_revisions) == await collection.count_documents({}, session=session)
