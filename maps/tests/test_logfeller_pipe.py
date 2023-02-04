import json
from datetime import datetime
import logfeller_pipe


def test_parse_simple_line():
    line = '2021-06-03 13:47:14,554 - fb79aec6a7d1cd90417f387709bae08d,171a1a6e-1911-450a-9978-37c5c4c63cdd - ' \
           '1036581 - MainThread.ya_courier_backend.app.order_details.get:334 - INFO -  order-details - ' \
           '_add_route_states_to_orders0.0038361549377441406s'
    record = logfeller_pipe.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2021-06-03 13:47:14.554',
        'timestamp': 1622717234554,
        'req_id': 'fb79aec6a7d1cd90417f387709bae08d,171a1a6e-1911-450a-9978-37c5c4c63cdd',
        'log_id': 1036581,
        'source': 'MainThread.ya_courier_backend.app.order_details.get:334',
        'level': 'INFO',
        'message': ' order-details - _add_route_states_to_orders0.0038361549377441406s',
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_in_request_line():
    line = '2021-06-03 13:47:14,544 - None - None - Thread-2.ya_courier_backend.app.util.wrapper:328 - INFO -  ' \
           'time_logger: function: update_route_state, time: 0.6832072734832764s'
    record = logfeller_pipe.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2021-06-03 13:47:14.544',
        'timestamp': 1622717234544,
        'req_id': None,
        'log_id': None,
        'source': 'Thread-2.ya_courier_backend.app.util.wrapper:328',
        'level': 'INFO',
        'message': ' time_logger: function: update_route_state, time: 0.6832072734832764s',
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_request_line():
    line = '[2021-06-03 13:47:14 +0300] [433] [INFO] [tornado.access] 200 POST ' \
           '/api/v1/gps-trackers/356173065078490/push-positions (0.0.0.0) 107.39ms'
    record = logfeller_pipe.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2021-06-03 13:47:14',
        'timestamp': 1622717234000,
        'req_id': None,
        'log_id': 433,
        'source': 'tornado.access',
        'level': 'INFO',
        'message': '200 POST /api/v1/gps-trackers/356173065078490/push-positions (0.0.0.0) 107.39ms',
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_line_with_prefix():
    line = '1;2;3;[2021-06-03 13:47:14 +0300] [433] [INFO] [tornado.access] 200 POST ' \
           '/api/v1/gps-trackers/356173065078490/push-positions (0.0.0.0) 107.39ms'
    record = logfeller_pipe.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2021-06-03 13:47:14',
        'timestamp': 1622717234000,
        'req_id': None,
        'log_id': 433,
        'source': 'tornado.access',
        'level': 'INFO',
        'message': '200 POST /api/v1/gps-trackers/356173065078490/push-positions (0.0.0.0) 107.39ms',
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == "1;2;3;" + json.dumps(excepted_record, sort_keys=True)


def test_fake_record():
    line = 'message in invalid format\n\n'
    record = logfeller_pipe.LogRecord()
    now = datetime.now()
    record.make_fake_record(line, now=now)
    excepted_record = {
        'time': now.isoformat(sep=' ', timespec='milliseconds'),
        'timestamp': int(1000 * now.timestamp()),
        'req_id': None,
        'log_id': None,
        'source': '',
        'level': '',
        'message': '',
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)
