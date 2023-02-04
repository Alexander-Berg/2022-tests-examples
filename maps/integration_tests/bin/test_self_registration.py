import logging
import unittest
from .common import BACKEND_AUTH_TOKEN_UNREGISTERED, backend_request, create_route_with_order


def _get_company_id(name):
    for company in backend_request("get", "companies").json():
        if company['name'] == name:
            return company['id']
    return None


def _cleanup_company(company_id):
    if not company_id:
        return

    for item_name in ['order', 'route', 'courier', 'depot', 'user']:
        items_path = f"companies/{company_id}/{item_name}s"
        items_result = backend_request('get', items_path)
        if items_result.ok:
            for item in items_result.json():
                item_id = item['id']
                logging.info(f"Clean-up: deleting {item_name} {item_id}")
                backend_request("delete", f"{items_path}/{item_id}")

    logging.info(f"Clean-up: deleting company id={company_id}")
    return backend_request("delete", f"companies/{company_id}")


class CreateCompanySelfRegistrationTest(unittest.TestCase):

    TEST_COMPANY_NAME = 'TEST_integration_company-self-registration'

    def setUp(self):
        company_id = _get_company_id(CreateCompanySelfRegistrationTest.TEST_COMPANY_NAME)
        if company_id:
            assert _cleanup_company(company_id).ok

        self.new_company_id = None
        logging.info("SetUp completed.")

    def tearDown(self):
        _cleanup_company(self.new_company_id)
        logging.info("TearDown completed.")

    def test_self_registration(self):
        new_company_data = {
            'name': CreateCompanySelfRegistrationTest.TEST_COMPANY_NAME,
            'logo_url': 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
            'bg_color': 'black',
            'sms_enabled': False,
        }

        self_registration_manager_info_data = {
            'manager_name': 'ITEST_name',
            'manager_position': 'ITEST_position',
            'manager_email': 'robot-b2bgeo-test-unregistered@ya.ru',
            'manager_phone': 'Dummy phone',
            'vehicle_park_size': '15 - 100500',
        }
        company_data = {**new_company_data, **self_registration_manager_info_data}

        assert 'apikey' not in company_data  # important for this testcase!
        company = backend_request(
            'post',
            "create-company?utm_content=ITEST_UTM_content&utm_source=ITEST_UTM_source",
            json=company_data,
            auth=BACKEND_AUTH_TOKEN_UNREGISTERED,
        ).json()
        logging.info(f"Creating company result: {company}")
        self.assertIn('id', company)
        self.new_company_id = company['id']
        self.assertIn('apikey', company)
        self.assertEqual(company['services'], [{'name': service, 'enabled': True} for service in ['courier', 'mvrp']])

        # check that the user is able to create a route with orders, i.e. their apikey is active.
        created_entity_ids = create_route_with_order(
            test_prefix="test_self_registration", company_id=self.new_company_id, auth=BACKEND_AUTH_TOKEN_UNREGISTERED
        )
        self.assertIn('order_id', created_entity_ids)
