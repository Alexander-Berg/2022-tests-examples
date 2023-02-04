# coding: utf-8
__author__ = 'fellow'


def msg_path_to_payments_file_name(msg_path, host_='greed-ts1f'):
    return msg_path.replace('/large/test_samples/', '').replace('.' + host_, '')


def chunks(list_, n=999):
    """Yield successive n-sized chunks from list_."""
    for i in xrange(0, len(list_), n):
        yield list_[i:i + n]
