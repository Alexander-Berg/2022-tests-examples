import pytest
import errno
import kern


def cached_pages(f):
    return kern.fcntl(f.fileno(), kern.F_GET_CACHED_PAGES)


def test_cache(make_temp_file):
    f = make_temp_file()
    try:
        assert cached_pages(f) == 0
    except OSError as e:
        if e.errno == errno.EINVAL:
            pytest.xfail()
        raise

    f.truncate(16)
    f.read()
    assert cached_pages(f) == 1

    f.truncate(1 << 20)
    f.read()
    assert cached_pages(f) == 257  # FIXME what 1 extra page?

    kern.fadvise(f.fileno(), 0, 0, kern.FADV_DONTNEED)
    assert cached_pages(f) == 0
