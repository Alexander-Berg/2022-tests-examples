import hashlib
import random
from collections import namedtuple
from typing import Callable, Dict, List, Tuple

import pytest

from maps_adv.export.lib.core.client.old_geoadv import OrgPlace
from maps_adv.export.lib.core.enum import ActionType, ImageType
from maps_adv.export.lib.core.utils import generate_image_filename_from_avatar
from maps_adv.export.lib.pipeline.xml.transform.action import action_transform

AvatarsTestStructure = namedtuple(
    "AvatarsTestStructure", ["avatar", "filename", "hash"]
)


@pytest.fixture()
def avatars_factory(faker, config) -> Callable:
    def wrapped(image_type: ImageType) -> AvatarsTestStructure:
        data = dict(
            type=image_type,
            image_name="".join(faker.random_letters(32)),
            group_id=faker.random_int(),
            alias_template="".join(faker.random_letters(16)) + "_{zoom}",
        )

        image_hash = hashlib.sha256()
        image_hash.update(str(data["group_id"]).encode())
        image_hash.update(str(data["image_name"]).encode())

        return AvatarsTestStructure(
            data, generate_image_filename_from_avatar(data), image_hash.hexdigest()
        )

    return wrapped


@pytest.fixture()
def avatars_batch(avatars_factory) -> Callable:
    def wrapped(batch: Dict[str, ImageType]) -> Dict[str, AvatarsTestStructure]:
        result = dict()
        for key, image_type in batch.items():
            result[key] = avatars_factory(image_type)

        return result

    return wrapped


@pytest.fixture()
def companies_factory(faker) -> Callable:
    def wrapped(count: int) -> List[dict]:
        return [
            OrgPlace(
                latitude=random.random(),
                longitude=random.random(),
                title=faker.text(),
                address=faker.text(),
                permalink=index,
            )
            for index in range(count)
        ]

    return wrapped


@pytest.fixture()
def actions_factory(faker) -> Callable:
    async def wrapped() -> Tuple[List[dict], List[dict]]:
        actions = [
            dict(type=ActionType.PHONE_CALL, phone="+7(111)2223344"),
            dict(
                type=ActionType.OPEN_SITE,
                url="".join(faker.random_letters()),
                title="".join(faker.random_letters()),
            ),
        ]
        expected_actions = [action_transform(action) for action in actions]
        return actions, expected_actions

    return wrapped
