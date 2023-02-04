# coding: utf8
from __future__ import unicode_literals

import itertools

import clemmer


TEXT = 'в -1 рыло'


def _test_impl(analyze, text, arg):
    result = []
    for fill_formas, stop, fix in itertools.product((False, True), repeat=3):
        rez = analyze(text, arg, fill_formas=fill_formas, stop=stop, fix=fix)
        print(repr(rez))
        for word in rez:
            assert type(word.text) is type(text)
            assert type(word.lemmas) is tuple
            for lemma in word.lemmas:
                assert type(lemma) is type(text)
            if hasattr(word, 'formas'):
                if fill_formas:
                    assert type(word.formas) is tuple
                    for forma in word.formas:
                        assert type(forma) is type(text)
                else:
                    assert word.formas is None
        result.append({
            'fill_formas': fill_formas,
            'stop': stop,
            'fix': fix,
            'result': [word.nword() for word in rez],
        })
    return result


def test_bytes_analyze():
    return _test_impl(clemmer.analyze, TEXT.encode('utf-8'), '-')


def test_unicode_analyze():
    return _test_impl(clemmer.analyze, TEXT, '-')


def test_bytes_analyze2():
    return _test_impl(clemmer.analyze2, TEXT.encode('utf-8'), clemmer.LANG_RUS)


def test_unicode_analyze2():
    return _test_impl(clemmer.analyze2, TEXT, clemmer.LANG_RUS)
