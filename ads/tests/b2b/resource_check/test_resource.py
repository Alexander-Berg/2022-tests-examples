import logging

from ads.bsyeti.big_rt.py_test_lib.logsampler.samplers import utils as samplers_utils
from ads.bsyeti.caesar.tools.logtests.config.config import config as samplers_config
from ads.bsyeti.caesar.tools.logtests.config.sources import get_sources

log = logging.getLogger(__name__)


def test_resource():
    """
    check that resource contains everything present in sources.py
    """
    orig_sources = set(get_sources())
    assert orig_sources, "did the format of sources change?"
    log.info("orig_sources: %s", sorted(orig_sources))
    resource_sources = set(samplers_utils.get_sample_names(samplers_config))
    log.info("resource_sources: %s", sorted(resource_sources))
    assert orig_sources <= resource_sources, "Extra items %s" % (orig_sources - resource_sources)
