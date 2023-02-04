from infra.rtc.nodeinfo.lib import yp_util


def test_infer_yp_for_host():
    f = yp_util.infer_yp_for_host
    assert f([], None) is None
    assert f(['rtc.ypmaster-man_pre'], 'sas') == 'man-pre.yp.yandex.net'
    assert f([], 'sas') == 'sas.yp.yandex.net'


def test_infer_yp_master_from_tags():
    f = yp_util._infer_yp_from_tags
    assert f([]) is None
    assert f(['rtc.ypmaster-sas_test']) == 'sas-test.yp.yandex.net'
    assert f(['rtc.ypmaster-man_pre']) == 'man-pre.yp.yandex.net'
    assert f(['rtc.ypmaster-sas']) == 'sas.yp.yandex.net'
    assert f(['rtc.ypmaster-man']) == 'man.yp.yandex.net'
    assert f(['rtc.ypmaster-vla']) == 'vla.yp.yandex.net'
    assert f(['rtc.ypmaster-iva']) == 'iva.yp.yandex.net'
    assert f(['rtc.ypmaster-myt']) == 'myt.yp.yandex.net'
    assert f(['rtc.ypmaster-xdc']) == 'xdc.yp.yandex.net'
    # multiple master tags should cause UB
    assert f(f(['rtc.ypmaster-xdc', 'rtc.ypmaster-xdc'])) is None


def test_infer_yp_master_from_dc():
    DCs = ('iva', 'myt', 'sas', 'vla', 'man')
    f = yp_util._infer_yp_from_dc
    assert f('') is None
    for dc in DCs:
        assert f(dc) == '{}.yp.yandex.net'.format(dc)
    assert f('unknown datacenter') is None
