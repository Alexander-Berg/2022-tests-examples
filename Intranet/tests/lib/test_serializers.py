from collections import namedtuple

from pretend import stub
from review.lib.serializers import Serializer, F
from tests.helpers import assert_is_substructure

GENDER_VERBOSE = {'m': 'male', 'f': 'female'}

USER_STUB = stub(
    name='simple_name',
    gender='m',
    age='42',
)


class UserSerializer(Serializer):
    fields = {
        'name',
        F('gender', verbose=GENDER_VERBOSE),
        F('age', cast_type=int)
    }


class GroupSerializer(Serializer):
    fields = {
        'name',
        F('users', complex=UserSerializer, many=True, complex_fields={'name', 'gender'}),
        F('roles', source='get_roles()'),
    }

    @classmethod
    def get_roles(cls, group):
        return list(dict.fromkeys([role.upper() for role in group.roles]))


class ParamsSerializer(Serializer):
    fields = {
        F('members_count', source='[members_count]', cast_type=int),
    }


class GroupInfoSerializer(Serializer):
    fields = {
        F('head', source='head_*', complex=UserSerializer),
        F('group', complex=GroupSerializer),
        F('params', complex=ParamsSerializer),
    }


class ExtendedUserSerializer(Serializer):
    fields = {
        F('user', source='*', complex=UserSerializer),
        'extension',
    }


def test_serializer_simple():
    obj = USER_STUB
    serialized_expected = {
        'name': 'simple_name',
        'gender': 'male',
        'age': 42,
    }
    serialized = UserSerializer.serialize(obj)
    assert_is_substructure(serialized_expected, serialized)


def test_serializer_with_method():
    obj = stub(name='simple_name', roles=['a', 'B'])
    serialized_expected = {
        'name': 'simple_name',
        'roles': ['A', 'B'],
    }
    serialized = GroupSerializer.serialize(obj, fields_requested={'name', 'roles'})
    assert_is_substructure(serialized_expected, serialized)


def test_serializer_inner():
    obj = stub(name='simple_name', roles=['a', 'B'], users=[
        stub(name='user_a', age=1, gender='m'),
        stub(name='user_b', age=2, gender='f'),
    ])
    serialized_expected = {
        'name': 'simple_name',
        'roles': ['A', 'B'],
        'users': [
            {
                'name': 'user_a',
                'gender': 'male',
            },
            {
                'name': 'user_b',
                'gender': 'female',
            },
        ]
    }
    serialized = GroupSerializer.serialize(obj)
    assert_is_substructure(serialized_expected, serialized)


def test_serializer_inner_with_filter():
    obj = stub(name='simple_name', roles=['a', 'B'], users=[
        stub(name='user_a', age=1, gender='m'),
        stub(name='user_b', age=2, gender='f'),
    ])
    serialized_expected = {
        'name': 'simple_name',
        'users': [
            {
                'gender': 'male',
            },
            {
                'gender': 'female',
            },
        ]
    }
    serialized = GroupSerializer.serialize(obj, fields_requested={'name', 'users.gender'})
    assert_is_substructure(serialized_expected, serialized)


def test_serializer_itemgetter():
    obj = {
        'members_count': 42,
        'useless': 'useless'
    }
    serialized_expected = {
        'members_count': 42,
    }
    serialized = ParamsSerializer.serialize(obj)
    assert_is_substructure(serialized_expected, serialized)


def test_field_source_cache_same_class_name():

    ToSerializeCls = namedtuple('ToSerializeCls', ['field_type1', 'field_type2'])
    to_serialize = ToSerializeCls(field_type1='1', field_type2='2')

    class InnerCtx(Serializer):
        fields = {F('field', source='field_type1')}
        default_fields = {'field'}

    InnerCtx.serialize(to_serialize)

    class InnerCtx(Serializer):
        fields = {F('field', source='field_?ctx?')}
        default_fields = {'field'}

    serialized_type2 = InnerCtx.serialize(to_serialize, context=dict(ctx='type2'))
    assert serialized_type2['field'] == to_serialize.field_type2


def test_serializer_complex():
    obj = stub(
        head_name='head_name', head_age='24', head_gender='m',
        group=stub(name='group_name', roles=[], users=[
            stub(name='user_a', age=1, gender='m'),
            stub(name='user_b', age=2, gender='f'),
        ]),
        params={'members_count': 42}
    )
    serialized_expected = {
        'head': {
            'name': 'head_name',
            'age': 24,
        },
        'group': {
            'name': 'group_name',
            'roles': [],
            'users': [
                {
                    'name': 'user_a',
                    'gender': 'male',
                },
                {
                    'name': 'user_b',
                    'gender': 'female',
                },
            ]
        },
        'params': {
            'members_count': 42
        }
    }

    serialized = GroupInfoSerializer.serialize(obj, fields_requested={
        'head_name',
        'head_age',
        'group',
        'params',
    })
    assert_is_substructure(serialized_expected, serialized)


def test_serializer_from_self_fields():
    obj = stub(name='simple_name', gender='m', age='42', extension='ext')
    serialized_expected = {
        'user': {
            'name': 'simple_name',
            'gender': 'male',
            'age': 42,
        },
        'extension': 'ext',
    }
    serialized = ExtendedUserSerializer.serialize(obj)
    assert_is_substructure(serialized_expected, serialized)


def test_complex_in_complex():
    obj = stub(
        group=stub(
            users=[USER_STUB]
        ),
    )
    serialized = GroupInfoSerializer.serialize(
        obj=obj,
        fields_requested=['group.users'],
    )

    assert serialized == {
        'group': {
            'users': [
                {
                    'name': 'simple_name',
                    'gender': 'male',
                },
            ]
        }
    }
