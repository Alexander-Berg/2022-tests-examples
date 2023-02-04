import gevent
import logging
import py
import pyjack
import pytest
import threading
import time

from skybone.rbtorrent import utils as rbutils


def test_human_size():
    hs = rbutils.human_size

    kb = 1024
    mb = kb * 1024
    gb = mb * 1024
    tb = gb * 1024

    assert hs(-1) == '0'
    assert hs(0) == '0'
    assert hs(123) == '123B'
    assert hs(123 * kb + 120) == '123.12KiB'
    assert hs(123 * mb + 120 * kb) == '123.12MiB'
    assert hs(1.5 * gb) == '1.50GiB'
    assert hs(15.2 * gb) == '15.20GiB'
    assert hs(123.12 * gb) == '123GiB'
    assert hs(1.5 * tb) == '1.50TiB'
    assert hs(15.2 * tb) == '15.20TiB'
    assert hs(123.12 * tb) == '123TiB'


def test_human_time():
    ht = rbutils.human_time

    assert ht(0) == '0ms'
    assert ht(0.5) == '500ms'
    assert ht(1) == '1s'
    assert ht(55) == '55s'
    assert ht(120) == '2m00s'
    assert ht(125) == '2m05s'
    assert ht(150) == '2m30s'
    assert ht(180) == '3m00s'
    assert ht(86399) == '23h59m59s'
    assert ht(86400) == '1d00h00m00s'
    assert ht(86400 * 100500) == '100500d00h00m00s'


def test_human_speed():
    hs = rbutils.human_speed

    assert hs(-10) == '0'
    assert hs(0) == '0'
    assert hs(1) == '0'
    assert hs(2) == '0.02kbps'
    assert hs(123 / 8) == '0.12kbps'
    assert hs(123123 / 8) == '123kbps'
    assert hs(123123123 / 8) == '123Mbps'


def test_slotted_dict():
    md = rbutils.SlottedDict({'some': {'sub': 'key'}})
    assert md['some']['sub'] == md.some.sub == 'key'

    md.some.sub = 'yek'

    assert md['some']['sub'] == md.some.sub == 'yek'

    assert md == {'some': {'sub': 'yek'}}
    assert md.some == md['some'] == {'sub': 'yek'}


def test_getaddrinfo_g():
    import _socket
    meth = rbutils.getaddrinfo_g

    any_ip = ['1.2.3.4']
    fake_ip = ['']

    def _res(hostname, port):
        assert port == 0
        if hostname == 'any.yandex.ru':
            if isinstance(any_ip[0], Exception):
                raise any_ip[0]
            return [['', '', '', '', [any_ip[0]]]]

        assert hostname.startswith('thost')
        return [['', '', '', '', [fake_ip[0]]]]

    # In other ips resolving should be as usual
    fake_ip[0] = '1.2.3.5'
    assert meth('thost1', 0, getaddrinfo=_res) == [['', '', '', '', [fake_ip[0]]]]

    # If we will return any.yandex.ru ip -- it should raise gaierror
    fake_ip[0] = '1.2.3.4'
    with pytest.raises(_socket.gaierror):
        meth('thost2', 0, getaddrinfo=_res)

    # Also any.yandex.ru could be not found at all
    any_ip[0] = _socket.gaierror(_socket.EAI_NONAME, 'Host was not found')

    # We also check anyips caching here
    with pytest.raises(_socket.gaierror):
        meth('thost3', 0, getaddrinfo=_res)

    del rbutils.getaddrinfo.anyips
    assert meth('thost4', 0, getaddrinfo=_res) == [['', '', '', '', [fake_ip[0]]]]

    # Also we could be unable to resolve any.yandex.ru with some different error
    class PEX(Exception):
        pass

    del rbutils.getaddrinfo.anyips
    any_ip[0] = PEX()
    with pytest.raises(PEX):
        meth('thost5', 0, getaddrinfo=_res)

    any_ip[0] = _socket.gaierror(100500, 'aaa')
    try:
        meth('thost6', 0, getaddrinfo=_res)
    except _socket.gaierror as ex:
        if ex.errno != 100500:
            raise


def test_gethostbyaddr_g():
    meth = rbutils.gethostbyaddr_g

    def _res(*args, **kwargs):
        assert args == (1, )
        assert kwargs == {'a': 2}
        return 3

    assert meth(1, a=2, gethostbyaddr=_res) == 3

    class MyEx(Exception):
        pass

    def _res():
        raise MyEx('errr')

    with pytest.raises(MyEx):
        meth(gethostbyaddr=_res)

    def _res():
        return gevent.getcurrent(), threading.currentThread()

    grn1, thr1 = _res()
    grn2, thr2 = meth(gethostbyaddr=_res)

    assert grn1 != grn2
    assert thr1 != thr2
    assert thr2.name == 'gethostbyaddr'

    for i in range(10):
        if thr2.is_alive():
            time.sleep(0.1)

    assert not thr2.is_alive()


def test_fastbonize_hostname():
    meth = rbutils.fastbonize_ip
    meth = rbutils.fastbonize_hostname
    import _socket

    fake_ip = '1.2.3.4'
    fake_getaddrinfo = {}

    def _getaddrinfo_fake(hostname, a, b, c, getaddrinfo):
        assert getaddrinfo == _socket.getaddrinfo
        value = fake_getaddrinfo[hostname]
        if isinstance(value, Exception):
            raise value
        return value

    noaddr_ex = _socket.gaierror(_socket.EAI_NONAME, 'Name or service not known')
    noaddr_ex2 = _socket.gaierror(_socket.EAI_NODATA, 'No data')

    try:
        getaddrinfo_g = pyjack.replace_all_refs(rbutils.getaddrinfo_g, _getaddrinfo_fake)

        log = logging.getLogger('fastbonize_ip')

        # CASE1: got thost.yandex.ru
        #            fastbone.thost.yandex.ru not available
        #            fb-thost.yandex.ru not available
        #            thost.fb.yandex.ru not available
        log.debug('CASE1')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['fb-thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['thost.fb.yandex.ru'] = noaddr_ex
        assert meth('thost.yandex.ru', log) == (None, None)

        # CASE2: got thost.yandex.ru
        #            fastbone.thost.yandex.ru not available (v2)
        #            fb-thost.yandex.ru not available (v2)
        #            thost.fb.yandex.ru not available
        log.debug('CASE2')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = noaddr_ex2
        fake_getaddrinfo['fb-thost.yandex.ru'] = noaddr_ex2
        fake_getaddrinfo['thost.fb.yandex.ru'] = noaddr_ex
        assert meth('thost.yandex.ru', log) == (None, None)

        # CASE3: got thost.yandex.ru
        #            fastbone.thost.yandex.ru not available
        #            fb-thost.yandex.ru not available
        #            thost.fb.yandex.ru available
        log.debug('CASE3')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['fb-thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['thost.fb.yandex.ru'] = [
            (_socket.AF_INET, '', '', '', ['1']),
            (_socket.AF_INET6, '', '', '', ['2'])
        ]
        assert meth('thost.yandex.ru', log) == ('thost.fb.yandex.ru', [['1'], ['2']])

        # CASE4: got thost.yandex.ru
        #           fb-thost.yandex.ru available
        #           thost.fb.yandex.ru available
        log.debug('CASE4')
        fake_getaddrinfo['fb-thost.yandex.ru'] = fake_getaddrinfo['thost.fb.yandex.ru']
        assert meth('thost.yandex.ru', log) == ('fb-thost.yandex.ru', [['1'], ['2']])

        # CASE5: got thost.yandex.ru
        #           fastbone.thost.yandex.ru available
        #           fb-thost.yandex.ru available
        #           thost.fb.yandex.ru available
        log.debug('CASE5')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = fake_getaddrinfo['thost.fb.yandex.ru']
        assert meth('thost.yandex.ru', log) == ('fb-thost.yandex.ru', [['1'], ['2']])

        # CASE6: got thost.yandex.ru
        #           fastbone.thost.yandex.ru available
        #           fb-thost.yandex.ru not available
        #           thost.fb.yandex.ru available
        log.debug('CASE6')
        fake_getaddrinfo['fb-thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['thost.yandex.ru'] = noaddr_ex
        assert meth('thost.yandex.ru', log) == ('fastbone.thost.yandex.ru', [['1'], ['2']])

        # CASE7: got thost
        #           fastbone.thost not available
        #           fb-thost not available
       #           thost.fb.yandex.ru available
        log.debug('CASE7')
        fake_getaddrinfo.pop('fb-thost.yandex.ru')
        fake_getaddrinfo['fastbone.thost'] = noaddr_ex
        fake_getaddrinfo['fb-thost'] = noaddr_ex
        assert meth('thost', log) == ('thost.fb.yandex.ru', [['1'], ['2']])

        # CASE8: got thost.yandex.net
        #           fastbone.thost.yandex.net not available
        #           fb-thost.yandex.net not available
        log.debug('CASE8')
        fake_getaddrinfo.clear()
        fake_getaddrinfo['fastbone.thost.yandex.net'] = noaddr_ex
        fake_getaddrinfo['fb-thost.yandex.net'] = noaddr_ex
        assert meth('thost.yandex.net', log) == (None, None)

        # CASE9: got thost.yandex.net
        #           getaddrinfo raised weird exception
        log.debug('CASE9')

        class FEX(Exception):
            pass

        fake_getaddrinfo['fb-thost.yandex.net'] = _socket.gaierror(100500, 'ff')
        try:
            meth('thost.yandex.net', log)
        except _socket.gaierror as ex:
            if ex.errno != 100500:
                raise

        fake_getaddrinfo['fb-thost.yandex.net'] = FEX()
        with pytest.raises(FEX):
            meth('thost.yandex.net', log)

        # CASE10: got thost.yandex.ru (SKYDEV-459)
        #            fastbone.thost.yandex.ru available, but result is the same as thost.yandex.ru
        #            fb-thost.yandex.ru not available
        #            thost.fb.yandex.ru not available
        log.debug('CASE10')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = [
            (_socket.AF_INET, '', '', '', [fake_ip]),
            (_socket.AF_INET6, '', '', '', ['2'])
        ]
        fake_getaddrinfo['fb-thost.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['thost.fb.yandex.ru'] = noaddr_ex
        fake_getaddrinfo['thost.yandex.ru'] = [
            (_socket.AF_INET, '', '', '', [fake_ip])
        ]
        assert meth('thost.yandex.ru', log) == (None, None)

        # CASE11: same as CASE11, but we have only ipv6
        log.debug('CASE11')
        fake_getaddrinfo['fastbone.thost.yandex.ru'] = [
            (_socket.AF_INET6, '', '', '', [fake_ip])
        ]
        assert meth('thost.yandex.ru', log) == (None, None)

    finally:
        pyjack.replace_all_refs(rbutils.getaddrinfo_g, getaddrinfo_g)


def test_fastbonize_ip():
    meth = rbutils.fastbonize_ip
    import _socket

    fake_ip = '1.2.3.4'
    fake_hostname = ['']

    def _gethostbyaddr_fake(ip, gethostbyaddr):
        assert ip == fake_ip
        if isinstance(fake_hostname[0], Exception):
            raise fake_hostname[0]
        return fake_hostname[0], 'fake'

    nohost_ex = _socket.herror(1, 'Unknown host')

    try:
        gethostbyaddr_g = pyjack.replace_all_refs(rbutils.gethostbyaddr_g, _gethostbyaddr_fake)

        assert rbutils.gethostbyaddr_g('1.2.3.4', _socket.gethostbyaddr) == ('', 'fake')

        log = logging.getLogger('fastbonize_ip')

        # CASE1: unable to grab ip hostname
        log.debug('CASE1')
        fake_hostname[0] = nohost_ex
        assert meth(fake_ip, log) == (None, None, None)

    finally:
        pyjack.replace_all_refs(rbutils.gethostbyaddr_g, gethostbyaddr_g)


def test_path(tmpdir):
    p = rbutils.Path(tmpdir.join('test1'))
    p.ensure(dir=1).chmod(0o700)
    assert p.stat().mode & 0o700 == 0o700

    p = rbutils.Path(tmpdir.join('test2'))
    p.mksymlinkto('test3')
    assert p.readlink() == 'test3'

    p2 = rbutils.Path(tmpdir.join('test2'))
    p2.ensure(dir=1, nolink=1)
    assert p2.check(link=0)
    p2.ensure(dir=1, nolink=1)

    p = rbutils.Path(tmpdir.join('file1').ensure(file=1))
    with pytest.raises(py.error.EEXIST):
        p.ensure(dir=1)
    p.ensure(dir=1, force=1)
    assert p.check(dir=1, file=0)
