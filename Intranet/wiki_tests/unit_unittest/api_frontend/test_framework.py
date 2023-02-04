
from unittest import skip

import mock
from pretend import stub

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class ReadonlyFrameworkTest(BaseApiTestCase):
    def test_storage_error_on_page_save(self):
        from wiki.api_core.errors.read_only import ReadonlyError
        from wiki.api_core.read_only_utils import raise_503_on_readonly
        from wiki.utils.backports.mds_compat import APIError

        @raise_503_on_readonly
        def raise_save_error(*args, **kwargs):
            raise APIError()

        self.assertRaises(ReadonlyError, raise_save_error)

    @skip('temporary disabled after py3 migration, fix ASAP')
    def test_master_is_readonly_WTF_on_page_save(self):
        from django.db import DatabaseError

        from wiki.api_core.errors.read_only import ReadonlyError
        from wiki.api_core.read_only_utils import raise_503_on_readonly

        @raise_503_on_readonly
        def do_nothing():
            error = DatabaseError()
            error.__cause__ = stub(pgcode=25006)

            raise error

        self.assertRaises(ReadonlyError, do_nothing)

    def test_read_only_protected_decorator(self):
        from wiki.api_core.errors.read_only import ReadonlyError
        from wiki.api_core.read_only_utils import raise_503_on_readonly

        with mock.patch('wiki.api_core.read_only_utils.service_is_readonly', lambda: True):
            with mock.patch('django_replicated.utils.routers.state', lambda: 'master'):

                @raise_503_on_readonly
                def do_nothing():
                    return False

                self.assertRaises(ReadonlyError, do_nothing)

            with mock.patch('django_replicated.utils.routers.state', lambda: 'slave'):

                @raise_503_on_readonly
                def do_nothing():
                    return False

                self.assertNotRaises(ReadonlyError, do_nothing)
