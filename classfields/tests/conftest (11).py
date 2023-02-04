import os
import asyncio
import gzip
import pickle
import pytest
import itertools
import pathlib
from typing import Dict, Generator

from tortoise.contrib.test import finalizer, initializer

from app.core import geo_tree, cur_tz
from app.core.config import settings


OFFER_ID = itertools.count(60873378)


@pytest.fixture(scope="session", autouse=True)
def event_loop():
    """Create an instance of the default event loop for each test case."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="module", autouse=True)
def database(request, event_loop):
    initializer(
        ["app.db.models"],
        db_url=f"postgres://{settings.POSTGRES_USER}:{settings.POSTGRES_PASSWORD}@"
        f"{settings.POSTGRES_SERVER}:{settings.POSTGRES_PORT}/{settings.POSTGRES_DB}?"
        f"minsize={settings.POSTGRES_POOL_MIN_SIZE}&maxsize={settings.POSTGRES_POOL_MAX_SIZE}",
        app_label="models",
        loop=event_loop,
    )
    os.environ["TIMEZONE"] = cur_tz.zone
    request.addfinalizer(finalizer)


@pytest.fixture(scope="session", autouse=True)
async def build_geo_tree(event_loop):
    if "DISABLE_GEO" not in os.environ:
        with gzip.open("tests/resources/geo_tree.gz") as r:
            geo_tree._tree = pickle.loads(r.read())


@pytest.fixture()
def drom_trucks_url() -> str:
    return f"https://spec.drom.ru/ufa/truck/kamaz-6520-ljux-{next(OFFER_ID)}.html"


@pytest.fixture()
def drom_cars_url() -> str:
    return f"https://krasnoyarsk.drom.ru/audi/a4/{next(OFFER_ID)}.html"


@pytest.fixture()
def drom_trucks_photo_url() -> str:
    return f"https://static.baza.farpost.ru/v/{next(OFFER_ID)}_bulletin"


@pytest.fixture()
def drom_cars_photo_url() -> str:
    return f"https://s.auto.drom.ru/i24221/s/photos/29853/29852756/gen1200_{next(OFFER_ID)}.jpg"


@pytest.fixture()
def avito_trucks_url() -> str:
    return f"https://www.avito.ru/taganrog/gruzoviki_i_spetstehnika/prodam_gazel_biznes_{next(OFFER_ID)}"


@pytest.fixture()
def avito_mobile_cars_url() -> str:
    return f"https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_{next(OFFER_ID)}"


@pytest.fixture()
def avito_cars_url() -> str:
    return f"https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_{next(OFFER_ID)}"


@pytest.fixture()
def amru_cars_url() -> str:
    return f"https://auto.youla.ru/advert/used/nissan/qashqai/prv--{next(OFFER_ID)}"

@pytest.fixture()
def pages() -> Generator[Dict[str, str], None, None]:
    pages = {}
    for page_file in pathlib.Path("tests/deactivate/pages").glob("*/*.html"):
        with open(page_file, "rb") as reader:
            key = f"{page_file.parts[-2]}/{page_file.parts[-1]}"
            pages[key] = reader.read()
    yield pages


@pytest.fixture()
def pages_class(request, pages: Dict[str, str]) -> None:
    request.cls.pages = pages
