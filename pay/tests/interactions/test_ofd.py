from hamcrest import assert_that, empty

from yb_darkspirit.interactions.ofd import OfdSyncStatus


def test_cash_register_sync_status(dev_ofd_private_client):
    result = (
        dev_ofd_private_client
        .check_cash_register_sync_status('0000012345123451', '8710000000000003')
    )
    assert_that(result, OfdSyncStatus())


def test_cash_registers_sync_status(dev_ofd_private_client):
    result = (
        dev_ofd_private_client
        .check_cash_registers_sync_status([{'rn': '0000012345123451', 'fn': '8710000000000003'}])
    )
    assert_that(result, empty())


def test_cash_registers_sync_status_multi(dev_ofd_private_client):
    result = (
        dev_ofd_private_client
        .check_cash_registers_sync_status([
            {'rn': '0000012345123451', 'fn': '8710000000000003'},
            {'rn': '0000012345123452', 'fn': '8710000000000004'}
        ])
    )
    assert_that(result, empty())
