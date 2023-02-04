from ads.bsyeti.samogon.resharder.fixtures.fixtures import make_fixtures

from ads.bsyeti.big_rt.py_test_lib.logsampler.samplers import common
from .config import config


def get_sources(*, slugify: bool = True, dev_key: int = config.default_stand):
    assert dev_key == 9

    return common.prepare_sources(config=config, sources=make_fixtures(testing=False), slugify=slugify)
