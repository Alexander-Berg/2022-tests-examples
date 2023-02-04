import clemmer


def test_uzb():
    text = 'келиб'
    rez = clemmer.analyze2(text, lang=clemmer.LANG_UZB)
    assert len(rez) == 1
    word = rez[0]
    assert text == word.text
    assert (text,) == word.lemmas
    return sorted(word.formas)
