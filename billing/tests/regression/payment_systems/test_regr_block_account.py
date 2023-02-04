import pytest


@pytest.mark.regression
@pytest.mark.parametrize(
    'account, block_type',
    [
        ('paypalch@yandex-team.ru', 0),
        ('700500', 1),
        ('valeriya.kostina@gmail.com', 2)
    ])
def test_blocked_accounts(account, block_type, bcl_get_account_info):
    result = bcl_get_account_info(account)
    assert result['account']['number'] == account
    assert result['account']['blocked'] == block_type
