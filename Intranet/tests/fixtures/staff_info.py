import pytest

from stackbot.logic.clients.staff import StaffInfo


@pytest.fixture(scope='function')
def staff_info():
    counter = 0

    def _staff_info(telegram_account: str = None, is_dismissed: bool = False):
        nonlocal counter
        counter += 1
        if telegram_account is None:
            telegram_account = f'smth_{counter}'
        return StaffInfo(
            login=f'test_user_{counter}',
            uid=f'999999{counter}',
            id=100 + counter,
            telegram_account=telegram_account,
            is_dismissed=is_dismissed
        )

    return _staff_info
