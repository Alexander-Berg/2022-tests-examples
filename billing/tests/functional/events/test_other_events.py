import pytest


@pytest.fixture
def params():
    return {
        'apiKey': 'key',
        'eventType': 'type',
    }


@pytest.fixture
def json():
    return {
        'date': 1423248912,
        'jobID': 'bf90a0edbb3c4fc09344120a3a91e355',
    }


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'url',
    ['/events/visa/', '/events/visa/paymentTxns',
     '/events/visa/tokenPushProvisionedToken', '/events/visa/jobStatus']
)
async def test_events_visa(app, url, params, json):
    r = await app.post(url, params=params, json=json)
    assert r.status == 200


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'url',
    [
        '/events/mastercard/notifications/',
        '/events/mastercard/notifications/transaction',
    ]
)
async def test_events_mastercard(app, url, params, json):
    r = await app.post(url, params=params, json=json)
    assert r.status == 200
