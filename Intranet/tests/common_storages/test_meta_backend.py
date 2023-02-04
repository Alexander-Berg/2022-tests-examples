# -*- coding: utf-8 -*-
from django.test import TestCase

from events.common_storages.factories import ProxyStorageModelFactory
from events.common_storages.models import ProxyStorageModel
from events.surveyme.factories import SurveyFactory


class TestGetBySha256(TestCase):
    def test_get_by_sha256(self):
        sha256 = '123456'
        meta_data = ProxyStorageModelFactory(path='/100/readme.txt', sha256=sha256)

        result = ProxyStorageModel.objects.get_by_sha256(sha256)
        self.assertEqual(result.pk, meta_data.pk)

    def test_get_by_sha256_last(self):
        sha256 = '123456'
        meta_data_list = [
            ProxyStorageModelFactory(path='/101/readme.txt', sha256=sha256),
            ProxyStorageModelFactory(path='/102/readme.txt', sha256=sha256),
            ProxyStorageModelFactory(path='/103/readme.txt', sha256=sha256),
        ]

        result = ProxyStorageModel.objects.get_by_sha256(sha256)
        self.assertEqual(result.pk, meta_data_list[-1].pk)

    def test_get_by_sha256_with_survey_id(self):
        surveys = [
            SurveyFactory(), SurveyFactory(),
        ]
        sha256 = '123456'
        meta_data_list = [
            ProxyStorageModelFactory(path='/101/readme.txt', sha256=sha256, survey=surveys[1]),
            ProxyStorageModelFactory(path='/102/readme.txt', sha256=sha256, survey=surveys[0]),
            ProxyStorageModelFactory(path='/103/readme.txt', sha256=sha256, survey=surveys[1]),
        ]
        result = ProxyStorageModel.objects.get_by_sha256(sha256, surveys[0].pk)
        self.assertEqual(result.pk, meta_data_list[1].pk)

        result = ProxyStorageModel.objects.get_by_sha256(sha256, surveys[1].pk)
        self.assertEqual(result.pk, meta_data_list[2].pk)
