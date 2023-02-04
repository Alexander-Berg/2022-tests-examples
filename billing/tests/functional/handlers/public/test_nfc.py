import re
from decimal import Decimal
from uuid import uuid4

import pytest
from yarl import URL

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, has_properties, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.nfc_install_reward import NFCInstallReward


@pytest.fixture(params=[19])  # should be allocated into 300 points bucket
def owner_uid(request):
    return request.param


@pytest.fixture
def device_id(rands):
    return rands()


@pytest.fixture
def application(db_engine) -> YandexPayPlusPublicApplication:
    # overrides the application fixture from common_conftest.py
    return YandexPayPlusPublicApplication(db_engine=db_engine)


@pytest.fixture
def saturn_endpoint(yandex_pay_plus_settings):
    base_url = URL(yandex_pay_plus_settings.SATURN_API_URL)
    return base_url / f'api/v1/{yandex_pay_plus_settings.SATURN_SERVICE_NAME}/search'


@pytest.fixture(params=[0.851])
def user_score(request):
    return request.param


@pytest.fixture(autouse=True)
def mock_saturn(aioresponses_mocker, saturn_endpoint, owner_uid, user_score):
    payload = {
        'request_id': 'fake_request_id',
        'uid': owner_uid,
        'score': user_score,
        'formula_id': '2489197606',
    }
    aioresponses_mocker.post(saturn_endpoint, payload=payload, repeat=True)


@pytest.fixture
def trusted_devices(device_id):
    return [str(uuid4()), device_id]


@pytest.fixture(autouse=True)
def mock_yt(aioresponses_mocker, owner_uid, trusted_devices, yandex_pay_plus_settings):
    yt_endpoint = URL(yandex_pay_plus_settings.YT_API_URL) / 'lookup_rows'
    payload = {
        'uid': str(owner_uid),
        'trusted_devices': trusted_devices,
    }
    aioresponses_mocker.put(re.compile(f'^{yt_endpoint}.*$'), payload=payload)


@pytest.fixture
def authentication(
    app, yandex_pay_plus_settings, owner_uid, aioresponses_mocker
):
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_plus_settings.BLACKBOX_API_URL}.*method=oauth.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': owner_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        }
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.fixture(autouse=True)
async def cleanup(storage):
    yield
    await storage.conn.execute('TRUNCATE yandex_pay_plus.customers CASCADE;')


class TestGetNFCInstallReward:
    URL = '/api/public/v1/nfc/rewards/get-install-reward'

    @pytest.fixture
    def ensure_no_rewards(self, storage, owner_uid, device_id):
        async def _inner():
            rewards = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
                owner_uid, device_id
            )
            assert_that(rewards, equal_to([]))  # ensure no rewards for user in storage
        return _inner

    @pytest.mark.asyncio
    async def test_success(self, app, device_id, authentication):
        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '300.0'}}, 'status': 'success', 'code': 200})
        )

    @pytest.mark.asyncio
    async def test_reward_already_received__uid(
        self, storage, app, authentication, owner_uid, ensure_no_rewards, rands
    ):
        device_id = rands()
        await ensure_no_rewards()
        await storage.customer.create(Customer(uid=owner_uid))
        await storage.nfc_install_reward.create(
            NFCInstallReward(uid=owner_uid, device_id=device_id, amount=Decimal('100'))
        )

        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '0.0'}}, 'status': 'success', 'code': 200})
        )

    @pytest.mark.asyncio
    async def test_reward_already_received__device_id(
        self, storage, app, device_id, authentication, owner_uid, ensure_no_rewards
    ):
        await ensure_no_rewards()
        uid = owner_uid + 1
        await storage.customer.create(Customer(uid=uid))
        await storage.nfc_install_reward.create(
            NFCInstallReward(uid=uid, device_id=device_id, amount=Decimal('100'))
        )

        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '0.0'}}, 'status': 'success', 'code': 200})
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('user_score', [0.3], indirect=True)
    async def test_bad_user_score(self, app, device_id, authentication, ensure_no_rewards):
        await ensure_no_rewards()

        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '0.0'}}, 'status': 'success', 'code': 200})
        )

    @pytest.mark.asyncio
    async def test_device_not_trusted(
        self, app, device_id, authentication, ensure_no_rewards, trusted_devices
    ):
        trusted_devices.remove(device_id)
        await ensure_no_rewards()

        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '0.0'}}, 'status': 'success', 'code': 200})
        )


class TestInitNFCInstallReward:
    URL = '/api/public/v1/nfc/rewards/init-install-reward'
    ZERO_AMOUNT = match_equality(convert_then_match(Decimal, Decimal('0')))

    @pytest.fixture
    def ensure_no_rewards(self, storage, owner_uid, device_id):
        async def _inner():
            rewards = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
                owner_uid, device_id
            )
            assert_that(rewards, equal_to([]))  # ensure no rewards for user in storage
        return _inner

    @pytest.mark.asyncio
    async def test_success(self, storage, app, owner_uid, device_id, authentication, ensure_no_rewards):
        await ensure_no_rewards()

        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True, **authentication)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': '300.0'}}, 'status': 'success', 'code': 200})
        )
        [reward] = await storage.nfc_install_reward.get_rewards_for_uid_or_device_id(
            uid=owner_uid,
            device_id=device_id,
        )
        assert_that(reward, has_properties(uid=owner_uid, device_id=device_id, amount=Decimal('300')))

    @pytest.mark.asyncio
    async def test_reward_already_received__uid(
        self, storage, app, authentication, owner_uid, ensure_no_rewards, rands
    ):
        device_id = rands()
        await ensure_no_rewards()
        await storage.customer.create(Customer(uid=owner_uid))
        await storage.nfc_install_reward.create(
            NFCInstallReward(uid=owner_uid, device_id=device_id, amount=Decimal('100'))
        )

        r = await app.post(self.URL, json={'device_id': device_id}, **authentication)

        assert_that(r.status, equal_to(409))
        assert_that(
            await r.json(),
            equal_to({'code': 409, 'data': {'message': 'REWARD_ALREADY_EXISTS'}, 'status': 'fail'})
        )

    @pytest.mark.asyncio
    async def test_reward_already_received__device_id(
        self, storage, app, device_id, authentication, owner_uid, ensure_no_rewards
    ):
        await ensure_no_rewards()
        uid = owner_uid + 1
        await storage.customer.create(Customer(uid=uid))
        await storage.nfc_install_reward.create(
            NFCInstallReward(uid=uid, device_id=device_id, amount=Decimal('100'))
        )

        r = await app.post(self.URL, json={'device_id': device_id}, **authentication)

        assert_that(r.status, equal_to(409))
        assert_that(
            await r.json(),
            equal_to({'code': 409, 'data': {'message': 'REWARD_ALREADY_EXISTS'}, 'status': 'fail'})
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('owner_uid', [2], indirect=True)  # uid 2 should get zero cashback for nfc
    async def test_cannot_init_zero_reward(self, app, device_id, authentication):
        r = await app.post(self.URL, json={'device_id': device_id}, **authentication)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 400,
                    'data': {
                        'params': {'amount': self.ZERO_AMOUNT},
                        'message': 'INVALID_REWARD_AMOUNT',
                    },
                    'status': 'fail',
                }
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('user_score', [0.3], indirect=True)
    async def test_bad_user_score(self, app, device_id, authentication, ensure_no_rewards):
        await ensure_no_rewards()

        r = await app.post(self.URL, json={'device_id': device_id}, **authentication)

        assert_that(r.status, equal_to(403))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 403,
                    'data': {
                        'message': 'BAD_USER_SCORE',
                    },
                    'status': 'fail',
                }
            )
        )

    @pytest.mark.asyncio
    async def test_device_not_trusted(
        self, app, device_id, authentication, ensure_no_rewards, trusted_devices
    ):
        trusted_devices.remove(device_id)
        await ensure_no_rewards()

        r = await app.post(self.URL, json={'device_id': device_id}, **authentication)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'code': 400,
                    'data': {
                        'params': {'amount': self.ZERO_AMOUNT},
                        'message': 'INVALID_REWARD_AMOUNT',
                    },
                    'status': 'fail',
                }
            )
        )
