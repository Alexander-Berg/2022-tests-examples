import pytest

from .tables import BannerData, Index


@pytest.fixture(autouse=True)
def create_data(local_yt_registry):
    BannerData.objects.create_table()
    Index.objects.create_table()

    BannerData(bannerhash=10, title='title').save()
    Index(timestamp=10, user_id=0, bannerhash=10).save()
    yield
    Index.objects.store.drop_store()
    BannerData.objects.store.drop_store()


@pytest.mark.linux
@pytest.mark.parametrize('left', [True, False])
@pytest.mark.parametrize('right', [True, False])
def test_mount(local_yt_client, left, right):
    if left:
        local_yt_client.mount_table(BannerData._table_path, sync=True)
    else:
        local_yt_client.unmount_table(BannerData._table_path, sync=True)

    if right:
        local_yt_client.mount_table(Index._table_path, sync=True)
    else:
        local_yt_client.unmount_table(Index._table_path, sync=True)

    labels = list(Index.objects.filter(user_id=0).join(BannerData, BannerData.bannerhash))
    assert len(labels) == 1
    assert labels[0].bannerhash == 10
