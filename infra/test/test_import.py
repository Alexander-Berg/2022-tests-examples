from walle_api import client


def test_client_costruction_without_credentials():
    client.WalleClient()


def test_get_host_without_name(monkeypatch):
    monkeypatch.setattr(client._Api, "call", lambda *args, **kwargs: {"result": [{}], "total": 1})
    c = client.WalleClient()

    assert c.get_hosts(invs=[0]) == {"result": [{}], "total": 1}
