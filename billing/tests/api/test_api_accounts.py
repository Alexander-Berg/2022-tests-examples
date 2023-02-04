from bcl.banks.registry import Sber


def test_accounts(api_client, get_assoc_acc_curr):

    _, acc, _ = get_assoc_acc_curr(Sber, account='5567/70,@some.com')

    response = api_client.get(f'/api/refs/accounts/?accounts=["1020/30","{acc.number}"]')
    assert response.ok
    response = response.json
    assert not response['errors']
    accounts = response['data']['items']
    assert len(accounts) == 1
    assert accounts[0]['blocked'] == 0

    acc.blocked = acc.SOFT_BLOCKED
    acc.save()

    response = api_client.get(f'/api/refs/accounts/?accounts=["{acc.number}"]')
    response = response.json
    assert response['data']['items'][0]['blocked'] == 1

