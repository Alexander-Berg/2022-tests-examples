from infra.reconf import ConfNode
from infra.reconf.declarative import DeclaredConf, DeclaredConfSet


class DC(DeclaredConf):
    @staticmethod
    def get_full_class_name(conf):
        return conf.get('class', None)


class DCS(DeclaredConfSet):
    branches = (DC,)


class Parent(ConfNode):
    validate_class = False


class Child(ConfNode):
    validate_class = False


class GrandChild(ConfNode):
    validate_class = False


Parent_class_name = Parent.__module__ + '.' + Parent.__name__
Child_class_name = Child.__module__ + '.' + Child.__name__
GrandChild_class_name = GrandChild.__module__ + '.' + GrandChild.__name__

TREE = {
    'one': {
        'children': {
            'two': {'class': Child_class_name},
            'three': None,
            'four': {
                'children': {
                    'five': {'class': GrandChild_class_name},
                    'six': {},
                },
                'class': Child_class_name
            },
        },
        'class': Parent_class_name,
    },
}


def test_default():
    cset = DCS(TREE)

    assert isinstance(cset['one'], Parent)
    assert isinstance(cset['one']['children']['two'], Child)
    assert cset['one']['children']['three'] is None
    assert isinstance(cset['one']['children']['four'], Child)
    assert isinstance(cset['one']['children']['four']['children']['five'], GrandChild)
    assert type(cset['one']['children']['four']['children']['six']) == dict  # trimmed conf
