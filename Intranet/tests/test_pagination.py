from builtins import object

from mock.mock import MagicMock, call

from kelvin.common.pagination import PaginationWithPageSize


class TestPaginationWithPageSize(object):
    """
    Тесты кастомной пагинации
    """

    def test_get_next_link(self, mocker):
        """
        Тест получения номера следующей страницы
        """
        paginator = PaginationWithPageSize()
        page = MagicMock()
        paginator.page = page

        # нет следующей страницы
        page.has_next.return_value = False
        assert paginator.get_next_link() is None
        assert page.mock_calls == [call.has_next()]

        # есть следующая страница
        page.reset_mock()
        page.has_next.return_value = True
        page.next_page_number.return_value = 10
        assert paginator.get_next_link() == 10
        assert page.mock_calls == [call.has_next(), call.next_page_number()]

    def test_get_previous_link(self, mocker):
        """
        Тест получения номера следующей страницы
        """
        paginator = PaginationWithPageSize()
        page = MagicMock()
        paginator.page = page

        # нет следующей страницы
        page.has_previous.return_value = False
        assert paginator.get_previous_link() is None
        assert page.mock_calls == [call.has_previous()]

        # есть следующая страница
        page.reset_mock()
        page.has_previous.return_value = True
        page.previous_page_number.return_value = 10
        assert paginator.get_previous_link() == 10
        assert page.mock_calls == [call.has_previous(),
                                   call.previous_page_number()]
