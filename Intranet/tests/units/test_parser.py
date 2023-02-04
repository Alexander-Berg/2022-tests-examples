import pytest
from intranet.search.core.query import parse_query, ParseError
from intranet.search.core.query import ast


def split(data):
    return [i.strip() for i in data.splitlines() if i.strip()]

TEST_QUERIES1 = split("""
qwerty
qwerty::1234
zone:qwerty
zone:qwerty::123
zone->qwerty
!qwerty
!!qwerty
qwerty /(-1 2) qwerty
qwerty /(1 2) qwerty
qwerty &/(-1 2) qwerty
qwerty &/(10 20) qwerty
qwerty & qwerty
qwerty ^ test
qwerty ~/(-1 2) qwerty
qwerty ~/(1 2) qwerty
qwerty ~ qwerty
qwerty qwerty
qwerty &&/(-1 2) qwerty
qwerty &&/(1 2) qwerty
qwerty ~~/(-1 2) qwerty
qwerty ~~/(1 2) qwerty
qwerty && qwerty
qwerty ~~ qwerty
qwerty << zone:qwerty
qwerty | qwerty
qwerty <- qwerty
qwerty:ololo::123
qwerty:(ololo::123 ~~ bugaga)

(мама::9367 ^ mamma::1556425 ^ mama::149098) &/(-2 4) (мыла::47270 ^ mila::751089 ^ myla::17509782) &/(-64 64) (раму::622176 ^ ramu::93385506)

(мама::9367 ^ mamma::1556425) &/(-2 4) (мыла::47270 ^ myla::17509782) &/(-64 64) (раму::622176 ^ ramu::93385506) << (public:"1"::840469560 | acl_groups_whitelist:"28674"::840469560)

""")


def test_smoke1():
    for query in TEST_QUERIES1:
        assert parse_query(query).to_string() == query


TEST_QUERIES2 = split("""
qwe ~~/-1 qwe
qwe ~/+1 qwe
qwe /-1 qwe
qwe &&/1 qwe
qwe &&/-1 qwe
qwe! qwe
qwe / qwe
qwe :: qwe
qwe : qwe
qwe: qwe
qwe :qwe
qwe ->
qwe :
a b c:d | e
a b (c:d | e)
""")


def test_smoke2():
    for query in TEST_QUERIES1 + TEST_QUERIES2:
        parsed = parse_query(query)
        assert parsed == parse_query(parsed.to_string())


TEST_QUERIES_ERROR = split("""
qwe::qwer
(qwe
qwe)
qwe /qwe
qwe:(
""")


def test_errors():
    for query in TEST_QUERIES_ERROR:
        with pytest.raises(ParseError):
            parse_query(query, text_fallback=False)


def test_strange_text():
    data = split("""
        qwe! qwe
        qwe / qwe
        qwe :: qwe
        qwe : qwe
        qwe: qwe
        qwe :qwe
        qwe ->
        qwe :
        qwe:
        qwe::
        qwe->
    """)

    results = split("""
        Text(qwe! qwe)
        Text(qwe / qwe)
        Text(qwe :: qwe)
        Text(qwe : qwe)
        Text(qwe: qwe)
        Text(qwe :qwe)
        Text(qwe ->)
        Text(qwe :)
        Text(qwe:)
        Text(qwe::)
        Text(qwe->)
    """)
    for q, res in zip(data, results):
        assert str(parse_query(q)) == res


def test_priorities():
    data = split("""
        qwe:qwe::123
        a & b << c
        a b c:d | e
        a b (c:d | e)
    """)

    results = split("""
        AttrSearch(Keyword(qwe):Weight(Text(qwe)::WeightValue(123)))
        Constr(And(Text(a) & Text(b)) << Text(c))
        Or(AndSoft(AndSoft(Text(a) _ Text(b)) _ AttrSearch(Keyword(c):Text(d))) | Text(e))
        AndSoft(AndSoft(Text(a) _ Text(b)) _ Or(AttrSearch(Keyword(c):Text(d)) | Text(e)))
    """)
    for q, res in zip(data, results):
        assert str(parse_query(q)) == res


def test_filter_text():
    data = split("""
    a b c
    a::123
    a:123
    "a" b
    "a" b->c
    a && b c
    (a && b) c
    a:b c
    """)

    results = split("""
    a b c
    a
    123
    a b
    a c
    a b c
    a b c
    b c
    """)

    for q, res in zip(data, results):
        assert ' '.join(i.text for i in parse_query(q).filter_text_nodes()) == res


def test_join():
    res = ast.Or.join([ast.Text('123'), ast.Text('test'), ast.Text('ololo')])
    assert res == parse_query('123 | test | ololo')

    res = ast.Or.join([])
    assert res is None


def test_join_iterable():
    res = ast.Or.join(
        iter([ast.Text('123'), ast.Text('test'), ast.Text('ololo')]))
    assert res == parse_query('123 | test | ololo')

    res = ast.Or.join(iter([]))
    assert res is None


PLACEHOLDER_QUERY = split("""
ololo:123 | #{qwe}#
ololo:123 | #{qwe}# | #{qweqwe}#
ololo:#{qwe}#
""")


def test_placeholder_not_compile():
    for q in PLACEHOLDER_QUERY:
        res = parse_query(q)

        with pytest.raises(ValueError):
            res.to_string()


def test_format():
    for labled_q in PLACEHOLDER_QUERY:
        for q in TEST_QUERIES1[:3]:
            str = parse_query(labled_q).format(
                qwe=parse_query(q),
                qweqwe=parse_query('simple'),
            ).to_string()

            assert str == labled_q.replace(
                '#{qwe}#', q).replace('#{qweqwe}#', 'simple')


def test_format_rec():
    q1 = parse_query('ololo:123 | #{qwe}#')
    q2 = q1.format(qwe=q1)
    q3 = q2.format(qwe=parse_query('simple'))
    assert q3.to_string() == 'ololo:123 | ololo:123 | simple'


def test_format_type_error():
    for q in PLACEHOLDER_QUERY:
        with pytest.raises(TypeError):
            parse_query(q).format(qwe='ololo', qweqwe=ast.Text('123'))


def test_unary():
    q = parse_query('ololo:123 | !qwerty!')
    res = 'Or(AttrSearch(Keyword(ololo):Text(123)) | ExactWord(! Text(qwerty!)))'
    assert str(q) == res


def test_parser_sanitize():
    assert parse_query('qwe::qwer', sanitize=True, text_fallback=False) == (
        ast.Text('qwe::qwer'))

    with pytest.raises(ParseError):
        parse_query('(qwe', False, False)
        parse_query('(qwe', True, False)

    assert parse_query('(qwe', False, True) == ast.Text('(qwe')

    assert parse_query('qwe)', True, False) == ast.Text('qwe)')
    assert parse_query('qwe)', False, True) == ast.Text('qwe)')

    q = 'qwe:qwe:qwe:qwe:qwe'
    with pytest.raises(ParseError):
        parse_query(q, text_fallback=False)
        parse_query(q, True, text_fallback=False)    # пытаемся санитайзить 1н раз

    # пытаемся санитайзить до конца!
    assert str(parse_query(q, 999, text_fallback=False)) == (
        'AttrSearch(Keyword(qwe):Text(qwe:qwe:qwe:qwe))')

    # сразу приводим к тексту
    assert parse_query(q, False, True) == ast.Text(q)


def test_parser_fallback():
    for q in TEST_QUERIES_ERROR:
        assert parse_query(q, text_fallback=True) == (
            ast.AndSoft.join(ast.Text(i) for i in q.split())
        )


def test_clone():
    for q in TEST_QUERIES1:
        parsed = parse_query(q)
        clone = parsed.clone()
        assert parsed == clone
        assert parsed is not clone

        for ch1, ch2 in zip(ast.get_kids(parsed), ast.get_kids(clone)):
            assert parsed == clone
            assert parsed is not clone
