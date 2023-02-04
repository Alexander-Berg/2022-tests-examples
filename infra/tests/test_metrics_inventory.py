from sepelib.metrics.registry import metrics_inventory


def test_list_children():
    metrics_inventory.clear()
    for service in ['/services/http', '/clients/cms', '/clients/authqe', '/clients/iss']:
        metrics_inventory.get_metrics(service)
    children = metrics_inventory.children('/')
    assert sorted(children) == sorted(['services', 'clients'])
    children = metrics_inventory.children('/clients')
    assert sorted(children) == sorted(['cms', 'authqe', 'iss'])


def test_list_all_metrics():
    metrics_inventory.clear()
    services = ['/services/http', '/clients/cms', '/clients/authqe', '/clients/iss']
    for service in services:
        metrics_inventory.get_metrics(service)

    assert sorted(services) == sorted(metrics_inventory.keys())

    assert sorted(services) == sorted(metrics_inventory.keys('/'))


def test_same_metrics_for_key():
    service = '/services/http'
    assert metrics_inventory.get_metrics(service) is metrics_inventory.get_metrics(service)
