# coding: utf-8


from review.core import const
from datetime import datetime, timedelta

from review.notifications.builders import (
    build_notifications_for_personreview_changes,
    DIGEST_PERIOD,
)


def test_digest(
        person_review_change_builder,
        person_review_builder,
        review_builder,
        person_builder,
        person_review_role_builder,
):
    person1 = person_builder()
    person2 = person_builder()
    reviewer1 = person_builder()
    reviewer2 = person_builder()
    changer1 = person_builder()
    changer2 = person_builder()

    review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    pr1 = person_review_builder(review=review, person=person1)
    pr2 = person_review_builder(review=review, person=person2)
    person_review_role_builder(
        person_review=pr1,
        person=reviewer1,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    person_review_role_builder(
        person_review=pr1,
        person=reviewer2,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=1,
    )
    person_review_role_builder(
        person_review=pr2,
        person=reviewer2,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    person_review_role_builder(
        person_review=pr1,
        person=reviewer1,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        position=0,
    )

    hours = timedelta(hours=1)
    end_date = datetime.now()
    start_date = end_date - timedelta(days=DIGEST_PERIOD)

    person_review_change_builder(
        diff={'level_change': {'new': 1, 'old': 0}},
        person_review=pr1,
        created_at=end_date - 2 * hours,
        subject=changer1,
    )
    person_review_change_builder(
        diff={'mark': {'new': 'D', 'old': 'C'}},
        person_review=pr2,
        created_at=start_date + 3 * hours,
        subject=changer2,
    )
    person_review_change_builder(
        diff={'anything': {'new': 'new', 'old': 'old'}},
        person_review=pr1,
        created_at=start_date + 3 * hours,
        subject=changer1,
    )

    # не попадает в выборку по дате
    person_review_change_builder(
        diff={'mark': {'new': 'D', 'old': 'C'}},
        person_review=pr2,
        created_at=start_date - 3 * hours,
        subject=changer2,
    )

    # tag_average_mark не должен учитываться
    person_review_change_builder(
        diff={'tag_average_mark': {'new': 'new_tag', 'old': 'old_tag'}},
        person_review=pr2,
        created_at=start_date + 3 * hours,
        subject=changer2,
    )

    data = build_notifications_for_personreview_changes()['notifications']
    assert len(data) == 2  # получателя писем.
    dn1, dn2 = sorted(data, key=lambda x: x.receiver.login)
    assert (dn1.receiver, dn2.receiver) == (reviewer1, reviewer2)

    assert len(dn1.context['change_data']) == 1
    context1 = dn1.context['change_data'][0]
    assert context1['person']['login'] == person1.login
    assert context1['level_changed_by']['login'] == changer1.login
    assert context1['mark_changed_by'] == {}

    assert len(dn2.context['change_data']) == 1
    context2 = dn2.context['change_data'][0]
    assert context2['person']['login'] == person2.login
    assert context2['mark_changed_by']['login'] == changer2.login
    assert context2['level_changed_by'] == {}
