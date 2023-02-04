# TODO: разнести по разным модулям в unit.stats.fetchers
from datetime import datetime
from operator import itemgetter

import pytest
import pytz
from dateutil.relativedelta import relativedelta
from django.conf import settings
from django.utils import timezone

from intranet.femida.src.interviews.choices import (
    APPLICATION_STATUSES,
    APPLICATION_PROPOSAL_STATUSES,
    APPLICATION_SOURCES,
    INTERVIEW_TYPES,
)
from intranet.femida.src.hire_orders.choices import (
    HIRE_ORDER_STATUSES,
    HIRE_ORDER_ACTIVE_STATUSES,
    HIRE_ORDER_RESOLUTIONS,
)
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.stats.registry import registry

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_proposals_fast_conversion():
    """
    Проверка корректного расчета показателей
    """
    params = (
        (True, APPLICATION_PROPOSAL_STATUSES.accepted, 'application_accept_proposal'),
        (True, APPLICATION_PROPOSAL_STATUSES.accepted, 'application_accept_proposal'),
        (False, APPLICATION_PROPOSAL_STATUSES.rejected, 'application_reject_proposal'),
        (False, APPLICATION_PROPOSAL_STATUSES.undefined, None),
    )
    cons = f.ConsiderationFactory()
    dt = datetime(2019, 7, 15, tzinfo=pytz.utc)
    dt_date = dt.strftime('%Y-%m-%d')
    for is_first, proposal_status, action_name in params:
        appl = f.create_application(
            created=dt,
            consideration_id=cons.id,
            source=APPLICATION_SOURCES.proposal,
            proposal_status=proposal_status,
            proposal_factors={
                'is_first_proposal': is_first,
            },
        )
        if action_name:
            f.SnapshotFactory(
                obj_str='application',
                obj_id=appl.id,
                log_record__action_name=action_name,
                log_record__action_time=dt,
            )
    report_class = registry.reports['proposals_fast_conversion']
    data = sorted(report_class().get_data(dt_date), key=itemgetter('accuracy'))
    assert data == [
        {
            'accuracy': 0.0,
            'appls_count': 2.0,
            'cons_count': 1,
            'fielddate': dt_date,
            'first_proposal': False,
            'inaccuracy': 50.0,
        },
        {
            'accuracy': 50.0,
            'appls_count': 4.0,
            'cons_count': 1,
            'fielddate': dt_date,
            'first_proposal': 'ALL',
            'inaccuracy': 25.0,
        },
        {
            'accuracy': 100.0,
            'appls_count': 2.0,
            'cons_count': 1,
            'fielddate': dt_date,
            'first_proposal': True,
            'inaccuracy': 0.0,
        },
    ]


def test_active_applications_queue():
    profession = f.ProfessionFactory()
    common_data = {
        'vacancy__profession': profession,
    }
    f.ApplicationFactory(status=APPLICATION_STATUSES.draft, **common_data)
    f.ApplicationFactory(
        status=APPLICATION_STATUSES.in_progress,
        proposal_status=APPLICATION_PROPOSAL_STATUSES.undefined,
        **common_data
    )
    f.ApplicationFactory(
        status=APPLICATION_STATUSES.in_progress,
        proposal_status=APPLICATION_PROPOSAL_STATUSES.accepted,
        **common_data
    )
    f.ApplicationFactory(
        status=APPLICATION_STATUSES.in_progress,
        proposal_status=APPLICATION_PROPOSAL_STATUSES.rejected,
        vacancy__profession=None,
        vacancy__professional_sphere=None,
    )
    f.ApplicationFactory(status=APPLICATION_STATUSES.closed)

    report_class = registry.reports['active_applications_queue']
    dt = '2017-02-08'
    result = sorted(report_class().get_data(fielddate=dt), key=lambda x: x['profession'])
    expected = [
        {
            'fielddate': dt,
            'profession': 'ALL',
            'draft': 1,
            'in_progress': 3,
            'accepted': 1,
        },
        {
            'fielddate': dt,
            'profession': 'P%d' % profession.id,
            'draft': 1,
            'in_progress': 2,
            'accepted': 1,
        },
        {
            'fielddate': dt,
            'profession': 'S%d' % profession.professional_sphere_id,
            'draft': 1,
            'in_progress': 2,
            'accepted': 1,
        },
        {
            'fielddate': dt,
            'profession': 'UNKNOWN',
            'draft': 0,
            'in_progress': 1,
            'accepted': 0,
        },
    ]

    assert result == expected


def test_interview_event_time():
    def profession_by_interview(interview):
        return (
            'P{}'.format(interview.application.vacancy.profession_id)
            if interview.application
            else 'UNKNOWN'
        )

    now = timezone.now()
    now_date = now.strftime('%Y-%m-%d')
    created = now - relativedelta(days=1)

    interview_aa = f.InterviewFactory(
        type=INTERVIEW_TYPES.aa,
        application=None,
        candidate=f.create_candidate_with_consideration(),
        created=created,
        event_start_time=now + relativedelta(days=1),
    )
    interview_no_event_time = f.create_interview(
        type=INTERVIEW_TYPES.regular,
        created=created,
    )
    interview_backdated = f.create_interview(
        type=INTERVIEW_TYPES.regular,
        event_start_time=now - relativedelta(days=2),
        created=created,
    )

    report_class = registry.reports['interview_event_time']
    result = report_class().get_data(fielddate=now_date)

    checks = {
        'no_event_time': {
            'fielddate': now_date,
            'profession': profession_by_interview(interview_no_event_time),
            'interview_type': 'ALL',
            'vacancy_type': 'ALL',
            'all_count': 1,
            'no_event_time_count': 1,
            'event_time_before_created_count': 0,
        },
        'backdated': {
            'fielddate': now_date,
            'profession': profession_by_interview(interview_backdated),
            'interview_type': 'ALL',
            'vacancy_type': 'ALL',
            'all_count': 1,
            'no_event_time_count': 0,
            'event_time_before_created_count': 1,
        },
        'no_extra_stuff': {
            'fielddate': now_date,
            'profession': 'ALL',
            'interview_type': 'ALL',
            'vacancy_type': 'ALL',
            'all_count': 3,
            'no_event_time_count': 1,
            'event_time_before_created_count': 1,
        },
        'aa_means_unknown_professional_sphere': {
            'fielddate': now_date,
            'profession': profession_by_interview(interview_aa),
            'interview_type': interview_aa.type,
            'vacancy_type': 'UNKNOWN',
            'all_count': 1,
            'no_event_time_count': 0,
            'event_time_before_created_count': 0,
        },
    }
    for check_name, check_value in checks.items():
        assert check_value in result, check_name


def test_interviews_processing_speed_detailed():
    same_day = timezone.now().replace(hour=0, minute=0, second=0, microsecond=0)
    next_day = same_day + relativedelta(days=1)
    after_next_day = same_day + relativedelta(days=5)
    fielddate = after_next_day.strftime('%Y-%m-%d')

    # Устраиваем интервьюера в ветку Яндекс,
    # чтобы департамент отоборажался в отчете как есть, а не как UNKNOWN_DEPARTMENT
    yandex_department = f.DepartmentFactory(id=settings.YANDEX_DEPARTMENT_ID)
    interviewer = f.create_user(department__ancestors=[yandex_department.id])

    def create_finished_interview(estimation_time, finished):
        interview = f.create_interview(
            event_start_time=same_day,
            state=Interview.STATES.finished,
            finished=finished,
            interviewer=interviewer,
        )
        f.SnapshotFactory(
            obj_str='interview',
            obj_id=interview.id,
            log_record__action_name='interview_estimate',
            log_record__action_time=estimation_time,
        )
        return interview

    # Не подходящий статус: должны не попасть в отчет
    f.create_interview()
    f.create_interview(state=Interview.STATES.estimated)

    # Без estimation_snapshot. В показатели по estimated должно попасть датой закрытия
    # Здесь же проверяем, что AA попадает в отчет
    f.InterviewFactory(
        type=INTERVIEW_TYPES.aa,
        application=None,
        candidate=f.create_candidate_with_consideration(),
        state=Interview.STATES.finished,
        finished=same_day,
        event_start_time=same_day,
        interviewer=interviewer,
    )

    # Без event_start_time: должно попасть только в ALL показатели
    f.create_interview(state=Interview.STATES.finished, finished=same_day, interviewer=interviewer)
    # Далее все interview создаем с estimation-снепшотами и event_start_time
    create_finished_interview(estimation_time=same_day, finished=same_day)
    create_finished_interview(estimation_time=same_day, finished=next_day)
    create_finished_interview(estimation_time=next_day, finished=after_next_day)
    create_finished_interview(estimation_time=after_next_day, finished=after_next_day)

    report_class = registry.reports['interviews_processing_speed_detailed']
    result = report_class().get_data(fielddate=fielddate)

    check_base = {
        'fielddate': fielddate,
        'staffunit': [yandex_department.id, interviewer.department_id, interviewer.username],
        'profession': 'ALL',
        'estimated_before_calendar_event_time': 0,
        'finished_before_calendar_event_time': 0,
    }
    checks = [
        {
            'interview_type': 'regular',
            'finished_count': 5,
            'estimated_same_day_count': 2,
            'finished_same_day_count': 1,
            'estimated_next_day_count': 1,
            'finished_next_day_count': 1,
            'estimated_after_next_day_count': 1,
            'finished_after_next_day_count': 2,
            **check_base
        },
        {
            'interview_type': 'ALL',
            'finished_count': 6,
            'estimated_same_day_count': 3,
            'finished_same_day_count': 2,
            'estimated_next_day_count': 1,
            'finished_next_day_count': 1,
            'estimated_after_next_day_count': 1,
            'finished_after_next_day_count': 2,
            **check_base
        },
        {
            'interview_type': 'aa',
            'finished_count': 1,
            'estimated_same_day_count': 1,
            'finished_same_day_count': 1,
            'estimated_next_day_count': 0,
            'finished_next_day_count': 0,
            'estimated_after_next_day_count': 0,
            'finished_after_next_day_count': 0,
            **check_base
        },
    ]

    for checks_id, check in enumerate(checks):
        assert check in result, checks_id


def test_hire_orders_queue():
    now = timezone.now()
    now_date = now.date().isoformat()

    outstaff_department = f.DepartmentFactory(id=settings.OUTSTAFF_DEPARTMENT_ID)
    statuses = HIRE_ORDER_ACTIVE_STATUSES._db_values
    raw_data = {'vacancy': {'department': outstaff_department.url}}
    for status in statuses:
        f.HireOrderFactory(status=status, raw_data=raw_data)

    report_class = registry.reports['hire_orders_queue']
    report = report_class()
    # при добавлении нового статуса в HireOrder тест упадёт
    # в этом случае нужно будет добавить статус
    # в intranet.femida.src.stats.configs.hire_orders_queue.yaml
    assert set(report.local_config.measures) == statuses

    result = report.get_data(fielddate=now_date)

    check = {
        'fielddate': now_date,
        'staffunit': [outstaff_department.id],
        **{status: 1 for status in statuses},
    }

    assert check in result, result


def test_hire_orders_processing_speed():
    now = timezone.now()
    now_date = now.date().isoformat()

    outstaff_department = f.DepartmentFactory(id=settings.OUTSTAFF_DEPARTMENT_ID)
    raw_data = {'vacancy': {'department': outstaff_department.url}}
    hire_orders = f.HireOrderFactory.create_batch(
        size=3,
        status=HIRE_ORDER_STATUSES.new,
        raw_data=raw_data,
    )
    resolutions = [HIRE_ORDER_RESOLUTIONS.hired] * 2 + [HIRE_ORDER_RESOLUTIONS.cancelled]
    for hire_order, resolution in zip(hire_orders, resolutions):
        for status, _ in HIRE_ORDER_ACTIVE_STATUSES:
            if status != 'new':
                f.HireOrderHistoryFactory(hire_order=hire_order, status=status)

        hire_order.status = HIRE_ORDER_STATUSES.closed
        hire_order.resolution = resolution
        hire_order.save()

    report_class = registry.reports['hire_orders_processing_speed_7d']
    result = report_class().get_data(fielddate=now_date)

    assert result[0]['cancelled_count'] == 1
    assert result[0]['hired_count'] == 2
