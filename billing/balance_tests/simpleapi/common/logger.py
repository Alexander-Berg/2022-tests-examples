__author__ = 'fellow'

# coding: utf-8

import btestlib.reporter as reporter

'''
Helper functions for logging.
'''

LOG_INSTANCE = None


def get_logger():
    global LOG_INSTANCE
    if not LOG_INSTANCE:
        LOG_INSTANCE = reporter.logger()
    return LOG_INSTANCE
