import logging
import sys


class TorrentsConfig:

    def __init__(self, brew_user='good_boy'):
        self.brewer = {
            "brew_discard_timeout": 5,
            "sleep_time": 0.1,
            "layers_per_chunk": 10,
            "brew_user": brew_user
        }
        self.logger = logging.getLogger(__name__)
        logger_stream = logging.StreamHandler(sys.__stdout__)
        logger_stream.setLevel(logging.DEBUG)
        formatter = logging.Formatter('%(levelname)-8s [%(asctime)-15s] %(message)s')
        logger_stream.setFormatter(formatter)
        self.logger.addHandler(logger_stream)
