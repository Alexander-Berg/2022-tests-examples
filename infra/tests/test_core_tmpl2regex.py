from __future__ import absolute_import, print_function

from procman.coredump import CoreCollectorConfig as CFG


def test_BSDPatterns():
    for tmpl, examples in (
        (
            '/cds/core-%H-%N-%U.%P',
            (('/cds/core-korum.yandex-team.ru-123-1.777', 777), ('/cds/core-xxx-1-123.7', 7))
        ), (
            '/cds/core-%%H-%%N-%H-%%U.%P',
            (('/cds/core-%H-%N-korum.yandex-team.ru-%U.777', 777), ('/cds/core-%H-%N-xxx-%U.7', 7))
        ), (
            'foobarzoo',
            (('foobarzoo', 777), )
        ), (
            'foo%%bar%%H%Pzoo',
            (('foo%bar%H777zoo', 777), )
        ), (
            'foo%%bar%%H%%Pzoo',
            (('foo%bar%H%Pzoo', 777), )
        )
    ):
        for example, pid in examples:
            regex = CFG.naming_pattern(tmpl, CFG.bsd_replaces(pid))
            print("Check pattern '%s' (%r) matches %r" % (regex.pattern, tmpl, example))
            assert regex.match(example)


def test_LinuxPatterns():
    for tmpl, examples in (
        (
            '/cds/core-%h-%c-%u.%p',
            (('/cds/core-korum.yandex-team.ru-123-1.777', 777), ('/cds/core-xxx-1-123.7', 7))
        ), (
            '/cds/core-%%h-%%c-%h-%%u.%p',
            (('/cds/core-%h-%c-korum.yandex-team.ru-%u.777', 777), ('/cds/core-%h-%c-xxx-%u.7', 7))
        ), (
            'foobarzoo',
            (('foobarzoo', 777), )
        ), (
            'foo%%bar%%h%pzoo',
            (('foo%bar%h777zoo', 777), )
        ), (
            'foo%%bar%%h%%pzoo',
            (('foo%bar%h%pzoo', 777), )
        )
    ):
        for example, pid in examples:
            regex = CFG.naming_pattern(tmpl, CFG.linux_replaces(pid))
            print("Check pattern '%s' (%r) matches %r" % (regex.pattern, tmpl, example))
            assert regex.match(example)
