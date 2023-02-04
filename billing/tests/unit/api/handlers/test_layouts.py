from datetime import datetime, timezone
from uuid import UUID, uuid4

import pytest
from aiohttp import MultipartWriter

from hamcrest import anything, assert_that, equal_to, has_properties, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout.create import CreateLayoutAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout

PARTNER_ID = uuid4()
ORIGIN_ID = uuid4()
FILE_CONTENT = b'1' * 1000


@pytest.mark.asyncio
async def test_success(app, mock_action, make_request, disable_tvm_checking, request_data):
    mock_action(
        CreateLayoutAction,
        Layout(
            layout_id=UUID('a94a2e18-5daa-4383-91f8-0cbe5fe7f423'),
            origin_id=UUID('f2b22eac-213a-4761-b5ee-43352fc7f025'),
            type=LayoutType.CHECKOUT,
            document_id=UUID('315cfdde-d86f-4ceb-9d8a-277023da5354'),
            document=Document(
                document_id=UUID('315cfdde-d86f-4ceb-9d8a-277023da5354'),
                partner_id=UUID('d510f226-5291-4643-a8d2-cfb83d0d3ecd'),
                path='pa/th',
                digest='digest',
                name='file.png',
                mime='mime/type',
                created=datetime(2021, 1, 15, 10, 30, 50, tzinfo=timezone.utc),
                updated=datetime(2021, 1, 30, 10, 30, 50, tzinfo=timezone.utc),
            ),
            created=datetime(2021, 1, 15, 10, 30, 50, tzinfo=timezone.utc),
            updated=datetime(2021, 1, 30, 10, 30, 50, tzinfo=timezone.utc),
        ),
    )

    response = await make_request(request_data)

    assert_that(response.status, equal_to(200))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'layout_id': 'a94a2e18-5daa-4383-91f8-0cbe5fe7f423',
                    'origin_id': 'f2b22eac-213a-4761-b5ee-43352fc7f025',
                    'type': 'checkout',
                    'document': {
                        'document_id': '315cfdde-d86f-4ceb-9d8a-277023da5354',
                        'name': 'file.png',
                        'mime': 'mime/type',
                        'created': '2021-01-15T10:30:50+00:00',
                        'updated': '2021-01-30T10:30:50+00:00',
                    },
                    'created': '2021-01-15T10:30:50+00:00',
                    'updated': '2021-01-30T10:30:50+00:00',
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_create_layout(disable_tvm_checking, make_request, request_data, mock_action, layout):
    mock_create_layout = mock_action(CreateLayoutAction, layout)

    await make_request(request_data)

    mock_create_layout.assert_run_once_with(
        partner_id=PARTNER_ID,
        origin_id=ORIGIN_ID,
        type=LayoutType.CHECKOUT,
        file_field=match_equality(
            has_properties(
                filename='file.png',
                content_type='image/png',
            )
        ),
    )


@pytest.mark.parametrize(
    'layout_type_str, expected_layout_type',
    (
        ('checkout', LayoutType.CHECKOUT),
        ('after_checkout', LayoutType.AFTER_CHECKOUT),
        ('before_checkout', LayoutType.BEFORE_CHECKOUT),
    ),
)
@pytest.mark.asyncio
async def test_layout_type_convertion(
    disable_tvm_checking, make_request, request_data, mock_action, layout, layout_type_str, expected_layout_type
):
    mock_create_layout = mock_action(CreateLayoutAction, layout)
    request_data['type'] = layout_type_str

    await make_request(request_data)

    mock_create_layout.assert_run_once_with(
        partner_id=match_equality(anything()),
        origin_id=match_equality(anything()),
        type=expected_layout_type,
        file_field=match_equality(anything()),
    )


@pytest.mark.asyncio
async def test_validation_path_bad_format(app, mock_action, make_request, disable_tvm_checking, request_data):
    response = await make_request(request_data, partner_id='foo', origin_id='bar')

    assert_that(response.status, equal_to(400))
    data = await response.json()
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
                        'origin_id': ['Not a valid UUID.'],
                    },
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_validation_body_bad_format(app, mock_action, make_request, disable_tvm_checking, request_data):
    request_data['type'] = 'wtf'

    response = await make_request(request_data)

    assert_that(response.status, equal_to(400))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'type': ['Invalid enum value wtf'],
                    },
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_validation_body_absent_fields(app, mock_action, make_request, disable_tvm_checking):
    response = await make_request({})

    assert_that(response.status, equal_to(400))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'type': ['Missing data for required field.'],
                        'file': ['Missing data for required field.'],
                    },
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_validation_bad_content_type(app, mock_action, disable_tvm_checking):
    response = await app.post(
        f'/api/web/v1/partners/{PARTNER_ID}/origins/{ORIGIN_ID}/layouts', json={'type': 'checkout', 'file': None}
    )

    assert_that(response.status, equal_to(400))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': {
                        'type': ['Missing data for required field.'],
                        'file': ['Missing data for required field.'],
                    },
                },
            }
        ),
    )


@pytest.fixture
def make_request(app):
    async def _make_request(data, partner_id=PARTNER_ID, origin_id=ORIGIN_ID):
        with MultipartWriter("form-data") as mpwriter:
            if 'file' in data:
                file_part = mpwriter.append(data['file']['content'], {'content-type': data['file']['content_type']})
                file_part.set_content_disposition('form-data', filename='file.png', name='file')

            if 'type' in data:
                type_part = mpwriter.append(data['type'])
                type_part.set_content_disposition('form-data', name='type')
            return await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/layouts', data=mpwriter)

    return _make_request


@pytest.fixture
def request_data():
    return {'type': 'checkout', 'file': {'content': 'file123123123', 'content_type': 'image/png'}}


@pytest.fixture
def layout():
    return Layout(
        layout_id=UUID('a94a2e18-5daa-4383-91f8-0cbe5fe7f423'),
        origin_id=UUID('f2b22eac-213a-4761-b5ee-43352fc7f025'),
        type=LayoutType.CHECKOUT,
        document_id=UUID('315cfdde-d86f-4ceb-9d8a-277023da5354'),
        document=Document(
            document_id=UUID('315cfdde-d86f-4ceb-9d8a-277023da5354'),
            partner_id=UUID('d510f226-5291-4643-a8d2-cfb83d0d3ecd'),
            path='pa/th',
            digest='digest',
            name='file.png',
            mime='mime/type',
            created=datetime(2021, 1, 15, 10, 30, 50, tzinfo=timezone.utc),
            updated=datetime(2021, 1, 30, 10, 30, 50, tzinfo=timezone.utc),
        ),
        created=datetime(2021, 1, 15, 10, 30, 50, tzinfo=timezone.utc),
        updated=datetime(2021, 1, 30, 10, 30, 50, tzinfo=timezone.utc),
    )
