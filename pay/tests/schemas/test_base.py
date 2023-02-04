from dataclasses import dataclass

import marshmallow

from hamcrest import assert_that, has_properties

from pay.lib.schemas.base import BaseDataClassSchema


@dataclass
class User:
    name: str
    age: int


@dataclass
class Entity:
    user: User
    description: str


class UserSchema(BaseDataClassSchema[User]):
    name = marshmallow.fields.String(required=True, allow_none=False)
    age = marshmallow.fields.Integer(required=True, allow_none=False)


class EntitySchema(BaseDataClassSchema[Entity]):
    user = marshmallow.fields.Nested(UserSchema, required=True, allow_none=False)
    description = marshmallow.fields.String(required=True, allow_none=False)


def test_load_one():
    entity_schema = EntitySchema()
    record = dict(user=dict(name='Alice', age=13), description='One')

    entity = entity_schema.load_one(record)

    assert_that(
        entity,
        has_properties(
            dict(
                user=has_properties(
                    dict(
                        name='Alice',
                        age=13,
                    )
                ),
                description='One',
            )
        ),
    )


def test_load_many():
    entity_schema = EntitySchema()

    def records():
        yield dict(user=dict(age=13, name='Alice'), description='One')
        yield dict(
            user=dict(
                age=14,
                name='Bob',
            ),
            description='Two',
        )

    collection = entity_schema.load_many(records())

    assert len(collection) == 2
    assert_that(
        collection[0],
        has_properties(
            dict(
                user=has_properties(
                    dict(
                        name='Alice',
                        age=13,
                    )
                ),
                description='One',
            )
        ),
    )
    assert_that(
        collection[1],
        has_properties(
            dict(
                user=has_properties(
                    dict(
                        name='Bob',
                        age=14,
                    )
                ),
                description='Two',
            )
        ),
    )
