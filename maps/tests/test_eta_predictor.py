from requests.exceptions import HTTPError
import json
import responses
import unittest

from maps.analyzer.services.eta_comparison.lib.eta_predictor import DGisEtaPredictor
from maps.analyzer.services.eta_comparison.lib.logger import logger
from yandex.maps.geolib3 import Point2

URL = 'https://catalog.api.2gis.ru/carrouting/6.0.0/global?key=rurbbn3446'


class TestDGisEtaPredictor(unittest.TestCase):
    def setUp(self):
        self.src = Point2(42, 42)
        self.dst = Point2(43, 43)
        self.via = [Point2(42.25, 42.25), Point2(42.75, 42.75)]
        self.resp_body = {"foo": "bar"}
        self.predictor = DGisEtaPredictor({
            'url': 'https://catalog.api.2gis.ru/carrouting/6.0.0/global?key=rurbbn3446',
            'proxy': {}
        }, logger)

    @responses.activate
    def test_dgis_eta_predictor_bad(self):
        responses.add(**{
            'method': responses.POST,
            'url': URL,
            'body': json.dumps(self.resp_body),
            'status': 404,
            'content_type': 'application/json',
        })

        self.assertRaises(HTTPError, lambda: self.predictor.predict(self.src, self.dst, self.via))

    @responses.activate
    def test_dgis_eta_predictor_good(self):
        def callback(request):
            payload = json.loads(request.body)

            self.assertEqual(payload['points'][0]['x'], self.src.lon)
            self.assertEqual(payload['points'][0]['y'], self.src.lat)

            for i in range(len(self.via)):
                self.assertEqual(payload['points'][1 + i]['x'], self.via[i].lon)
                self.assertEqual(payload['points'][1 + i]['y'], self.via[i].lat)

            self.assertEqual(payload['points'][-1]['x'], self.dst.lon)
            self.assertEqual(payload['points'][-1]['y'], self.dst.lat)

            headers = {'content_type': 'application/json'}
            return 200, headers, json.dumps(self.resp_body)

        responses.add_callback(
            responses.POST,
            URL,
            callback=callback,
            content_type='application/json'
        )
        responses.add(**{
            'method': responses.POST,
            'url': URL,
            'body': json.dumps(self.resp_body),
            'status': 200,
            'content_type': 'application/json',
        })

        self.assertEqual(
            json.loads(self.predictor.predict(self.src, self.dst, self.via)),
            self.resp_body
        )
