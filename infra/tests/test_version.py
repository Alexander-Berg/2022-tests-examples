import pytest
import infra.dist.cacus.lib.utils.version as version


@pytest.mark.parametrize(('ver', 'epoch', 'upstream', 'deb'), [
    ('1.0', 0, '1.0', "0"),
    ('1.1.0', 0, '1.1.0', "0"),
    ('0:1.0', 0, '1.0', "0"),
    ('0:1.0-0', 0, '1.0', "0"),
    ('1:1.0-0', 1, '1.0', "0"),
    ('1:1.2-2', 1, '1.2', "2"),
    ('1.0-2', 0, '1.0', "2"),
    ('1:1.0-1yandex0', 1, '1.0', "1yandex0"),
    ('1:1.0-p1337~master-1yandex0~trunk8841~develop', 1, '1.0-p1337~master', "-1yandex0~trunk8841~develop"),
    ('1:1.0-p1337~master:0-1yandex0~trunk8841~develop', 1, '1.0-p1337~master:0', "-1yandex0~trunk8841~develop"),
])
def test_version_parser(ver, epoch, upstream, deb):
    v = version.Version.from_string(ver)
    assert v.epoch == epoch
    assert v.debian_revision == v.debian_revision
    assert v.upstream_version == v.upstream_version


@pytest.mark.parametrize(('ver', 'str_repr'), [
    ('1.0', '1.0'),
    ('0:1.0', '1.0'),
    ('1.0-0', '1.0'),
    ('0:1.0-1', '1.0-1'),
    ('1:1.1-1', '1:1.1-1'),
    ('1:1.0-p1337~master-1yandex0~trunk8841~develop', '1:1.0-p1337~master-1yandex0~trunk8841~develop'),
])
def test_version_str(ver, str_repr):
    v = version.Version.from_string(ver)
    assert str(v) == str_repr


@pytest.mark.parametrize(('lo', 'hi'), [
    ('1.0', '1.1'),
    ('5.0', '1:0.0'),
    ('1:1.0-25', '1:1.0-26'),
    ('1:1.0-25~svn44', '1:1.0-25~svn47'),
    ('1:1.0~beta1~svn1245-1', '3:1.0~beta1~svn1245-1'),
    ('1.0~beta1~svn1245-1', '1:1.0~beta1~svn1245-1'),
    ('1.0~beta1~svn1245-1', '1.0~beta1~svn1245-2'),
    ('3:1.0~beta1~svn1245-1', '3:1.0~beta1-1'),
    ('1.0~beta1~svn1245', '1.0~beta1'),
    ('1.0~beta1', '1.0'),
    ('1.0-133-avc', '1.1'),
    ('1.0', '1.0-133-avc'),
])
def test_version_compare(lo, hi):
    a, b = version.Version.from_string(lo), version.Version.from_string(hi)
    assert a < b
    assert b > a


@pytest.mark.parametrize(('a', 'b'), [
    ('1.0', '1.0'),
    ('0:1.0', '1.0'),
    ('0:1.0-0', '1.0'),
    ('1:1.0', '1:1.0'),
    ('1:1.0', '1:1.0-0'),
    ('1:1.0-1', '1:1.0-1'),
])
def test_version_equal(a, b):
    a, b = version.Version.from_string(a), version.Version.from_string(b)
    assert a == b


@pytest.mark.parametrize(('a', 'b'), [
    ('1.0', '1.1'),
    ('1.0', '1:0.1'),
    ('1:1.0', '1:1.0-1'),
    ('1:1.0-2', '1.0'),
    ('1:1.0-2', '1:1.0-20'),
])
def test_version_inequal(a, b):
    a, b = version.Version.from_string(a), version.Version.from_string(b)
    assert a != b
