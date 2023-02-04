from typing import Optional

import pytest
from marshmallow import ValidationError, fields, pre_load

from maps_adv.common.protomallow import (
    OutputValidationError,
    ProtobufSchema,
    with_schemas,
)
from maps_adv.common.protomallow.tests.proto.for_tests_pb2 import (
    WithSchemasInputMessage,
    WithSchemasOutputMessage,
)

pytestmark = [pytest.mark.asyncio]


class InputSchema(ProtobufSchema):
    class Meta:
        pb_message_class = WithSchemasInputMessage

    some_input = fields.Integer()

    @pre_load
    def replace_value(self, data):
        if data["some_input"] == 99:
            data["some_input"] = "string_to_fail_validation"

        return data


class OutputSchema(ProtobufSchema):
    class Meta:
        pb_message_class = WithSchemasOutputMessage

    some_output = fields.Integer()


@pytest.mark.parametrize(
    "input_schema, output_schema, expected_result",
    [
        (InputSchema, None, {"some_output": 1}),
        (None, OutputSchema, b"\x08*"),
        (InputSchema, OutputSchema, b"\x08\x01"),
    ],
)
async def test_applies_validation_schemas_correctly(
    input_schema, output_schema, expected_result
):
    class ForTest:
        @with_schemas(input_schema=input_schema, output_schema=output_schema)
        async def wrap_me(self, some_input: Optional[int] = 42):
            return {"some_output": some_input}

    data = WithSchemasInputMessage(some_input=1).SerializeToString()
    got = await ForTest().wrap_me(data=data)

    assert got == expected_result


async def test_raises_if_output_validation_fails():
    class ForTest:
        @with_schemas(input_schema=None, output_schema=OutputSchema)
        async def wrap_me(self):
            return {"some_output": "string_to_fail_validation"}

    with pytest.raises(OutputValidationError) as exc:
        await ForTest().wrap_me()

    assert isinstance(exc.value.inner_exception, ValidationError)


async def test_raises_if_input_validation_fails():
    class ForTest:
        @with_schemas(input_schema=InputSchema, output_schema=None)
        async def wrap_me(self, some_input):
            return {"some_output": some_input}

    data = WithSchemasInputMessage(some_input=99).SerializeToString()

    with pytest.raises(ValidationError) as exc:
        await ForTest().wrap_me(data=data)

    assert exc.value.messages == {"some_input": ["Not a valid integer."]}
