import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries


@pytest.fixture
def uri():
    return '/api/v1/documents'


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_create_document(app, authenticate_client, uri):
    authenticate_client(app)
    resp = await app.post(
        uri,
        json={'type': 'DRIVER_LICENSE', 'title': 'TheTitle', 'value': '1692555555'},
    )

    assert_that(resp.status, equal_to(201))
    assert_that(
        await resp.json(),
        has_entries(
            data=has_entries(
                document=has_entries(
                    title='TheTitle',
                    value='1692555555',
                    type='DRIVER_LICENSE',
                )
            ),
        ),
    )


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_get_documents(app, authenticate_client, uri):
    authenticate_client(app)
    await app.post(
        uri,
        json={'type': 'DRIVER_LICENSE', 'title': 'TheTitle', 'value': '1692555555'},
        raise_for_status=True,
    )
    await app.post(
        uri,
        json={'type': 'DRIVER_LICENSE', 'title': 'The second thing', 'value': '1692666666'},
        raise_for_status=True,
    )

    resp = await app.get(
        uri,
    )

    assert_that(resp.status, equal_to(200))
    assert_that(
        await resp.json(),
        has_entries(
            data=has_entries(
                documents=contains_inanyorder(
                    has_entries(value='1692555555'),
                    has_entries(value='1692666666'),
                )
            ),
        ),
    )
