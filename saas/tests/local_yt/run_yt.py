import traceback
import os
import os.path
import logging
import yatest.common
from robot.library.yuppie.modules.environment import Environment
from robot.library.yuppie.modules.yt_mod import LocalYt


def run_yt():
    os.environ['YT_TESTS_HOST'] = 'NO_HOST'
    Environment()
    try:
        yt = LocalYt()
        os.chdir(yatest.common.output_path())
        logging.info('yt get_proxy: %s', yt.get_proxy())
        os.environ['YT_TESTS_HOST'] = yt.get_proxy()
    except Exception as err:
        logging.error('%s', err)
        logging.error(traceback.format_exc())
        raise err
        # yp.Sys.hang()
