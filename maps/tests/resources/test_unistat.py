from http import HTTPStatus


def test_responses_ok_if_healthy(test_client):
    test_client.get('/api/v1/healthcheck')

    resp = test_client.get('/unistat')

    assert resp.status_code == HTTPStatus.OK
    assert len(resp.json) > 0
    res_dict = {e[0]: e[1] for e in resp.json}
    # FIXME: depends on test execution order, second access to healthcheck is in test_healthcheck.py
    assert res_dict['statuses-healthcheckresource-200_summ'] in {1, 2}
