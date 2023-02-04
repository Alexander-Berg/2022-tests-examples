import yaml
import requests


def test_run_local(vmproxy_url, auth_user, ):
    resp = requests.get(vmproxy_url + '/config')
    assert resp.status_code == 200

    config = yaml.load(resp.content)

    assert config.get('run', {}).get('force_return_user') == auth_user
