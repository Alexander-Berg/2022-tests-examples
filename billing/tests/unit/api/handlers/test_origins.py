from datetime import datetime, timezone
from uuid import UUID, uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.create import CreateOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.list import GetOriginsByMerchantIDAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration

PARTNER_ID = uuid4()
MERCHANT_ID = uuid4()


class TestGet:
    @pytest.mark.asyncio
    async def test_success(self, app, disable_tvm_checking, request_data, mock_action):
        mock_action(
            GetOriginsByMerchantIDAction,
            [
                Origin(
                    origin_id=UUID('e8a5d99c-50bd-489e-8db7-e9f4ff07d8da'),
                    merchant_id=UUID('4950a773-c240-4ba0-af7c-e4776052ea88'),
                    origin='https://the.origin.test',
                    created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                    updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                    moderation=None,
                    layouts=[],
                ),
                Origin(
                    origin_id=UUID('bfb82c7b-2b2e-4a61-90ac-029091675d79'),
                    merchant_id=UUID('31fda58b-2c8c-4a36-821a-6e532e071e62'),
                    origin='https://the-other.origin.test',
                    created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                    updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                    moderation=OriginModeration(
                        origin_moderation_id=UUID('a6393843-da41-4240-8658-52a6db9cdd26'),
                        origin_id=UUID('7af2bac0-035c-42cb-bc9f-2c0f29e33e34'),
                        ticket='TICKET-1',
                        ignored=False,
                        resolved=True,
                        approved=True,
                        revision=1,
                        reason={'rea': 'son'},
                        created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                        updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                    ),
                    layouts=[
                        Layout(
                            layout_id=UUID('0bebddc1-0e8a-4a1e-91c9-4f7df7073c24'),
                            origin_id=UUID('7ac5c831-35bf-4734-bfda-ead27de776dc'),
                            type=LayoutType.CHECKOUT,
                            document_id=UUID('b269749e-7ab0-4148-a46a-331c6d1e10a1'),
                            document=Document(
                                document_id=UUID('315cfdde-d86f-4ceb-9d8a-277023da5354'),
                                partner_id=UUID('d510f226-5291-4643-a8d2-cfb83d0d3ecd'),
                                path='pa/th',
                                digest='digest',
                                name='file.png',
                                mime='mime/type',
                                created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                                updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                            ),
                            created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                            updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                        ),
                    ],
                ),
            ],
        )

        response = await app.get(
            f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json=request_data
        )
        data = await response.json()

        assert_that(response.status, equal_to(200))
        assert data == {
            'status': 'success',
            'code': 200,
            'data': {
                'origins': [
                    {
                        'origin_id': 'e8a5d99c-50bd-489e-8db7-e9f4ff07d8da',
                        'merchant_id': '4950a773-c240-4ba0-af7c-e4776052ea88',
                        'origin': 'https://the.origin.test',
                        'created': '2021-11-30T10:30:50+00:00',
                        'updated': '2021-12-30T10:30:50+00:00',
                        'moderation': None,
                        'layouts': [],
                    },
                    {
                        'origin_id': 'bfb82c7b-2b2e-4a61-90ac-029091675d79',
                        'merchant_id': '31fda58b-2c8c-4a36-821a-6e532e071e62',
                        'origin': 'https://the-other.origin.test',
                        'created': '2021-11-30T10:30:50+00:00',
                        'updated': '2021-12-30T10:30:50+00:00',
                        'moderation': {
                            'origin_moderation_id': 'a6393843-da41-4240-8658-52a6db9cdd26',
                            'origin_id': '7af2bac0-035c-42cb-bc9f-2c0f29e33e34',
                            'resolved': True,
                            'approved': True,
                            'created': '2021-11-30T10:30:50+00:00',
                            'updated': '2021-12-30T10:30:50+00:00',
                        },
                        'layouts': [
                            {
                                'layout_id': '0bebddc1-0e8a-4a1e-91c9-4f7df7073c24',
                                'origin_id': '7ac5c831-35bf-4734-bfda-ead27de776dc',
                                'type': 'checkout',
                                'document': {
                                    'document_id': '315cfdde-d86f-4ceb-9d8a-277023da5354',
                                    'name': 'file.png',
                                    'mime': 'mime/type',
                                    'created': '2021-11-30T10:30:50+00:00',
                                    'updated': '2021-12-30T10:30:50+00:00',
                                },
                                'created': '2021-11-30T10:30:50+00:00',
                                'updated': '2021-12-30T10:30:50+00:00',
                            },
                        ],
                    },
                ]
            },
        }

    @pytest.mark.asyncio
    async def test_calls_get_origins(
        self,
        app,
        disable_tvm_checking,
        mock_action,
        request_data,
        user,
    ):
        mock_get_origins = mock_action(GetOriginsByMerchantIDAction)

        await app.get(f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json=request_data)

        mock_get_origins.assert_run_once_with(partner_id=PARTNER_ID, merchant_id=MERCHANT_ID, user=user)

    @pytest.mark.asyncio
    async def test_validation_validates_required_fields(self, app, disable_tvm_checking, request_data, mock_action):
        response = await app.post(f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json={})
        data = await response.json()

        assert_that(response.status, equal_to(400))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {
                        'message': 'SCHEMA_VALIDATION_ERROR',
                        'params': {'origin': ['Missing data for required field.']},
                    },
                }
            ),
        )

    @pytest.fixture
    def request_data(self):
        return {'origin': 'https://the.origin.test'}


class TestPost:
    @pytest.mark.asyncio
    async def test_success(self, app, disable_tvm_checking, request_data, mock_action):
        mock_action(
            CreateOriginAction,
            Origin(
                origin_id=UUID('e8a5d99c-50bd-489e-8db7-e9f4ff07d8da'),
                merchant_id=UUID('d0555774-ddef-4cc3-a916-944da83424aa'),
                origin='https://the.origin.test',
                created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
                updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
                moderation=None,
                layouts=[],
            ),
        )

        response = await app.post(
            f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json=request_data
        )
        data = await response.json()

        assert_that(response.status, equal_to(200))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'origin_id': 'e8a5d99c-50bd-489e-8db7-e9f4ff07d8da',
                        'merchant_id': 'd0555774-ddef-4cc3-a916-944da83424aa',
                        'origin': 'https://the.origin.test',
                        'created': '2021-11-30T10:30:50+00:00',
                        'updated': '2021-12-30T10:30:50+00:00',
                        'moderation': None,
                        'layouts': [],
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_create_origin(
        self,
        app,
        disable_tvm_checking,
        mock_action,
        request_data,
        user,
    ):
        mock_create_origin = mock_action(CreateOriginAction)

        await app.post(f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json=request_data)

        mock_create_origin.assert_run_once_with(
            partner_id=PARTNER_ID,
            merchant_id=MERCHANT_ID,
            origin='https://the.origin.test',
            user=user,
        )

    @pytest.mark.asyncio
    async def test_validation_validates_path_fields(self, app, disable_tvm_checking, request_data, mock_action):
        response = await app.post('/api/web/v1/partners/foo/merchants/bar/origins', json=request_data)
        data = await response.json()

        assert_that(response.status, equal_to(400))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {
                        'message': 'SCHEMA_VALIDATION_ERROR',
                        'params': {
                            'partner_id': ['Not a valid UUID.'],
                            'merchant_id': ['Not a valid UUID.'],
                        },
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_validation_validates_required_fields(self, app, disable_tvm_checking, request_data, mock_action):
        response = await app.post(f'/api/web/v1/partners/{PARTNER_ID}/merchants/{MERCHANT_ID}/origins', json={})
        data = await response.json()

        assert_that(response.status, equal_to(400))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'fail',
                    'code': 400,
                    'data': {
                        'message': 'SCHEMA_VALIDATION_ERROR',
                        'params': {'origin': ['Missing data for required field.']},
                    },
                }
            ),
        )

    @pytest.fixture
    def request_data(self):
        return {'origin': 'https://the.origin.test'}
