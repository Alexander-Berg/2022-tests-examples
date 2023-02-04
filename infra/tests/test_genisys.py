from __future__ import absolute_import

import os

from skycore.kernel_util import logging
from skycore.kernel_util.unittest import TestCase, main, skipIf

from skycore.framework.utils import detect_hostname
from skycore import genisys
from skycore import initialize_skycore


hostname = detect_hostname()


@skipIf(not os.getenv('UNITTEST_NET'), 'network tests disabled')
class TestGenisys(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestGenisys, self).setUp()

        self.log = logging.getLogger('gnsys')
        self.log.setLevel(logging.DEBUG)

    def test_download_config(self):
        last_modified, cfg = genisys.download_config(hostname, self.log)
        self.assertTrue(last_modified)
        self.assertTrue(cfg)

        last_modified, cfg = genisys.download_config(hostname, self.log,
                                                     last_modified='Thu, 01 Jan 1970 03:00:00 GMT')
        self.assertTrue(last_modified)
        self.assertTrue(cfg)

        last_modified, cfg = genisys.download_config(hostname, self.log, config_hash='0')
        self.assertTrue(last_modified)
        self.assertTrue(cfg)


if __name__ == '__main__':
    main()
