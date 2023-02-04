import pytest

from hamcrest import assert_that, equal_to, has_entries


@pytest.fixture
def uri():
    return '/api/v1/documents'


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_update_document(app, authenticate_client, uri):
    authenticate_client(app)
    resp = await app.post(
        uri,
        json={'type': 'DRIVER_LICENSE', 'title': 'TheTitle', 'value': '1692555555'},
        raise_for_status=True,
    )

    document_id = (await resp.json())['data']['document']['document_id']

    resp = await app.put(
        f'{uri}/{document_id}',
        json={'type': 'VEHICLE_REGISTRATION_CERTIFICATE', 'title': 'Title2', 'value': '1221777777'},
    )

    assert_that(resp.status, equal_to(200))
    assert_that(
        await resp.json(),
        has_entries(
            data=has_entries(
                document=has_entries(
                    title='Title2',
                    value='1221777777',
                    type='VEHICLE_REGISTRATION_CERTIFICATE',
                )
            ),
        ),
    )


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_delete_document(app, authenticate_client, uri):
    authenticate_client(app)
    resp = await app.post(
        uri,
        json={'type': 'DRIVER_LICENSE', 'title': 'TheTitle', 'value': '1692555555'},
        raise_for_status=True,
    )

    document_id = (await resp.json())['data']['document']['document_id']

    resp = await app.delete(f'{uri}/{document_id}')

    assert_that(resp.status, equal_to(200))
