from staff.person.passport.base import BBField
from mock import Mock


def test_custom_converters():
    to_bb = Mock(return_value='c')
    from_bb = Mock(return_value='d')
    field = BBField(
        passport_name='foo', passport_field='bar', to_bb=to_bb, from_bb=from_bb
    )
    assert field.to_bb('a') == 'c'
    assert field.from_bb('a') == 'd'


def test_default_converters():
    field = BBField(passport_name='foo', passport_field='bar')

    assert field.to_bb('a') == 'a'
    assert field.from_bb('a') == 'a'


def test_set_get():
    field = BBField(passport_name='foo', passport_field='bar')

    instance = Mock(bb_data={'foo': 'a'}, for_update={})
    assert field.__get__(instance, owner=Mock()) == 'a'
    field.__set__(instance, 'b')
    assert instance.for_update == {'foo': 'b'}


def test_set_name():
    field = BBField(passport_name='foo', passport_field='bar')

    field.set_name('doom')
    assert field.name == 'doom'
