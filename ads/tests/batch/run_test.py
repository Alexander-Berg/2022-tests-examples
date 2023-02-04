# coding: utf-8

import os

import pytest
import yatest.common

import clemmer


SERVER_DATA_DIR = yatest.common.source_path('ads/clemmer/tests/server-data')
SERVER_DATA_FNAME_PREFIX = 'clemmer_input.'

LANG_MAP = {
    'ru': clemmer.LANG_RUS,
    'by': clemmer.LANG_BEL,
    'kz': clemmer.LANG_KAZ,
    'en': clemmer.LANG_ENG,
    'tr': clemmer.LANG_TUR,
    'tt': clemmer.LANG_TAT,
    'uk': clemmer.LANG_UKR,
    'uz': clemmer.LANG_UZB,
}


def get_batch_params():
    fname_prefix = SERVER_DATA_FNAME_PREFIX
    result = []
    for fname in os.listdir(SERVER_DATA_DIR):
        assert fname.startswith(fname_prefix)
        tail = fname[len(fname_prefix):].split('.')
        if len(tail) == 1:
            chunk, lang_str = '', tail[0]
        else:
            chunk, lang_str = tail
        result.append((chunk, lang_str))
    assert result
    return result


def batch_params_to_fname(chunk, lang_str):
    result = SERVER_DATA_FNAME_PREFIX
    if chunk:
        result += chunk + '.'
    result += lang_str
    return result


def analyze_and_write(analyze_clb, line, lang):
    line = line.rstrip('\n').replace('\t', ' ')
    result = analyze_clb(line, lang) if lang is not None else analyze_clb(line)
    for word in result:
        yield ''.join(
            'text: [{}] word: [{}] lemma: [{}]\n'.format(
                line, word.text, lemma,
            )
            for lemma in word.lemmas
        )
        if not hasattr(word, 'formas'):
            continue
        yield ''.join(
            'text: [{}] word: [{}] forma: [{}]\n'.format(
                line, word.text, forma,
            )
            for forma in word.formas
        )


def _test_batch(output_suffix, chunk, lang_str, process_line):
    inp_fname = batch_params_to_fname(chunk, lang_str)
    inp_fpath = os.path.join(SERVER_DATA_DIR, inp_fname)
    out_fpath = inp_fname + output_suffix
    lang = LANG_MAP[lang_str]
    with open(inp_fpath, 'r') as inp_fobj, open(out_fpath, 'w') as out_fobj:
        for line in inp_fobj:
            for out_line in process_line(line, lang):
                out_fobj.write(out_line)
    return yatest.common.canonical_file(out_fpath)


@pytest.mark.parametrize('chunk,lang_str', get_batch_params())
def test_batch_clemmer1(chunk, lang_str):
    return _test_batch(
        '.clemmer1.out',
        chunk,
        lang_str,
        lambda line, lang: analyze_and_write(clemmer.analyze, line, None),
    )


@pytest.mark.parametrize('chunk,lang_str', get_batch_params())
def test_batch_clemmer2(chunk, lang_str):
    return _test_batch(
        '.clemmer2.out',
        chunk,
        lang_str,
        lambda line, lang: analyze_and_write(clemmer.analyze2, line, lang),
    )
