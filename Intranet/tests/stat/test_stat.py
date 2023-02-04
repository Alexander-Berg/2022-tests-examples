import mock
import re

from post_office import models as post_models

from review.lib import datetimes
from tests.helpers import get_json


CHECKING_URL = '/stat/'


@mock.patch('review.stats.celery_stats.CeleryStats.get_queue_size', return_value=42)
def test_response_structure(celery_stats_mock, client):
    """
    Expected format described here:
    https://wiki.yandex-team.ru/jandekspoisk/sepe/monitoring/stat-handle

    and here:
    https://wiki.yandex-team.ru/golovan/userdocs/aggregation-types/
    """
    received_stat = get_json(client, CHECKING_URL)
    assert isinstance(received_stat, list)
    for metric in received_stat:
        assert isinstance(metric, list) and len(metric) == 2
        key, val = metric
        assert isinstance(key, str)
        is_number = isinstance(val, int) or isinstance(val, float)
        if not is_number:
            assert isinstance(val, list)
            for gist in val:
                assert len(gist) == 2
                assert all(isinstance(it, int) for it in gist)

    metric_regex_str = r'^[a-zA-Z0-9\.\-/@_]+_([ad][vehmntx]{3}|summ|hgram|max)$'
    metric_regex = re.compile(metric_regex_str)
    for metric in received_stat:
        assert metric_regex.match(metric[0]), '%s does not follow regex %s' % (metric[0], metric_regex_str)


@mock.patch('review.stats.celery_stats.CeleryStats.get_queue_size', return_value=42)
def test_processed_change(
    celery_stats_mock,
    client,
    staff_structure_change_builder,
):
    structure_change = staff_structure_change_builder()
    structure_change.processed_at = structure_change.created_at_auto
    structure_change.save()
    received_stat = get_json(client, CHECKING_URL)
    assert dict(received_stat)['staff_structure_delay_max'] == 0


@mock.patch('review.stats.celery_stats.CeleryStats.get_queue_size', return_value=42)
def test_non_sent_emails(celery_stats_mock, client):
    now = datetimes.now()
    mails = []
    for hours in (1, 12, 13, 14, 24, 35, 37):
        mail = post_models.Email.objects.create(
            status=post_models.STATUS.failed,
            from_email='something@somewhere.why',
        )
        mail.created = datetimes.shifted(now, hours=-hours)
        mail.save()
        mails.append(mail)
    received_stat = get_json(client, CHECKING_URL)
    assert dict(received_stat)['notifications_not_sent_max'] == 4


@mock.patch('review.stats.celery_stats.CeleryStats.get_queue_size', return_value=42)
def test_non_processed_change(
    celery_stats_mock,
    client,
    staff_structure_change_builder,
):
    structure_change = staff_structure_change_builder()
    created_at = structure_change.created_at_auto
    structure_change.created_at_auto = datetimes.shifted(
        created_at,
        minutes=-2,
    )
    structure_change.save()
    received_stat = get_json(client, CHECKING_URL)
    assert dict(received_stat)['staff_structure_delay_max'] >= 2
