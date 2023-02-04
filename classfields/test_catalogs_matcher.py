import re
import json
import requests
import requests_mock
from django.apps import apps
from django.test import TestCase
from common.s3_controller import S3Controller
from catalogs.models import VerbaMatch, CategoryMatch
from catalogs.jobs.st_6276.data_models import MatchParams, CatalogsParams
from catalogs.jobs.st_6276.tasks.cars_match import CatalogsCarsMatcher
from catalogs.jobs.st_6276.tasks.cars_rematch import CatalogsCarsRematcher


@requests_mock.Mocker()
class CatalogsMatcherTest(TestCase):
    TEST_DATA = S3Controller().get_json("tests/catalogs_data.json")
    MOCKS = [
        re.compile(r"catalogs/bmw/cars-parameters"),
    ]
    def setUp(self):
        for obj in self.TEST_DATA["objects"]:
            model = apps.get_model(obj["app"], obj["model"])
            model.objects.bulk_create([model(**data) for data in obj["data"]])

    def set_mocks(self, m: requests_mock.Mocker):
        for row in self.TEST_DATA["mocks"]:
            kwargs = {}
            args = []
            if "args" in row:
                args.append(row["args"])
            if "result" in row:
                kwargs["json"] = row["result"]
            m.get(re.compile(row["url"]), *args, **kwargs)

    def test_good_match(self, m: requests_mock.Mocker):
        self.set_mocks(m)
        params = MatchParams()
        # matcher = CatalogsCarsMatcher(params)
        # matcher.main()
        # print("wow")
