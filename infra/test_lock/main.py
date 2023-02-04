import gevent
import gevent.monkey
gevent.monkey.patch_all(thread=False)  # noqa
import thread
import signal
import os

import infra.callisto.libraries.discovery as discovery
import logging
import time
import infra.callisto.libraries.yt as yt_utils


def main():
    logging.basicConfig(level=logging.DEBUG)
    yt_client = yt_utils.create_yt_client('locke')
    # with yt_client.Transaction(type='master') as transaction:

    with discovery.lock_path('locke', '//home/cajuper/discovery/t1'):
        # lock_id = yt_client.lock('//home/cajuper/discovery/t1', waitable=True, wait_for=1000)
        # logging.info('locked %s, lock_id: %s, transaction_id: %s', 'path', lock_id, transaction.transaction_id)

        try:
            while True:
                logging.info('tick')
                time.sleep(2)
        except KeyboardInterrupt:
            print 'ctrl+c, exiting'
