
from pretend import stub

from wiki.utils.logic import missing_primary_keys
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class MissingPrimaryKeys(BaseApiTestCase):
    def test_logic(self):
        query_set = [stub(pk=1), stub(pk=2)]
        self.assertEqual(missing_primary_keys(query_set, [1, 2, 3]), {3})
        self.assertEqual(missing_primary_keys(query_set, [1, 2]), set([]))
