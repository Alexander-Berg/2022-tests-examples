import json

import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportGoodsDataTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportGoodsDataTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_goods_data_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task()

    mock_yql["query"].assert_called_with(
        """
$by_num_of_items_others_last = ($x) -> {
    RETURN IF ($x.0 = '__$OTHERS$__', 0, $x.1)
};

$collapse_categories = ($x) -> {
    RETURN ListExtend(
        ListTake($x, 10), -- take 10 with most number of items
        [('__$OTHERS$__', 0)] -- append special category for the rest of goods
    )
};

$categories = SELECT
    company_permalink as permalink,
    IF(group_name IS NULL OR group_name = "", '__$OTHERS$__', group_name) AS category
FROM hahn.`//path/to/yt_goods_table` as d
RIGHT JOIN (SELECT DISTINCT permalink FROM hahn.`//path/to/biz_state_table` WHERE published) AS b
ON CAST(b.permalink AS Int64) = d.company_permalink
where source = 'TYCOON' AND Yson::ConvertToString(status) <> 'Deleted';

$categories_with_num = SELECT
    permalink,
    AsTuple(category, count(*)) AS cat
FROM $categories
GROUP BY permalink, category;

$groupped_categories = SELECT
    permalink,
    ListSortDesc(AGGREGATE_LIST(cat), $by_num_of_items_others_last) AS categories
FROM $categories_with_num
GROUP BY permalink;

$selected_categories = SELECT
    permalink,
    IF(
        ListLength(categories) <= 10,
        categories,
        $collapse_categories(categories)
    ) AS categories
FROM $groupped_categories;

SELECT
    permalink,
    ListMap(categories, ($x) -> {RETURN $x.0;}) AS categories
FROM $selected_categories;
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (
            12345,
            ["Category 1"],
        ),
        (
            23456,
            ["Category 2", "Category 22"],
        ),
        (
            34567,
            ["Category 3", "Category 33", "__$OTHERS$__"],
        ),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_goods_data_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (12345, json.dumps({"categories": [{"name": "Category 1"}], "source_name": "TYCOON"})),
        (
            23456,
            json.dumps({"categories": [{"name": "Category 2"}, {"name": "Category 22"}], "source_name": "TYCOON"}),
        ),
        (
            34567,
            json.dumps(
                {
                    "categories": [{"name": "Category 3"}, {"name": "Category 33"}, {"name": "__$OTHERS$__"}],
                    "source_name": "TYCOON",
                }
            ),
        ),
    ]
