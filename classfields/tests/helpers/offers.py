import os

from app.schemas.enums import CategoryType
from app.schemas.offer import OfferBaseInDbSchema
from tests.helpers import random_data


def offer_db() -> OfferBaseInDbSchema:
    return OfferBaseInDbSchema(
        hash=random_data.md5(),
        origin_url=f"http://avito.ru/moskva/cool_offer_" f"{random_data.random_lower_string()}",
        category=CategoryType.from_input(random_data.random_small_integer()),
        phone=str(random_data.random_integer(lower=89000000000, upper=89999999999)),
        region_id=random_data.random_integer(lower=1, upper=512),
        data=os.urandom(10),
    )
