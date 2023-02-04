import time
import requests
import json
import pytest


@pytest.mark.parametrize("mode", ["driving", "walking", "transit"])
def test_routing_matrix(url_routing_matrix_api, routing_matrix_cgi, mode):
    cgi = routing_matrix_cgi
    cgi["mode"] = mode
    url = url_routing_matrix_api + "/routing_matrix"
    response = requests.get(url, params=cgi)
    assert response.ok

    status = json.loads(response.content)
    assert status.get("id")

    ready = False
    state_url = "{host}/current_state?data_id={id}&timeout=2000000".format(host=url_routing_matrix_api, id=status.get("id"))
    for _ in range(30):
        time.sleep(1)
        state_response = requests.get(state_url)
        assert state_response.ok

        state = json.loads(state_response.content)
        if state.get("state").get("data").get("ready"):
            ready = state["state"]["data"]["ready"]
            if ready:
                break
    assert ready

    matrix_url = state_url + "&pron=need_result"
    matrix_response = requests.get(matrix_url)
    assert matrix_response.ok

    matrix = json.loads(matrix_response.content)
    assert matrix["state"]["data"]["count_calc"] == matrix["state"]["data"]["correct_size"]
    assert matrix["state"]["data"]["incorrect_size"] == 0
    assert matrix["state"]["data"]["not_initial_size"] == 0
