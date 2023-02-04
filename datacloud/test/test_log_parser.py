import unittest
from freezegun import freeze_time
from datacloud.log_reader.lib.log_parser import ScoreApiLogParser


class TestScoreApiLogParser(unittest.TestCase):

    def test_parse_raw(self):
        raw_data = '''{"status":"200","req_id":"c396f2cbe35a9d9231c975e7b20f6b31","blnsr_req_id":"623788fc91ac3e1c205ec708e3ee0547","timestamp":"2019-06-27T12:34:49+00:00","user_agent":"curl/7.47.0","remote_addr":"172.17.0.1","request":"GET /v1/accounts/internal/scores HTTP/1.1","resp_time":"0.024","req_body":"{\\"user_ids\\": {\\"emails\\": [{\\"id_value\\":\\"797dff8a7a059d6660f35e45790aeade\\"}]}, \\"scores\\":[{\\"score_name\\":\\"ubrr_xprod_1158_socdef\\"}]}","resp_body":"{\\"scores\\":[{\\"has_score\\":true,\\"score_name\\":\\"ubrr_xprod_1158_socdef\\",\\"score_value\\":0.13230115415779847}]}\\n"}'''  # noqa

        expected_rec = {
            'status': u'200',
            'timestamp': u'2019-06-27T12:34:49+00:00',
            'request': u'GET /v1/accounts/internal/scores HTTP/1.1',
            'other': None,
            'resp_body': {
                u'scores': [{
                    u'score_name': u'ubrr_xprod_1158_socdef',
                    u'has_score': True,
                    u'score_value': 0.13230115415779847
                }]
            },
            'req_id': u'c396f2cbe35a9d9231c975e7b20f6b31',
            'req_body': {
                u'user_ids': {
                    u'emails': [{
                        u'id_value': u'797dff8a7a059d6660f35e45790aeade'
                    }]
                },
                u'scores': [{
                    u'score_name': u'ubrr_xprod_1158_socdef'
                }]
            },
            u'user_agent': u'curl/7.47.0',
            u'remote_addr': u'172.17.0.1',
            u'resp_time': u'0.024',
            u'host': None,
            u'dc': None,
            u'partner_id': u'internal',
            u'method': u'GET',
            u'req_type': u'scores',
            u'blnsr_req_id': u'623788fc91ac3e1c205ec708e3ee0547'
        }
        parser = ScoreApiLogParser()
        data, status = parser.parse(raw_data)
        self.assertTrue(status)
        self.assertEqual(data, expected_rec)

    @freeze_time('2019-07-01 18:56:01')
    def test_wrong_line(self):
        raw_data = '''{somethingr-wrong: "ololo|'''
        expected_rec = {
            'timestamp': '2019-07-01T18:56:01',
            'other': None,
            'hash': 'c80ac0dff94547b1e799db0a44f08510',
            'data': '{somethingr-wrong: "ololo|'
        }
        parser = ScoreApiLogParser()
        data, status = parser.parse(raw_data)
        self.assertEqual(data, expected_rec)
        self.assertFalse(status)
