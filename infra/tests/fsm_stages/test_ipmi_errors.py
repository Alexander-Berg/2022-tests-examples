"""Tests IPMI error handling during host powering on/off."""

from unittest.mock import call

import pytest

import walle.admin_requests.request as admin_requests
import walle.fsm_stages.common as common
from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    mock_commit_stage_changes,
    mock_fail_current_stage,
    mock_stage_internal_error,
    monkeypatch_function,
)
from sepelib.core import config
from walle import authorization
from walle.admin_requests.constants import EineCode
from walle.admin_requests.severity import BotTag
from walle.clients.ipmiproxy import InternalError
from walle.fsm_stages import power_ipmi, ipmi_errors
from walle.models import timestamp
from walle.stages import Stage, Stages

BOT_ID = 99
TICKET = "ITDC"


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture
def create_request(mp):
    return mp.function(admin_requests._create, return_value=admin_requests.Result(bot_id=BOT_ID, ticket=TICKET))


def mock_get_request_status(mp, return_value=None, side_effect=None):
    return monkeypatch_function(
        mp, admin_requests.get_last_request_status, return_value=return_value, side_effect=side_effect
    )


@pytest.mark.parametrize(
    "stage,status",
    [
        (Stages.POWER_OFF, power_ipmi._STATUS_SOFT_POWER_OFF),
        (Stages.POWER_OFF, power_ipmi._STATUS_SOFT_POWERING_OFF),
        (Stages.POWER_OFF, power_ipmi._STATUS_POWER_OFF),
        (Stages.POWER_OFF, power_ipmi._STATUS_POWERING_OFF),
        (Stages.POWER_ON, power_ipmi._STATUS_POWER_ON),
        (Stages.POWER_ON, power_ipmi._STATUS_POWERING_ON),
    ],
)
def test_internal_hardware_error(test, mp, stage, status):
    host = test.mock_host({"task": mock_task(stage=Stage(name=stage, status=status))})
    clients = monkeypatch_clients_for_host(mp, host, mock_internal_hw_error=True)

    handle_host(host, suppress_internal_errors=(InternalError,))

    assert clients.mock_calls == [call.hardware.is_power_on()]
    mock_stage_internal_error(host, "Internal hardware error mock.")

    test.hosts.assert_equal()


class TestMissingIpmiHost:
    @pytest.fixture(
        autouse=True,
        params=[
            (Stages.POWER_OFF, power_ipmi._STATUS_SOFT_POWERING_OFF),
            (Stages.POWER_OFF, power_ipmi._STATUS_POWERING_OFF),
            (Stages.POWER_ON, power_ipmi._STATUS_POWERING_ON),
        ],
    )
    def init(self, test, mp, request):
        stage, status = request.param
        self.test = test
        self.host = test.mock_host(
            {
                "task": mock_task(
                    hardware_error_count=0,
                    power_error_count=0,  # To ensure that it will be deleted
                    stage=Stage(name=stage, status=status),
                )
            }
        )
        self.clients = monkeypatch_clients_for_host(mp, self.host, mock_hw_lookup_error=True)

    def test_new(self, create_request, mp, test):
        host = self.host
        get_status = mock_get_request_status(mp, return_value=None)

        handle_host(host)

        error = "IPMI host {} is missing.".format(test.ipmi_host)
        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
        ]
        assert create_request.mock_calls == [
            call(
                authorization.ISSUER_WALLE,
                admin_requests.RequestTypes.IPMI_HOST_MISSING,
                host,
                reason=error,
                request_id_extra_parts=(),
                shelf_inv=None,
                params={"eine_code": EineCode.IPMI_DNS_RESOLUTION_FAILED},
                severity_tag=BotTag.MEDIUM,
            )
        ]
        mock_commit_stage_changes(host, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL, error=error)

        test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process(self, create_request, mp, test, in_process_request_type):
        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS}

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls[-1] == call(in_process_request_type, host.inv)
        assert create_request.call_count == 0
        mock_commit_stage_changes(
            host,
            check_after=common.ADMIN_REQUEST_CHECK_INTERVAL,
            error="IPMI host ipmi.host.mock.yandex.net is missing. "
            "There is an active admin request for broken IPMI. Waiting when it will be processed.",
        )

        test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process_save_ticket(self, create_request, mp, test, in_process_request_type):
        ticket_key = "BURNE-10001"

        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS, "ticket": ticket_key}

        mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        host.ticket = ticket_key
        common.get_current_stage(host).set_data("tickets", [ticket_key])
        mock_commit_stage_changes(
            host,
            inc_revision=1,
            check_after=common.ADMIN_REQUEST_CHECK_INTERVAL,
            error="IPMI host ipmi.host.mock.yandex.net is missing. "
            "There is an active admin request for broken IPMI. Waiting when it will be processed.",
        )

        test.hosts.assert_equal()

    @pytest.mark.parametrize("max_reopened_admin_requests", (0, 1))
    @pytest.mark.parametrize(
        "request_status",
        [admin_requests.STATUS_PROCESSED, admin_requests.STATUS_DELETED, admin_requests.STATUS_NOT_EXIST],
    )
    def test_processed(self, create_request, test, mp, request_status, max_reopened_admin_requests):
        mp.setitem(config.get_value("hardware"), "max_reopened_admin_requests", max_reopened_admin_requests)

        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_HOST_MISSING:
                response = {"status": request_status}
                if request_status == admin_requests.STATUS_PROCESSED:
                    response.update(close_time=timestamp() - ipmi_errors.HOST_RECOVERY_TIMEOUT)
                if request_status == admin_requests.STATUS_NOT_EXIST:
                    response["create_time"] = timestamp() - ipmi_errors.LOST_REQUEST_TIMEOUT
                return response

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
        ]

        error = (
            "IPMI host {} is missing. There is a processed admin request for missing IPMI host"
            " but the problem persists.".format(test.ipmi_host)
        )
        if max_reopened_admin_requests:
            create_request.assert_called_once_with(
                authorization.ISSUER_WALLE,
                admin_requests.RequestTypes.IPMI_HOST_MISSING,
                host,
                reason=error,
                request_id_extra_parts=(),
                shelf_inv=None,
                params={"eine_code": EineCode.IPMI_DNS_RESOLUTION_FAILED},
                severity_tag=BotTag.MEDIUM,
            )
            host.task.reopened_admin_request_count = 1
            mock_commit_stage_changes(host, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL, error=error)
        else:
            assert create_request.call_count == 0
            mock_fail_current_stage(host, reason=error)

        test.hosts.assert_equal()

    def test_processed_just_now(self, create_request, mp):
        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_HOST_MISSING:
                return {
                    "status": admin_requests.STATUS_PROCESSED,
                    "close_time": timestamp(),
                    "create_time": timestamp(),
                }

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == [call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv)]
        assert create_request.call_count == 0

        mock_commit_stage_changes(
            host,
            check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL,
            error="IPMI host ipmi.host.mock.yandex.net is missing.",
        )
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("per_task", (True, False))
    def test_disabled_admin_requests(self, create_request, mp, test, per_task):
        get_status = mock_get_request_status(mp)

        host = self.host
        if per_task:
            host.task.disable_admin_requests = True
            host.save()
        else:
            mp.setitem(config.get_value("hardware"), "enable_admin_requests", False)

        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.call_count == 0
        assert create_request.call_count == 0
        mock_fail_current_stage(host, reason="IPMI host ipmi.host.mock.yandex.net is missing.")

        test.hosts.assert_equal()


class TestUnreachableIpmi:
    @pytest.fixture(
        autouse=True,
        params=[
            (Stages.POWER_OFF, power_ipmi._STATUS_SOFT_POWERING_OFF),
            (Stages.POWER_OFF, power_ipmi._STATUS_POWERING_OFF),
            (Stages.POWER_ON, power_ipmi._STATUS_POWERING_ON),
        ],
    )
    def init(self, test, mp, request):
        stage, status = request.param
        self.test = test
        self.host = test.mock_host(
            {
                "task": mock_task(
                    power_error_count=0, stage=Stage(name=stage, status=status)  # To ensure that it will be deleted
                )
            }
        )
        self.clients = monkeypatch_clients_for_host(mp, self.host, hw_available=False)

    def test_new(self, create_request, mp, exceed_max_error_count):
        host = self.host
        get_status = mock_get_request_status(mp, return_value=None)

        mp.setitem(
            config.get_value("hardware"),
            "max_ipmi_errors",
            host.task.hardware_error_count if exceed_max_error_count else host.task.hardware_error_count + 1,
        )

        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        expected_get_status_calls = [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]

        error = "Hardware error mock."
        if exceed_max_error_count:
            expected_get_status_calls.append(call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv))
            create_request.assert_called_once_with(
                authorization.ISSUER_WALLE,
                admin_requests.RequestTypes.IPMI_UNREACHABLE,
                host,
                reason=error,
                request_id_extra_parts=(),
                shelf_inv=None,
                params={"eine_code": EineCode.IPMI_PROTO_ERROR},
                severity_tag=BotTag.MEDIUM,
            )
            del host.task.hardware_error_count
            del host.task.power_error_count
            mock_commit_stage_changes(host, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL, error=error)
        else:
            assert create_request.call_count == 0
            host.task.hardware_error_count += 1
            mock_commit_stage_changes(host, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL, error=error)

        assert get_status.mock_calls == expected_get_status_calls
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process(self, mp, create_request, in_process_request_type):
        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS}

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls[-1] == call(in_process_request_type, host.inv)
        assert create_request.call_count == 0

        del host.task.hardware_error_count
        del host.task.power_error_count
        mock_commit_stage_changes(
            host,
            check_after=common.ADMIN_REQUEST_CHECK_INTERVAL,
            error="Hardware error mock. There is an active admin request for broken IPMI."
            " Waiting when it will be processed.",
        )

        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process_save_ticket(self, mp, create_request, in_process_request_type):
        ticket_key = "ITDC-00001"

        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS, "ticket": ticket_key}

        mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        del host.task.hardware_error_count
        del host.task.power_error_count
        host.ticket = ticket_key
        common.get_current_stage(host).set_data("tickets", [ticket_key])
        mock_commit_stage_changes(
            host,
            inc_revision=1,
            check_after=common.ADMIN_REQUEST_CHECK_INTERVAL,
            error="Hardware error mock. There is an active admin request for broken IPMI."
            " Waiting when it will be processed.",
        )

        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("max_reopened_admin_requests", (0, 1))
    @pytest.mark.parametrize(
        "request_status",
        [admin_requests.STATUS_PROCESSED, admin_requests.STATUS_NOT_EXIST, admin_requests.STATUS_DELETED],
    )
    def test_processed(self, create_request, mp, request_status, exceed_max_error_count, max_reopened_admin_requests):

        host = self.host
        mp.setitem(
            config.get_value("hardware"),
            "max_ipmi_errors",
            host.task.hardware_error_count if exceed_max_error_count else host.task.hardware_error_count + 1,
        )
        mp.setitem(config.get_value("hardware"), "max_reopened_admin_requests", max_reopened_admin_requests)

        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_UNREACHABLE:
                response = {"status": request_status}
                if request_status == admin_requests.STATUS_PROCESSED:
                    response.update(close_time=timestamp() - ipmi_errors.HOST_RECOVERY_TIMEOUT)
                if request_status == admin_requests.STATUS_NOT_EXIST:
                    response["create_time"] = timestamp() - ipmi_errors.LOST_REQUEST_TIMEOUT
                return response

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        expected_get_status_calls = [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]

        if exceed_max_error_count:
            expected_get_status_calls.append(call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv))

            error = (
                "Hardware error mock. There is a processed admin request for unreachable IPMI"
                " but the problem persists."
            )
            if max_reopened_admin_requests:
                create_request.assert_called_once_with(
                    authorization.ISSUER_WALLE,
                    admin_requests.RequestTypes.IPMI_UNREACHABLE,
                    host,
                    reason=error,
                    request_id_extra_parts=(),
                    shelf_inv=None,
                    params={"eine_code": EineCode.IPMI_PROTO_ERROR},
                    severity_tag=BotTag.MEDIUM,
                )
                host.task.reopened_admin_request_count = 1
                mock_commit_stage_changes(host, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL, error=error)
            else:
                assert create_request.call_count == 0
                mock_fail_current_stage(host, reason=error)
        else:
            assert create_request.call_count == 0
            host.task.hardware_error_count += 1
            mock_commit_stage_changes(
                host, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL, error="Hardware error mock."
            )

        assert get_status.mock_calls == expected_get_status_calls
        self.test.hosts.assert_equal()

    def test_processed_just_now(self, mp, create_request):
        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_UNREACHABLE:
                return {
                    "status": admin_requests.STATUS_PROCESSED,
                    "close_time": timestamp(),
                    "create_time": timestamp(),
                }

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]
        assert create_request.call_count == 0

        mock_commit_stage_changes(
            host, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL, error="Hardware error mock."
        )
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("per_task", (True, False))
    def test_disabled_admin_requests(self, create_request, mp, per_task, exceed_max_error_count):
        get_status = mock_get_request_status(mp)
        host = self.host

        if per_task:
            host.task.disable_admin_requests = True
            host.save()
        else:
            mp.setitem(config.get_value("hardware"), "enable_admin_requests", False)

        mp.setitem(
            config.get_value("hardware"),
            "max_ipmi_errors",
            host.task.hardware_error_count if exceed_max_error_count else host.task.hardware_error_count + 1,
        )

        handle_host(host)

        if exceed_max_error_count:
            mock_fail_current_stage(host, reason="Hardware error mock.")
        else:
            host.task.hardware_error_count += 1
            mock_commit_stage_changes(
                host, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL, error="Hardware error mock."
            )

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == []
        assert create_request.call_count == 0

        self.test.hosts.assert_equal()


class TestPowerOnOfTimeout:
    @pytest.fixture(
        autouse=True,
        params=[
            (Stages.POWER_OFF, power_ipmi._STATUS_POWERING_OFF, power_ipmi._STATUS_POWER_OFF),
            (Stages.POWER_ON, power_ipmi._STATUS_POWERING_ON, power_ipmi._STATUS_POWER_ON),
        ],
    )
    def init(self, request, mp, test):
        self.test = test
        self.stage, self.from_status, self.to_status = request.param
        self.action = "on" if self.from_status == power_ipmi._STATUS_POWERING_ON else "off"
        self.host = test.mock_host(
            {
                "task": mock_task(
                    hardware_error_count=0,  # To ensure that it will be deleted
                    stage=Stage(
                        name=self.stage,
                        status=self.from_status,
                        status_time=timestamp() - power_ipmi._POWER_ON_OF_TIMEOUT,
                    ),
                )
            }
        )
        self.clients = monkeypatch_clients_for_host(
            mp, self.host, power_on=self.from_status == power_ipmi._STATUS_POWERING_OFF
        )

    def test_new(self, create_request, mp, exceed_max_error_count):
        host = self.host
        get_status = mock_get_request_status(mp, return_value=None)

        mp.setitem(
            config.get_value("hardware"),
            "max_power_errors",
            host.task.power_error_count if exceed_max_error_count else host.task.power_error_count + 1,
        )

        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        expected_get_status_calls = [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]

        error = "Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout.".format(
            self.action
        )

        if exceed_max_error_count:
            expected_get_status_calls.append(call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv))
            create_request.assert_called_once_with(
                authorization.ISSUER_WALLE,
                admin_requests.RequestTypes.IPMI_UNREACHABLE,
                host,
                reason=error,
                request_id_extra_parts=(),
                shelf_inv=None,
                params={"eine_code": EineCode.IPMI_PROTO_ERROR},
                severity_tag=BotTag.MEDIUM,
            )
            del host.task.hardware_error_count
            del host.task.power_error_count
            mock_commit_stage_changes(
                host, status=self.to_status, error=error, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL
            )
        else:
            host.task.power_error_count += 1
            assert create_request.call_count == 0
            mock_commit_stage_changes(
                host, status=self.to_status, error=error, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL
            )

        assert get_status.mock_calls == expected_get_status_calls
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process(self, mp, create_request, in_process_request_type):
        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS}

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls[-1] == call(in_process_request_type, host.inv)
        assert create_request.call_count == 0

        error = (
            "Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout. "
            "There is an active admin request for broken IPMI. Waiting when it will be processed.".format(self.action)
        )

        mock_commit_stage_changes(
            host, status=self.to_status, error=error, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL
        )

        del host.task.hardware_error_count
        del host.task.power_error_count

        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("in_process_request_type", admin_requests.RequestTypes.ALL_IPMI)
    def test_in_process_save_ticket(self, mp, create_request, in_process_request_type):
        ticket_key = "BURNE-10001"

        def get_last_request_status(request_type, host_inv):
            if request_type == in_process_request_type:
                return {"status": admin_requests.STATUS_IN_PROCESS, "ticket": ticket_key}

        mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        error = (
            "Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout. "
            "There is an active admin request for broken IPMI. Waiting when it will be processed.".format(self.action)
        )

        del host.task.hardware_error_count
        del host.task.power_error_count
        host.ticket = ticket_key
        common.get_current_stage(host).set_data("tickets", [ticket_key])

        mock_commit_stage_changes(
            host, inc_revision=1, status=self.to_status, error=error, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL
        )

        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("max_reopened_admin_requests", (0, 1))
    @pytest.mark.parametrize(
        "request_status",
        [admin_requests.STATUS_PROCESSED, admin_requests.STATUS_NOT_EXIST, admin_requests.STATUS_DELETED],
    )
    def test_processed(self, create_request, mp, request_status, exceed_max_error_count, max_reopened_admin_requests):

        host = self.host

        mp.setitem(
            config.get_value("hardware"),
            "max_power_errors",
            host.task.power_error_count if exceed_max_error_count else host.task.power_error_count + 1,
        )
        mp.setitem(config.get_value("hardware"), "max_reopened_admin_requests", max_reopened_admin_requests)

        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_UNREACHABLE:
                response = {"status": request_status}
                if request_status == admin_requests.STATUS_PROCESSED:
                    response["close_time"] = timestamp() - ipmi_errors.HOST_RECOVERY_TIMEOUT
                if request_status == admin_requests.STATUS_NOT_EXIST:
                    response["create_time"] = timestamp() - ipmi_errors.LOST_REQUEST_TIMEOUT
                return response

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        expected_get_status_calls = [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]

        error = "Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout.".format(
            self.action
        )

        if exceed_max_error_count:
            expected_get_status_calls.append(call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv))

            error += " There is a processed admin request for broken IPMI but the problem persists."
            if max_reopened_admin_requests:
                create_request.assert_called_once_with(
                    authorization.ISSUER_WALLE,
                    admin_requests.RequestTypes.IPMI_UNREACHABLE,
                    host,
                    reason=error,
                    request_id_extra_parts=(),
                    shelf_inv=None,
                    params={"eine_code": EineCode.IPMI_PROTO_ERROR},
                    severity_tag=BotTag.MEDIUM,
                )
                host.task.reopened_admin_request_count = 1
                mock_commit_stage_changes(
                    host, status=self.to_status, error=error, check_after=common.ADMIN_REQUEST_CHECK_INTERVAL
                )
            else:
                assert create_request.call_count == 0
                mock_fail_current_stage(host, reason=error)
        else:
            assert create_request.call_count == 0
            host.task.power_error_count += 1
            mock_commit_stage_changes(
                host, status=self.to_status, error=error, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL
            )

        assert get_status.mock_calls == expected_get_status_calls
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("request_status", [admin_requests.STATUS_PROCESSED, admin_requests.STATUS_NOT_EXIST])
    def test_processed_just_now(self, mp, create_request, request_status):
        def get_last_request_status(request_type, host_inv):
            if request_type == admin_requests.RequestTypes.IPMI_UNREACHABLE:
                return {"status": request_status, "close_time": timestamp(), "create_time": timestamp()}

        get_status = mock_get_request_status(mp, side_effect=get_last_request_status)

        host = self.host
        handle_host(host)

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == [
            call(admin_requests.RequestTypes.IPMI_HOST_MISSING, host.inv),
            call(admin_requests.RequestTypes.IPMI_UNREACHABLE, host.inv),
        ]
        assert create_request.call_count == 0

        mock_commit_stage_changes(
            host,
            status=self.to_status,
            check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL,
            error="Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout.".format(
                self.action
            ),
        )

        self.test.hosts.assert_equal()

    @pytest.mark.parametrize("per_task", (False, True))
    def test_disabled_admin_requests(self, create_request, mp, per_task, exceed_max_error_count):
        get_status = mock_get_request_status(mp)

        host = self.host

        if per_task:
            host.task.disable_admin_requests = True
            host.save()
        else:
            mp.setitem(config.get_value("hardware"), "enable_admin_requests", False)

        mp.setitem(
            config.get_value("hardware"),
            "max_power_errors",
            host.task.power_error_count if exceed_max_error_count else host.task.power_error_count + 1,
        )

        handle_host(host)

        error = "Failed to power {0} the host: the host hasn't powered {0} during power {0} timeout.".format(
            self.action
        )

        if exceed_max_error_count:
            mock_fail_current_stage(host, reason=error)
        else:
            host.task.power_error_count += 1
            mock_commit_stage_changes(
                host, status=self.to_status, error=error, check_after=ipmi_errors.HARDWARE_ERROR_RETRY_INTERVAL
            )

        assert self.clients.mock_calls == [call.hardware.is_power_on()]
        assert get_status.mock_calls == []
        assert create_request.call_count == 0

        self.test.hosts.assert_equal()
