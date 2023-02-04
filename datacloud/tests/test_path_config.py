from hamcrest import assert_that, is_, equal_to
from datacloud.features.cluster import path_config


class TestPathConfig(object):
    def test_create_default_path_config(self):
        config = path_config.PathConfig()
        assert_that(config.date, equal_to('current'))
        assert_that(config.days_to_take, equal_to(175))
        assert_that(config.is_retro, is_(False))
