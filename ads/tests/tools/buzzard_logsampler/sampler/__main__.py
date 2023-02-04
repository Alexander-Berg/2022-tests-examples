from ads.bsyeti.big_rt.py_test_lib.logsampler.sampler import sampler
from ads.bsyeti.tests.tools.buzzard_logsampler.config.config import config
from ads.bsyeti.tests.tools.buzzard_logsampler.config.sources import get_sources

if __name__ == "__main__":
    sampler.main(config=config, source_getter=get_sources)
