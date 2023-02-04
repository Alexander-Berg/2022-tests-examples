import socket
import base64
import json
import time
from library.python import resource
import requests


LOCAL_HOST = '127.0.0.1'


def wait_until(condition, check_interval=0.1, timeout=10):
    start_time = time.time()
    while True:
        result = condition()
        if bool(result) or (timeout is not None and start_time + timeout < time.time()):
            return result
        time.sleep(check_interval)


def test_simple(egts_proxy):
    stats = egts_proxy["stats"]
    dbg_data = resource.find("data.json")
    assert dbg_data is not None
    dbg = json.loads(dbg_data)

    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((LOCAL_HOST, egts_proxy["port"]))

    client.send(base64.b64decode(dbg["simple_position_packet"][0]))
    time.sleep(0.2)

    wait_until(lambda: "POST" in stats and stats['POST'] == 1)
    assert "POST" in stats
    assert stats['POST'] == 1
    TID_FROM_BINARY_MESSAGE = "2044051"
    assert TID_FROM_BINARY_MESSAGE in stats['tracks']
    assert stats['tracks'][TID_FROM_BINARY_MESSAGE]['positions'] == 1


# example of hgram:
# ["backend_response_time_hgram", [[0, 8], [10, 0], [50, 0], [100, 0], [200, 0], [500, 0], [1000, 0], [2000, 0], [5000, 0]]],
def remove_unstable(j):
    UNSTABLE_FIELDS = [
        "backend_response_time_hgram",
        "tcp_packets_received_summ",
        "positions_status_ERROR_NEED_MORE_DATA_summ",
        "backend_response_200_summ",
        "pg_queue_size_axxx",
        "pg_queue_tids_size_axxx",
        "pg_terminal_lock_size_axxx",
        "queue_wait_time_axxx"
    ]
    return [item for item in j if item[0] not in UNSTABLE_FIELDS]


def count_hgram(j):
    hgram = next(item for item in j if "_hgram" in item[0])
    return sum(pair[1] for pair in hgram[1])


def find_unistat_value(unistat, field_name):
    for item in unistat:
        if item[0] == field_name:
            return item[1]
    return None


def check_unistat(egts_proxy):
    r = requests.get("http://{host}:{port}/unistat".format(host=LOCAL_HOST, port=egts_proxy["http_port"]))
    assert r.status_code == 200
    unistat = r.json()
    assert remove_unstable(unistat) == [
        ["active_session_count_summ", 3],
        ["backend_error_summ", 0],
        ["backend_response_2xx_summ", 0],
        ["backend_response_3xx_summ", 0],
        ["backend_response_4xx_summ", 0],
        ["backend_response_5xx_summ", 0],
        ["backend_response_unknown_summ", 0],
        ["bytes_received_summ", 2342],
        ["imei_collision_summ", 0],
        ["io_thread_failure_summ", 0],
        ["io_threads_count_axxx", 0],
        ["io_threads_usage_axxx", 0],
        ["io_threads_usage_avg_avvv", 0],
        ["pg_commit_failed_summ", 0],
        ["position_count_expired_summ", 0],
        ["position_count_future_summ", 0],
        ["position_count_not_sent_summ", 0],
        ["position_count_notid_summ", 0],
        ["position_count_sent_summ", 52],
        ["position_count_total_summ", 52],
        ["position_count_zero_coord_summ", 0],
        ["positions_status_ERROR_AUTH_DENIED_summ", 0],
        ["positions_status_ERROR_INTERNAL_summ", 0],
        ["positions_status_OK_summ", 8],
        ["positions_status_OK_NO_POSITIONS_summ", 3],
        ["positions_status_OK_NO_RESPONSE_summ", 1],
        ["submitter_thread_failure_summ", 0],
        ["submitter_threads_count_axxx", 0],
        ["submitter_threads_usage_avg_avvv", 0]
        ]
    assert count_hgram(unistat) >= 5
    assert find_unistat_value(unistat, "backend_response_200_summ") >= 5

    r = requests.get("http://{host}:{port}/unistat".format(host=LOCAL_HOST, port=egts_proxy["http_port"]))
    assert r.status_code == 200
    unistat = r.json()
    assert remove_unstable(unistat) == [
        ["active_session_count_summ", 6],  # BBGEO-2026 value should not increment
        ["backend_error_summ", 0],
        ["backend_response_2xx_summ", 0],
        ["backend_response_3xx_summ", 0],
        ["backend_response_4xx_summ", 0],
        ["backend_response_5xx_summ", 0],
        ["backend_response_unknown_summ", 0],
        ["bytes_received_summ", 2342],
        ["imei_collision_summ", 0],
        ["io_thread_failure_summ", 0],
        ["io_threads_count_axxx", 0],
        ["io_threads_usage_axxx", 0],
        ["io_threads_usage_avg_avvv", 0],
        ["pg_commit_failed_summ", 0],
        ["position_count_expired_summ", 0],
        ["position_count_future_summ", 0],
        ["position_count_not_sent_summ", 0],
        ["position_count_notid_summ", 0],
        ["position_count_sent_summ", 52],
        ["position_count_total_summ", 52],
        ["position_count_zero_coord_summ", 0],
        ["positions_status_ERROR_AUTH_DENIED_summ", 0],
        ["positions_status_ERROR_INTERNAL_summ", 0],
        ["positions_status_OK_summ", 8],
        ["positions_status_OK_NO_POSITIONS_summ", 3],
        ["positions_status_OK_NO_RESPONSE_summ", 1],
        ["submitter_thread_failure_summ", 0],
        ["submitter_threads_count_axxx", 0],
        ["submitter_threads_usage_avg_avvv", 0]
        ]
    assert count_hgram(r.json()) >= 5
    assert find_unistat_value(unistat, "backend_response_200_summ") >= 5


def test_all_data(egts_proxy):
    stats = egts_proxy["stats"]
    dbg_data = resource.find("data.json")
    assert dbg_data is not None
    dbg = json.loads(dbg_data)

    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((LOCAL_HOST, egts_proxy["port"]))

    for key, value in dbg.items():
        for chunk in value:
            client.send(base64.b64decode(chunk))
            time.sleep(0.2)

    wait_until(lambda: "POST" in stats and stats['POST'] >= 5)
    assert "POST" in stats
    assert stats['POST'] >= 5
    assert set(stats['tracks'].keys()) == set(['356307046486162', '352094087647829', '356307046906649', '2005097', '2044051'])
    client.close()
    client = None
    time.sleep(1)
    check_unistat(egts_proxy)
