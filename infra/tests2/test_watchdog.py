import pytest
from py import std

from api.copier import errors


class ThreadGet(object):
    def __init__(self, handle):
        self.handle = handle
        self.result = None
        self.resultIsError = False
        self.thr = None

    def wait(self, timeout=None):
        if self.thr is None:
            self.thr = std.threading.Thread(target=self._wait, kwargs={'timeout': timeout})
            self.thr.start()

        if self.thr.isAlive():
            return False, None

        if self.resultIsError:
            raise self.result[0], self.result[1], self.result[2]

        return True, self.result

    def _wait(self, *args, **kwargs):
        try:
            self.result = self.handle.wait(*args, **kwargs)
        except Exception as err:
            self.resultIsError = True
            self.result = std.sys.exc_info()


@pytest.mark.xnew
def test_watchdog_run_during_simple_downloading(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=10)
        resid = copier1.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        dest = tmpdir.join('dest').ensure(dir=1)

        handle = copier2.api.get(resid, dest=dest.strpath)

        thr = std.threading.Thread(target=handle.wait, kwargs={'timeout': 10})
        thr.start()

        while thr.isAlive():
            copier1.runWatchDog(expect=0)
            copier2.runWatchDog(expect=0)

        # Ensure what we really wait()-ed for result
        try:
            handle.wait()
        except errors.ApiError as err:
            assert 'already got result' in err.message

        # Check all data
        for f in files:
            assert f.read(mode='rb') == dest.join(f.basename).read(mode='rb')

        files[5].remove()
        dest.remove()

        copier1.runWatchDog(expect=2)
        copier2.runWatchDog(expect=11)


@pytest.mark.xnew
def test_watchdog_run_during_download_with_plaindata(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        for x in range(100):
            tmpdir.join('file%d' % x).ensure(file=1)
            tmpdir.join('symlink%d' % x).mksymlinkto('file%d' % x)
            tmpdir.join('invsymlink%d' % x).mksymlinkto('xx%d' % x)

        resid = copier1.api.createExEx([f.basename for f in tmpdir.listdir()], cwd=tmpdir.strpath).wait().resid()

        dest = tmpdir.join('dest').ensure(dir=1)

        handle = copier2.api.get(resid, dest=dest.strpath)
        thr = std.threading.Thread(target=handle.wait, kwargs={'timeout': 5})
        thr.start()

        while thr.isAlive():
            for copier in copier1, copier2:
                copier.runWatchDog(expect=0)

        # Ensure what we really wait()-ed for result
        try:
            handle.wait()
        except errors.ApiError as err:
            assert 'already got result' in err.message

        # Check all data
        for f in tmpdir.listdir():
            if f.basename == 'dest':
                continue

            nf = dest.join(f.basename)
            if f.check(link=1):
                assert f.readlink() == nf.readlink()
            else:
                assert f.size() == nf.size() == 0

        tmpdir.join('symlink50').remove()
        dest.join('invsymlink50').remove()

        copier1.runWatchDog(expect=1)
        copier2.runWatchDog(expect=1)

        dest.remove()
        copier2.runWatchDog(expect=0)


@pytest.mark.xnew
@pytest.mark.slow
@pytest.mark.xfail
def test_watchdog_run_during_plain_copy(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        files = fileGenerator(count=1, bs='5M')
        data = files[0].read(mode='rb')
        for i in range(1, 32):
            file_ = tmpdir.join('file%d' % i)
            fp = file_.open(mode='wb')
            fp.write('fp%d' % i)
            fp.write(data[:-fp.tell()])
            files.append(file_)

        resid = copier1.api.create([f.basename for f in files], cwd=files[0].dirpath().strpath).resid()

        dest1 = tmpdir.join('dest1').ensure(dir=1)
        dest2 = tmpdir.join('dest2').ensure(dir=1)
        dest3 = tmpdir.join('dest3').ensure(dir=1)

        handle1 = ThreadGet(copier1.api.get(resid, dest=dest1.strpath))
        handle2 = ThreadGet(copier2.api.get(resid, dest=dest2.strpath))
        handle3 = ThreadGet(copier2.api.get(resid, dest=dest3.strpath))

        active = set((handle1, handle2, handle3))

        while active:
            for h in active.copy():
                ready, result = h.wait(timeout=40)
                if ready:
                    active.remove(h)

                for copier in (copier1, copier2):
                    copier.runWatchDog(expect=0)

        # Check files
        for f in files:
            assert f.read(mode='rb') == \
                   dest1.join(f.basename).read(mode='rb') == \
                   dest2.join(f.basename).read(mode='rb') == \
                   dest3.join(f.basename).read(mode='rb')

        dest3.remove()
        dest2.remove()
        copier2.runWatchDog(expect=33)

        dest1.remove()
        copier1.runWatchDog(expect=0)

        [f.remove() for f in files]
        copier1.runWatchDog(expect=33)


@pytest.mark.xnew
@pytest.mark.slow
def test_watchdog_run_during_plain_copy_with_different_resources(tmpdir, copierApiFactory, fileGenerator):
    with copierApiFactory.make(2) as (copier1, copier2):
        file0 = fileGenerator(count=1, bs='1M')[0]
        data = file0.read(mode='rb')
        file0fp = file0.open(mode='ab')
        for i in range(127):
            file0fp.write(data)

        file1 = tmpdir.join('file1')
        file0.copy(file1)

        resid1 = copier1.api.create([file0.basename], tmpdir.strpath).resid()
        resid2 = copier1.api.create([file1.basename], tmpdir.strpath).resid()

        dest = tmpdir.join('dest').ensure(dir=1)

        handle1 = ThreadGet(copier2.api.get(resid1, dest=dest.strpath))
        handle2 = ThreadGet(copier2.api.get(resid2, dest=dest.strpath))

        active = set((handle1, handle2))

        while active:
            for h in active.copy():
                ready, result = h.wait(timeout=60)
                if ready:
                    active.remove(h)

                copier2.runWatchDog(expect=0)

        df = file0.read(mode='rb')
        assert df == file1.read(mode='rb')
        assert df == dest.join('file0').read(mode='rb')
        assert df == dest.join('file1').read(mode='rb')
