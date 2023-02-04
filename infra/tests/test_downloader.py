from __future__ import absolute_import

import os
from py.path import local as Path

from skycore.kernel_util import logging
from skycore.kernel_util.unittest import TestCase, main, skipIf
from skycore.kernel_util.sys import TempDir

from skycore.components.configupdater import ConfigUpdater
from skycore.procs.mailbox import mailbox
from skycore.framework.utils import detect_hostname
from skycore.framework.greendeblock import Deblock
from skycore import downloader
from skycore import initialize_skycore

import mock
import gevent


hostname = detect_hostname()


@skipIf(not os.getenv('UNITTEST_NET'), 'network tests disabled')
@skipIf(not os.getenv('UNITTEST_SLOW'), 'slow tests disabled')
class TestDownloader(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestDownloader, self).setUp()

        self.log = logging.getLogger('dwnl')
        self.log.setLevel(logging.DEBUG)

        self.deblock = Deblock(name='test_downloader')

        self.tempdir = TempDir()
        self.tempdir.open()
        cfg = ConfigUpdater(
            hostname='torkve.skydev.search.yandex.net',
            config_dir=self.tempdir.dir(),
            filename='cfg.yaml',
            log=self.log,
            deblock=self.deblock,
        )
        cfg.update_config(self.log)
        self.cfg = cfg.query(['skynet', 'skycore', 'namespaces', 'skynet', 'skybone'])['config']
        self.mbiter = gevent.spawn(self._iterate_mbox)

    def tearDown(self):
        self.mbiter.kill(gevent.GreenletExit)
        self.tempdir.close()
        self.deblock.stop()
        super(TestDownloader, self).tearDown()

    def _iterate_mbox(self):
        for cb in mailbox().iterate():
            cb()

    def test_download(self):
        release = downloader.parse_advertised_release(self.cfg)
        with TempDir() as workdir:
            downloader.download(workdir, release['filename'], release['urls'], release['md5'], self.deblock)
            m1 = mock.patch('skycore.downloader.download_via_skynet')
            m2 = mock.patch('skycore.downloader.download_via_http')
            m3 = mock.patch('skycore.downloader.download_via_rsync')
            with m1 as d1, m2 as d2, m3 as d3:
                downloader.download(workdir, release['filename'], release['urls'], release['md5'], self.deblock)
                self.assertFalse(d1.called)
                self.assertFalse(d2.called)
                self.assertFalse(d3.called)

    @skipIf(not hostname.endswith('.yandex.ru')
            and not hostname.endswith('.search.yandex.net'),
            'copier cannot be tested from non-yandex network')
    def test_copier(self):
        release = downloader.parse_advertised_release(self.cfg)
        url = release['urls']['skynet'][0]
        with TempDir() as workdir:
            dst = Path(workdir).join(os.path.basename(release['filename']))
            downloader.download_via_skynet(dst, url, release['md5'], self.deblock)
            # self.assertEqual(release['size'], dst.stat().size)

    def test_http(self):
        release = downloader.parse_advertised_release(self.cfg)
        for url in release['urls']['http']:
            u = downloader.fix_parsed_url(url, 'backbone')
            with TempDir() as workdir:
                dst = Path(workdir).join(os.path.basename(release['filename']))
                self.assertEqual(dst, downloader.download_via_http(dst, u, release['md5'], self.deblock, skybone_available=False))
        release['urls']['skynet'] = ()
        release['urls']['rsync'] = ()
        with TempDir() as workdir:
            dst = Path(workdir).join(os.path.basename(release['filename']))
            self.assertEqual(dst, downloader.download(workdir, release['filename'], release['urls'], release['md5'], self.deblock, skybone_available=False))
            # self.assertEqual(release['size'], dst.stat().size)

    @skipIf(not hostname.endswith('.yandex.ru')
            and not hostname.endswith('.search.yandex.net'),
            'rsync cannot be tested from non-yandex network')
    def test_rsync(self):
        release = downloader.parse_advertised_release(self.cfg)
        if not release['urls']['rsync']:
            return
        url = downloader.fix_parsed_url(release['urls']['rsync'][0], 'backbone')
        with TempDir() as workdir:
            dst = Path(workdir).join(os.path.basename(release['filename']))
            self.assertEqual(dst, downloader.download_via_rsync(dst, url, release['md5'], self.deblock))
            # self.assertEqual(release['size'], dst.stat().size)


if __name__ == '__main__':
    main()
