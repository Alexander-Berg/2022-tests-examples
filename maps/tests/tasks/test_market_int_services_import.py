import json

import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportMarketIntServicesTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportMarketIntServicesTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_market_int_services_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
$service_categories_active =
    SELECT
        service_categories.name AS category_name,
        service_categories_services.service_id as service_id
    FROM hahn.`//path/to/market_int_tables/service_categories` AS service_categories
    LEFT JOIN hahn.`//path/to/market_int_tables/service_categories_services` AS service_categories_services
        ON service_categories_services.category_id = service_categories.id
    WHERE service_categories.deleted = false;

$service_prices =
    SELECT 
        MIN(service_prices.duration_minutes) AS duration_minutes,
        MIN(service_prices.price) AS price,
        service_prices.service_id as service_id
    FROM hahn.`//path/to/market_int_tables/service_prices` AS service_prices
    GROUP BY service_prices.service_id;

$main_image_url_templates =
    SELECT 
        services.gallery_id as gallery_id,
        MIN_BY(url_template, ordering) AS url_template
    FROM hahn.`//path/to/market_int_tables/images` as images
    JOIN hahn.`//path/to/market_int_tables/services` as services
        ON images.gallery_id = services.gallery_id
    GROUP BY services.gallery_id;

$category_names =
    SELECT
        service_id,
        AGGREGATE_LIST(category_name) as category_names
    FROM $service_categories_active
    GROUP BY service_id;

$services = 
    SELECT 
        services.id AS id,
        services.biz_id AS biz_id,
        services.name AS name,
        services.description AS description,
        category_names.category_names AS categories,
        service_prices.price AS min_cost,
        service_prices.duration_minutes AS min_duration,
        services.client_action AS action_type,
        main_image_url_templates.url_template AS image,
        services.status AS status
    FROM hahn.`//path/to/market_int_tables/services` as services
    LEFT JOIN $service_prices AS service_prices 
        ON services.id = service_prices.service_id
    LEFT JOIN $main_image_url_templates AS main_image_url_templates 
        ON services.gallery_id = main_image_url_templates.gallery_id
    LEFT JOIN $category_names as category_names 
        ON services.id = category_names.service_id;

SELECT
    id,
    biz_id,
    name,
    description,
    categories,
    min_cost,
    min_duration,
    action_type,
    image
FROM $services WHERE status = 2; -- PUBLISHED
    """,  # noqa
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (
            100,
            9870,
            "title1",
            "description1",
            ["category1"],
            10,
            10,
            "action",
            "image1",
        ),
        (
            279105,
            3488109,
            "Репетитор по Английскому",
            "Английский по Скайпу.",
            [b"General English (45/60\xa0min)"],
            500.0,
            45,
            "booking_with_employee",
            "image2/%s",
        ),
        (
            19490,
            1539326,
            "Окрашивание корней",
            b"NIRVEL\nL'Or\xe9al INOA",
            ["Популярные услуги", "Окрашивание", "Стрижки"],
            1990.0,
            60,
            "booking_with_employee",
            None,
        ),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_market_int_services_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (
            100,
            9870,
            json.dumps(
                {
                    "id": 100,
                    "name": "title1",
                    "description": "description1",
                    "categories": ["category1"],
                    "min_cost": 10,
                    "min_duration": 10,
                    "action_type": "action",
                    "image": "image1/%s",
                }
            ),
        ),
        (
            279105,
            3488109,
            json.dumps(
                {
                    "id": 279105,
                    "name": "Репетитор по Английскому",
                    "description": "Английский по Скайпу.",
                    "categories": ["General English (45/60\xa0min)"],
                    "min_cost": 500.0,
                    "min_duration": 45,
                    "action_type": "booking_with_employee",
                    "image": "image2/%s",
                }
            ),
        ),
        (
            19490,
            1539326,
            json.dumps(
                {
                    "id": 19490,
                    "name": "Окрашивание корней",
                    "description": """NIRVEL
L'Oréal INOA""",
                    "categories": ["Популярные услуги", "Окрашивание", "Стрижки"],
                    "min_cost": 1990.0,
                    "min_duration": 60,
                    "action_type": "booking_with_employee",
                    "image": None,
                }
            ),
        ),
    ]
