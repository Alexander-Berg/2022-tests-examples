import pytest
from multidict import CIMultiDict

from sendr_tvm.common.exceptions import TicketParsingException
from sendr_tvm.qloud_async_tvm import ServiceTicket, TvmChecker, UserTicket

from hamcrest import assert_that, equal_to, is_

from pay.lib.tvm.acl_matcher import TvmAclMatcher
from pay.lib.tvm.exceptions import (
    TVMServiceTicketInvalidException,
    TVMServiceTicketNotAllowedException,
    TVMServiceTicketNotPassedException,
    TVMUserTicketInvalidException,
    TVMUserTicketNotPassedException,
)
from pay.lib.tvm.ticket_checker import TvmTicketChecker


class TestServiceTicket:
    @pytest.fixture
    def route_name(self):
        return 'the-route'

    @pytest.fixture
    def route_acls(self, route_name):
        return {route_name: ['acl-1', 'acl-2']}

    @pytest.fixture
    def allowed_src(self):
        return (
            123,
            (456, 'acl-1'),
            (789, ['acl-2', 'acl-3']),
        )

    @pytest.fixture
    def checker(self, allowed_src, route_acls):
        return TvmTicketChecker(
            'test-client',
            'http://tvm-check-srv:8080',
            'test-token',
            allowed_src,
            route_acls,
        )

    @pytest.mark.asyncio
    async def test_no_ticket(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': None}))

        with pytest.raises(TVMServiceTicketNotPassedException):
            await checker.check_tvm_service_ticket(request)

    @pytest.mark.asyncio
    async def test_ticket_not_allowed(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': 'ticket'}))
        mocker.patch.object(
            TvmChecker,
            'parse_service_ticket',
            mocker.AsyncMock(return_value=mocker.Mock(src=13337)),
        )

        with pytest.raises(TVMServiceTicketNotAllowedException):
            await checker.check_tvm_service_ticket(request)

    @pytest.mark.asyncio
    async def test_route_acl_not_matched(self, mocker, checker, route_name):
        spy = mocker.spy(TvmAclMatcher, 'match')

        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': 'ticket'}))
        request.match_info.route.name = route_name
        mocker.patch.object(
            TvmChecker,
            'parse_service_ticket',
            mocker.AsyncMock(return_value=mocker.Mock(src=123)),
        )

        with pytest.raises(TVMServiceTicketNotAllowedException):
            await checker.check_tvm_service_ticket(request)

        assert_that(spy.call_count, equal_to(3))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('tvm_id', [456, 789])
    async def test_route_acl_is_matched(self, mocker, checker, route_name, tvm_id):
        spy = mocker.spy(TvmAclMatcher, 'match')

        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': 'ticket'}))
        request.match_info.route.name = route_name
        mocker.patch.object(
            TvmChecker,
            'parse_service_ticket',
            mocker.AsyncMock(return_value=mocker.Mock(src=tvm_id)),
        )

        await checker.check_tvm_service_ticket(request)

        spy.assert_called_with(
            mocker.ANY,  # self
            tvm_id,
            route_name,
        )
        assert_that(spy.spy_return, is_(True))

    @pytest.mark.asyncio
    async def test_calls_tvm_service_checker(self, mocker, checker, route_name):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': 'the-ticket'}))
        request.match_info.route.name = route_name
        tvm_checker_mock = mocker.patch.object(
            TvmChecker,
            'parse_service_ticket',
            mocker.AsyncMock(return_value=ServiceTicket(456)),
        )

        await checker.check_tvm_service_ticket(request)

        tvm_checker_mock.assert_called_once_with('the-ticket')

    @pytest.mark.asyncio
    async def test_raises_core_error_on_invalid_tvm_ticket(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-service-ticket': 'the-ticket'}))
        mocker.patch.object(
            TvmChecker,
            'parse_service_ticket',
            mocker.AsyncMock(side_effect=TicketParsingException('a', 'b', 'c')),
        )

        with pytest.raises(TVMServiceTicketInvalidException):
            await checker.check_tvm_service_ticket(request)


class TestUserTicket:
    @pytest.fixture
    def checker(self):
        return TvmTicketChecker(
            'test-client',
            'http://tvm-check-srv:8080',
            'test-token',
            (),
            dict(),
        )

    @pytest.mark.asyncio
    async def test_no_ticket(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-user-ticket': None}))

        with pytest.raises(TVMUserTicketNotPassedException):
            await checker.get_tvm_user_ticket(request)

    @pytest.mark.asyncio
    async def test_calls_tvm_service_checker(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-user-ticket': 'the-ticket'}))
        tvm_checker_mock = mocker.patch.object(
            TvmChecker,
            'parse_user_ticket',
            mocker.AsyncMock(return_value=UserTicket(456)),
        )

        ticket = await checker.get_tvm_user_ticket(request)

        assert_that(ticket, equal_to(UserTicket(456)))
        tvm_checker_mock.assert_called_once_with('the-ticket')

    @pytest.mark.asyncio
    async def test_raises_core_error_on_invalid_user_ticket(self, mocker, checker):
        request = mocker.Mock(headers=CIMultiDict({'x-ya-user-ticket': 'the-ticket'}))
        mocker.patch.object(
            TvmChecker,
            'parse_user_ticket',
            mocker.AsyncMock(side_effect=TicketParsingException('a', 'b', 'c')),
        )

        with pytest.raises(TVMUserTicketInvalidException):
            await checker.get_tvm_user_ticket(request)
