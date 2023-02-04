from maps.wikimap.stat.tasks_payment.reports.geotest_report.lib.report import (
    _FULL_ACCESS_GROUP,
    _make_acl,
    _make_user_name_path,
    _tree_names_for_unknown_values,
    make_report_job,
)
from maps.wikimap.stat.tasks_payment.dictionaries.tariff.schema import TARIFF_RUB_PER_SEC

from nile.api.v1 import (
    Record,
    clusters,
    local,
    statface
)


REPORT = statface.StatfaceReport(
    scale='daily',
    path='Path/To/Report',
    client=statface.client.MockStatfaceClient()
)


def job():
    return clusters.MockCluster().job()


def test_should_generate_tree_names():
    records = [
        Record(tree_name=b'tree: 1', value=1),
        Record(tree_name=None,       value=2),
        Record(tree_name=b'tree: 3', value=3),
    ]
    result = _tree_names_for_unknown_values(tree_name_col=b'tree_name', value_col=b'value')(records)
    assert sorted([
        Record(tree_name=b'tree: 1',             value=1),
        Record(tree_name=b'\tall\t',             value=2),
        Record(tree_name=b'\tall\tunknown\t',    value=2),
        Record(tree_name=b'\tall\tunknown\t2\t', value=2),
        Record(tree_name=b'tree: 3',             value=3),
    ]) == sorted(result)


def test_should_make_user_name_path():
    assert _make_user_name_path(
        Record(primary_department=b'dep', last_name=b'last name', first_name=b'first name', login=b'login', staff_uid=42)
    ) == [b'all', b'dep', b'last name first name (login)']
    assert _make_user_name_path(
        Record(primary_department=None, last_name=None, first_name=None, login=None, staff_uid=42)
    ) == [b'all', b'unknown', b'42']


def test_should_make_acl():
    assert _make_acl(login=b'login', is_leaf=False) == _FULL_ACCESS_GROUP
    assert _make_acl(login=b'login', is_leaf=True)  == _FULL_ACCESS_GROUP + b',@login'  # noqa
    assert _make_acl(login=None,     is_leaf=True)  == _FULL_ACCESS_GROUP               # noqa


def test_should_add_staff_info():
    result = []

    log = [
        Record(iso_datetime=b'2020-01-25 01:01:01', staff_uid=1, task_id=b'task 1', quantity=1.1),
        Record(iso_datetime=b'2020-01-25 03:03:03', staff_uid=3, task_id=b'task 3', quantity=3.3),
    ]
    staff_dump = [
        Record(uid=1, login=b'login_1', last_name=b'last name 1', first_name=b'first name 1', primary_department=b'dep 1', test_field=11),
        Record(uid=2, login=b'login_2', last_name=b'last name 2', first_name=b'first name 2', primary_department=b'dep 2', test_field=22),  # unused
        Record(uid=3, login=b'login_3', last_name=b'last name 3', first_name=b'first name 3', primary_department=b'dep 3', test_field=33),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={
            'log': local.StreamSource(log),
            'staff_dump': local.StreamSource(staff_dump),
        },
        sinks={'log_with_staff_info': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\t',                                            task_id=b'task 1', quantity=1.1, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tdep 1\t',                                     task_id=b'task 1', quantity=1.1, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tdep 1\tlast name 1 first name 1 (login_1)\t', task_id=b'task 1', quantity=1.1, _acl=_FULL_ACCESS_GROUP + b',@login_1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\t',                                            task_id=b'task 3', quantity=3.3, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tdep 3\t',                                     task_id=b'task 3', quantity=3.3, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tdep 3\tlast name 3 first name 3 (login_3)\t', task_id=b'task 3', quantity=3.3, _acl=_FULL_ACCESS_GROUP + b',@login_3'),
    ]) == sorted(result)


def test_should_add_staff_info_for_unknown_user():
    result = []

    log = [
        Record(iso_datetime=b'2020-01-25 01:01:01', staff_uid=1, task_id=b'task', quantity=1.1),
    ]
    staff_dump = []

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={
            'log': local.StreamSource(log),
            'staff_dump': local.StreamSource(staff_dump),
        },
        sinks={'log_with_staff_info': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\t',             task_id=b'task', quantity=1.1, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tunknown\t',    task_id=b'task', quantity=1.1, _acl=_FULL_ACCESS_GROUP),
        Record(fielddate=b'2020-01-25', user_name_tree=b'\tall\tunknown\t1\t', task_id=b'task', quantity=1.1, _acl=_FULL_ACCESS_GROUP),
    ]) == sorted(result)


def test_should_add_tariffs():
    result = []

    log_with_staff_info = [
        Record(task_id=b'task 1', quantity=1.0),
        Record(task_id=b'task 2', quantity=2.0),
        Record(task_id=b'task 1', quantity=3.0),
    ]
    task_tariff_map = [
        Record(task_id=b'task 1', task_name_tree=b'\tall\ttariff 1\ttask 1\t', seconds_per_task=1),
        Record(task_id=b'task 1', task_name_tree=b'\tall\ttariff 1\t',         seconds_per_task=1),
        Record(task_id=b'task 1', task_name_tree=b'\tall\t',                   seconds_per_task=1),
        Record(task_id=b'task 2', task_name_tree=b'\tall\ttariff 2\ttask 2\t', seconds_per_task=2),
        Record(task_id=b'task 2', task_name_tree=b'\tall\ttariff 2\t',         seconds_per_task=2),
        Record(task_id=b'task 2', task_name_tree=b'\tall',                     seconds_per_task=2),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={
            'log_with_staff_info': local.StreamSource(log_with_staff_info),
            'task_tariff_map': local.StreamSource(task_tariff_map),
        },
        sinks={'log_with_tariffs': local.ListSink(result)}
    )

    assert sorted([
        Record(task_name_tree=b'\tall\ttariff 1\ttask 1\t',  quantity=1.0, time_spent_sec=1.0, cost_rub=1.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall\ttariff 1\t',          quantity=1.0, time_spent_sec=1.0, cost_rub=1.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall\t',                    quantity=1.0, time_spent_sec=1.0, cost_rub=1.0 * TARIFF_RUB_PER_SEC),

        Record(task_name_tree=b'\tall\ttariff 2\ttask 2\t',  quantity=2.0, time_spent_sec=4.0, cost_rub=4.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall\ttariff 2\t',          quantity=2.0, time_spent_sec=4.0, cost_rub=4.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall',                      quantity=2.0, time_spent_sec=4.0, cost_rub=4.0 * TARIFF_RUB_PER_SEC),

        Record(task_name_tree=b'\tall\ttariff 1\ttask 1\t',  quantity=3.0, time_spent_sec=3.0, cost_rub=3.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall\ttariff 1\t',          quantity=3.0, time_spent_sec=3.0, cost_rub=3.0 * TARIFF_RUB_PER_SEC),
        Record(task_name_tree=b'\tall\t',                    quantity=3.0, time_spent_sec=3.0, cost_rub=3.0 * TARIFF_RUB_PER_SEC),
    ]) == sorted(result)


def test_should_add_tariffs_for_unknown_task_id():
    result = []

    log_with_staff_info = [
        Record(task_id=b'some task', quantity=1.0),
    ]
    task_tariff_map = [
        Record(task_id=b'task 1', task_name_tree=b'\tall\tgroup 1\ttariff 1\ttask 1\t', seconds_per_task=1),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={
            'log_with_staff_info': local.StreamSource(log_with_staff_info),
            'task_tariff_map': local.StreamSource(task_tariff_map),
        },
        sinks={'log_with_tariffs': local.ListSink(result)}
    )

    assert sorted([
        Record(task_name_tree=b'\tall\tunknown\tsome task\t', quantity=1.0, time_spent_sec=0.0, cost_rub=0.0),
        Record(task_name_tree=b'\tall\tunknown\t',            quantity=1.0, time_spent_sec=0.0, cost_rub=0.0),
        Record(task_name_tree=b'\tall\t',                     quantity=1.0, time_spent_sec=0.0, cost_rub=0.0),
    ]) == sorted(result)


def test_should_aggregate_tasks_by_fielddate():
    result = []

    log_with_tariffs = [
        Record(fielddate=b'2020-01-24', user_name_tree=b'1', task_name_tree=b'1', quantity=1, time_spent_sec=1, cost_rub=1.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-24', user_name_tree=b'1', task_name_tree=b'1', quantity=2, time_spent_sec=2, cost_rub=2.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=4, time_spent_sec=4, cost_rub=4.0, _acl=b'acl 1'),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={'log_with_tariffs': local.StreamSource(log_with_tariffs)},
        sinks={'result': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-24', user_name_tree=b'1', task_name_tree=b'1', quantity_total=3, time_spent_total_sec=3, cost_total_rub=3.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity_total=4, time_spent_total_sec=4, cost_total_rub=4.0, _acl=b'acl 1'),
    ]) == sorted(result)


def test_should_aggregate_tasks_by_user_name():
    result = []

    log_with_tariffs = [
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=1, time_spent_sec=1, cost_rub=1.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'2', task_name_tree=b'1', quantity=2, time_spent_sec=2, cost_rub=2.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=4, time_spent_sec=4, cost_rub=4.0, _acl=b'acl 1'),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={'log_with_tariffs': local.StreamSource(log_with_tariffs)},
        sinks={'result': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity_total=5, time_spent_total_sec=5, cost_total_rub=5.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'2', task_name_tree=b'1', quantity_total=2, time_spent_total_sec=2, cost_total_rub=2.0, _acl=b'acl 1'),
    ]) == sorted(result)


def test_should_aggregate_tasks_by_task_name():
    result = []

    log_with_tariffs = [
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=1, time_spent_sec=1, cost_rub=1.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'2', quantity=2, time_spent_sec=2, cost_rub=2.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'2', quantity=4, time_spent_sec=4, cost_rub=4.0, _acl=b'acl 1'),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={'log_with_tariffs': local.StreamSource(log_with_tariffs)},
        sinks={'result': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity_total=1, time_spent_total_sec=1, cost_total_rub=1.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'2', quantity_total=6, time_spent_total_sec=6, cost_total_rub=6.0, _acl=b'acl 1'),
    ]) == sorted(result)


def test_should_aggregate_tasks_by_acl():
    result = []

    log_with_tariffs = [
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=1, time_spent_sec=1, cost_rub=1.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=2, time_spent_sec=2, cost_rub=2.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity=4, time_spent_sec=4, cost_rub=4.0, _acl=b'acl 2'),
    ]

    make_report_job(job(), '2020-01-25', REPORT).local_run(
        sources={'log_with_tariffs': local.StreamSource(log_with_tariffs)},
        sinks={'result': local.ListSink(result)}
    )

    assert sorted([
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity_total=3, time_spent_total_sec=3, cost_total_rub=3.0, _acl=b'acl 1'),
        Record(fielddate=b'2020-01-25', user_name_tree=b'1', task_name_tree=b'1', quantity_total=4, time_spent_total_sec=4, cost_total_rub=4.0, _acl=b'acl 2'),
    ]) == sorted(result)


def test_should_prepare_report():
    'Checks that the whole job works as it is expected. (an integration test)'

    result = []

    log = [
        Record(iso_datetime=b'2020-01-25 01:01:01', staff_uid=1, task_id=b'task 1', quantity=1.1),
        # Unknown uid
        Record(iso_datetime=b'2020-01-25 02:02:02', staff_uid=2, task_id=b'task 2', quantity=2.2),
        # Unknown task_id
        Record(iso_datetime=b'2020-01-25 03:03:03', staff_uid=3, task_id=b'unknown task', quantity=3.3),
    ]
    staff_dump = [
        Record(uid=1, login=b'login_1', last_name=b'last name 1', first_name=b'first name 1', primary_department=b'dep 1'),
        Record(uid=3, login=b'login_3', last_name=b'last name 3', first_name=b'first name 3', primary_department=b'dep 3'),
        Record(uid=4, login=b'login_4', last_name=b'last name 4', first_name=b'first name 4', primary_department=b'dep 4'),  # unused
    ]
    task_tariff_map = [
        Record(task_id=b'task 1', task_name_tree=b'\tall\t',         seconds_per_task=1),
        Record(task_id=b'task 1', task_name_tree=b'\tall\ttask 1\t', seconds_per_task=1),
        Record(task_id=b'task 2', task_name_tree=b'\tall\t',         seconds_per_task=2),
        Record(task_id=b'task 2', task_name_tree=b'\tall\ttask 2\t', seconds_per_task=2),
        Record(task_id=b'task 3', task_name_tree=b'\tall\t',         seconds_per_task=3),  # unused
        Record(task_id=b'task 3', task_name_tree=b'\tall\ttask 3\t', seconds_per_task=3),  # unused
    ]

    make_report_job(job(), '2020-01-25', report=REPORT).local_run(
        sources={
            'log': local.StreamSource(log),
            'staff_dump': local.StreamSource(staff_dump),
            'task_tariff_map': local.StreamSource(task_tariff_map),
        },
        sinks={
            'result': local.ListSink(result)
        }
    )

    assert sorted([
        # All
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\t',
            quantity_total=6.6, time_spent_total_sec=5.5, cost_total_rub=5.5 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),

        # First log entry
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tdep 1\t',
            quantity_total=1.1, time_spent_total_sec=1.1, cost_total_rub=1.1 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tdep 1\tlast name 1 first name 1 (login_1)\t',
            quantity_total=1.1, time_spent_total_sec=1.1, cost_total_rub=1.1 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP + b',@login_1'
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 1\t', user_name_tree=b'\tall\t',
            quantity_total=1.1, time_spent_total_sec=1.1, cost_total_rub=1.1 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 1\t', user_name_tree=b'\tall\tdep 1\t',
            quantity_total=1.1, time_spent_total_sec=1.1, cost_total_rub=1.1 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 1\t', user_name_tree=b'\tall\tdep 1\tlast name 1 first name 1 (login_1)\t',
            quantity_total=1.1, time_spent_total_sec=1.1, cost_total_rub=1.1 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP + b',@login_1'
        ),

        # Second log entry
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tunknown\t',
            quantity_total=2.2, time_spent_total_sec=4.4, cost_total_rub=4.4 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tunknown\t2\t',
            quantity_total=2.2, time_spent_total_sec=4.4, cost_total_rub=4.4 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 2\t', user_name_tree=b'\tall\t',
            quantity_total=2.2, time_spent_total_sec=4.4, cost_total_rub=4.4 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 2\t', user_name_tree=b'\tall\tunknown\t',
            quantity_total=2.2, time_spent_total_sec=4.4, cost_total_rub=4.4 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\ttask 2\t', user_name_tree=b'\tall\tunknown\t2\t',
            quantity_total=2.2, time_spent_total_sec=4.4, cost_total_rub=4.4 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),

        # Third log entry
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tdep 3\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\t', user_name_tree=b'\tall\tdep 3\tlast name 3 first name 3 (login_3)\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP + b',@login_3'
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\t', user_name_tree=b'\tall\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\t', user_name_tree=b'\tall\tdep 3\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\t', user_name_tree=b'\tall\tdep 3\tlast name 3 first name 3 (login_3)\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP + b',@login_3'
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\tunknown task\t', user_name_tree=b'\tall\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\tunknown task\t', user_name_tree=b'\tall\tdep 3\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP
        ),
        Record(
            fielddate=b'2020-01-25', task_name_tree=b'\tall\tunknown\tunknown task\t', user_name_tree=b'\tall\tdep 3\tlast name 3 first name 3 (login_3)\t',
            quantity_total=3.3, time_spent_total_sec=0.0, cost_total_rub=0.0 * TARIFF_RUB_PER_SEC, _acl=_FULL_ACCESS_GROUP + b',@login_3'
        ),
    ]) == sorted(result)
