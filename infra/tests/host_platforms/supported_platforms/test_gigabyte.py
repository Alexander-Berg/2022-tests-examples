from unittest.mock import Mock

import pytest

import walle.host_platforms.supported_platforms.gigabyte as gigabyte
from walle.host_platforms.platform import PlatformProblems


def mock_ipmi_client(ipmi_value, ipmi_success=True):
    ipmi_ret = {"message": ipmi_value}
    if ipmi_success:
        ipmi_ret["success"] = ipmi_success
    return Mock(raw_command=Mock(return_value=ipmi_ret))


def mock_ipmi_platform(test, klass):
    host = test.mock_host({"inv": 0, "name": "mocked.search.yandex.net", "ipmi_mac": "00:00:00:00:00:00"})
    platform = klass(host)
    return platform


class PlatformWithPostCodeTest:
    # force linter to be silent about this member
    klass = None

    def check_get_current_post_code(self, test, ipmi_value, expected_code):
        platform = mock_ipmi_platform(test, self.klass)
        assert platform.get_current_post_code(mock_ipmi_client(ipmi_value)) == expected_code

    def check_get_post_problem_for(self, test, code, expected_problem):
        platform = mock_ipmi_platform(test, self.klass)
        assert platform.get_post_problem_for_code(code) == expected_problem


class TestPlatformGigabyteV2(PlatformWithPostCodeTest):
    klass = gigabyte.PlatformGigabyteV2

    @pytest.mark.parametrize(
        ["ipmi_value", "expected_code"],
        [
            (" 01 a1", 0x01),
            (" b7 a1", 0xB7),
            (" b9 a1", 0xB9),
            (" ba a1", 0xBA),
            (" bd a1", 0xBD),
            ("non-parsable-line", None),
        ],
    )
    def test_get_current_post_code(self, test, ipmi_value, expected_code):
        self.check_get_current_post_code(test, ipmi_value, expected_code)

    @pytest.mark.parametrize(
        ['code', 'expected_problem'], [(gigabyte.PlatformGigabyteV2.POST_CODE_OK, PlatformProblems.POST_OK)]
    )
    def test_get_post_problem_for_code_ok(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)

    @pytest.mark.parametrize(
        ['code', 'expected_problem'],
        [(c, PlatformProblems.POST_MEMORY_PROBLEM) for c in gigabyte.PlatformGigabyteV2.POST_CODES_MEMORY_PROBLEM],
    )
    def test_get_post_problem_for_code_mem_problem(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)

    @pytest.mark.xfail(reason="should be removed after PlatformGigabyte.get_post_problem_for_code will be fixed")
    @pytest.mark.parametrize(['code', 'expected_problem'], [(0xFF, PlatformProblems.POST_UNKNOWN)])
    def test_get_post_problem_for_code_unk_problem(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)


class TestPlatformGigabyteV3(PlatformWithPostCodeTest):
    klass = gigabyte.PlatformGigabyteV3

    @pytest.mark.parametrize(
        ["ipmi_value", "expected_code"],
        [
            (" 00 40 b2 80", 0xB2),
            (" 00 40 b7 80", 0xB7),
            (" 00 40 b9 80", 0xB9),
            (" 00 40 ba 80", 0xBA),
            (" 00 40 bd 80", 0xBD),
            ("non-parsable-line", None),
        ],
    )
    def test_get_current_post_code(self, test, ipmi_value, expected_code):
        self.check_get_current_post_code(test, ipmi_value, expected_code)

    @pytest.mark.parametrize(
        ['code', 'expected_problem'], [(gigabyte.PlatformGigabyteV3.POST_CODE_OK, PlatformProblems.POST_OK)]
    )
    def test_get_post_problem_for_code_ok(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)

    @pytest.mark.parametrize(
        ['code', 'expected_problem'],
        [(c, PlatformProblems.POST_MEMORY_PROBLEM) for c in gigabyte.PlatformGigabyteV3.POST_CODES_MEMORY_PROBLEM],
    )
    def test_get_post_problem_for_code_mem_problem(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)

    @pytest.mark.xfail(reason="should be removed after PlatformGigabyte.get_post_problem_for_code will be fixed")
    @pytest.mark.parametrize(['code', 'expected_problem'], [(0xFF, PlatformProblems.POST_UNKNOWN)])
    def test_get_post_problem_for_code_unk_problem(self, test, code, expected_problem):
        self.check_get_post_problem_for(test, code, expected_problem)
