import pytest

from maps_adv.common.shared_mock import SharedMockMpManager


def test_returns_shared_mock_multiprocessing_manager(shared_proxy_mp_manager):
    assert isinstance(shared_proxy_mp_manager, SharedMockMpManager)


def test_returned_is_started(shared_proxy_mp_manager):
    try:
        shared_proxy_mp_manager.SharedMock()
    except AssertionError:
        pytest.fail("Should return started server")
