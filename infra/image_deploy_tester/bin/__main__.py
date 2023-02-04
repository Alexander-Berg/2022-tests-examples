import logging
import logging.handlers
import sys

from sepelib.core import config
from infra.qyp.image_deploy_tester.src import runner


DEFAULT_CONFIG_PATH = './cfg_default.yml'
ENV_PREFIX = 'QYP'
FORMAT = "%(process)s %(asctime)s %(levelname)s [%(name)s] %(message)s"


def setup_logging():
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    log_path = config.get_value('log.path', None)
    if log_path is not None:
        handler = logging.handlers.TimedRotatingFileHandler(log_path, when='midnight', backupCount=10)
    else:
        handler = logging.StreamHandler(sys.stdout)

    handler.setFormatter(logging.Formatter(FORMAT))
    logger.addHandler(handler)


def main():
    config_context = config.get_context_from_env(prefix=ENV_PREFIX)
    config.load(DEFAULT_CONFIG_PATH, config_context=config_context)
    setup_logging()
    runner.Runner().run()


if __name__ == '__main__':
    main()
