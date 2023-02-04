import pytest
import datetime
from unittest.mock import MagicMock
from maps.infra.sedem.machine.lib.job_manager import JobManager
from maps.infra.sedem.machine.lib.deploy_api import (
    TooLongServiceDeploys, Deploy, Commit
)
from maps.infra.sedem.machine.lib.config_api import (
    ServiceDeployUnits
)
from bson import ObjectId

RETURN_VALUE = TooLongServiceDeploys(
    service_name='maps-fake-service',
    deploys=[
        Deploy(
            deploy_id=ObjectId('624aadd78ec224502f41da56'),
            service_name='maps-fake-service',
            version=Deploy.ReleaseVersion(major=1, minor=1),
            deploy_unit='prestable',
            commit=Commit(
                arc_commit_hash='fake-hash',
                svn_revision=12345,
                message='new feature',
                author='jane-doe',
                time=datetime.datetime(2022, 4, 4, 10, 35, 35)
            ),
            author='john-doe',
            status='executing',
            start_time=datetime.datetime(2022, 4, 4, 10, 55, 55)
        ),
        Deploy(
            deploy_id=ObjectId('624aadd78ec224502f41da58'),
            service_name='maps-fake-service',
            version=Deploy.ReleaseVersion(major=1, minor=1),
            deploy_unit='stable',
            commit=Commit(
                arc_commit_hash='fake-hash',
                svn_revision=12345,
                message='new feature',
                author='jane-doe',
                time=datetime.datetime(2022, 4, 4, 10, 35, 35)
            ),
            author='john-doe',
            status='executing',
            start_time=datetime.datetime(2022, 4, 4, 10, 45, 45)
        )
    ]
)

RETURN_VALUE_ALL_SERVICES = [
    ServiceDeployUnits(service_name='maps-fake-service',
                       deploy_units={'prestable', 'stable', 'testing'}),
    ServiceDeployUnits(service_name='maps-fake-service1',
                       deploy_units={'prestable', 'stable', 'testing'})
]


@pytest.mark.asyncio
async def test_long_executing_deploys(job_manager: JobManager, deploy_api: MagicMock, config_api: MagicMock):
    async def lookup_mock():
        yield RETURN_VALUE

    async def lookup_mock_services():
        for service in RETURN_VALUE_ALL_SERVICES:
            yield service
    deploy_api.lookup_too_long_deploys.side_effect = lookup_mock
    config_api.iter_services_deploy_units.side_effect = lookup_mock_services
    await job_manager.long_executing_deploys()
    assert deploy_api.lookup_too_long_deploys.call_count == 1
    assert job_manager.juggler.send_too_long_deploys_events.call_count == 1
    first_arg, second_arg = job_manager.juggler.send_too_long_deploys_events.call_args.args
    async for value in first_arg:
        assert value == RETURN_VALUE
    all_services = []
    async for value in second_arg:
        all_services.append(value)
    assert all_services == RETURN_VALUE_ALL_SERVICES
