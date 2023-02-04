from py import std
import pytest
import time

from api.copier import errors


@pytest.mark.xnew
def test_share(copierApiFactory, fileGenerator):
    """
    Simple test -- just try to share something and grab resource id.
    After sharing perform Copier().list() and see if we have all our files really
    shared.
    """
    with copierApiFactory.make(1) as copier:
        files = fileGenerator(count=100)
        resid = copier.api.create([x.strpath for x in files]).resid()

        assert sorted(copier.api.list(resid).files(), key=lambda x: x['name']) == [
            {
                'name': f.strpath.lstrip('/'),
                'md5sum': std.hashlib.md5(f.read()).hexdigest(),
                'size': 16 * 1024,
                'executable': False,
                'type': 'file'
            } for f in sorted(files)
        ]


@pytest.mark.xnew
def test_multiple_share(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(1) as copier:
        files = fileGenerator(count=100)

        resid = None
        for i in range(10):
            newResid = copier.api.create([x.strpath for x in files]).resid()
            if resid:
                assert newResid == resid
            resid = newResid


@pytest.mark.xnew
def test_multiple_share_different_files(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(1) as copier:
        allFiles = []
        resid = None
        for i in range(10):
            st = time.time()
            files = fileGenerator(count=100)
            allFiles.extend(files)
            newResid = copier.api.create([x.strpath for x in files]).resid()
            assert newResid != resid
            resid = newResid
            sleepfor = 1 - (time.time() - st)
            if sleepfor > 0:
                time.sleep(sleepfor)


@pytest.mark.xnew
def test_threaded_share(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(1) as copier:
        files = fileGenerator(count=1)

        threads = []
        results = []

        def _shareFiles():
            results.append(copier.api.create([x.strpath for x in files]).resid())

        for i in range(300):
            threads.append(std.threading.Thread(target=_shareFiles))

        [thread.start() for thread in threads]
        [thread.join() for thread in threads]

        assert len(results) == 300, 'Not all threads have results (%d of 30 okay)' % len(results)
        assert len(set(results)) == 1, 'Got %d different resources while sharing same files' % len(set(results))
        assert len(results[0]) == len('rbtorrent:%s' % ('x' * 40)), 'Share results does not look like resource ids'


@pytest.mark.xnew
def test_threaded_share_different_files(tmpdir, copierApiFactory, fileGenerator):
    """
    Try to share 100 * 30 files in 30 different threads.
    This will also check share results with Copier.list() as well.
    """

    with copierApiFactory.make(1) as copier:
        threads = []
        results = []
        allFiles = []

        for i in range(300):
            files = fileGenerator(count=3, nameTemplate='%destFile%%d' % i)
            allFiles.extend(files)

            def _makeSharer(files):
                def _shareFiles():
                    results.append(copier.api.create([x.strpath for x in files]).resid())
                return _shareFiles
            threads.append(std.threading.Thread(target=_makeSharer(files)))

        [thread.start() for thread in threads]
        [thread.join() for thread in threads]

        assert len(results) == 300, 'Not all threads have results (%d of 30 okay)' % len(results)
        assert len(set(results)) == 300
        assert all([len(r) == len('rbtorrent:%s' % ('x' * 40)) for r in results])

        leftFiles = [f.strpath for f in allFiles]

        for resid in results:
            sharedFiles = copier.api.list(resid).files()
            for sharedPathDesc in sharedFiles:
                sharedPath, sharedSize = sharedPathDesc['name'], sharedPathDesc['size']
                assert '/' + sharedPath in leftFiles
                assert sharedSize == 16384
                leftFiles.remove('/' + sharedPath)

        assert len(leftFiles) == 0, 'Some (%d) files were not shared!' % len(leftFiles)


@pytest.mark.xnew
@pytest.mark.xfail
@pytest.mark.quick
def test_share_colliding_file(tmpdir, copierApiFactory, fileGenerator):
    """ THIS TEST IS CURRENTLY BROKEN ON PLATFORMS WITHOUT HIGH RESOLUTION OF SYSTEM time().
        TODO: we should take into account ctime, so this will be able to work
    """

    with copierApiFactory.make(1) as copier:
        #sharedBefore = s1.execute('result = len(self.torrentmanager.torrents)')

        generated = set()
        file_ = None
        for i in range(100):
            file_ = fileGenerator(count=1)[0]
            try:
                resid = copier.api.create([file_.strpath]).resid()
            except:
                from kernel.util.errors import formatException
                print formatException()

            assert resid not in generated
            generated.add(resid)
            #s1.runWatchDog()
            #assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore + 2

        #file_.remove()
        #s1.runWatchDog()
        #assert s1.execute('result = len(self.torrentmanager.torrents)') == sharedBefore


@pytest.mark.xnew
@pytest.mark.quick
def test_share_colliding_file_update_timestamp(tmpdir, copierApiFactory, fileGenerator):
    """
    This tests what if we want share file already shared timestamp saved
    in Torrent object will be updated to new one.
    """

    with copierApiFactory.make(1) as copier:
        file_ = fileGenerator(count=1)[0]
        resid1 = copier.api.create([file_.strpath]).resid()

        file_.setmtime(123)
        resid2 = copier.api.create([file_.strpath]).resid()

        assert resid1 == resid2

        assert copier.execute('''
            metatorrent = self.torrentmanager.torrents[%r]
            result = self.torrentmanager.torrents[metatorrent.torrents.keys()[0]].tsFinished.value
        ''' % resid2[len('rbtorrent:'):]) == 123.0


@pytest.mark.xnew
@pytest.mark.quick
def test_share_nothing(tmpdir, copierApiFactory):
    with copierApiFactory.make(1) as copier:
        with pytest.raises(errors.ApiError) as err:
            print copier.api.create([]).resid()
        assert err.value.message == 'You cant make resource with no contents (no files nor directories)'


@pytest.mark.xnew
def test_share_same_resid(tmpdir, copierApiFactory, fileGenerator):
    """
    Check what if we share files, symlinks, empty files and dirs several times --
    all times we will have the same resource id.
    """

    with copierApiFactory.make(2) as copiers:
        [c.runWatchDog() for c in copiers]
        stats = [c.stats() for c in copiers]

        N = 5

        emptyFiles = [tmpdir.join('empty file %s' % i).ensure(file=1) for i in range(N)]
        emptyDirs = [tmpdir.join('empty dir %s' % i).ensure(dir=1) for i in range(N)]
        someFiles = fileGenerator(count=N)

        [tmpdir.join('symlink to empty %s' % i).mksymlinkto(emptyFiles[i], absolute=0) for i in range(N)]
        [tmpdir.join('symlink to empty dir %s' % i).mksymlinkto(emptyDirs[i], absolute=0) for i in range(N)]
        [tmpdir.join('file symlink %s' % i).mksymlinkto(someFiles[i], absolute=0) for i in range(N)]
        [tmpdir.join('invalid symlink %s' % i).mksymlinkto('some invalid link %s' % i) for i in range(N)]
        [
            tmpdir.join('sub1').join('sub2').ensure(dir=1).join('link %s' % i).
            mksymlinkto(someFiles[i], absolute=0) for i in range(N)
        ]
        [
            tmpdir.join('sub1').join('sub2').ensure(dir=1).join('invalid link %s' % i).
            mksymlinkto('deep invalid link %s' % i) for i in range(N)
        ]

        allSymlinks = [f for f in tmpdir.listdir() if f.check(link=1)]
        allSymlinks += [f for f in tmpdir.join('sub1', 'sub2').listdir() if f.check(link=1)]
        assert len(allSymlinks) == N * 6

        allFiles = emptyFiles + emptyDirs + someFiles + allSymlinks

        resids = [
            c.api.createExEx(
                [x.strpath[len(tmpdir.strpath) + 1:] for x in allFiles],
                cwd=tmpdir.strpath
            ).wait().resid() for c in copiers
        ]
        assert resids[0] == resids[1]
        resid1 = resids[0]

        # Change some mtimes, resids should not differ
        for fileSet in (emptyFiles, someFiles):
            for x in fileSet:
                x.setmtime(std.random.randrange(10, int(x.stat().mtime)))

            resids = [
                c.api.createExEx(
                    [x.strpath[len(tmpdir.strpath) + 1:] for x in allFiles],
                    cwd=tmpdir.strpath
                ).wait().resid() for c in copiers
            ]
            assert resids[0] == resids[1]
            resid2 = resids[0]

            assert resid2 == resid1

        for idx, copier in enumerate(copiers):
            curStats = copier.stats()
            assert curStats['torrentsCnt'] == stats[idx]['torrentsCnt'] + N + 1

        # If we remove empty files or symlinks or directories -- resource still should be here!
        [f.remove() for f in emptyFiles]
        [f.remove() for f in emptyDirs]
        [f.remove() for f in allSymlinks]

        for idx, copier in enumerate(copiers):
            watchDogResult = copier.runWatchDog()
            assert watchDogResult == 0
            curStats = copier.stats()
            assert curStats['torrentsCnt'] == stats[idx]['torrentsCnt'] + N + 1

        # But if we remove some data file -- resource should dissapear
        someFiles[0].remove()

        for idx, copier in enumerate(copiers):
            watchDogResult = copier.runWatchDog()
            assert watchDogResult == 2  # torrent we removed + metatorrent
            curStats = copier.stats()
            assert curStats['torrentsCnt'] == stats[idx]['torrentsCnt'] + N - 1


@pytest.mark.xnew
def test_share_only_empty_files(tmpdir, copierApiFactory):
    """
    Check what if we have resource without any torrents at all --
    it will be removed if empty files removed.
    """
    N = 5

    with copierApiFactory.make(1) as copier:
        copier.runWatchDog()
        #stats = copier.stats()

        emptyFiles = [tmpdir.join('empty %s' % i).ensure(file=1) for i in range(N)]
        [tmpdir.join('symlink %s' % i).mksymlinkto(emptyFiles[i]) for i in range(N)]
        [tmpdir.join('symlink bad %s' % i).mksymlinkto('bad %s' % i) for i in range(N)]
        goodSymlinks = [f for f in tmpdir.listdir() if f.check(link=1, exists=1)]
        badSymlinks = [f for f in tmpdir.listdir() if f.check(link=1) and f not in goodSymlinks]

        resid1 = copier.api.create([f.basename for f in emptyFiles[:-1]], cwd=tmpdir.strpath).resid()
        resid2 = copier.api.create([f.basename for f in emptyFiles], cwd=tmpdir.strpath).resid()
        resid3 = copier.api.create([f.strpath for f in emptyFiles]).resid()
        resid4 = copier.api.createExEx([f.basename for f in goodSymlinks], cwd=tmpdir.strpath).wait().resid()
        resid5 = copier.api.createExEx([f.basename for f in badSymlinks], cwd=tmpdir.strpath).wait().resid()

        assert resid1 != resid2 != resid3 != resid4 != resid5

        watchDogResult = copier.runWatchDog()
        assert watchDogResult == 0
        emptyFiles[0].remove()
        goodSymlinks[0].remove()
        badSymlinks[0].remove()

        watchDogResult = copier.runWatchDog()
        assert watchDogResult == 5  # 5 metatorrents should be removed


@pytest.mark.xnew
def test_share_empty_file(tmpdir, copierApiFactory):
    with copierApiFactory.make(1) as copier:
        copier.runWatchDog()

        file_ = tmpdir.join('empty file xx')
        file_.write('', mode='wb')

        stats = copier.stats()

        resid1 = copier.api.create([file_.basename], cwd=tmpdir.strpath).resid()
        stats1 = copier.stats()

        # We should have +1 in torrents and plaindata
        assert stats1['torrentsCnt'] == stats['torrentsCnt'] + 1

        # And that should remain after watchdog run!
        assert copier.runWatchDog() == 0
        stats2 = copier.stats()
        assert stats1 == stats2

        # If we change file's mtime -- plaindata and metatorrent should NOT be removed!
        file_.setmtime(123)
        watchDogRemoved = copier.runWatchDog()
        assert watchDogRemoved == 0

        stats3 = copier.stats()
        assert stats3 == stats1

        # Now, try to share the same file again! Resid must not change!
        resid2 = copier.api.create([file_.basename], cwd=tmpdir.strpath).resid()
        assert resid2 == resid1

        stats4 = copier.stats()
        assert stats4 == stats1

        file_.remove()
        watchDogRemoved = copier.runWatchDog()
        assert watchDogRemoved == 1  # metatorrent only

        stats5 = copier.stats()
        assert stats5 == stats


@pytest.mark.xnew
@pytest.mark.quick
def test_share_with_one_empty_file_and_many_torrents(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(1) as copier:
        copier.runWatchDog()
        stats = copier.stats()

        files = fileGenerator(count=100)
        files[50].write('', mode='wb')

        copier.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        stats1 = copier.stats()
        assert stats1['torrentsCnt'] == stats['torrentsCnt'] + 100

        watchDogResult = copier.runWatchDog()
        assert watchDogResult == 0

        # Removing empty file should not affect resource
        files[50].remove()

        watchDogResult = copier.runWatchDog()
        stats2 = copier.stats()

        assert watchDogResult == 0
        assert stats2['torrentsCnt'] == stats['torrentsCnt'] + 100

        # .. but normal torrent - should
        files[49].remove()

        watchDogResult = copier.runWatchDog()
        stats3 = copier.stats()

        assert watchDogResult == 2
        assert stats3['torrentsCnt'] == stats['torrentsCnt'] + 98


@pytest.mark.xnew
def test_share_same_file_several_times_from_different_locations(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        copier1.runWatchDog()

        stats1 = copier1.stats()

        resid = None
        files = [fileGenerator(count=10)]

        for i in range(10):
            dest = tmpdir.join('src%d' % i).ensure(dir=1)
            fileset = []
            for f in files[0]:
                nf = dest.join(f.basename)
                f.copy(nf)
                fileset.append(nf)
            files.append(fileset)

        resids = set()
        for fileset in files:
            resid = copier1.api.create([f.basename for f in fileset], cwd=fileset[0].dirpath().strpath).resid()
            resids.add(resid)
        assert len(resids) == 1

        stats11 = copier1.stats()
        assert stats11['torrentsCnt'] == stats1['torrentsCnt'] + 11

        for fileset in files[:-1]:
            [f.remove() for f in fileset]

        watchDogResult1 = copier1.runWatchDog()
        assert watchDogResult1 == 0

        dest = tmpdir.join('dest').ensure(dir=1)
        copier2.api.get(resid, dest=dest.strpath).wait(timeout=20)

        for f in files[-1]:
            df = dest.join(f.basename)
            assert df.check(exists=1)
            assert df.size() == f.size()
            assert f.read(mode='rb') == df.read(mode='rb')


@pytest.mark.xnew
def test_share_downloading_file(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        stats1, stats2 = [c.stats() for c in (copier1, copier2)]

        file_ = fileGenerator(count=1, bs='10M')[0]
        resid = copier1.api.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        copier1.setSpeed(up=1024 * 1024)

        dest = tmpdir.join('dest')
        destFile = dest.join(file_.basename)
        handle = copier2.api.get(resid, dest=dest.strpath)

        for i in range(20):
            status = copier2.resourceStats(resid)
            if status['torrents'][destFile]['totalPayloadDownload'] < 200 * 1024:
                std.time.sleep(0.5)
                continue
            else:
                break

        assert status['torrents'][destFile]['totalPayloadDownload'] >= 200 * 1024
        assert status['torrents'][destFile]['totalPayloadDownload'] < 5 * 1024 * 1024

        # Force flushing data to disk..
        torrentHash = status['torrents'][destFile]['hash']
        copier2.execute('t = self.torrentmanager.torrents[%r]; t.pause(); t.resume()' % torrentHash)

        # And wait untill it will be flushed max 1 second
        for i in range(10):
            if not destFile.size() >= 200 * 1024:
                std.time.sleep(0.1)
                continue

        assert destFile.size() >= 200 * 1024

        # Now replace not yet downloaded file by finished one and reshare.
        file_.copy(destFile)
        copier2.api.create([destFile.basename], cwd=dest.strpath).resid()

        # Torrent should be immidiately marked as finished
        assert copier2.execute('''
            t = self.torrentmanager.torrents[%r]
            result = t.tsFinished.value, t.tsFinished.exception, t.state
        ''' % torrentHash) == (destFile.stat().mtime, None, 'Seeding')

        # Old handle used for downloading also should be immidiately marked as finished
        handle.wait(timeout=0.1)

        for op in (lambda: None, lambda: [c.runWatchDog() for c in (copier1, copier2)]):
            op()
            stats11, stats22 = [c.stats() for c in (copier1, copier2)]
            assert stats11['torrentsCnt'] == stats1['torrentsCnt'] + 2
            assert stats22['torrentsCnt'] == stats2['torrentsCnt'] + 2

        assert file_.read(mode='rb') == destFile.read(mode='rb')


@pytest.mark.xnew
def test_share_downloading_file_from_different_location(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        stats1, stats2 = [c.stats() for c in (copier1, copier2)]

        file_ = fileGenerator(count=1, bs='10M')[0]
        resid = copier1.api.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        copier1.setSpeed(up=1024 * 1024 * 1)

        dest = tmpdir.join('dest')
        destFile = dest.join(file_.basename)
        destFile2 = dest.join(file_.basename + '2')
        handle = copier2.api.get(resid, dest=dest.strpath)

        for i in range(20):
            status = copier2.resourceStats(resid)
            if status['torrents'][destFile]['totalPayloadDownload'] < 200 * 1024:
                std.time.sleep(0.3)
                continue
            else:
                break

        assert status['torrents'][destFile]['totalPayloadDownload'] >= 200 * 1024
        assert status['torrents'][destFile]['totalPayloadDownload'] < 5 * 1024 * 1024

        # Force flushing data to disk
        torrentHash = status['torrents'][destFile]['hash']
        copier2.execute('t = self.torrentmanager.torrents[%r]; t.pause(); t.resume()' % torrentHash)

        # And wait utill it will be flushed, max 1 second
        for i in range(10):
            if not destFile.size() >= 200 * 1024:
                std.time.sleep(0.1)
                continue

        assert destFile.size() >= 200 * 1024

        file_.copy(destFile2)

        copier2.api.create([destFile2.basename], cwd=dest.strpath).resid()

        mtime, exc, state = copier2.execute('''
            t = self.torrentmanager.torrents[%r]
            result = t.tsFinished.value, t.tsFinished.exception, t.state
        ''' % torrentHash)
        assert (mtime, exc, state) == (destFile2.stat().mtime, None, 'Seeding')

        # Old handle used for downloading also should be very soon copied to original location
        handle.wait(timeout=1)

        data = file_.read(mode='rb')
        for f in destFile, destFile2:
            assert f.check(exists=1)
            assert f.size() == len(data)
            assert f.read(mode='rb') == data

        for op in (lambda: None, lambda: [c.runWatchDog() for c in (copier1, copier2)]):
            op()
            stats11, stats22 = [c.stats() for c in (copier1, copier2)]
            assert stats11['torrentsCnt'] == stats1['torrentsCnt'] + 2
            assert stats22['torrentsCnt'] == stats2['torrentsCnt'] + 3  # 2 metaresources here


@pytest.mark.xnew
def test_if_fs_broken_in_metacaches(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(1) as copier:
        file_ = fileGenerator(count=1)[0]
        print file_

        def _reshare():
            return copier.api.create([file_.basename], cwd=file_.dirpath().strpath).resid()

        resid = _reshare()

        metaDir = copier.workdir.join('metacache')
        subDir = metaDir.join(resid.split(':', 1)[1][:2])
        hashDir = subDir.join(resid.split(':', 1)[1])
        metaFile = hashDir.join('metadata')

        def _checkAll():
            assert metaDir.check(dir=1)
            assert subDir.check(dir=1)
            assert hashDir.check(dir=1)
            assert metaFile.check(file=1)

        def _clean():
            file_.setmtime(std.random.randrange(1000, 9999))
            copier.runWatchDog('forceAll=1, forceMetaCleanup=1', expect=2)
            assert metaDir.check(dir=1)
            assert subDir.check(exists=0)

        _checkAll()
        _clean()

        for testPath in metaFile, hashDir, subDir:
            # Make a directory
            testPath.ensure(dir=1)
            assert _reshare() == resid
            _checkAll()
            _clean()

            # Next try to make it a symbolic link
            testPath.dirpath().ensure(dir=1)
            testPath.mksymlinkto('non existent!')
            assert _reshare() == resid
            _checkAll()
            _clean()

            # Next try to make it proper symbolic link
            testPath.dirpath().ensure(dir=1)
            testPath.mksymlinkto('/etc/hosts')
            assert _reshare() == resid
            _checkAll()
            _clean()

            # Finally, make it simple file
            testPath.ensure(file=1).write('asd', mode='wb')
            assert _reshare() == resid
            _checkAll()
            _clean()


@pytest.mark.xneww
def test_reshare_after_get_and_restart(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=10)
        resid = copier1.api.create([x.basename for x in files], cwd=tmpdir.strpath).resid()

        dest = tmpdir.ensure('dest', dir=1)
        handler = copier2.api.get(resid, dest=dest.strpath)
        handler.wait(timeout=30)

        resid2 = copier2.api.create([x.basename for x in files], cwd=dest.strpath).resid()
        assert resid == resid2

        copier2.stop()
        copier2.run()

        resid3 = copier2.api.create([x.basename for x in files], cwd=dest.strpath).resid()
        assert resid == resid3
