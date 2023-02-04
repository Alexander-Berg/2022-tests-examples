import pytest
from datetime import datetime, date, timedelta

from staff.map.models import TableBook

from staff.gap.workflows.office_work.workflow import OfficeWorkWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS


@pytest.mark.django_db
def test_new_gap(gap_test):
    now = gap_test.mongo_now()
    base_gap = gap_test.get_base_gap(OfficeWorkWorkflow)

    gap = OfficeWorkWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(OfficeWorkWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['table'] == gap_test.test_table.id

    table_book = TableBook.objects.get(gap_id=1)
    assert table_book.gap_id == 1
    assert table_book.table_id == gap_test.test_table.id


@pytest.mark.django_db
def test_edit_gap(gap_test):
    base_gap = gap_test.get_base_gap(OfficeWorkWorkflow)

    gap = OfficeWorkWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    table_book = TableBook.objects.get(gap_id=1)
    assert table_book.gap_id == 1
    assert table_book.table_id == gap_test.test_table.id
    assert table_book.date_from == base_gap['date_from'].date()
    assert table_book.date_to == base_gap['date_to'].date() - timedelta(days=1)

    base_gap['date_from'] += timedelta(days=10)
    base_gap['date_to'] += timedelta(days=20)
    base_gap['is_periodic_gap'] = False

    OfficeWorkWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        gap,
    ).edit_gap(base_gap)

    table_book = TableBook.objects.get(gap_id=1)
    assert table_book.gap_id == 1
    assert table_book.table_id == gap_test.test_table.id
    assert table_book.date_from == base_gap['date_from'].date()
    assert table_book.date_to == base_gap['date_to'].date() - timedelta(days=1)


@pytest.mark.django_db
@pytest.mark.parametrize('date_from, date_to, book_date_from, book_date_to', (
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 1),
        date(2015, 1, 2),
    ),
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 2),
        date(2015, 1, 2),
    ),
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 2),
        date(2015, 1, 3),
    ),
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 3),
        date(2015, 1, 3),
    ),
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 3),
        date(2015, 1, 5),
    ),
    (
        datetime(2015, 1, 2, 10, 20),
        datetime(2015, 1, 4, 11, 30),
        date(2015, 1, 1),
        date(2015, 1, 5),
    ),
))
def test_new_gap_without_table(gap_test, date_from, date_to, book_date_from, book_date_to):
    now = gap_test.mongo_now()
    base_gap = gap_test.get_base_gap(OfficeWorkWorkflow)
    base_gap['date_from'] = date_from
    base_gap['date_to'] = date_to

    TableBook(
        table_id=gap_test.test_table.id,
        staff_id=gap_test.test_person.id,
        date_from=book_date_from,
        date_to=book_date_to,
        created_at=now,
        modified_at=now,
    ).save()
    gap = OfficeWorkWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(OfficeWorkWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['table'] is None


@pytest.mark.django_db
def test_new_periodic_gap(gap_test):
    now = gap_test.mongo_now()
    base_gap = gap_test.get_base_periodic_gap(OfficeWorkWorkflow)

    first_gap = OfficeWorkWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_periodic_gap(base_gap)

    gap_test.base_assert_new_gap(OfficeWorkWorkflow, now, base_gap, first_gap)

    assert first_gap['id'] == 1
    assert first_gap['state'] == GS.NEW
    assert first_gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert first_gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert first_gap['table'] == gap_test.test_table.id
