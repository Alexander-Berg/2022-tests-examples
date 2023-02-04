import pytest


@pytest.fixture(autouse=True)
def mock_yt(mocker, shared_proxy_mp_manager):
    methods = (
        "remove",
        "create",
        "exists",
        "read_table",
        "run_sort",
        "write_table",
        "set_attribute",
        "Transaction",
    )
    mocks = {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", shared_proxy_mp_manager.SharedMock()
        )
        for method in methods
    }
    mocks["read_table"].return_value = []

    return mocks
