from maps_adv.stat_tasks_starter.lib.charger.events_saver.query_builders import (  # noqa
    build_insert,
)
from maps_adv.stat_tasks_starter.tests.tools import squash_whitespaces


def test_with_single_select():
    select_query = squash_whitespaces(
        """SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID=10
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 5"""
    )

    got = build_insert(
        database="stat", table="accepted_sample", select_sqls=[select_query]
    )

    assert squash_whitespaces(got) == squash_whitespaces(
        """INSERT INTO stat.accepted_sample
        SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID=10
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 5"""
    )


def test_union_with_several_selects():
    select_query_template = squash_whitespaces(
        """SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID={campaign_id}
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 5"""
    )

    select_sqls = [
        select_query_template.format(campaign_id=campaign_id) for campaign_id in (1, 2)
    ]

    got = build_insert(
        database="stat", table="accepted_sample", select_sqls=select_sqls
    )

    assert squash_whitespaces(got) == squash_whitespaces(
        """
        INSERT INTO stat.accepted_sample

        SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID=1
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 5

        UNION ALL

        SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID=2
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 5
        """
    )
