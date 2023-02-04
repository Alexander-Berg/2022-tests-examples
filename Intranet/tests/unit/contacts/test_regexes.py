import pytest

from plan.contacts import regexes

true_data = [
    (regexes.URL_RE, 'https://planner.yandex-team.ru/'),
    (regexes.URL_RE, 'http://planner.yandex-team.ru/services/'),
    (regexes.URL_RE, 'http://p.yandex-team.ru/?a=5&b=6#chik'),

    (regexes.QUEUE_RE, 'PLAN'),
    (regexes.QUEUE_RE, 'plan'),

    (regexes.FILTER_RE, 'https://st.yandex-team.ru/filters/order:updated:false/filter?assignee=printsesso'),
    (regexes.FILTER_RE, 'https://st.yandex-team.ru/filters/filter:28101'),
    (regexes.FILTER_QUERY_RE, 'https://st.yandex-team.ru/issues/?_q=Queue'),
    (regexes.FILTER_QUERY_RE, 'https://st.yandex-team.ru/issues/?a=1&_q=Queue'),
    (regexes.FILTER_QUERY_RE, 'https://st.yandex-team.ru/issues/?_o=updated+DESC&queue=%22TASHA%22&type=%22bug%22&resolution=%22empty%28%29%22'),
    (regexes.FILTER_QUERY_RE, 'https://st.yandex-team.ru/issues?_q=Queue'),

    (regexes.ST_RE, 'https://st.yandex-team.ru/PLAN'),
    (regexes.ST_RE, 'https://st.yandex-team.ru/PLAN/'),
    (regexes.ST_RE, 'https://st.yandex-team.ru/PLAN/'),
    (regexes.ST_RE, 'st.yandex-team.ru/PLAN/'),

    (regexes.ML_EMAIL_RE, 'lala@yandex-team.ru'),

    (regexes.ML_RE, 'http://ml.yandex-team.ru/lists/lala/'),
    (regexes.ML_RE, 'http://ml.yandex-team.ru/lists/lala'),
    (regexes.ML_RE, 'maillists.yandex-team.ru/lists/lala'),
    (regexes.ML_RE, 'https://maillists.yandex-team.ru/lists/lala'),
    (regexes.ML_RE, 'ml.yandex-team.ru/lists/lala'),
    (regexes.ML_RE, 'lala'),
    (regexes.ML_RE, 'plan-dev'),
    (regexes.ML_RE, 'lala@'),

    (regexes.METRIKA_RE, '12345'),
    (regexes.METRIKA_RE, 'http://metrika.yandex.ru/stat/dashboard/?counter_id=1234'),
    (regexes.METRIKA_RE, 'metrika.yandex.ru/stat/dashboard/?counter_id=1234'),
    (regexes.METRIKA_RE, 'http://metrika.yandex.ru/stat/dashboard?counter_id=1234'),

    (regexes.AT_CLUB_RE, 'http://at.yandex-team.ru/club/'),
    (regexes.AT_CLUB_RE, 'http://at.yandex-team.ru/club'),
    (regexes.AT_CLUB_RE, 'at.yandex-team.ru/club/'),
    (regexes.AT_CLUB_RE, 'clubs.at.yandex-team.ru/club/'),

    (regexes.AT_ONLY_CLUB_RE, 'club'),

    (regexes.WIKI_RE, 'http://wiki.yandex-team.ru/path'),
    (regexes.WIKI_RE, 'http://wiki.yandex-team.ru/path/'),
    (regexes.WIKI_RE, 'wiki.yandex-team.ru/path'),
    (regexes.WIKI_RE, 'http://wiki.yandex-team.ru/path/path/path'),
    (regexes.WIKI_RE, 'wiki.yandex-team.ru/path?a=5&b=6#chik'),

    (regexes.WIKI_PATH_RE, 'path'),
    (regexes.WIKI_PATH_RE, 'path/path/'),
    (regexes.WIKI_PATH_RE, '/path'),
    (regexes.WIKI_PATH_RE, '/'),
]


@pytest.mark.parametrize('re_object,string', true_data)
def test_true_regexes(re_object, string):
    match = re_object.match(string)
    assert match is not None, f'{re_object.pattern} ({string})'


false_data = [
    (regexes.URL_RE, 'yandex.ru'),
    (regexes.URL_RE, 'planner.yandex-team.ru'),
    (regexes.URL_RE, 'p.yandex-team.ru/?a=5&b=6#chik'),
    (regexes.URL_RE, 'lalala'),
    (regexes.URL_RE, 'http://'),
    (regexes.URL_RE, 'http://lalala'),
    (regexes.URL_RE, 'lalala.ru lalala.ru'),
    (regexes.URL_RE, 'lalala.ru/lala/? a'),

    (regexes.ST_RE, 'https://st.yandex-team.ru/PLAN/a'),

    (regexes.FILTER_RE, 'http://st.yandex-team.ru/filters/'),
    (regexes.FILTER_RE, 'https://st.yandex-team.ru/ABC-1'),
    (regexes.FILTER_RE, 'https://st.yandex-team.ru/filters/filter'
                        '?query=queue%3A%20TEST%20created%3A%20>%3D%2001.03.2018'),
    (regexes.FILTER_RE, 'https://st.yandex-team.ru/filters/filter'
                        '?x=123&query=queue%3A%20TEST%20created%3A%20>%3D%2001.03.2018'),
    (regexes.ML_EMAIL_RE, 'lala@yandex.ru'),
    (regexes.ML_EMAIL_RE, '@yandex-team.ru'),
    (regexes.ML_EMAIL_RE, 'lala'),

    (regexes.ML_RE, 'ml.yandex-team.ru/lala'),
    (regexes.ML_RE, 'lala@yandex-team.ru'),

    (regexes.METRIKA_RE, 'abc'),
    (regexes.METRIKA_RE, '123abc'),
    (regexes.METRIKA_RE, 'http://metrika.yandex.ru/stat/dashboard?counter_id=abc'),

    (regexes.AT_CLUB_RE, 'http://at.yandex-team.ru/club/lala'),

    (regexes.AT_ONLY_CLUB_RE, 'club/lala'),

    (regexes.WIKI_RE, 'http://woko.yandex-team.ru/gege'),
]


@pytest.mark.parametrize('re_object,string', false_data)
def test_false_regexes(re_object, string):
    match = re_object.match(string)
    assert match is None, re_object.pattern
