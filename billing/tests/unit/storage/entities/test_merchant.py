from sendr_utils import json_value

from hamcrest import all_of, assert_that, contains, contains_string, equal_to, not_

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import YandexDeliveryParams


class TestYandexDeliveryParams:
    def test_does_not_expose_secrets(self):
        yd_params = YandexDeliveryParams(
            oauth_token='token',
        )

        assert_that(
            [str(yd_params), repr(yd_params), json_value(yd_params)],
            contains(
                *(
                    [
                        all_of(
                            not_(contains_string('token')),
                            not_(contains_string('oauth_token')),
                        )
                    ]
                    * 3
                ),
            ),
        )

    def test_encrypt_decrypt_oauth_token(self):
        encrypt_decrypt = YandexDeliveryParams(
            oauth_token=YandexDeliveryParams.encrypt_oauth_token('spicy')
        ).get_oauth_token()

        assert_that(
            encrypt_decrypt,
            equal_to('spicy'),
        )
