import clemmer


def test_space():
    ''' CONTEXT-199: в выдаче не должно быть пустых лемм/форм и пробельных символов '''
    result = clemmer.analyze2(b'\xef\xb1\xa2 \xc6\x92\xcf\x85\xc5\x82\xce\xb2\xcf\x83')
    assert len(result) == 2
    assert result[0].text == b'\xd9\x90\xd9\x91'
    assert result[0].lemmas == (b'\xef\xb1\xa2',)
    assert result[0].formas == (b'\xd9\x90\xd9\x91',)
    assert result[1].text == b'\xc6\x92\xcf\x85\xc5\x82\xce\xb2\xcf\x83'
    assert result[1].lemmas == (b'\xc6\x92\xcf\x85\xc5\x82\xce\xb2\xcf\x83',)
    assert result[1].formas == (b'\xc6\x92\xcf\x85\xc5\x82\xce\xb2\xcf\x83',)


def _test_lowercase_impl(analyze, check_formas=True):
    s_lower = lambda str_: str_.decode('utf-8').lower().encode('utf-8')
    result = analyze(b'wORd m0De1Ka \xd1\x81\xd0\xbb0\xd0\xb2\xd0\xbe')
    for word in result:
        assert word.text == s_lower(word.text)
        for lemm in word.lemmas:
            assert lemm == s_lower(lemm)
        if not check_formas:
            continue
        for form in word.formas:
            assert form == s_lower(form)


def test_lowercase():
    _test_lowercase_impl(clemmer.analyze, False)
    _test_lowercase_impl(clemmer.analyze2)
    _test_lowercase_impl(lambda text: clemmer.analyze2(text, lang=clemmer.LANG_RUS))


def _test_rawflags_impl(analyze_noformas):
    r = analyze_noformas(b'!-test @ for ~1-@2 -wi-fi -+flags')
    assert len(r) == 6
    assert r[0].text == b'for'
    assert r[0].raw_flags == 0
    assert r[1].text == b'test'
    assert r[1].raw_flags == clemmer.RAWFLAG_MINUS | clemmer.RAWFLAG_EXACT
    assert r[2].text == b'1'
    assert r[2].raw_flags == clemmer.RAWFLAG_NUM
    assert r[3].text == b'2'
    assert r[3].raw_flags == clemmer.RAWFLAG_MINUS | clemmer.RAWFLAG_REG
    assert r[4].text == b'wi-fi'
    assert r[4].raw_flags == clemmer.RAWFLAG_MINUS
    assert r[5].text == b'flags'
    assert r[5].raw_flags == clemmer.RAWFLAG_MINUS | clemmer.RAWFLAG_PLUS


def test_rawflags_impl():
    _test_rawflags_impl(clemmer.analyze2)
    _test_rawflags_impl(clemmer.analyze2_noformas)


def _test_analyze_flags_impl(analyze):
    r = analyze(b'!+test @ for +1 +@2 +wifi +!flags -0', '+@-!')
    assert len(r) == 7
    assert r[0].text == b'test'
    assert r[0].flags == 9
    assert r[1].text == b'for'
    assert r[1].flags == 0
    assert r[2].text == b'1'
    assert r[2].flags == 1
    assert r[3].text == b'2'
    assert r[3].flags == 3
    assert r[4].text == b'wifi'
    assert r[4].flags == 1
    assert r[5].text == b'flags'
    assert r[5].flags == 9
    assert r[6].text == b'0'
    assert r[6].flags == 4


def test_analyze_flags():
    _test_analyze_flags_impl(clemmer.analyze)
    _test_analyze_flags_impl(clemmer.analyze_noformas)


def test_langs():
    for lang in dir(clemmer):
        if not lang.startswith('LANG_'):
            continue
        clemmer.analyze2('this 1 текст', lang=getattr(clemmer, lang), stop=True, fix=True)
