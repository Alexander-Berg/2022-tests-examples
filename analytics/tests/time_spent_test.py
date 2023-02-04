from nile.api.v1 import Record
from analytics.collections.plotter_collections.plots.time_spent import convert_ui, filter_session


def test_convert_ui():
    assert convert_ui('yandexApp') == 'mobile'
    assert convert_ui('mobile') == 'mobile'
    assert convert_ui('tablet') == 'desktop'
    assert convert_ui('desktop') == 'desktop'
    assert convert_ui('') == 'undefined'
    assert convert_ui('qwe') == 'qwe'


def test_filter_session():
    session = [Record(path='start.session'), Record(path='access'), Record(path='finish.session')]
    assert filter_session(session, 'any') == session
    assert filter_session(session, 'has_access') == session
    assert filter_session(session, 'access') == [Record(path='access')]

    session = [Record(path='start.session'), Record(path='finish.session')]
    assert filter_session(session, 'any') == session
    assert filter_session(session, 'has_access') == []
    assert filter_session(session, 'access') == []
