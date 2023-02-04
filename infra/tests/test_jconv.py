import copy  # deepcopy used to avoid common data occasional curruption
import pytest

from infra.reconf_juggler.tools import jconv


JSDK_DUMP = [
    {
        'children': [],
        'host': 'child_two',
        'service': 'META'
    },
    {
        'children': [
            {
                'group_type': 'HOST',
                'host': 'child_one',
                'service': 'META'
            },
            {
                'group_type': 'HOST',
                'host': 'child_two',
                'service': 'META'
            }
        ],
        'host': 'parent',
        'service': 'META'
    }
]

TREE_DUMP = {
    'parent:META': {
        'children': {
            'child_one:META': None,
            'child_two:META': {
                'children': {}
            }
        }
    }
}


def test_convert_jsdk2tree():
    assert TREE_DUMP == jconv.convert(copy.deepcopy(JSDK_DUMP), 'jsdk', 'tree')


def test_convert_tree2jsdk():
    assert JSDK_DUMP == jconv.convert(copy.deepcopy(TREE_DUMP), 'tree', 'jsdk')


def test_unsupported_format():
    with pytest.raises(RuntimeError):
        jconv.convert([], 'jsdk', 'unsupported_format')
