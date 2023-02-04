import pytest
from faker import Faker

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.tools.devops.lb_dc_checker.dc_checker.tests.fake import CheckResultProvider, LogbrokerDataProvider


@pytest.fixture
def fake():
    fake = Faker()
    fake.add_provider(CommonProvider)
    fake.add_provider(CheckResultProvider)
    fake.add_provider(LogbrokerDataProvider)
    return fake


@pytest.fixture
def last_check(fake):
    return fake.get_offset_check_result(with_read_session=True)


@pytest.fixture
def curr_check(fake, last_check):
    return fake.get_offset_check_result(next_to=last_check, with_read_session=True)
