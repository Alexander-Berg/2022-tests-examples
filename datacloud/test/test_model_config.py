from hamcrest import assert_that, instance_of, none, equal_to, raises, calling
from datacloud.model_applyer.lib import model_config
from datacloud.model_applyer.lib import general_features as gf


class TestModelConfig(object):
    def test_parse_feature(self):
        feature = model_config._parse_feature('DSSMFeatureBase400')
        assert_that(feature, instance_of(gf.DSSMFeatureBase400))
        assert_that(feature.default, none())

    def test_parse_feature_with_default(self):
        feature = model_config._parse_feature('DSSMFeatureBase400:f42.0')
        assert_that(feature, instance_of(gf.DSSMFeatureBase400))
        assert_that(feature.default, equal_to(42.0))

    def test_assertion_if_wrong_format(self):
        assert_that(
            calling(model_config._parse_feature).with_args('WTF'),
            raises(AssertionError))
        assert_that(
            calling(model_config._parse_feature).with_args('DSSMFeatureBase400:'),
            raises(AssertionError))
        assert_that(
            calling(model_config._parse_feature).with_args('DSSMFeatureBase400:42'),
            raises(AssertionError))
