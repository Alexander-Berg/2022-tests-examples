import typing as tp

import pytest
from google.protobuf import json_format

from maps.infra.sandbox import ReleaseSpec
from sandbox import sdk2


def create_test_parameters(required: bool = False) -> sdk2.Parameters:
    class ExampleParameters(sdk2.Parameters):
        secret = sdk2.parameters.YavSecret('Some secret', required=required)
        string = sdk2.parameters.String('Some string', required=required)
        integer = sdk2.parameters.Integer('Some integer', required=required)
        float_ = sdk2.parameters.Float('Some float', required=required)
        json = sdk2.parameters.JSON('Some json', required=required)
        staff = sdk2.parameters.Staff('Some staff login', required=required)
        vault = sdk2.parameters.Vault('Some old vault secret', required=required)
        boolean = sdk2.parameters.Bool('Some boolean', required=required)

        arcadia_url = sdk2.parameters.ArcadiaUrl('Arcadia URL', required=required)
        strict_string = sdk2.parameters.StrictString('Some strict string', regexp=r'[a-zA-Z]+', required=required)

        info_block = sdk2.parameters.Info('Just info block', required=required)
        resource = sdk2.parameters.Resource('Some resource', required=required)
        task = sdk2.parameters.Task('Some task', required=required)

        with sdk2.parameters.Group('Just a group', required=required) as group:
            list_ = sdk2.parameters.List('Some list')
            dict_ = sdk2.parameters.Dict('Some dict')

        with sdk2.parameters.CheckGroup('Just a multiselect field', required=required) as multiselect:
            multiselect.values.option1 = 'Option One'
            multiselect.values.option2 = multiselect.Value('Option Two', checked=True)
            multiselect.values.option3 = 'Option Three'

        with sdk2.parameters.RadioGroup('Just a radio field group', required=required) as radio_group:
            radio_group.values.value1 = None
            radio_group.values.value2 = radio_group.Value(default=True)
            radio_group.values.value3 = None

    return ExampleParameters


@pytest.mark.parametrize('required', (False, True))
def test_spec_serialization(required: bool) -> None:
    spec = ReleaseSpec(create_test_parameters(required))

    assigned = spec.add_deploy_unit('assigned')
    assigned.secret = 'sec-XXX@ver-YYY#key'
    assigned.string = 'some value'
    assigned.integer = 42
    assigned.float_ = 123.45
    assigned.json = '{"key": "value"}'
    assigned.staff = 'john-doe'
    assigned.vault = 'OWNER:NAME'
    assigned.boolean = True

    assigned.arcadia_url = 'arcadia-arc:/#trunk'
    assigned.strict_string = 'abcdef'

    assigned.resource = 123456
    assigned.task = 1234

    assigned.list_ = ['a', 'b', 'c']
    assigned.dict_ = {'key': 'value'}

    assigned.multiselect = ['option1', 'option2']
    assigned.radio_group = 'value3'

    spec.add_deploy_unit('unassigned')

    spec_dict = json_format.MessageToDict(
        spec.as_proto(),
        preserving_proto_field_name=True,
    )
    assert spec_dict == {'deploy_units': [
        {
            'name': 'assigned',
            'secrets': [{
                'name': 'secret',
                'secret': {
                    'secret_id': 'sec-XXX',
                    'version_id': 'ver-YYY',
                    'key': 'key',
                },
            }],
            'parameters': [{
                'name': 'string',
                'jsonValue': '"some value"',
            }, {
                'name': 'integer',
                'jsonValue': '42',
            }, {
                'name': 'float_',
                'jsonValue': '123.45',
            }, {
                'name': 'json',
                'jsonValue': '"{\\"key\\": \\"value\\"}"',
            }, {
                'name': 'staff',
                'jsonValue': '"john-doe"',
            }, {
                'name': 'vault',
                'jsonValue': '"OWNER:NAME"',
            }, {
                'name': 'boolean',
                'jsonValue': 'true',
            }, {
                'name': 'arcadia_url',
                'jsonValue': '"arcadia-arc:/#trunk"',
            }, {
                'name': 'strict_string',
                'jsonValue': '"abcdef"',
            }, {
                'name': 'resource',
                'jsonValue': '123456',
            }, {
                'name': 'task',
                'jsonValue': '1234',
            }, {
                'name': 'list_',
                'jsonValue': '["a", "b", "c"]',
            }, {
                'name': 'dict_',
                'jsonValue': '{"key": "value"}',
            }, {
                'name': 'multiselect',
                'jsonValue': '["option1", "option2"]',
            }, {
                'name': 'radio_group',
                'jsonValue': '"value3"',
            }],
        },
        {
            'name': 'unassigned',
            'secrets': [{
                'name': 'secret',
            }],
            'parameters': [{
                'name': 'string',
                'jsonValue': '""',
            }, {
                'name': 'integer',
                'jsonValue': '0',
            }, {
                'name': 'float_',
                'jsonValue': '0.0',
            }, {
                'name': 'json',
                'jsonValue': 'null',
            }, {
                'name': 'staff',
                'jsonValue': '""',
            }, {
                'name': 'vault',
                'jsonValue': '""',
            }, {
                'name': 'boolean',
                'jsonValue': 'false',
            }, {
                'name': 'arcadia_url',
                'jsonValue': 'null' if required else '""',
            }, {
                'name': 'strict_string',
                'jsonValue': '""',
            }, {
                'name': 'resource',
                'jsonValue': 'null',
            }, {
                'name': 'task',
                'jsonValue': 'null',
            }, {
                'name': 'list_',
                'jsonValue': '[]',
            }, {
                'name': 'dict_',
                'jsonValue': '{}',
            }, {
                'name': 'multiselect',
                'jsonValue': '["option2"]',
            }, {
                'name': 'radio_group',
                'jsonValue': '"value2"',
            }],
        },
    ]}


def test_wrong_parameter_name() -> None:
    spec = ReleaseSpec(create_test_parameters())

    stable_unit = spec.add_deploy_unit('stable')
    with pytest.raises(Exception, match=r'Unknown parameter: unknown'):
        stable_unit.unknown = 'some value'


def test_custom_default_for_secret_parameter() -> None:
    class SingleSecretParameters(sdk2.Parameters):
        secret = sdk2.parameters.YavSecret('Some secret', default='sec-ABC')

    spec = ReleaseSpec(SingleSecretParameters)
    spec.add_deploy_unit('stable')

    spec_dict = json_format.MessageToDict(
        spec.as_proto(),
        preserving_proto_field_name=True,
    )
    assert spec_dict == {'deploy_units': [{
        'name': 'stable',
        'secrets': [{
            'name': 'secret',
            'secret': {
                'secret_id': 'sec-ABC',
            },
        }],
    }]}


def test_bad_value_for_secret_parameter() -> None:
    class SingleSecretParameters(sdk2.Parameters):
        secret = sdk2.parameters.YavSecret('Some secret')

    spec = ReleaseSpec(SingleSecretParameters)
    stable_unit = spec.add_deploy_unit('stable')
    stable_unit.secret = 'not-a-secret'

    with pytest.raises(Exception, match=r'invalid secret uuid: not-a-secret'):
        spec.as_proto()


@pytest.mark.parametrize('selector_value,expected_spec', [
    (
        'all',
        {'deploy_units': [{
            'name': 'stable',
            'secrets': [{'name': 'common_secret'}],
            'parameters': [{'name': 'service_selector', 'jsonValue': '"all"'},
                           {'name': 'path_prefix', 'jsonValue': '"maps/"'}],
        }]},
    ),
    (
        'selected',
        {'deploy_units': [{
            'name': 'stable',
            'secrets': [{'name': 'common_secret'}],
            'parameters': [{'name': 'service_selector', 'jsonValue': '"selected"'},
                           {'name': 'service_names', 'jsonValue': '[]'},
                           {'name': 'path_prefix', 'jsonValue': '"maps/"'}],
        }]},
    ),
    (
        'affected',
        {'deploy_units': [{
            'name': 'stable',
            'secrets': [{'name': 'arc_secret'}, {'name': 'common_secret'}],
            'parameters': [{'name': 'service_selector', 'jsonValue': '"affected"'},
                           {'name': 'arcadia_url', 'jsonValue': '"arcadia-arc:/#trunk"'},
                           {'name': 'review_id', 'jsonValue': '0'},
                           {'name': 'path_prefix', 'jsonValue': '"maps/"'}],
        }]},
    ),
])
def test_ignore_hidden_by_radio_group(selector_value: str, expected_spec: dict[str, tp.Any]) -> None:
    class SedemLikeParameters(sdk2.Parameters):
        with sdk2.parameters.RadioGroup('Services to process') as service_selector:
            service_selector.values['all'] = service_selector.Value(
                'All services',
            )
            service_selector.values['selected'] = service_selector.Value(
                'Selected services',
            )
            service_selector.values['affected'] = service_selector.Value(
                'Services affected by commit',
                default=True,
            )

        with service_selector.value['selected']:
            service_names = sdk2.parameters.List('Names of selected services', required=True)

        with service_selector.value['affected']:
            arcadia_url = sdk2.parameters.ArcadiaUrl('Arcadia Url', default='arcadia-arc:/#trunk', required=True)
            review_id = sdk2.parameters.Integer('Arcanum review id')
            arc_secret = sdk2.parameters.YavSecret('Arc secret', required=True)

        path_prefix = sdk2.parameters.String('Path prefix', default='maps/', required=True)
        common_secret = sdk2.parameters.YavSecret('Common secret')

    spec = ReleaseSpec(SedemLikeParameters)
    stable_spec = spec.add_deploy_unit('stable')
    stable_spec.service_selector = selector_value

    spec_dict = json_format.MessageToDict(
        spec.as_proto(),
        preserving_proto_field_name=True,
    )
    assert spec_dict == expected_spec


@pytest.mark.parametrize('checkbox_value,expected_spec', [
    (
        True,
        {'deploy_units': [{
            'name': 'stable',
            'secrets': [{'name': 'enabled_secret'}],
            'parameters': [{'name': 'enabled', 'jsonValue': 'true'},
                           {'name': 'enabled_string', 'jsonValue': '""'}],
        }]},
    ),
    (
        False,
        {'deploy_units': [{
            'name': 'stable',
            'secrets': [{'name': 'disabled_secret'}],
            'parameters': [{'name': 'enabled', 'jsonValue': 'false'},
                           {'name': 'disabled_string', 'jsonValue': '""'}],
        }]},
    ),
])
def test_ignore_hidden_by_checkbox(checkbox_value: bool, expected_spec: dict[str, tp.Any]) -> None:
    class CheckboxedParameters(sdk2.Parameters):
        enabled = sdk2.parameters.Bool('Enabled', default=False)

        with enabled.value[True]:
            enabled_secret = sdk2.parameters.YavSecret('Enabled secret', required=True)
            enabled_string = sdk2.parameters.String('Enabled string', required=True)

        with enabled.value[False]:
            disabled_secret = sdk2.parameters.YavSecret('Disabled secret', required=True)
            disabled_string = sdk2.parameters.String('Disabled string', required=True)

    spec = ReleaseSpec(CheckboxedParameters)
    stable_spec = spec.add_deploy_unit('stable')
    stable_spec.enabled = checkbox_value

    spec_dict = json_format.MessageToDict(
        spec.as_proto(),
        preserving_proto_field_name=True,
    )
    assert spec_dict == expected_spec


def test_ignore_hidden_by_outer_checkbox() -> None:
    class NestedCheckboxedParameters(sdk2.Parameters):
        outer_checkbox = sdk2.parameters.Bool('Outer checkbox', default=False)
        with outer_checkbox.value[True]:
            inner_checkbox = sdk2.parameters.Bool('Inner checkbox', default=False)
            with inner_checkbox.value[True]:
                secret = sdk2.parameters.YavSecret('Secret', required=True)

    spec = ReleaseSpec(NestedCheckboxedParameters)
    stable_spec = spec.add_deploy_unit('stable')
    stable_spec.outer_checkbox = False
    stable_spec.inner_checkbox = True

    spec_dict = json_format.MessageToDict(
        spec.as_proto(),
        preserving_proto_field_name=True,
    )
    assert spec_dict == {'deploy_units': [{
        'name': 'stable',
        'parameters': [{
            'name': 'outer_checkbox',
            'jsonValue': 'false',
        }],
    }]}
