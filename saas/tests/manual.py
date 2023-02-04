import yt.wrapper as yt
from saas.tools.devops.check_backup.lib import routines


def test_fm_get_config():
    # import pdb; pdb.set_trace()
    ferryman_config = routines.get_ferryman_config('answers', 'stable', fm_host=None)
    routines.validate_ferryman(ferryman_config, 'answers', 'stable')

    yt_cluster = routines.get_ferryman_yt_cluster(ferryman_config)
    assert yt_cluster == 'arnold'


def test_fm_get_last_dish():
    yt_proxy = 'arnold'
    dishes_path = '//home/saas/ferryman-stable/edadeal_lb/dishes'

    yt_client = yt.YtClient(proxy=yt_proxy, config=yt.default_config.get_config_from_env())
    last_dish_ts = routines.get_last_dish_ts(yt_client, dishes_path)
    assert last_dish_ts
    index_blob_path = routines.read_path_from_dish(yt_client, dishes_path, last_dish_ts)
    assert index_blob_path
