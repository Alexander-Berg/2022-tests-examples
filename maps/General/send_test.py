from maps.poi.notification.lib.sup import parse_response


class SupResponse(object):
    def __init__(self, status_code, json_content):
        self.status_code = status_code
        self.json_content = json_content

    def json(self):
        return self.json_content


def parse_sup_response(status_code, json_content):
    response = SupResponse(status_code, json_content)
    return parse_response(response)


def test_parse_response():
    assert parse_sup_response(401, {}) == {}

    assert parse_sup_response(200, {}) == {}

    assert parse_sup_response(
        200,
        {
            'jobName': 'pushesBatchJob',
            'jobId': 3654,
            'lastUpdated': 1596897335997,
            'jobExecutionId': 3654,
            'createTime': 1596897335997,
            'exitCode': 'UNKNOWN',
            'jobParameters': '{reqid=1596897335337788-13317837246523385832, '
                             'path=//home/table, limit=null, user=robot-marketinggeo, '
                             'batch_size=null}',
            'exitDescription': '',
            'version': 0,
            'status': 'STARTING'
        }

    ) == {
        'reqid': '1596897335337788-13317837246523385832',
        'path': '//home/table',
        'limit': 'null',
        'user': 'robot-marketinggeo',
        'batch_size': 'null'
    }

    assert parse_sup_response(
        200,
        {
            'path': '//home/table[#0:#100]',
            'reqid': '1596896838081030-11080578076519756508'
        }
    ) == {
        'path': '//home/table[#0:#100]',
        'reqid': '1596896838081030-11080578076519756508'
    }
