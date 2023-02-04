# flake8: noqa: E131
import pytest

from mock import AsyncMock, patch

from intranet.trip.src.enums import ServiceType
from intranet.trip.src.lib.hub.sync import PersonHubProfileSync, ExtPersonHubProfileSync


pytestmark = pytest.mark.asyncio


hub_xml_response = (
    '<profileSynchronizationResponse>'
        '<profileSynchronizationBatchId>'
            '1'
        '</profileSynchronizationBatchId>'
    '</profileSynchronizationResponse>'
)


class MockedHub:

    async def push_one_profile(self, *args, **kwargs):
        return hub_xml_response

    async def push_many_profiles(self, *args, **kwargs):
        return hub_xml_response

    async def get_synchronization_status(self, *args, **kwargs):
        return {
            'batchProfileSynchronizationStatus': 'Completed',
            'profileSynchronizationStatuses': [{
                'profile': {
                    'uniqueIdentifier': '123456',
                    'profileId': '123',
                },
                'profileSynchronizationState': 'Created',
            }]
        }


@patch('intranet.trip.src.lib.hub.sync.get_tvm_service_ticket', AsyncMock(return_value='ticket'))
async def test_person_hub_profile_sync(f, uow):
    await f.create_person(person_id=1)
    await f.create_person_document(person_id=1, document_id=1)
    await f.create_service_provider(service_type=ServiceType.avia, code='SU')
    await f.create_bonus_card(person_id=1, provider_type=ServiceType.avia, provider_code='SU')
    hub_sync = await PersonHubProfileSync.init(uow)
    with patch('intranet.trip.src.lib.hub.sync.Hub', MockedHub):
        await hub_sync.sync(person_ids=[1])


@patch('intranet.trip.src.lib.hub.sync.get_tvm_service_ticket', AsyncMock(return_value='ticket'))
async def test_ext_person_hub_profile_sync(f, uow):
    await f.create_person(person_id=1)
    await f.create_ext_person(person_id=1, ext_person_id=1)
    await f.create_person_document(ext_person_id=1, document_id=1)
    await f.create_service_provider(service_type=ServiceType.avia, code='SU')
    await f.create_bonus_card(ext_person_id=1, provider_type=ServiceType.avia, provider_code='SU')
    hub_sync = await ExtPersonHubProfileSync.init(uow)
    with patch('intranet.trip.src.lib.hub.sync.Hub', MockedHub):
        await hub_sync.sync(person_ids=[1])
