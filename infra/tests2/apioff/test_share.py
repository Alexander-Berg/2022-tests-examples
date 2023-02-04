import py
import pytest
from py import std

from api.copier import errors
from ya.skynet.services.copier.rbtorrent import errors as rberrors


CLEAN_AFTER_EACH_RUN = True



def test_get_shared_but_non_existent(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    files = fileGenerator(count=5)

    s2.makeActive()
    resid = copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    s1.makeActive()
    dest = tmpdir.ensure('dest', dir=1)
    handler = copier.get(resid, dest=dest.strpath)
    handler.wait(timeout=30)

    for i in range(3):
        dest.remove()

        handler = copier.get(resid, dest=dest.strpath)
        handler.wait(timeout=30)

        assert len(dest.listdir()) == 5
        for f in files:
            destFile = dest.join(f.basename)
            assert destFile.check(file=1, exists=1)
            assert f.size() == destFile.size()
            assert f.read(mode='rb') == destFile.read(mode='rb')

    if CLEAN_AFTER_EACH_RUN:
        dest.remove()
        [f.remove() for f in files]


def test_watchdog_remove_non_existent(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    s1.makeActive()
    s1.runWatchDog()

    files = fileGenerator(count=10)

    torrentsBefore = s1.execute('result = len(self.torrentmanager.torrents)')
    copier.create([f.strpath for f in files])
    assert s1.execute('result = len(self.torrentmanager.torrents)') == torrentsBefore + 11

    [f.remove() for f in files]

    s1.runWatchDog()

    assert s1.execute('result = len(self.torrentmanager.torrents)') == torrentsBefore


def test_watchdog_remove_non_existent_auto_mode(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    files = fileGenerator(count=10)

    s1.execute('self.watchdog.CHECK_INTERVAL_FINISHED = 0.5')
    s1.execute('self.watchdog.CHECK_INTERVAL_DOWNLOADING = 0.5')

    try:
        s1.runWatchDog()

        sharedBefore = s1.execute('result = len(self.torrentmanager.torrents)')

        copier.create([f.strpath for f in files])

        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 11
        files[5].remove()

        std.time.sleep(1.5)

        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 9

        [f.remove() for f in files if f.check(exists=1)]

        std.time.sleep(1.5)

        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore
    finally:
        s1.execute('self.watchdog.CHECK_INTERVAL_FINISHED = 60')
        s1.execute('self.watchdog.CHECK_INTERVAL_DOWNLOADING = 5')


def test_watchdog_remove_changed_auto_mode(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    s1.execute('self.watchdog.CHECK_INTERVAL_FINISHED = 0.5')

    try:
        s1.runWatchDog()

        sharedBefore = s1.execute('result = len(self.torrentmanager.torrents)')

        files = fileGenerator(count=10)

        copier.create([f.strpath for f in files])
        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 11

        files[5].setmtime(123123)

        std.time.sleep(1.5)

        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 9

        [f.setmtime(123123) for f in files]

        std.time.sleep(1.5)

        assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore
    finally:
        s1.execute('self.watchdog.CHECK_INTERVAL_FINISHED = 60')

        if CLEAN_AFTER_EACH_RUN:
            [f.remove() for f in files]


def test_quickly_change_files_and_share_again(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    s1.runWatchDog()

    sharedBefore = s1.execute('result = len(self.torrentmanager.torrents)')

    files = fileGenerator(count=500)

    copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    [f.write('zuko', mode='ab') for f in files]

    resid = copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    s1.runWatchDog()
    assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 501

    s2.makeActive()

    dest = tmpdir.join('dest').ensure(dir=1)
    copier.get(resid, dest=dest.strpath).wait(timeout=60)

    for f in files:
        df = dest.join(f.basename)
        assert df.check(exists=1, file=1)
        assert f.size() == df.size()
        assert f.read(mode='rb') == df.read(mode='rb')

    if CLEAN_AFTER_EACH_RUN:
        dest.remove()
        [f.remove() for f in files]


def test_get_resource_with_empty_file(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    s1.makeActive()
    s1.runWatchDog()

    files = fileGenerator(count=10)
    files[5].write('', mode='wb')

    resid = copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    s2.makeActive()
    s2.runWatchDog()
    sharedBefore, plainDataBefore = s2.execute(
        'result = len(self.torrentmanager.torrents), len(self.plaindatamanager.items)'
    )

    dest = tmpdir.ensure('dest', dir=1)
    copier.get(resid, dest=dest.strpath).wait()

    assert \
        s2.execute('result = len(self.torrentmanager.torrents), len(self.plaindatamanager.items)') == \
        (sharedBefore + 10, plainDataBefore + 1)

    for f in files:
        df = dest.join(f.basename)
        assert df.check()
        assert f.size() == df.size()
        assert f.read(mode='rb') == df.read(mode='rb')

    dest.join(files[5].basename).remove()

    s2.runWatchDog()

    assert \
        s2.execute('result = len(self.torrentmanager.torrents), len(self.plaindatamanager.items)') == \
        (sharedBefore + 9, plainDataBefore)

    if CLEAN_AFTER_EACH_RUN:
        [f.remove() for f in files]
        dest.remove()


def test_download_shared_file(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    files = fileGenerator(count=5)

    s1.runWatchDog()

    resid = s1.copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()
    s1.copier.get(resid, dest=files[0].dirpath().strpath).wait(timeout=1)

    s2.copier.get(resid, dest=tmpdir.join('dest').strpath).wait(timeout=30)

    [f.remove() for f in files]

    s1.copier.get(resid, dest=tmpdir.join('dest2').strpath).wait(10)

    for file_ in tmpdir.join('dest').listdir():
        df = tmpdir.join('dest2').join(file_.basename)
        assert file_.check()
        assert file_.size() == df.size()
        assert file_.read(mode='rb') == df.read(mode='rb')

    if CLEAN_AFTER_EACH_RUN:
        tmpdir.join('dest').remove()
        tmpdir.join('dest2').remove()


def test_download_shared_file_to_different_location(tmpdir, supervisors, fileGenerator):
    """
    We have shared file A and want to download resource with the same file A, but to
    different location.
    """

    try:
        s1, s2, s3 = supervisors

        files = fileGenerator(count=50)

        s1.runWatchDog()

        resid = s1.copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        dest = tmpdir.join('dest')
        s1.copier.get(resid, dest=dest.strpath).wait()

        for f in files:
            df = dest.join(f.basename)
            assert df.check(exists=1, file=1)
            assert f.size() == df.size()
            assert f.read(mode='rb') == df.read(mode='rb')
            assert int(f.stat().mtime) == int(df.stat().mtime)
    finally:
        if CLEAN_AFTER_EACH_RUN:
            [f.remove() for f in files]


def test_download_shared_file_to_different_location_already_existent(tmpdir, supervisors, fileGenerator):
    """
    Check how plain copy works if destination path already exists
    """
    try:
        s1, s2, s3 = supervisors

        file_ = fileGenerator(count=1, bs='5M')[0]

        s1.runWatchDog()

        resid = s1.copier.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        dest = tmpdir.join('dest')
        destFile = dest.join(file_.basename)

        destFile.ensure(file=1).write('fake' * 1024 * 1024, mode='wb')
        s1.copier.get(resid, dest=dest.strpath).wait()

        def _checkFile():
            assert destFile.check(exists=1, file=1)
            assert file_.size() == destFile.size()
            assert int(file_.stat().mtime) == int(destFile.stat().mtime)
            assert file_.read(mode='rb') == destFile.read(mode='rb')

        _checkFile()

        s1.copier.get(resid, dest=dest.strpath).wait()
        _checkFile()

        destFile.write('fake' * 1024 * 1024, mode='ab')
        s1.copier.get(resid, dest=dest.strpath).wait()
        _checkFile()

        destFile.write('fake' * 1024 * 1024 * 2, mode='wb')
        s1.copier.get(resid, dest=dest.strpath).wait()
        _checkFile()
    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                file_.remove()
            except BaseException:
                pass


def test_download_already_downloading_file_to_different_location(tmpdir, copier, supervisors, fileGenerator):
    try:
        s1, s2, s3 = supervisors
        file_ = fileGenerator(bs='10M', count=1)[0]

        s2.setDownloadSpeed(1024 * 1024 * 1)

        s1.makeActive()
        resid = copier.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        s2.makeActive()
        s2stats = s2.getStatus('torrentsCount', 'plainDataCount')

        destA = tmpdir.join('destA')
        destB = tmpdir.join('destB')

        totalDownloaded = 0

        handleA = copier.get(resid, dest=destA.strpath)

        torrentInfoHash = s2.execute('''
            torr = self.torrentmanager.torrents[%r]
            assert torr.evtLoaded.wait(timeout=10)
            result = torr.torrents.keys()[0]
        ''' % resid.split(':', 1)[1])

        # Wait until it will download at least 200kb
        for i in range(20):
            totalDownloaded = s2.execute('''
                result = self.torrentmanager.torrents[%r].getStatus('totalPayloadDownload')
            ''' % torrentInfoHash)
            if totalDownloaded < 1024 * 1024:
                std.time.sleep(0.5)
                continue
            else:
                break

        assert totalDownloaded >= 1024 * 1024
        assert totalDownloaded < 5 * 1024 * 1024

        s2.setDownloadSpeed(0)
        copier.get(resid, dest=destB.strpath).wait()

        for d in destA, destB:
            df = d.join(file_.basename)
            assert df.check(exists=1)
            assert df.size() == file_.size()
            assert df.read(mode='rb') == file_.read(mode='rb')

        news2Stats = s2.getStatus('torrentsCount', 'plainDataCount')
        assert news2Stats['torrentsCount'] == s2stats['torrentsCount'] + 2
        assert news2Stats['plainDataCount'] == s2stats['plainDataCount'] + 1

        handleA.wait(timeout=1)
    finally:
        s2.setDownloadSpeed(0)
        if CLEAN_AFTER_EACH_RUN:
            file_.remove()
            try:
                destA.remove()
            except BaseException:
                pass
            try:
                destB.remove()
            except BaseException:
                pass


def test_threaded_download_same_resource_to_the_same_location(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    files = fileGenerator(count=100)

    s1.makeActive()
    s1.runWatchDog()

    resid = copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    s2.makeActive()
    s2.runWatchDog()

    threads = []
    dest = tmpdir.join('dest')

    for i in range(100):
        t = std.threading.Thread(target=lambda: copier.get(resid, dest=dest.strpath).wait(timeout=20))
        threads.append(t)

    [t.start() for t in threads]
    [t.join() for t in threads]

    for f in files:
        df = dest.join(f.basename)
        assert df.check(exists=1, file=1)
        assert f.size() == df.size()
        assert f.read(mode='rb') == df.read(mode='rb')

    if CLEAN_AFTER_EACH_RUN:
        [f.remove() for f in files]
        dest.remove()


@pytest.mark.plaincopy
def test_threaded_download_same_resource_to_different_locations(tmpdir, copier, supervisors, fileGenerator):
    s1, s2, s3 = supervisors
    files = fileGenerator(count=10)

    s1.makeActive()
    #s1stat = s1.getStatus('torrentsCount', 'plainDataCount')
    resid = copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

    s2.makeActive()
    #s2stat = s2.getStatus('torrentsCount', 'plainDataCount')
    threads = []

    for i in range(20):
        dest = tmpdir.join('tgt/dest%d' % i)

        def _mk_dl(d):
            def _dl():
                copier.get(resid, dest=d.strpath).wait(timeout=20)
            return _dl

        thread = std.threading.Thread(target=_mk_dl(dest))
        threads.append(thread)

    for t in threads:
        t.start()

    [t.join() for t in threads]

    for i in range(20):
        for f in files:
            df = tmpdir.join('tgt/dest%d' % i).join(f.basename)
            assert df.check(exists=1, file=1)
            assert df.size() == f.size()
            assert df.read(mode='rb') == f.read(mode='rb')

    if CLEAN_AFTER_EACH_RUN:
        [f.remove() for f in files]
        tmpdir.join('tgt').remove()


def test_not_enough_free_space(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    try:
        file1 = fileGenerator(bs='10M', bsCount=1, count=1)[0]
        resid = s1.copier.create([file1.basename], cwd=file1.dirpath().strpath).resid()
        torrentHash = s1.execute('''
            metaTorrent = self.torrentmanager.torrents[%r]
            result = metaTorrent.torrents.keys()[0]
        ''' % resid.split(':', 1)[1])

        s2.setDownloadSpeed(1024 * 1024)
        dest = tmpdir.join('dest')

        handle = s2.copier.get(resid, dest=dest.strpath)

        downloaded = 0
        wait = 0.0
        while downloaded < 1024 * 1024:
            try:
                downloaded = s2.execute('''
                    if {0!r:s} in self.torrentmanager.torrents:
                        result = self.torrentmanager.torrents[{0!r:s}].getStatus("totalPayloadDownload")
                    else:
                        result = 0
                '''.format(torrentHash))
            except BaseException:
                pass

            std.time.sleep(0.3)
            wait += 0.3
            assert wait < 20

        destFile = dest.join(file1.basename)

        assert destFile.check()

        s2.execute('''
            class MockAlert(object):
                msg = 'No space left on device'
                file = {1!r:s}

                class handle:
                    @staticmethod
                    def info_hash():
                        return {0!r:s}

            self.torrentmanager.onAlertFileError(MockAlert())
        '''.format(torrentHash, destFile.strpath))

        #noinspection PyUnresolvedReferences
        with pytest.raises(errors.FilesystemError):
            handle.wait(timeout=2)

        # Ensure what file which we are downloading is removed
        wait = 0
        while destFile.check():
            std.time.sleep(0.1)
            wait += 0.1
            assert wait < 5, 'Downloading file was not removed after "Not enough free space error"'
        assert not destFile.check()

    finally:
        s2.setDownloadSpeed(0)
        try:
            file1.remove()
        except BaseException:
            pass

        try:
            dest.remove()
        except BaseException:
            pass


#noinspection PyUnresolvedReferences
@pytest.mark.xfail
def test_file_errors():
    raise 1


#noinspection PyUnresolvedReferences
@pytest.mark.slow
def test_many_simultaneous_metatorrent_downloads(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    try:
        files = fileGenerator(bs='1M', bsCount=1, count=300)

        residHandles = [s1.copier.createEx([f.basename], cwd=f.dirpath().strpath) for f in files]
        resids = [handle.wait(timeout=20).resid() for handle in residHandles]

        handles = []

        dest = tmpdir.join('dest')
        dest2 = tmpdir.join('dest2')

        for resid in resids:
            handle = s2.copier.get(resid, dest=dest.strpath)
            handles.append(handle)

            anotherHandle = s2.copier.get(resid, dest=dest2.strpath)
            handles.append(anotherHandle)

        for handle in handles:
            handle.wait(timeout=120)

        for f in files:
            fc = f.read(mode='rb')

            for d in dest, dest2:
                df = d.join(f.basename)
                assert df.check(exists=1)
                assert df.size() == f.size()
                assert df.read(mode='rb') == fc

    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                dest.remove()
                dest2.remove()
                [f.remove() for f in files]
            except BaseException:
                pass


#noinspection PyUnresolvedReferences
@pytest.mark.slow
@pytest.mark.heavy
@pytest.mark.plaincopy
def test_big_file_download(tmpdir, supervisors, fileGenerator):
    """ Check big (120mb and 500mb files download). This tests rsync as well."""

    try:
        s1, s2, s3 = supervisors

        randomData = fileGenerator(bs='1M', bsCount='1', count=1)[0].read(mode='rb')
        files = [tmpdir.join('file0'), tmpdir.join('file1')]

        for i in range(120):
            files[0].write(randomData, mode='ab')
        for i in range(500):
            files[1].write(randomData, mode='ab')

        destA = tmpdir.join('destA')
        destB = tmpdir.join('destB')

        resid = s1.copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        s2.copier.get(resid, dest=destA.strpath).wait(timeout=60)
        s2.copier.get(resid, dest=destB.strpath).wait(timeout=60)

        md5 = py.path.local.sysfind('md5')
        md5args = ['-r']

        if not md5:
            md5 = py.path.local.sysfind('md5sum')
            md5args = []

        assert md5 is not None, 'Cant find md5 compare utility!'

        for dest in (destA, destB):
            for f in files:
                df = dest.join(f.basename)
                assert df.check(exists=1)
                assert df.size() == f.size()
                assert md5.sysexec(*(md5args + [df.strpath]))[:32] == md5.sysexec(*(md5args + [f.strpath]))[:32]

    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                [f.remove() for f in files]
                destA.remove()
                destB.remove()
            except BaseException:
                pass


#noinspection PyUnresolvedReferences
@pytest.mark.xfail
def test_removal_of_errored_torrents():
    raise 1


def test_uploading_removed_file(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    try:
        file_ = fileGenerator(count=1)[0]
        file2 = file_.dirpath().join('_file_backup')

        # Remove shared file very soon -- even finished alert should be later than this.
        # Thus, meta resource need to be removed, and thus meta resource not available error
        # should occur
        resid = s1.copier.create([file_.basename], cwd=file_.dirpath().strpath).resid()
        file_.move(file2)

        #noinspection PyUnresolvedReferences
        with py.test.raises(rberrors.TorrentStalled):
            s2.execute('''
                self.watchdog.OLD_STALL_TORRENTS_TIMEOUT = self.watchdog.STALL_TORRENTS_TIMEOUT
                self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING = self.watchdog.CHECK_INTERVAL_DOWNLOADING
                self.watchdog.STALL_TORRENTS_TIMEOUT = 3
                self.watchdog.CHECK_INTERVAL_DOWNLOADING = 1
            ''')

            handle = s2.copier.get(resid, dest=tmpdir.join('dest').strpath)
            handle.wait(timeout=7)

        std.time.sleep(1)

        file2.move(file_)

        resid2 = s1.copier.create([file_.basename], cwd=file_.dirpath().strpath).resid()
        assert resid2 == resid

        s2.execute('self.watchdog.STALL_TORRENTS_TIMEOUT = self.watchdog.OLD_STALL_TORRENTS_TIMEOUT')
        handle = s2.copier.get(resid, dest=tmpdir.join('dest').strpath)
        handle.wait(timeout=30)

        dfile = tmpdir.join('dest').join(file_.basename)
        assert dfile.check()
        assert dfile.size() == file_.size()
        assert dfile.read(mode='rb') == file_.read(mode='rb')

    finally:
        s2.execute('''
            self.watchdog.STALL_TORRENTS_TIMEOUT = self.watchdog.OLD_STALL_TORRENTS_TIMEOUT
            del self.watchdog.OLD_STALL_TORRENTS_TIMEOUT

            self.watchdog.CHECK_INTERVAL_DOWNLOADING = self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING
            del self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING
        ''')

        if CLEAN_AFTER_EACH_RUN:
            if file_.check():
                file_.remove()
            if file2.check():
                file2.remove()

            try:
                tmpdir.join('dest').remove()
            except BaseException:
                pass


def test_downloading_stall_reannounce(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors
    s1.runWatchDog()

    try:
        s2.execute('''
            self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING = self.watchdog.CHECK_INTERVAL_DOWNLOADING
            self.watchdog.OLD_MIN_REANNOUNCE_INTERVAL = self.watchdog.MIN_REANNOUNCE_INTERVAL
            self.watchdog.CHECK_INTERVAL_DOWNLOADING = 1
            self.watchdog.MIN_REANNOUNCE_INTERVAL = 2
        ''')

        file1 = fileGenerator(count=1)[0]
        file2 = file1.dirpath().join(file1.basename + '2')

        resid = s1.copier.create([file1.basename], cwd=file1.dirpath().strpath).resid()

        # This will avoid ResourceNotAvailable exception to be popped out after runWatchDog in s1.
        assert s3.copier.create([file1.basename], cwd=file1.dirpath().strpath).resid() == resid

        file1.move(file2)
        tmpdir.dirpath().join('supervisor2/supervisor/copier/rbtorrent/metacache/%s' % resid.split(':', 1)[1]).remove()

        # Now watchdog should remove both torrent and metatorrent
        assert s1.runWatchDog() == 2

        dest = tmpdir.join('dest')
        handle = s2.copier.get(resid, dest=dest.strpath)

        file2.move(file1)
        resid2 = s1.copier.create([file1.basename], cwd=file1.dirpath().strpath).resid()
        assert resid2 == resid

        handle.wait(timeout=30)

        assert len(dest.listdir()) == 1
        df = dest.join(file1.basename)

        assert df.check()
        assert df.size() == file1.size()
        assert df.read(mode='rb') == file1.read(mode='rb')

    finally:
        s2.execute('''
            self.watchdog.CHECK_INTERVAL_DOWNLOADING = self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING
            del self.watchdog.OLD_CHECK_INTERVAL_DOWNLOADING

            self.watchdog.MIN_REANNOUNCE_INTERVAL = self.watchdog.OLD_MIN_REANNOUNCE_INTERVAL
            del self.watchdog.OLD_MIN_REANNOUNCE_INTERVAL
        ''')

        try:
            file1.remove()
        except BaseException:
            pass

        try:
            dest.remove()
        except BaseException:
            pass


#noinspection PyUnresolvedReferences
@pytest.mark.xfail
def test_run_watchdog_during_sharing():
    raise 1


#noinspection PyUnresolvedReferences
@pytest.mark.xfail
def test_run_watchdog_during_downloading():
    raise 1


@pytest.mark.resnotavailable
def test_resource_not_available(tmpdir, supervisors):
    """
    Check what if resource is completely not available -- it raises error very soon after starting
    download process.
    """
    s1, s2, s3 = supervisors

    try:
        s1.copier.get('rbtorrent:%s' % ('abcd' * 10), dest=tmpdir.strpath).wait(timeout=2)
    except rberrors.ResourceNotAvailable as err:
        if err.message != 'Got 0 servers having full copy and 1 waiting for resource ' + \
                          "'abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd'":
            raise


@pytest.mark.resnotavailable
def test_resource_partly_not_available(tmpdir, supervisors, fileGenerator):
    """
    Check what if resource partly is not available -- it raises error very soon after starting
    download process.
    """
    s1, s2, s3 = supervisors
    files = fileGenerator(count=10)

    try:
        resid = s1.copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()
        files[5].remove()

        # First one need because we will want s1 to remove
        # one errored file.
        with pytest.raises(rberrors.Timeout):
            s2.copier.get(resid, dest=tmpdir.join('dest').strpath).wait(timeout=5)

        tmpdir.join('dest').remove()

        try:
            s2.copier.get(resid, dest=tmpdir.join('dest').strpath).wait(timeout=5)
        except rberrors.ResourceNotAvailable as err:
            if err.message != 'Got 0 servers having full copy and 1 waiting for file %r from resource %r' % (
                tmpdir.join('dest').join(files[5].basename), resid.split(':', 1)[1]
            ):
                raise
        else:
            raise Exception('Expected exception was not raised')
    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                [f.remove() for f in files if f.check()]
            except:
                pass

            try:
                tmpdir.join('dest').remove()
            except:
                pass


@pytest.mark.slow
@pytest.mark.resnotavailable
def test_resource_metatorrent_not_available_during_download(tmpdir, supervisors, fileGenerator):
    """
    Check if ResourceNotAvailable will be raised if resource (metatorrent) will become
    not available during downloading.
    """
    s1, s2, s3 = supervisors

    file_ = fileGenerator(count=1)[0]

    try:
        resid = s1.copier.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        s1.setUploadSpeed(1)
        try:
            handle = s2.copier.get(resid, dest=tmpdir.join('dest').strpath)
            std.time.sleep(2)
            file_.remove()

            s1.runWatchDog()
            s1.setUploadSpeed(0)

            try:
                handle.wait(timeout=30)
            except rberrors.ResourceNotAvailable as err:
                if err.message != 'Got 0 servers having full copy and 1 waiting from tracker for %r' % (
                    resid.split(':', 1)[1]
                ):
                    raise
            else:
                raise Exception('Expected exception was not raised')
        finally:
            s1.setUploadSpeed(0)
    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                file_.remove()
            except:
                pass

            try:
                tmpdir.join('dest').remove()
            except:
                pass


@pytest.mark.slow
def test_resource_partly_not_available_during_download(tmpdir, supervisors, fileGenerator):
    s1, s2, s3 = supervisors

    files = fileGenerator(count=10)

    try:
        resid = s1.copier.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        files[5].remove()

        handle = s2.copier.get(resid, dest=tmpdir.join('dest').strpath)
        std.time.sleep(2)

        s1.runWatchDog()

        try:
            handle.wait(timeout=30)
        except rberrors.ResourceNotAvailable as err:
            if err.message != 'Got 0 servers having full copy and 1 waiting from tracker for %r' % (
                tmpdir.join('dest').join(files[5].basename)
            ):
                raise
    finally:
        if CLEAN_AFTER_EACH_RUN:
            try:
                [f.remove() for f in files if f.check()]
            except:
                pass

            try:
                tmpdir.join('dest').remove()
            except:
                pass


@pytest.mark.new
def test_gc_run_during_adding_resource(tmpdir, copierApiFactory, fileGenerator):
    """
    Here we run watchdog very often and run gc during torrents being added to session.
    """

    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=100)
        resid = copier1.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        dest = tmpdir.join('dest').ensure(dir=1)
        handle = copier2.api.get(resid, dest=dest.strpath)
        for i in range(30):
            watchDogRemoved = copier2.runWatchDog()
            assert watchDogRemoved == 0
            std.time.sleep(0.1)
        handle.wait(timeout=60)


# test_not_save_info_about_errored_torrents_during_state_saving
# test_test_remove_file_if_error_during_plaincopy
# test_pyro_connection_retry
# test_errcode_on_sky_unhandled_errors
# test_downloading_already_downloading_torrent_with_files_removed_for_old_torrent
