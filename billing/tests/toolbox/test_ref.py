from bcl.toolbox.ref_oksm import OKSM


def test_oksm():
    ru = OKSM.get(643)
    assert ru.title_short == 'РОССИЯ'
    assert ru.alfa2 == 'RU'
