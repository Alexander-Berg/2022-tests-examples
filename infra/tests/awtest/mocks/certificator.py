# coding=utf-8
from awtest.mocks.startrek import SECTASK_ID


class MockCertificatorClient(object):
    environment = 'testing'
    last_response = None

    @classmethod
    def send_create_request(cls, is_ecc=False, desired_ttl_days=None, *_, **__):
        cls.last_response = {
            'url': 'fake/url',
            'approve_request': 'fake_approve',
            'st_issue_key': SECTASK_ID,
            'status': 'fake_status',
            'is_ecc': is_ecc,
            'desired_ttl_days': desired_ttl_days,
        }
        return cls.last_response

    @staticmethod
    def get_cert(*_, **__):
        return {
            'uploaded_to_yav': True,
            'yav_secret_id': 'fake_secret_id',
            'yav_secret_version': 'fake_secret_ver',
            'status': 'fake_status',
            'serial_number': 'fake_sn',
        }

    @staticmethod
    def list_auto_managed_hosts(*_, **__):
        return [u'yandex.ru', u'yandex-team.ru', u'other.ru', u'test', u'привет.рф']
