from multiprocessing.managers import BaseManager

from maps_adv.common.shared_mock import SharedMockMpManager, SharedMockProxy


def test_is_multiprocessing_manager():
    assert issubclass(SharedMockMpManager, BaseManager)


def test_has_shared_mock_type_registered():
    with SharedMockMpManager() as manager:
        proxy = manager.SharedMock()

    assert isinstance(proxy, SharedMockProxy)
