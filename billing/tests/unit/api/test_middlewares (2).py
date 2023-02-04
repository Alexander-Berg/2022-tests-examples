import pytest

from sendr_tvm.qloud_async_tvm import TicketCheckResult

from billing.yandex_pay_plus.yandex_pay_plus.api.middlewares import tvm_check_func
from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication


class TestTVMRestrictor:
    class RequestMock(dict):
        @property
        def headers(self):
            return self.get('headers', {})

        @property
        def match_info(self):
            return self

        @property
        def route(self):
            return self

        @property
        def name(self):
            return self.get('route_name', 'order')

    @pytest.fixture
    def request_mock(self):
        return self.RequestMock()

    @pytest.fixture(autouse=True)
    def setup_allowed_clients(self, yandex_pay_plus_settings):
        yandex_pay_plus_settings.TVM_ALLOWED_SRC = (224,)

    def test_result_not_valid(self, request_mock):
        assert not tvm_check_func(request_mock, TicketCheckResult(None, None))

    def test_success(self, request_mock, yandex_pay_plus_settings):
        check_result = TicketCheckResult({'src': 224}, {'default_uid': 500})

        assert tvm_check_func(request_mock, check_result)

    def test_set_tvm_to_request(self, request_mock):
        check_result = TicketCheckResult({'src': 224}, {'default_uid': 500})

        tvm_check_func(request_mock, check_result)

        assert request_mock['tvm'] == check_result

    def test_not_allowed_src(self, request_mock, yandex_pay_plus_settings):
        yandex_pay_plus_settings.TVM_ALLOWED_SRC = (442,)
        check_result = TicketCheckResult({'src': 224}, {'default_uid': 500})

        assert not tvm_check_func(request_mock, check_result)

    def test_apply_debug_uid(self, request_mock, yandex_pay_plus_settings):
        check_result = TicketCheckResult({'src': 224}, {'default_uid': 500})
        request_mock['headers'] = {'X-Ya-User-Ticket-Debug': '100'}

        tvm_check_func(request_mock, check_result)

        assert request_mock['tvm'].default_uid == 100

    def test_tvm_path_open(self, request_mock):
        request_mock['route_name'] = 'unistat'
        assert tvm_check_func(request_mock, TicketCheckResult(None, None))


def test_has_csrf_middleware():
    assert "csrf_middleware" in list(map(lambda x: x.__name__, YandexPayPlusPublicApplication._middlewares))
