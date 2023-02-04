import pytest
import logging

from yatest.common import test_source_path

from maps.libs.ymapsdf.py.name_type import NameType
from maps.pylibs import locale

from maps.garden.sdk.test_utils import task_tester
from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.libs.osm.osm_object.osm_object_id import OsmObjectId, OsmObjectType
from maps.garden.modules.ymapsdf_osm.lib.names import YmapsdfName, get_all_names, ComputeNames, LogQueue

from .utils import TEST_PROPERTIES

logger = logging.getLogger("test_names")


@pytest.mark.parametrize(
    ("tags", "expected"),
    [
        (
            {
                "name": "улица Ленина",
            },
            [
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "name": "улица Ленина",
                "name:be-tarask": "вулица Ленина",
                "name:en": "Lenina street",
            },
            [
                YmapsdfName(lang="be", variant="tarask", name="вулица Ленина", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="en", name="Lenina street", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "name": "улица Ленина",
                "name:ru": "улица Ленина",
                "name:en": "Lenina street",
            },
            [
                YmapsdfName(lang="en", name="Lenina street", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "official_name": "улица Ленина",
                "official_name:en": "Lenina street",
            },
            [
                YmapsdfName(lang="en", name="Lenina street", name_type=NameType.OFFICIAL, is_local=False),
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.OFFICIAL, is_local=True),
            ],
        ),
        (
            {
                "name": "улица Ленина",
                "name:en": "Lenina street",
                "alt_name": "улица Ленина",
                "alt_name:en": "Lenina street; Lenin street",
            },
            [
                YmapsdfName(lang="en", name="Lenina street", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
                YmapsdfName(lang="en", name="Lenina street", name_type=NameType.SYNONYM, is_local=False),
                YmapsdfName(lang='en', name='Lenin street', name_type=NameType.SYNONYM, is_local=False),
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.SYNONYM, is_local=True),
            ],
        ),
        (
            # Synonym without an official name
            {
                "alt_name": "улица Ленина",
            },
            [
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            # Bad language suffixes
            {
                "name": "Осиновка",
                "name:prefix": "деревня",
                "name:invalid": "this name must be ignored",
            },
            [
                YmapsdfName(lang="ru", name="Осиновка", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "name": "; улица Ленина",
            },
            [
                YmapsdfName(lang="ru", name="улица Ленина", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "name": "italian street",
                "name:fr": "french street",
                "name:it": "italian street",
            },
            [
                YmapsdfName(lang="fr", name="french street", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="it", name="italian street", name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang="ru", name="italian street", name_type=NameType.FOR_LABEL, is_local=True),
            ],
        ),
        (
            {
                "name:en": "street",
                "name:it": "street",
                "name": "street",
            },
            [
                YmapsdfName(lang='en', name='street', name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang='it', name='street', name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang='ru', name='street', name_type=NameType.FOR_LABEL, is_local=True),
            ]
        ),
        (
            {
                "name:ru": "улица Ленина",
                "name:en": "улица Ленина; Lenina street",
                "name": "улица Ленина",
            },
            [
                YmapsdfName(lang='en', name='улица Ленина', name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang='en', name='Lenina street', name_type=NameType.FOR_LABEL, is_local=False),
                YmapsdfName(lang='ru', name='улица Ленина', name_type=NameType.FOR_LABEL, is_local=True),
            ]
        ),
        (
            {
                "name": "Хорошевское шоссе",
                "name:": "Хорошевский проспект",
            },
            [
                YmapsdfName(lang='ru', name='Хорошевское шоссе', name_type=NameType.FOR_LABEL, is_local=True)
            ]
        ),
        (
            {
                "name:en": "Birmingham",
                "name:en:ipa": "/ˈbɜːrmɪŋəm/",
            },
            [
                YmapsdfName(lang='en', name='Birmingham', name_type=NameType.FOR_LABEL, is_local=False)
            ]
        )
    ],
)
def test_names(tags, expected):
    log_queue = LogQueue("test_names", logger)
    object_id = OsmObjectId(type=OsmObjectType.WAY, id=1)
    result = list(
        get_all_names(
            object_id.to_ymapsdf_id(index=0),
            tags,
            local_language=locale.Language("ru"),
            log_queue=log_queue)
    )
    result.sort(key=lambda name: (name.name_type, name.lang))
    assert result == expected


@pytest.fixture
def task_executor(environment_settings):
    return task_tester.TestTaskExecutor(
        environment_settings,
        properties=TEST_PROPERTIES,
        test_data_path=task_tester.TestDataPath(
            schemas_path=test_source_path("schemas"),
            input_path=test_source_path("data/test_compute_names"),
            output_path="",
        ),
    )


@pytest.mark.use_local_yt("hahn")
def test_compute_names(task_executor):
    input_resources = {
        "objects": task_executor.create_ymapsdf_input_yt_table_resource("rd"),
        "tags": task_executor.create_custom_input_yt_table_resource("rd_tags"),
    }

    output_resources = {
        "names": task_executor.create_yt_table_resource("rd_nm"),
        "log_table": task_executor.create_yt_table_resource("log_bad_rd_nm")
    }

    task_executor.execute_task(
        task=ComputeNames("rd_nm"),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=[output_resources["names"]]
    )
