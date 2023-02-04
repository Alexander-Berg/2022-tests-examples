# coding: utf-8
import os
import json
import StringIO

import unittest
import yatest.common

from ads.quality.phf.phf_direct_loader.tests.test_helpers import TEST_REGIONS, TEST_IMAGE_FILE

from ads.quality.phf.phf_direct_loader.lib.extensions.database import ClientDBO, RegionDBO
from ads.quality.phf.phf_direct_loader.lib.config import TestingConfig
from ads.quality.phf.phf_direct_loader.lib.app import make_app, db

DATA_PATH = yatest.common.source_path('ads/quality/phf/phf_direct_loader/tests/resources')


class TestConfigWithRealDirectClient(TestingConfig):
    DIRECT_CLIENT_LOGIN = yatest.common.get_param('DIRECT_TEST_CLIENT')
    DIRECT_TOKEN = yatest.common.get_param('DIRECT_TEST_TOKEN')
    DIRECT_ID = 12345
    DIRECT_NAME = "TestClient"


def init_test_database():
    db.Base.metadata.reflect(bind=db.engine)
    db.Base.metadata.drop_all(bind=db.engine)

    db.init_db()
    db.db_session.add(ClientDBO(TestConfigWithRealDirectClient.DIRECT_ID, TestConfigWithRealDirectClient.DIRECT_NAME,
                                client_login=TestConfigWithRealDirectClient.DIRECT_CLIENT_LOGIN,
                                client_token=TestConfigWithRealDirectClient.DIRECT_TOKEN))

    for region in TEST_REGIONS:
        db.db_session.add(RegionDBO(**region))
    db.db_session.commit()


def remove_test_database():
    db.db_session.remove()
    db.Base.metadata.reflect(bind=db.engine)
    db.Base.metadata.drop_all(bind=db.engine)


class TestImageUpload(unittest.TestCase):
    def setUp(self):
        self.client = make_app(TestConfigWithRealDirectClient).test_client()
        init_test_database()

    def tearDown(self):
        remove_test_database()

    def test_200_on_post(self):
        with open(os.path.join(DATA_PATH, TEST_IMAGE_FILE)) as f:
            res = self.client.post('clients/{}/images'.format(TestConfigWithRealDirectClient.DIRECT_ID),
                                   data={'image': (f, TEST_IMAGE_FILE)})

        self.assertEqual(res.status_code, 200)

    def hash_not_empty_on_post(self):
        with open(os.path.join(os.path.dirname(os.path.realpath(__file__)), TEST_IMAGE_FILE)) as f:
            res = self.client.post('clients/{}/images'.format(TestConfigWithRealDirectClient.DIRECT_ID),
                                   data={'image': (f, TEST_IMAGE_FILE)})

        self.assertTrue(json.loads(res.data)['hash'])

    def test_error_on_bad_image(self):
        res = self.client.post('clients/{}/images'.format(TestConfigWithRealDirectClient.DIRECT_ID),
                               data={'image': (StringIO.StringIO('error image'), TEST_IMAGE_FILE)})

        self.assertEqual(res.status_code, 419)
        self.assertEqual(5004, json.loads(res.data)['error_code'])
        self.assertEqual(u'direct_error', json.loads(res.data)['error_name'])
