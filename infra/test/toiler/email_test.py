import unittest

import mock

from genisys.toiler import email


class GetRecipientsTestCase(unittest.TestCase):
    CONFIG = {
        'STAFF_HEADERS': {'x-staff-header': 'bzz'},
        'STAFF_TIMEOUT': 4,
        'STAFF_URI': 'https://staff',
        'EMAIL_MAX_RECIPIENTS': 10,
    }

    class Response(object):
        def __init__(self, status_code, json):
            self.status_code = status_code
            self.json = lambda: json

        def raise_for_status(self):
            if self.status_code != 200:
                raise Exception(str(self.status_code))

    def setUp(self):
        super(GetRecipientsTestCase, self).setUp()
        self.cfgmock = mock.patch('genisys.toiler.email.config')
        m = self.cfgmock.start()
        for key, value in self.CONFIG.items():
            setattr(m, key, value)

    def tearDown(self):
        self.cfgmock.stop()
        super(GetRecipientsTestCase, self).tearDown()

    def test(self):
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = self.Response(200, {
                "links": {}, "page": 1, "limit": 10, "result": [
                    {"login": "user1"},
                    {"login": "xgen"},
                    {"login": "kozhapenko"},
                    {"login": "atroynikov"},
                    {"login": "yumal"},
                    {"login": "terry"},
                    {"login": "bgleb"}
                ], "total": 6, "pages": 1})
            rec, err = email._get_email_recipients([
                'user1', 'user2', 'user3', 'user4', 'user5',
                'group:svc_qloud_administration'
            ])

        self.assertEquals(rec, [
            'atroynikov',
            'bgleb',
            'kozhapenko',
            'terry',
            'user1',
            'user2',
            'user3',
            'user4',
            'user5',
            'xgen'
        ])
        self.assertEquals(err, [])
        mock_get.assert_called_once_with(
            'https://staff/persons', allow_redirects=False,
            headers={'x-staff-header': 'bzz'},
            params={'_query': 'department_group.ancestors.url=="svc_qloud_administration" or groups.group.url=="svc_qloud_administration"',
                    '_fields': 'login',
                    '_limit': 10,
                    'official.is_dismissed': 'false'},
            timeout=4
        )

    def test_staff_error(self):
        with mock.patch('requests.get') as mock_get:
            mock_get.return_value = self.Response(500, None)
            rec, err = email._get_email_recipients([
                'user1', 'user2', 'user3', 'user4', 'user5',
                'group:svc_qloud_administration'
            ])

        self.assertEquals(rec, [
            'user1', 'user2', 'user3', 'user4', 'user5',
        ])
        self.assertEquals(err, [err[0]])
        self.assertTrue(err[0].endswith('Exception: 500\n'))


class EmailTestCase(unittest.TestCase):
    def test(self):
        with mock.patch('genisys.toiler.email._get_email_recipients') as getr:
            getr.return_value = ([], ['error1'])
            with self.assertRaises(RuntimeError) as ar:
                email.email(send_to=['group:g1'],
                            subject_template='broken_selectors_subj.txt',
                            body_template='broken_selectors.txt',
                            context={})
        self.assertEquals(str(ar.exception),
                          "no one to send email to! extra errors: ['error1']")
