import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema.dependencies import DependenciesSection, DependencyService
from maps.infra.sedem.lib.config.schema.tests.shared import extract_errors


def test_valid_dependencies_section() -> None:
    dependencies = DependenciesSection.parse_obj({
        'datasets': ['jams-speeds'],
        'services': [{'path': 'maps/dependency'}],
    })

    assert dependencies == DependenciesSection.construct(
        datasets=['jams-speeds'],
        services=[DependencyService(path='maps/dependency')],
    )


def test_invalid_empty_dependencies_section() -> None:
    with pytest.raises(ValidationError) as exc:
        DependenciesSection.parse_obj({})

    assert extract_errors(exc) == [
        'one of "datasets" or "services" must be defined'
    ]
