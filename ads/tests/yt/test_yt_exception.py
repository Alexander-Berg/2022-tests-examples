from ads_pytorch.yt.yt_exception import (
    raise_transaction_expired_or_aborted_if_possible,
    TransactionExpiredOrAborted
)
import pytest


def test_not_tx_expired():
    res = raise_transaction_expired_or_aborted_if_possible("rfgecnf")
    assert res is None


def test_tx_expired():
    with pytest.raises(TransactionExpiredOrAborted) as value:
        msg = "Transaction a-b-c-d has expired or was aborted"
        raise_transaction_expired_or_aborted_if_possible(msg)
    assert value.value.tx == "a-b-c-d"
