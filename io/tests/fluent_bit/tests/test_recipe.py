import os
import requests
import time


def test_port():
    assert 'FB_SERVER_PORT' in os.environ


def get(method):
    port = os.environ['FB_SERVER_PORT']
    uri = f"http://0.0.0.0:{port}/api/v1/{method}"

    for _ in range(5):
        res = requests.get(uri)
        if res.status_code == 404:
            time.sleep(1)
            continue
        else:
            assert res.status_code == 200
            break
    else:
        raise RuntimeError(f"Couldn't get {uri} after 5 retries")

    return res.json()


def test_fluent_bit_uptime():
    res = get('uptime')
    assert 'uptime_sec' in res


def test_fluent_bit_metrics():
    res = get('metrics')
    assert res['input']['tail.0']['records'] == 3
    for _ in range(10):
        if res['output']['stdout.0']['proc_records'] < 3:
            time.sleep(0.1)
            res = get('metrics')
            continue
        break
    assert res['output']['stdout.0']['proc_records'] == 3
