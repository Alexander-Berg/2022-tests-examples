# -*- coding: utf-8 -*-
from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.auth.decorators import requires


class Test__requires(TestCase):
    def test_requires_should_set_attributes_to_decorated_function(self):
        for value in [True, False]:
            @requires(org_id=value, user=value)
            def func():
                pass

            self.assertEqual(func.requires_org_id, value)
            self.assertEqual(func.requires_user, value)
