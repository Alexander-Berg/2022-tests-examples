import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema.main import MainSection, ServiceType
from maps.infra.sedem.lib.config.schema.tests.shared import extract_errors


def test_valid_minimal_main_section() -> None:
    main = MainSection.parse_obj({
        'service_name': 'maps-core-fake',
    })

    assert main == MainSection.construct(
        service_name='maps-core-fake',
        service_type=ServiceType.RTC,
        abc_service='maps-core-fake',
        sox=False,
        custom_tag=[],
        use_testing_machine=False,
    )


def test_valid_full_main_section() -> None:
    main = MainSection.parse_obj({
        'service_name': 'maps-custom-fake',
        'abc_service': 'maps-fake',
        'sox': True,
        'custom_tag': ['tag'],
        'use_testing_machine': True,
    })

    assert main == MainSection.construct(
        service_name='maps-custom-fake',
        service_type=ServiceType.RTC,
        abc_service='maps-fake',
        sox=True,
        custom_tag=['tag'],
        use_testing_machine=True,
    )


def test_old_format_name() -> None:
    main = MainSection.parse_obj({
        'name': 'fake',
    })

    assert main.service_name == 'maps-core-fake'


def test_old_format_name_with_custom_subsystem() -> None:
    main = MainSection.parse_obj({
        'name': 'service',
        'subsystem': 'b2bgeo',
    })

    assert main.service_name == 'maps-b2bgeo-service'


def test_missing_name() -> None:
    with pytest.raises(ValidationError) as exc:
        MainSection.parse_obj({})

    assert extract_errors(exc) == [
        '"service_name" field required'
    ]
