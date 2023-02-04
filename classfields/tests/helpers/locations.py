import pytest

from app.schemas import location

from . import random_data as rnd

T = location.Location


def random_loc() -> T:
    loc = T(
        id=rnd.random_integer(),
        parent_id=None,
        country=rnd.random_integer(),
        name=rnd.random_lower_string(),
        en_name=rnd.random_lower_string(),
        synonyms=[rnd.random_lower_string() for _ in range(10)],
        type=rnd.random_integer(upper=5),
        lat=rnd.random_float(180),
        lon=rnd.random_float(180),
    )
    return loc


@pytest.fixture(scope="function")
def loc() -> T:
    return random_loc()
