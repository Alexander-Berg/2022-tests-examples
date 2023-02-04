import unittest
import requests
from .common import BACKEND_API_ENDPOINT, BACKEND_AUTH_TOKEN, create_route_with_order, DEFAULT_COMPANY_ID


class RoutedOrdersUnprocessableTest(unittest.TestCase):
    def test_routed_orders_unprocessable(self):
        entities = create_route_with_order("test_routed_orders_unprocessable", company_id=DEFAULT_COMPANY_ID)

        url = "{}/couriers/{}/routes/{}/routed-orders?lat={}&lon={}&time_now={}".format(
            BACKEND_API_ENDPOINT, entities['courier_id'], entities['route_id'], 65.925475, 78.192675, '0.19:15:29'
        )
        headers = {"Authorization": f"OAuth {BACKEND_AUTH_TOKEN}"}
        response = requests.get(url, headers=headers)

        self.assertEqual(response.status_code, requests.codes.unprocessable)
        self.assertTrue(response.json()['message'].find("Planning cannot be performed for 55% of the routes") >= 0)
