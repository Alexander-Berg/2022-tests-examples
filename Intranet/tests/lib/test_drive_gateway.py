import pytest

from collections import defaultdict
from mock import patch

from intranet.trip.src.lib.drive.gateway import DriveGateway

from ..mocks import async_return


pytestmark = pytest.mark.asyncio


class DriveClientMock:

    def __init__(self, *args, **kwargs):
        self.users = {}
        self.accounts = defaultdict(list)
        self.get_accounts_called = False
        self.get_user_called = False
        self.activate_called = False
        self.deactivate_called = False

    @classmethod
    async def init(cls, *args, **kwargs):
        return cls()

    async def get_user(self, phone):
        self.get_user_called = True
        data = {'users': []}
        if phone in self.users:
            data['users'].append({'phone': phone, 'id': self.users[phone]})
        return data

    async def get_accounts(self, org_id, user_id=None):
        self.get_accounts_called = True
        result = self.accounts[org_id]
        if user_id is not None:
            result = [item for item in result if item == user_id]
        return {'accounts': result}

    async def activate(self, org_id, user_id, tracker_issue):
        self.activate_called = True
        if user_id in self.accounts[org_id]:
            raise Exception()
        self.accounts[org_id].append(user_id)

    async def deactivate(self, org_id, user_id, tracker_issue):
        self.deactivate_called = True
        if user_id not in self.accounts[org_id]:
            raise Exception()
        self.accounts[org_id].remove(user_id)


@pytest.mark.parametrize('is_activated', (True, False))
async def test_activate(is_activated):
    with patch(
        'intranet.trip.src.lib.drive.gateway.get_tvm_service_ticket',
        return_value=async_return('service_ticket'),
    ), patch(
        'intranet.trip.src.lib.drive.gateway.DriveClient',
        DriveClientMock,
    ):
        gateway = await DriveGateway.init()
        gateway.api.users = {'+79001234567': 'user1id'}
        if is_activated:
            gateway.api.accounts['org1'].append('user1id')
        await gateway.activate('org1', 'user1id', 'TRAVEL-00000')
        assert gateway.api.get_accounts_called is True
        assert gateway.api.activate_called != is_activated


@pytest.mark.parametrize('is_activated', (True, False))
async def test_deactivate(is_activated):
    with patch(
        'intranet.trip.src.lib.drive.gateway.get_tvm_service_ticket',
        return_value=async_return('service_ticket'),
    ), patch(
        'intranet.trip.src.lib.drive.gateway.DriveClient',
        DriveClientMock,
    ):
        gateway = await DriveGateway.init()
        gateway.api.users = {'+79001234567': 'user1id'}
        if is_activated:
            gateway.api.accounts['org1'].append('user1id')
        await gateway.deactivate('org1', 'user1id', 'TRAVEL-00000')
        assert gateway.api.get_accounts_called is True
        assert gateway.api.deactivate_called == is_activated
