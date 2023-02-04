from ya.skynet.services.heartbeatserver.bulldozer.plugins import skyinfo


def test_svn2ver():
    for url, exp in [
        # Synthetic samples
        ('foobarzoo', 'foobarzoo'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/tags/skynet/9.20.30/skynet', '9.20.30'),
        ('https://sepe@arcadia.yandex.ru/arc/tags/skynet/9.20.30/skynet', '9.20.30'),
        ('https://arcadia.yandex.ru/arc/tags/skynet/9.20.30/skynet', '9.20.30'),
        ('svn+ssh://arcadia.yandex.ru/arc/tags/skynet/9.20.30/skynet', '9.20.30'),
        ('svn+ssh://arcadia.yandex.ru/arc/tags/skynet/9.20.30/skynet@1033999', '9.20.30@1033999'),
        ('svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/skynet@1033999', 'trunk@1033999'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/trunk/arcadia/skynet@1033999', 'trunk@1033999'),
        ('svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/skynet', 'trunk'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/trunk/arcadia/skynet/services/heartbeat-client', 'trunk'),
        ('https://sepe@arcadia.yandex.ru/arc/trunk/arcadia/skynet/services/heartbeat-client', 'trunk'),
        ('https://sepe@arcadia.yandex.ru/arc/branches/skynet/release-9.16/skynet', 'release-9.16'),
        ('https://sepe@arcadia.yandex.ru/arc/branches/skynet/release-9.16/skynet@1033999', 'release-9.16@1033999'),
        ('https://arcadia.yandex.ru/arc/branches/skynet/release-9.16/skynet/services@1033999', 'release-9.16@1033999'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/branches/skynet/release-9.16/skynet/@1033999', 'release-9.16@1033999'),
        # Real-life samples
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/branches/skynet/release-9.16/skynet', 'release-9.16'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/tags/skynet/10.0.0a13/skynet', '10.0.0a13'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/tags/skynet/8.19.14.4/skynet', '8.19.14.4'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/tags/skynet/9.2.16c/skynet', '9.2.16c'),
        ('svn+ssh://sepe@arcadia.yandex.ru/arc/tags/skynet/stable-8-14-6/skynet', '8.14.6'),
    ]:
        ver = skyinfo.svnURL2Version(url)
        assert ver == exp, 'Invalid parsing of %r -> %r (expected %r)' % (url, ver, exp)
