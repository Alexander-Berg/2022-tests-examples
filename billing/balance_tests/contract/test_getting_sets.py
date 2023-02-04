# -*- coding: utf-8 -*-

from balance.printform.rules import *
import pytest


@pytest.mark.parametrize(
    'mode', [
        {
            'item': 'alter',
            'sets': [{'::a', '::aANY', '::b', '::bANY', '::c', '::cANY'}, {'::d', '::dANY'}, {'::e', '::eANY'}]
        },
        {
            'item': 'inter',
            'sets': [{'::a', '::aANY', '::b', '::bANY', '::c', '::cANY', '::d', '::dANY'},
                     {'::a', '::aANY', '::b', '::bANY', '::c', '::cANY', '::e', '::eANY'}]
        }
    ]
)
def test_getting_sets(mode):
    # checking that the collection of possible sets is correct
    terminals = dict()

    for i in range(26):
        symb = chr(ord('a') + i)
        terminals[symb] = Terminal('', '', symb)

    st1 = Interleave([terminals['a'], terminals['b'], terminals['c']])
    st2 = Alternation([terminals['d'], terminals['e']])
    if mode['item'] == 'alter':
        st3 = Alternation([st1, st2])
    else:
        st3 = Interleave([st1, st2])

    r = Rule(item=st3)
    sets = r.get_sets()

    assert sets == mode['sets']
