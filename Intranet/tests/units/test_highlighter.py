import pytest
from intranet.search.core.highlighter import hilite


@pytest.mark.parametrize('text,query,result', [
    ('svn_test', 'test', 'svn_@hlword@test@/hlword@'),
    ('svn_test', 'svn_test', '@hlword@svn_test@/hlword@'),
    ('svn_test', 'svn test', '@hlword@svn@/hlword@_@hlword@test@/hlword@'),
    ('svn-test', 'svn-test', '@hlword@svn-test@/hlword@'),
    ('svn-test', 'svn test', '@hlword@svn@/hlword@-@hlword@test@/hlword@'),
    ('svn, test', 'svn test', '@hlword@svn@/hlword@, @hlword@test@/hlword@'),
    ('svn test', 'svn test', '@hlword@svn@/hlword@ @hlword@test@/hlword@'),
    ('qwe', 'qwe', '@hlword@qwe@/hlword@'),
    ('qwe test', 'qwe', '@hlword@qwe@/hlword@ test'),
    ('qwe Qwe', 'qwe', '@hlword@qwe@/hlword@ @hlword@Qwe@/hlword@'),
    # частичное вхождение
    ('svn testing', 'svn test', '@hlword@svn@/hlword@ @hlword@test@/hlword@ing'),
    ('pysvn test', 'svn test', 'pysvn @hlword@test@/hlword@'),
    ('pysvn testing', 'svn test', 'pysvn @hlword@test@/hlword@ing'),
    ('web-svn pytest', 'svn test', 'web-@hlword@svn@/hlword@ pytest'),
    ('web-svn py-test', 'svn test', 'web-@hlword@svn@/hlword@ py-@hlword@test@/hlword@'),
    ('web-svn py-tests', 'svn test', 'web-@hlword@svn@/hlword@ py-@hlword@test@/hlword@s'),
    ('web_svn pytest', 'svn test', 'web_@hlword@svn@/hlword@ pytest'),
    ('web_svn py_test', 'svn test', 'web_@hlword@svn@/hlword@ py_@hlword@test@/hlword@'),
    ('web_svn py_tests', 'svn test', 'web_@hlword@svn@/hlword@ py_@hlword@test@/hlword@s'),
    ('qwerty', 'qwe', '@hlword@qwe@/hlword@rty'),
    ('qwerty test', 'qwe', '@hlword@qwe@/hlword@rty test'),
    ('saqwe test', 'qwe', 'saqwe test'),
    ('saqwerty test', 'qwe', 'saqwerty test'),
    ('qwerty Qwerty', 'qwe', '@hlword@qwe@/hlword@rty @hlword@Qwe@/hlword@rty'),
    ('saqwerty saQwerty', 'qwe', 'saqwerty saQwerty'),
    ('Победу морды', 'обед', 'Победу морды'),
    ('Победу морды', 'победа', '@hlword@Победу@/hlword@ морды'),
    ('Победу морды', 'побед', '@hlword@Побед@/hlword@у морды'),
    ('Победу морды', 'орда', 'Победу морды'),
    ('Победу морды', 'орды', 'Победу морды'),
    ('Победу морды', 'морда', 'Победу @hlword@морды@/hlword@'),
    ('Победу морды', 'обед морда', 'Победу @hlword@морды@/hlword@'),
    ('Победу морды', 'обед орда', 'Победу морды'),
    ('Победу морды', 'обед орды', 'Победу морды'),
    ('Победу морды', 'победе морду', '@hlword@Победу@/hlword@ @hlword@морды@/hlword@'),
    # Фильтр по зоне дорисовывает '*' к словам в изначальном запросе
    ('Победу морды', 'обед морда*', 'Победу @hlword@морды@/hlword@'),
    ('Победу морды', 'обед орда*', 'Победу морды'),
    ('Победу морды', 'обед орды*', 'Победу морды'),
    ('Победу морды', 'победе морду*', '@hlword@Победу@/hlword@ @hlword@морды@/hlword@'),
    ('Победу морды', 'обед* морда', 'Победу @hlword@морды@/hlword@'),
    ('Победу морды', 'обед* орда', 'Победу морды'),
    ('Победу морды', 'обед* орды', 'Победу морды'),
    ('Победу морды', 'победе* морду', '@hlword@Победу@/hlword@ @hlword@морды@/hlword@'),
    ('Победу морды', 'обед* морда*', 'Победу @hlword@морды@/hlword@'),
    ('Победу морды', 'обед* орда*', 'Победу морды'),
    ('Победу морды', 'обед* орды*', 'Победу морды'),
    ('Победу морды', 'победе* морду*', '@hlword@Победу@/hlword@ @hlword@морды@/hlword@'),
])
def test_words(text, query, result):
    assert hilite(query, text) == result


MAMA = {'text': 'Мама мыла', 'test': 'qwe'}


def test_dict():
    data = MAMA
    result = {'text': 'Мама мыла', 'test': '@hlword@qwe@/hlword@'}
    assert hilite('qwe', data) == result
    assert hilite('qwe', data, fields=['text']) == data
    assert hilite('qwe', data, fields=['test']) == result


def test_morf():
    data = MAMA
    result = {'text': '@hlword@Мама@/hlword@ мыла', 'test': 'qwe'}
    assert hilite('мама', data) == result
    assert hilite('мамой', data) == result
    assert hilite('Мамой', data) == result


def test_many_words():
    """ Тестируем что хайлайтер понимает огромное количество вариантов словоформ """
    data = 'Мама мыла морду'
    res = 'Мама мыла @hlword@морду@/hlword@'
    q = (
        '(морда::254919 ^ morda::22715393 ^ (главная::1097 &/(-2 2)'
        ' страница::1741)) << (public:"1"::840469560 |'
        ' acl_groups_whitelist:"28674"::840469560 | acl_groups_whitelist:"1543"::840469560'
        ' | acl_groups_whitelist:"137"::840469560 | acl_groups_whitelist:"17"::840469560'
        ' | acl_groups_whitelist:"27801"::840469560 | acl_groups_whitelist:"282"::840469560'
        ' | acl_groups_whitelist:"29733"::840469560 | acl_groups_whitelist:"31928"::840469560'
        ' | acl_groups_whitelist:"699"::840469560 | acl_groups_whitelist:"24510"::840469560'
        ' | acl_groups_whitelist:"962"::840469560 | acl_groups_whitelist:"24779"::840469560'
        ' | acl_groups_whitelist:"716"::840469560 | acl_groups_whitelist:"27092"::840469560'
        ' | acl_groups_whitelist:"1635"::840469560 | acl_groups_whitelist:"745"::840469560'
        ' | acl_groups_whitelist:"31216"::840469560 | acl_groups_whitelist:"26870"::840469560'
        ' | acl_groups_whitelist:"29012"::840469560 | acl_groups_whitelist:"7930"::840469560'
        ' | acl_groups_whitelist:"766"::840469560 | acl_groups_whitelist:"26879"::840469560'
        ' | acl_users_whitelist:"zubchick"::840469560)'
    )

    assert hilite(q, data) == res


def test_ignore_constraints():
    """ Подсвечиваем только слова из самого запроса, а не его уточнения
    """
    data = '1 мама мыла 10 морд'
    res = '1 мама мыла 10 @hlword@морд@/hlword@'
    q = 'морда << (public:"1" | acl_groups_whitelist:"10")'
    assert hilite(q, data) == res


AT_DATA = {
    'author': {
        'login': 'zubchick',
        'name': 'Никита Зубков',
        'name_en': 'Nikita Zubkov',
        'qu': 33
    },
    'url': 'http://zubchick.at.xfront02-test.yandex-team.ru/1107',
    'type': 'post',
    'content': 'ololo',
}


def test_inner_fields():
    res = hilite('zubchick', AT_DATA, fields=['login'])
    assert AT_DATA['author']['login'] != res['author']['login'] == (
        '@hlword@%s@/hlword@' % AT_DATA['author']['login'])
    assert AT_DATA['url'] == res['url']


def test_inner_fields2():
    """ Подсвечиваем author, и ожидаем подсветки author.login """
    res = hilite('zubchick', AT_DATA, fields=['author'])
    assert AT_DATA['author']['login'] != res['author']['login'] == (
        '@hlword@%s@/hlword@' % AT_DATA['author']['login'])


def test_inner_list():
    hilite('qwe', [])
    res = hilite('test', {'test': [{'1': 'test'}]}, fields=['1'])
    assert res == {'test': [{'1': '@hlword@test@/hlword@'}]}


def test_type():
    res = hilite('post', AT_DATA, fields=('content', 'author'))
    assert res['type'] == AT_DATA['type'] == 'post'
