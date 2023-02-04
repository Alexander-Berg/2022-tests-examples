import base64

import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.steps import simple_steps as simple

'''
https://st.yandex-team.ru/TRUST-1556
'''

pytestmark = marks.simple_internal_logic


class PartnerException(Exception):
    pass


@reporter.feature(features.Methods.GetServiceProductPublicKey)
class TestGetServiceProductPublicKey(object):
    """
    https://st.yandex-team.ru/TRUST-1556
    """

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize("service", [Services.SHAD,
                                         Services.DISK,
                                         Services.YAC,
                                         Services.REALTYPAY],
                             ids=DataObject.ids_service)
    def test_get_public_key_from_product_with_partner(self, service):
        service_product = simple.get_service_product_id()
        simple.create_service_product(service,
                                      service_product)

        with check_mode(CheckMode.FAILED):
            simple.get_service_product_public_key(service, service_product)

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize("service", [Services.STORE,
                                         # Services.PARKOVKI,
                                         Services.TICKETS,
                                         Services.MARKETPLACE,
                                         Services.TAXI,
                                         Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_get_public_key_from_product_without_partner(self, service):
        service_product = simple.get_service_product_id()

        _, partner = simple.create_partner(service)
        simple.create_service_product(service,
                                      service_product,
                                      partner_id=partner)

        with check_mode(CheckMode.FAILED):
            simple.get_service_product_public_key(service, service_product)


@reporter.feature(features.Methods.SignServiceProductMessage)
class TestSignServiceProductMessage(object):
    """
    https://st.yandex-team.ru/TRUST-1610
    """
    test_string = 'it is so test string as it can be'

    @reporter.story(stories.Methods.Call)
    def test_method_with_message(self):
        service_product = simple.get_service_product_id()

        _, partner = simple.create_partner(Services.STORE)

        simple.create_service_product(Services.STORE,
                                      service_product,
                                      partner_id=partner)
        simple.sign_service_product_message(Services.STORE, service_product, message=self.test_string)

    @reporter.story(stories.Methods.Call)
    def test_method_with_binary_message(self):
        service_product = simple.get_service_product_id()

        _, partner = simple.create_partner(Services.STORE)

        simple.create_service_product(Services.STORE,
                                      service_product,
                                      partner_id=partner)
        bin_test_string = base64.b64encode(self.test_string)
        simple.sign_service_product_message(Services.STORE, service_product, binary_message=bin_test_string)
