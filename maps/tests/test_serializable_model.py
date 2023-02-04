from enum import Enum

import pytest

from maps.infra.sedem.common.utils.serializable_pydantic_model import SerializableBaseModel


def test_bad_bound_type() -> None:
    with pytest.raises(Exception, match=r'Bound type must be a subclass of EnumStr'):
        class AbstractModel(SerializableBaseModel, bind_type=object):
            pass


def test_missing_bound_type() -> None:
    with pytest.raises(Exception, match=r'Bound type is missing'):
        class AbstractModel(SerializableBaseModel):
            pass


def test_bound_type_redefinition() -> None:
    class FirstEnum(str, Enum):
        pass

    class SecondEnum(str, Enum):
        pass

    class AbstractModel(SerializableBaseModel, bind_type=FirstEnum):
        pass

    with pytest.raises(Exception, match=r'Redefinition of bound type'):
        class SubclassModel(AbstractModel, bind_type=SecondEnum):
            pass


def test_bind_type_with_member_in_same_class() -> None:
    class TestEnum(str, Enum):
        MEMBER = 'member'

    with pytest.raises(Exception, match=r'Enum members must be bound to an inheritor of a class with bound type'):
        class AbstractModel(SerializableBaseModel, bind_type=TestEnum, bind=TestEnum.MEMBER):
            pass


def test_bound_type_mismatch_bound_enum_member() -> None:
    class FirstEnum(str, Enum):
        MEMBER = 'member'

    class SecondEnum(str, Enum):
        MEMBER = 'member'

    class AbstractModel(SerializableBaseModel, bind_type=FirstEnum):
        pass

    with pytest.raises(Exception, match=r'Bound enum member doesn\'t match with bound type'):
        class SubclassModel(AbstractModel, bind=SecondEnum.MEMBER):
            pass


class ServiceType(str, Enum):
    NANNY = 'nanny'
    GARDEN = 'garden'


class ServiceModel(SerializableBaseModel, bind_type=ServiceType, type_selector='service_type'):
    pass


class NannyModel(ServiceModel, bind=ServiceType.NANNY):
    nanny_field: str


class GardenModel(ServiceModel, bind=ServiceType.GARDEN):
    garden_field: str


@pytest.mark.parametrize('model,expected_value', [
    (NannyModel(nanny_field='123'), {'service_type': 'nanny', 'nanny_field': '123'}),
    (GardenModel(garden_field='456'), {'service_type': 'garden', 'garden_field': '456'}),
])
def test_serialize(model: ServiceModel, expected_value: dict[str, str]) -> None:
    assert model.dict() == expected_value


@pytest.mark.parametrize('value,expected_model', [
    ({'service_type': 'nanny', 'nanny_field': '123'}, NannyModel(nanny_field='123')),
    ({'service_type': 'garden', 'garden_field': '456'}, GardenModel(garden_field='456')),
])
def test_deserialize(value: dict[str, str], expected_model: ServiceModel) -> None:
    actual_model = ServiceModel.parse_obj(value)
    assert expected_model == actual_model


def test_deserialize_unknown_type() -> None:
    with pytest.raises(Exception, match=r'No known subclasses bound with type "unknown"'):
        ServiceModel.parse_obj({'service_type': 'unknown'})


def test_deserialize_invalid_type() -> None:
    with pytest.raises(Exception, match=r'Missing "service_type" field'):
        ServiceModel.parse_obj({'field': 'value'})
