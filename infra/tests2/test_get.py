import os
import pytest
from py import std

from api.copier import Copier, errors

class cd:
    """
    Temporary change current working directory
    """
    def __init__(self, newPath):
        self.newPath = newPath

    def __enter__(self):
        self.savedPath = os.getcwd()
        os.chdir(self.newPath)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.savedPath)

@pytest.mark.xnew
def test_get(tmpdir, copierApiFactory, fileGenerator):
    """
    Share some files and get them in different daemon.
    """
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=100)
        resid = copier1.api.create([x.strpath for x in files]).resid()

        destA = tmpdir.ensure('dest1', dir=1)
        destB = tmpdir.ensure('dest2', dir=1)

        handler = copier2.api.get(resid, dest=destA.strpath)
        handler2 = copier2.api.get(resid, dest=destB.strpath)

        handler.wait(timeout=30)
        handler2.wait(timeout=30)

        for file_ in files:
            data = file_.open('rb').read()
            assert data == destA.join(file_.strpath).open('rb').read()
            assert data == destB.join(file_.strpath).open('rb').read()

        # Now check if we run watchdog -- nothing should be removed, because
        # files are exist.
        for copier in (copier1, copier2):
            files = copier.api.list(resid).files()
            assert len(files) == 100

            copier.runWatchDog()

            files = copier.api.list(resid).files()
            assert len(files) == 100


@pytest.mark.xnew
def test_all(tmpdir, copierApiFactory, fileGenerator):
    """
    Share 3 chunks by 10 files each and get them all back.
    """
    copier = Copier()
    chunks = [ fileGenerator(count=10, bs='16', nameTemplate='file_%d-%%d' % i) for i in range(0, 3) ]
    resids = [
        copier.create([x.strpath for x in chunk], transport='tgz').resid()
        for chunk in chunks
    ]

    dest = tmpdir.ensure('dest', dir=1)

    get_str = 'all:' + ','.join([ resid for resid in resids ])
    with cd(dest.strpath):
        handler = copier.get(get_str)
        handler.wait()

    for chunk in chunks:
        for file in chunk:
            data = file.open('rb').read()
            assert data == dest.join(file.strpath).open('rb').read()

    pass


@pytest.mark.xnew
def test_any(tmpdir, copierApiFactory, fileGenerator):
    """
    Share 3 chunks by 10 files each and get only first files chunk back.
    """
    copier = Copier()
    chunks = [ fileGenerator(count=10, bs='16', nameTemplate='file_%d-%%d' % i) for i in range(0, 3) ]
    resids = [
        copier.create([x.strpath for x in chunk], transport='tgz').resid()
        for chunk in chunks
    ]

    dest = tmpdir.ensure('dest', dir=1)
    get_str = 'any:tgz:foo,%s,tgz:bar,%s,tgz:zoo,%s' % tuple(resids)
    with cd(dest.strpath):
        handler = copier.get(get_str)
        handler.wait()

    for file in chunks[1] + chunks[2]:
        assert dest.join(file.strpath).check(exists=0)

    for file in chunks[0]:
        data = file.open('rb').read()
        assert data == dest.join(file.strpath).open('rb').read()

    pass

@pytest.mark.xnew
def test_get_many_times(tmpdir, copierApiFactory, fileGenerator):
    """
    Share some files and get them in different daemon several times.
    """
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=100)
        resid = copier1.api.create([x.strpath for x in files]).resid()

        for i in range(10):
            handler = copier2.api.get(resid, dest=tmpdir.ensure('dest', dir=1).strpath)
            handler.wait(timeout=20)

            for file_ in files:
                assert file_.open('rb').read() == tmpdir.join('dest').join(file_.strpath).open('rb').read()


@pytest.mark.xnewy
def test_get_timestamp_more_than_one_second_old(tmpdir, copierApiFactory, fileGenerator):
    """ Check what copier sets modification time of all files for now - 1 second, so
    changing file right after download completes **should** modify that timestamp. Even on platforms
    with int-resolution modification times.
    """
    with copierApiFactory.make(2) as (copier1, copier2):
        file_ = fileGenerator(count=1)[0]
        resid = copier1.api.create([file_.strpath]).resid()

        for i in range(10):
            handler = copier2.api.get(resid, dest=tmpdir.ensure('dest', dir=1).strpath)
            handler.wait(timeout=20)

            dest_file = tmpdir.join('dest').join(file_.strpath)
            #assert file_.open('rb').read() == dest_file.open('rb').read()
            assert 2 > std.time.time() - dest_file.mtime() > 1



@pytest.mark.xnew
@pytest.mark.quick
def test_resource_not_available_error(tmpdir, copierApiFactory):
    """ Check that ResourceNotAvailable error will pop up instantly. """
    with copierApiFactory.make(1) as copier:
        with pytest.raises(errors.ResourceNotAvailable):
            copier.api.get('rbtorrent:%s' % ('a' * 40), dest=tmpdir.strpath).wait(timeout=3)


@pytest.mark.xnew
def test_resource_not_available_error_and_become_available_again(tmpdir, copierApiFactory, fileGenerator):
    """
    This is regression test:
    1) we have resource not available
    2) and soon it will be available again
    3) download should work!
    """
    with copierApiFactory.make(2) as (copier1, copier2):
        file1 = fileGenerator(count=1)[0]
        file2 = file1.dirpath().join(file1.basename + '_new')

        resid = copier1.api.create([file1.strpath]).resid()
        file1.rename(file2)

        watchDogRemoved = copier1.runWatchDog()
        assert watchDogRemoved == 2

        dest = tmpdir.join('dest')

        with pytest.raises(errors.ResourceNotAvailable):
            copier2.api.get(resid, dest=dest.strpath).wait(timeout=10)

        file2.rename(file1)
        assert copier1.api.create([file1.strpath]).resid() == resid

        copier2.api.get(resid, dest=dest.strpath).wait(timeout=10)


@pytest.mark.xnew
def test_downloading_empty_directories_and_files(tmpdir, copierApiFactory, fileGenerator):
    srcDir = tmpdir.join('src').ensure(dir=1)

    file1 = fileGenerator(path=srcDir.basename, count=1)[0]
    file2 = fileGenerator(path=srcDir.basename, count=1, nameTemplate='file2%d')[0]
    file3 = file1.dirpath().join(file1.basename + 'duplicate')
    file4 = file2.dirpath().join(file2.basename + 'duplicate')
    file5 = file1.dirpath().join(file1.basename + 'duplicate2')

    file1.copy(file3)
    file2.copy(file4)
    file1.copy(file5)

    file2.chmod(0755)
    file4.chmod(0755)
    file5.chmod(0755)

    srcDir.join('emptydir').ensure(dir=1)
    srcDir.join('emptyfile').ensure(file=1)
    srcDir.join('emptyfile2').ensure(file=1).chmod(0777)

    with copierApiFactory.make(2) as (copier1, copier2):
        resid = copier1.api.create([srcDir.basename], cwd=srcDir.dirpath().strpath).resid()

        dest = tmpdir.join('dest')
        copier2.api.get(resid, dest=dest.strpath).wait(timeout=10)

        newSrc = dest.join('src')
        assert newSrc.check(exists=1, dir=1)

        newEmptyFile = newSrc.join('emptyfile')
        newEmptyFile2 = newSrc.join('emptyfile2')

        newEmptyDir = newSrc.join('emptydir')
        newFile1 = newSrc.join(file1.basename)
        newFile2 = newSrc.join(file2.basename)
        newFile3 = newSrc.join(file3.basename)
        newFile4 = newSrc.join(file4.basename)
        newFile5 = newSrc.join(file5.basename)

        for src, dst, mode in (
            (file1, newFile1, 0666),
            (file2, newFile2, 0777),
            (file3, newFile3, 0666),
            (file4, newFile4, 0777),
            (file5, newFile5, 0777),
        ):
            assert dst.check(exists=1)
            assert dst.size() == src.size()
            assert dst.read(mode='rb') == src.read(mode='rb')
            stat = dst.stat()
            assert stat.mode & mode == mode

        assert newEmptyFile.check(exists=1, file=1)
        assert newEmptyFile.size() == 0
        assert newEmptyFile.stat().mode & 0666 == 0666

        assert newEmptyFile2.check(exists=1, file=1)
        assert newEmptyFile2.size() == 0
        assert newEmptyFile2.stat().mode & 0777 == 0777

        assert newEmptyDir.check(exists=1, dir=1)
        assert not newEmptyDir.listdir()
        assert newEmptyDir.stat().mode & 0777 == 0777

        # Also check that anything also not appeared in dest path
        assert set([f.basename for f in newSrc.listdir()]) == set([
            newFile1.basename,
            newFile2.basename,
            newFile3.basename,
            newFile4.basename,
            newFile5.basename,
            newEmptyFile.basename,
            newEmptyFile2.basename,
            newEmptyDir.basename,
        ])


@pytest.mark.xnew
def test_dl_resource_with_only_empty_file(tmpdir, copierApiFactory):
    emptyFile = tmpdir.join('empty file').ensure(file=1)

    with copierApiFactory.make(2) as (copier1, copier2):
        resid = copier1.api.create([emptyFile.basename], cwd=tmpdir.strpath).resid()
        dest = tmpdir.join('dest').ensure(dir=1)

        handle = copier2.api.get(resid, dest=dest.strpath)
        handle.wait(timeout=5)

        destFile = dest.join(emptyFile.basename)
        assert destFile.check(exists=1, file=1)
        assert destFile.size() == 0

        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 0

        destFile.remove()
        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 1


@pytest.mark.xnew
def test_dl_resource_with_only_symlink(tmpdir, copierApiFactory):
    symlink = tmpdir.join('symlink file')
    symlink.mksymlinkto('test symlink')

    with copierApiFactory.make(2) as (copier1, copier2):
        resid = copier1.api.createExEx([symlink.basename], cwd=tmpdir.strpath).wait().resid()
        dest = tmpdir.join('dest').ensure(dir=1)

        handle = copier2.api.get(resid, dest=dest.strpath)
        handle.wait(timeout=5)

        destSymlink = dest.join(symlink.basename)
        assert destSymlink.check(link=1), 'Symlink was not created!'
        assert destSymlink.readlink() == 'test symlink'

        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 0

        destSymlink.remove()
        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 1


@pytest.mark.xnew
def test_dl_resource_with_deep_symlink(tmpdir, copierApiFactory):
    source = tmpdir.join('src')
    subdir = source.join('sub1', 'sub2', 'sub3').ensure(dir=1)
    symlink = subdir.join('symlink file')
    subdir.join('test symlink').ensure(file=1).open('wb').write('something usefull')
    symlink.mksymlinkto('test symlink')

    with copierApiFactory.make(2) as (copier1, copier2):
        resid = copier1.api.createExEx(['sub1'], cwd=source.strpath).wait().resid()
        dest = tmpdir.join('dest').ensure(dir=1)

        handle = copier2.api.get(resid, dest=dest.strpath)
        handle.wait(timeout=5)

        destSymlink = dest.join('sub1', 'sub2', 'sub3', symlink.basename)
        assert destSymlink.check(link=1), 'Symlink was not created'
        assert destSymlink.readlink() == 'test symlink'

        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 0

        destSymlink.remove()
        dest.join('sub1', 'sub2', 'sub3', 'test symlink').remove()
        watchDogResult = copier2.runWatchDog()
        assert watchDogResult == 2
