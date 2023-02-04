from ads.watchman.timeline.api.lib.config import Config


def test_config_has_get_item():
    Config.DB_DRIVER = 'test'
    assert Config['DB_DRIVER'] == 'test'
