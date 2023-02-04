from cStringIO import StringIO
import pytest

from skybone_coord.daemon.context import magicPrint


def magicPrintCheck(*args, **kwargs):
    buf = StringIO()
    kwargs['file'] = buf
    magicPrint(*args, **kwargs)
    return buf.getvalue()


@pytest.mark.parametrize(
    ('inp', 'exp'), [
        (('asd',), 'asd\n'),
        (('asd', 'dsa'), 'asd dsa\n'),
        (('asd', ('x', 'y')), 'asd (\'x\', \'y\')\n'),
        ((hash, ), '<built-in function hash>\n'),
        ((1, hash), '1 <built-in function hash>\n'),
        (([1]*30, ), '[' + '1,\n ' * 29 + '1]\n'),
        (('a', [1]*30, 'b'), 'a [' + '1,\n ' * 29 + '1] b\n'),
    ], ids=[
        'simple string',
        'tuple of strings',
        'tuple string and inner tuple',
        'function',
        'integer and function',
        'big list',
        'a + big list + b',
    ]
)
def test_magic_print(inp, exp):
    assert magicPrintCheck(*inp) == exp
