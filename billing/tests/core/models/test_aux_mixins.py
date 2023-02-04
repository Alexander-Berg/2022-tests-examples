
from mdh.core.models.aux_mixins import WithBasics


def test_validate_alias():

    validate = WithBasics.validate_alias

    assert validate('some1_')
    assert not validate('_some1_')
    assert not validate('some-')
    assert not validate('some ')
    assert not validate('someчто')
    assert not validate('?ome')
    assert not validate('111')  # не должны пересекаться с id

    # проверка максимальной длины
    assert validate('s' * 150)
    assert not validate('s' * 151)
