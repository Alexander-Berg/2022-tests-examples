
from mock import patch
from pretend import stub

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PaginationTestCase(BaseApiTestCase):
    def test_has_next(self):
        from wiki.api_core.pagination_utils import StreamPaginator

        with patch(
            'wiki.api_core.pagination_utils.Stream.get_qs', lambda *args: [stub(id=1), stub(id=2), stub(id=1100)]
        ):
            paginator = StreamPaginator(None)
            stream = paginator.paginate(2)
            self.assertEqual(2, stream.next_start_id)

    def test_has_no_next(self):
        from wiki.api_core.pagination_utils import StreamPaginator

        with patch('wiki.api_core.pagination_utils.Stream.get_qs', lambda *args: [stub(id=1), stub(id=2)]):
            paginator = StreamPaginator(None)
            stream = paginator.paginate(2)
            self.assertEqual(None, stream.next_start_id)

        with patch('wiki.api_core.pagination_utils.Stream.get_qs', lambda *args: []):
            paginator = StreamPaginator(None)
            stream = paginator.paginate(2)
            self.assertEqual(None, stream.next_start_id)

    def test_can_be_iterated_over_empty_list(self):
        from wiki.api_core.pagination_utils import StreamPaginator

        with patch('wiki.api_core.pagination_utils.Stream.get_qs', lambda *args: []):
            paginator = StreamPaginator(None)
            stream = paginator.paginate(2)
            self.assertEqual([], list(stream))

    def test_can_be_iterated(self):
        from wiki.api_core.pagination_utils import StreamPaginator

        obj = stub(id=100)
        with patch('wiki.api_core.pagination_utils.Stream.get_qs', lambda *args: [obj]):
            paginator = StreamPaginator(None)
            stream = paginator.paginate(2)
            self.assertEqual([obj], list(stream))
