import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.util.errors import SchemaValidatorError


@skip_if_remote
def test_array(schema_validator):
    with pytest.raises(SchemaValidatorError, match="Json schema validation failed"):
        schema_validator.validate_object("Company", [{"name1": "name"}])


@skip_if_remote
def test_required(schema_validator):
    schema_validator.validate_object("Company", {"name": "name"})


@skip_if_remote
def test_type(schema_validator):
    with pytest.raises(SchemaValidatorError, match="Json schema validation failed: Company: 2.1 is not of type 'string'"):
        schema_validator.validate_object("Company", {"name": 2.1})

    with pytest.raises(SchemaValidatorError, match="Json schema validation failed: Company: 1 is not of type 'string'"):
        schema_validator.validate_object(
            "Company", {"name": "name", "logo_url": 1})

    schema_validator.validate_object(
        "Company", {"name": "name", "logo_url": "logo_url"})


@skip_if_remote
def test_skip_non_existing(schema_validator):
    schema_validator.validate_object(
        "Company", {"name": "name", "non_existing1": "non_existing", "non_existing2": 42})
