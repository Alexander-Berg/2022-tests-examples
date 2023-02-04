from py import std
import pytest


@pytest.mark.new
def test_looks_like_fastbonized():
    from ya.skynet.services.copier.client.utils import looksLikeFastbonized

    # Positive checks
    assert looksLikeFastbonized('Fb-host')
    assert looksLikeFastbonized('fb-host')
    assert looksLikeFastbonized('fB-host.')
    assert looksLikeFastbonized('FB-host.yandex.ru')
    assert looksLikeFastbonized('fb-host.yandex.ru.')
    assert looksLikeFastbonized('FB-host.yandex.net')
    assert looksLikeFastbonized('fb-host.yandex.net.')
    assert looksLikeFastbonized('host.FB.yandex.ru')
    assert looksLikeFastbonized('host.fb.yandex.ru.')

    # Negative checks
    assert not looksLikeFastbonized('xfb-host')
    assert not looksLikeFastbonized('xfb-host.')
    assert not looksLikeFastbonized('xfb-host.yandex.ru')
    assert not looksLikeFastbonized('fbx-host')
    assert not looksLikeFastbonized('host.Fb')
    assert not looksLikeFastbonized('host.fb')
    assert not looksLikeFastbonized('host.fB.')
    assert not looksLikeFastbonized('host.fbx')
    assert not looksLikeFastbonized('host.xfb')
    assert not looksLikeFastbonized('host.fbx.')
    assert not looksLikeFastbonized('host.fbx.yandex.ru')
    assert not looksLikeFastbonized('host.xfb.yandex.ru')
    assert not looksLikeFastbonized('host.fbx.yandex.ru.')


@pytest.mark.new
def test_reverse_record():
    from ya.skynet.services.copier.client.utils import hasReverseRecord

    # I cannot hack /etc/hosts here (the test is executed under non-privileged user),
    # so I'll relay on real host name in this test. Is it bad?

    # Positive checks
    assert hasReverseRecord('pond.fb')
    assert hasReverseRecord('PoNd.Fb.')
    assert hasReverseRecord('pond.fb.yandex.ru')
    assert hasReverseRecord('PoNd.Fb.YaNdEx.Ru.')

    # Negative checks
    assert not hasReverseRecord('www.fb')
    assert not hasReverseRecord('WwW.fB')
    assert not hasReverseRecord('www.fb.')
    assert not hasReverseRecord('www.fb.yandex.ru')
    assert not hasReverseRecord('WwW.fB.yAnDeX.Ru')


@pytest.mark.new
def test_fastbonize():
    from ya.skynet.services.copier.client.utils import fastbonizeHostName, fastbonizeURL

    # Positive checks
    ETHALON = 'pond.fb.yandex.ru'
    assert fastbonizeHostName('pond.yandex.ru') == ETHALON
    assert fastbonizeHostName('PoNd.YaNdEx.Ru.') == ETHALON

    assert fastbonizeHostName('pond.fb.yandex.ru') == ETHALON
    assert fastbonizeHostName('PoNd.Fb.YaNdEx.Ru.') == ETHALON

    TEASER_URL = 'http://user:password@pond.fb.yandex.ru:80/' \
                 'http://user@pond.fb.yandex.ru:80/?some=query&some=value'
    assert fastbonizeURL(TEASER_URL, checkLocalHost=False) == TEASER_URL

    # Negative checks
    assert not fastbonizeHostName('www')
    assert not fastbonizeHostName('www.yandex.ru')
    assert not fastbonizeHostName('www.fb.yandex.ru')
    assert not fastbonizeHostName('fb-www.yandex.ru')
