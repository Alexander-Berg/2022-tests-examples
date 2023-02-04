# coding: utf-8

'''
Helper functions for logging.
'''

import logging


def config_logger(log):
    '''Configuring logger'''
    log.setLevel(logging.DEBUG)
    fhandler = logging.FileHandler('music_inapp_test.log')
    formatter = logging.Formatter(
        u'%(filename)s[LINE:%(lineno)d]# %(levelname)-8s '
        u'[%(asctime)s]  %(message)s')
    fhandler.setLevel(logging.DEBUG)
    fhandler.setFormatter(formatter)
    log.addHandler(fhandler)


LOG = logging.getLogger()
config_logger(LOG)


def get_logger():
    '''
    Returns configured logger.
    '''
    # log = logging.getLogger()
    return LOG
