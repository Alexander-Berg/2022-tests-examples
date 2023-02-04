# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
)
from testutils import (
    TestCase,
    assert_not_called,
)
from intranet.yandex_directory.src.yandex_directory import fouras


class TestFouras(TestCase):
    domain_name = 'mydomain.com'
    good_response = {
       'status': 'ok',
       "response": {
          "public_key": "v=DKIM1; k=rsa; t=s; p=...",
          "private_key": "....",
          "domain": "yandex.ru",
          "selector": "mail",
          "enabled": True
       },
    }

    def test_get_domain_info(self):
        self.mocked_fouras_get.return_value = self.good_response
        result = fouras.domain_status(self.domain_name)

        assert_that(
            result,
            equal_to(self.good_response['response'])
        )

    def test_error_in_fouras(self):
        self.mocked_fouras_get.side_effect = fouras.FourasError()
        with self.assertRaises(fouras.FourasError):
            fouras.domain_status(self.domain_name)

    def test_get_or_gen_key_if_error(self):
        self.mocked_fouras_get.side_effect = fouras.FourasNotFoundError()
        self.mocked_fouras_post.return_value = self.good_response
        result = fouras.get_or_gen_domain_key(self.domain_name)
        assert_that(
            result,
            equal_to('v=DKIM1; k=rsa; t=s; p=...')
        )

    def test_get_or_gen_key_no_error(self):
        self.mocked_fouras_get.return_value = self.good_response
        result = fouras.get_or_gen_domain_key(self.domain_name)
        assert_that(
            result,
            equal_to('v=DKIM1; k=rsa; t=s; p=...')
        )
        assert_not_called(self.mocked_fouras_post)
