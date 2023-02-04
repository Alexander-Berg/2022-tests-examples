import pytest

from maps.infra.sedem.machine.tests.typing import ServiceConfigFactory, MongoFixture
from maps.infra.sedem.machine.lib.config_api import ServiceConfigApi, ServiceDeployUnits, ServiceConfig


@pytest.mark.asyncio
async def test_load_existing_config(mongo: MongoFixture,
                                    service_config_factory: ServiceConfigFactory) -> None:
    for name, path, abc_slug in (
        ('maps-fake-service1', 'maps/dir1', 'maps-fake-abc1'),
        ('maps-fake-service2', 'maps/dir2', 'maps-fake-abc2'),
        ('maps-fake-service3', 'maps/dir3', 'maps-fake-abc3'),
    ):
        await service_config_factory(
            name=name,
            path=path,
            abc_slug=abc_slug,
        )

    async with await mongo.async_client.start_session() as session:
        config1 = await ServiceConfigApi(session).load_config(name='maps-fake-service2')
        config2 = await ServiceConfigApi(session).load_config(path='maps/dir2')
    assert config1.name == config2.name == 'maps-fake-service2'
    assert config1.path == config2.path == 'maps/dir2'
    assert config1.abc_slug == config2.abc_slug == 'maps-fake-abc2'


@pytest.mark.asyncio
async def test_load_nonexistent_config(mongo: MongoFixture,
                                       service_config_factory: ServiceConfigFactory) -> None:
    await service_config_factory(name='maps-fake-service', path='maps/dir')

    async with await mongo.async_client.start_session() as session:
        with pytest.raises(Exception, match=r'No config for [^\s]+ found'):
            await ServiceConfigApi(session).load_config(name='maps-fake-another-service')

        with pytest.raises(Exception, match=r'No config at [^\s]+ found'):
            await ServiceConfigApi(session).load_config(path='maps/nonexistent/dir')


@pytest.mark.asyncio
async def test_iter_services_deploy_units(mongo: MongoFixture,
                                          service_config_factory: ServiceConfigFactory) -> None:
    stages = [
        ServiceConfig.Stage(
            name=name,
            deploy_units=list(deploy_units),
        )
        for name, deploy_units in (
            ('testing', ('testing', 'load')),
            ('prestable', ('prestable',)),
            ('stable', ('stable',)),
        )
    ]
    await service_config_factory(name='maps-fake-service', path='maps/dir', stages=stages)

    stages = [
        ServiceConfig.Stage(
            name=name,
            deploy_units=list(deploy_units),
        )
        for name, deploy_units in (
            ('testing', ('testing', 'load')),
            ('stable', ('stable',)),
        )
    ]
    await service_config_factory(name='maps-fake-service1', path='maps/dir1', stages=stages)

    expected_services = [
        ServiceDeployUnits(
            service_name='maps-fake-service',
            deploy_units={'prestable', 'stable', 'testing', 'load'}
        ),
        ServiceDeployUnits(
            service_name='maps-fake-service1',
            deploy_units={'stable', 'testing', 'load'}
        )
    ]
    actual_services = []
    async with await mongo.async_client.start_session() as session:
        deploy_cursor = ServiceConfigApi(session).iter_services_deploy_units()
        async for service in deploy_cursor:
            actual_services.append(service)

    actual_services.sort(key=lambda service: service.service_name)
    expected_services.sort(key=lambda service: service.service_name)

    assert actual_services == expected_services
