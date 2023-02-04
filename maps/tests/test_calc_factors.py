from maps.analyzer.pylibs.time_estimator.calc_factors import get_feature_names
from maps.analyzer.pylibs.time_estimator.model import PyModelConfig

from .common import canonical_data, get_test_data


def test_get_feature_names():
    config = PyModelConfig.from_file(get_test_data('model_config.meta'))
    return canonical_data(get_feature_names(config), 'feature_names.json')
