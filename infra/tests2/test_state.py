import pytest
from py import std

from api.copier import errors


@pytest.mark.xnew
def test_torrent_state_save_load(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):

        files = fileGenerator(count=10)

        resid = copier1.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        copier1.stop()
        copier1.run()

        copier1.runWatchDog(expect=0)

        dest = tmpdir.join('dest').ensure(dir=1)
        handle = copier2.api.get(resid, dest=dest.strpath)

        handle.wait(timeout=10)
        dest.remove()

        copier2.runWatchDog(expect=11)

        files[0].remove()
        copier1.runWatchDog(expect=2)

        dest.ensure(dir=1)
        with pytest.raises(errors.ResourceNotAvailable):
            copier2.api.get(resid, dest=dest.strpath).wait(timeout=5)


@pytest.mark.xnew
def test_emptyfile_and_symlink_state_load(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        file_ = tmpdir.join('empty file').ensure(file=1)
        symlink = tmpdir.join('symlink')
        symlink.mksymlinkto(file_, absolute=0)
        invalidSymlink = tmpdir.join('bad symlink')
        invalidSymlink.mksymlinkto('baaad')

        resid1 = copier1.api.create([file_.basename], cwd=tmpdir.strpath).resid()
        resid2 = copier1.api.createExEx([symlink.basename], cwd=tmpdir.strpath).wait().resid()
        resid3 = copier1.api.createExEx([invalidSymlink.basename], cwd=tmpdir.strpath).wait().resid()

        assert resid1 != resid2 != resid3

        copier1.stop()
        copier1.run()

        copier1.runWatchDog(expect=0)

        dest = tmpdir.join('dest').ensure(dir=1)
        copier2.api.get(resid1, dest=dest.strpath).wait(timeout=5)
        copier2.api.get(resid2, dest=dest.strpath).wait(timeout=5)
        copier2.api.get(resid3, dest=dest.strpath).wait(timeout=5)

        dest.remove()
        copier2.runWatchDog(expect=3)  # 3 metatorrents

        file_.remove()
        symlink.remove()
        invalidSymlink.remove()

        copier1.runWatchDog(expect=3)  # 3 metatorrents

        dest.ensure(dir=1)
        for res in resid1, resid2, resid3:
            with pytest.raises(errors.ResourceNotAvailable):
                copier2.api.get(res, dest=dest.strpath).wait(timeout=5)


@pytest.mark.xnew
def test_save_metatorrent_if_some_torrents_errored(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=10)

        resid = copier1.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        stats11 = copier1.stats()
        file0hash = copier1.resourceStats(resid)['torrents'][files[0]]['hash']

        copier1.execute('self.torrentmanager.torrents[%r].state = "Error"' % file0hash)
        copier1.stop()

        copier1.run()
        stats12 = copier1.stats()

        assert stats12['torrentsCnt'] == stats11['torrentsCnt'] - 2  # 1 torrent we Error'd + metatorrent


@pytest.mark.xnew2
@pytest.mark.xfail
def test_plaincopy_state_load(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(3) as (copier1, copier2, copier3):
        file0 = fileGenerator(count=1)[0]
        file1 = tmpdir.join('file1')
        file0.copy(file1)

        resid1 = copier1.api.create([file0.basename], cwd=tmpdir.strpath).resid()
        resid2 = copier1.api.create([file1.basename], cwd=tmpdir.strpath).resid()

        dest1 = tmpdir.join('dest1').ensure(dir=1)
        dest2 = tmpdir.join('dest2').ensure(dir=1)

        copier1.api.get(resid1, dest=dest1.strpath).wait(timeout=10)
        copier1.api.get(resid2, dest=dest1.strpath).wait(timeout=10)
        copier2.api.get(resid1, dest=dest2.strpath).wait(timeout=10)
        copier2.api.get(resid2, dest=dest2.strpath).wait(timeout=10)

        # Rigth now we should have:
        #  copier1: 2 metatorrents + 1 torrent + 3 plaindata copies
        #  copier2: 2 metatorrents + 1 torrent + 1 plaindata copy

        dest2.join('file1').remove()
        copier2.runWatchDog(expect=0)  # plain copy was completed, but torrent still shared from file0

        dest2.remove()
        copier2.runWatchDog(expect=3)  # 2 metatorrents + 1 torrent

        # This test is not complete yet
        assert 0
