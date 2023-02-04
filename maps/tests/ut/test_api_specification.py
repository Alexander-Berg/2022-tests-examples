import yaml
from maps.b2bgeo.test_lib.schema_validation import check_russian_translations
from openapi_spec_validator import validate_spec
import library.python.resource as lpr


def test_openapi_specification_is_valid():
    spec_openapi = yaml.full_load(lpr.find('/ya_courier_backend/spec.yaml'))

    # We have to remove some 'required' items because openapi_spec_validator
    # fails to handle 'required' properly if 'allOf' is present.

    x = spec_openapi['components']['schemas']['CompanyWithInitialLogin']
    assert set(x['required']) == {'name', 'services', 'initial_login'}
    del x['required']

    x = spec_openapi['components']['schemas']['CourierCreate']
    assert set(x['required']) == {'number'}
    del x['required']

    x = spec_openapi['components']['schemas']['DepotCreate']
    assert set(x['required']) == {'number', 'address', 'lat', 'lon'}
    del x['required']

    x = spec_openapi['components']['schemas']['RouteCreate']
    assert set(x['required']) == {'number', 'date'}
    del x['required']

    x = spec_openapi['components']['schemas']['UserPost']
    assert set(x['required']) == {'login'}
    del x['required']

    x = spec_openapi['components']['schemas']['VehiclePost']
    assert set(x['required']) == {'number', 'routing_mode', 'name'}
    del x['required']

    x = spec_openapi['components']['schemas']['CourierReferencePost']
    assert set(x['required']) == {'number', 'phone'}
    del x['required']

    x = spec_openapi['components']['schemas']['ZoneReferencePost']
    assert set(x['required']) == {'number', 'polygon'}
    del x['required']

    # Validate (raises an exception if invalid)

    validate_spec(spec_openapi)
    check_russian_translations(spec_openapi)
