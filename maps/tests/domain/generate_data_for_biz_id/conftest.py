import pytest


@pytest.fixture(autouse=True)
async def common_mocks(dm, bvm):
    dm.fetch_biz_state.coro.return_value = None
    dm.fetch_biz_state_by_slug.coro.return_value = None
    dm.create_biz_state.cor.return_value = None
    dm.save_landing_data_for_biz_id.coro.return_value = {"some": "data"}
