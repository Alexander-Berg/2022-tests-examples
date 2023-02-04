from infra.reconf_juggler.builders import DeclaredChecksBuilder
from infra.reconf_juggler.util.d3js import convert


class Builder(DeclaredChecksBuilder):
    default_check_class = 'infra.reconf_juggler.checks.META'

    def build_initial_tree(self, initial_data):
        return {
            'parent:META': {
                'children': {
                    'child_one:META': {
                        'children': {
                            'trimmed_grandchild:META': {},  # should not be dumped separately
                            'grandchild:META': None,  # should not be dumped separately
                            'usual_grandchild:META': {'children': {}},
                        },
                    },
                    'child_two:META': {
                        'children': {}
                    },
                },
            },
        }


def test_convert():
    expected = {
        'children': [
            {
                'children': [
                    {
                        'children': [
                            {'name': 'grandchild:META'},
                            {'name': 'trimmed_grandchild:META'},
                            {'name': 'usual_grandchild:META'},
                        ],
                        'name': 'child_one:META',
                    },
                    {'name': 'child_two:META'},
                ],
                'name': 'parent:META',
            },
        ],
        'name': 'root',
    }

    assert expected == convert(Builder().build('checks_tree'))
