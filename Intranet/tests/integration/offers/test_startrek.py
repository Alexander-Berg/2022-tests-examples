import pytest

from datetime import date
from functools import partial
from unittest.mock import patch, Mock, ANY

from django.conf import settings
from django.test import override_settings

from intranet.femida.src.offers.choices import OFFER_STATUSES, EMPLOYEE_TYPES, WORK_PLACES
from intranet.femida.src.offers.startrek.issues import create_adaptation_issue
from intranet.femida.src.staff.choices import DEPARTMENT_TAGS
from intranet.femida.src.startrek.utils import IssueTypeEnum

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@override_settings(YANDEX_DEPARTMENT_ID=100500)
@patch(
    target='intranet.femida.src.offers.startrek.issues.create_issue',
    return_value=Mock(key='ADAPTROTATE-1'),
)
def test_create_rotation_adaptation_issue(mocked_create_issue):
    """
    Тестирование создания тикета адаптации при ротации внутри ветки Яндекс
    """
    yandex = f.DepartmentFactory(id=100500, name='Yandex')
    create_bu = partial(
        f.DepartmentFactory,
        ancestors=[yandex.id],
        tags=[DEPARTMENT_TAGS.business_unit],
    )
    current_bu = create_bu(url='current_bu', name='Current BU')
    new_bu = create_bu(url='new_bu', name='New BU')
    current_department = f.DepartmentFactory(
        ancestors=[yandex.id, current_bu.id],
        name='Current Department',
    )
    new_department = f.DepartmentFactory(
        ancestors=[yandex.id, new_bu.id],
        name='New Department',
    )
    f.create_department_chief(new_department)

    employee = f.UserFactory(username='rotaru', is_dismissed=False, department=current_department)
    candidate = f.CandidateFactory(
        login=employee.username,
        first_name='София',
        last_name='Ротару',
    )
    offer = f.OfferFactory(
        candidate=candidate,
        status=OFFER_STATUSES.accepted,
        employee_type=EMPLOYEE_TYPES.rotation,
        username=employee.username,
        is_rotation_within_yandex=True,
        department=new_department,
        join_at='2023-09-23',
        work_place=WORK_PLACES.office,
        office__name_ru='БЦ Мамонтов',
    )

    create_adaptation_issue(offer)

    major_fields = dict(
        summary='[Ротация] Ротару София (rotaru@)',
        type=IssueTypeEnum.new_employee,
        queue=settings.STARTREK_ROTATION_ADAPTATION_QUEUE,
        employee=employee.username,
        start='2023-09-23',
        currentDepartment='Yandex / Current BU / Current Department',
        newDepartment='Yandex / New BU / New Department',
        legalEntity=offer.org.startrek_id,
        newPosition=offer.position.name_ru,
        office=offer.office.name_ru,
        head=offer.boss.username,
        hrbp=[],
        tags=['Ротация'],
        # Поля, которые берутся из профайла – при ротациях его быть не может
        address=None,
        candidateCitizenship=None,
        personalEmail=None,
        personalPhone=None,
    )
    local_fields = {
        settings.STARTREK_ROTATION_ADAPTATION_CURRENT_BU_FIELD: current_bu.url,
        settings.STARTREK_ROTATION_ADAPTATION_NEW_BU_FIELD: new_bu.url,
    }
    minor_fields = (
        'description',
        'formerWorker',
        'hrPl',
        'probationPeriod',
        'recruiter',
        'typeOfEmploymentContract',
        'unique',
    )
    minor_fields = dict.fromkeys(minor_fields, ANY)

    mocked_create_issue.assert_called_once_with(
        **major_fields,
        **local_fields,
        **minor_fields,
    )
    offer.refresh_from_db()
    assert offer.startrek_adaptation_key == 'ADAPTROTATE-1'


@override_settings(OUTSTAFF_DEPARTMENT_ID=100500)
@patch(
    target='intranet.femida.src.offers.startrek.issues.create_issue',
    return_value=Mock(key='BSTONBOARDING-1'),
)
def test_create_adaptation_issue_specific_department(mocked_create_issue):
    """
    Тестирование создания тикета адаптации для отдела
    с очередью и типом задачи из DepartmentAdaptation
    """
    outstaff_department = f.DepartmentFactory(
        id=100500,
        name='Команда поддержки бизнеса',
    )
    department = f.DepartmentFactory(
        id=1337,
        ancestors=[outstaff_department.id],
        name='Центр открытых разработок (Поддержка бизнеса)',
    )
    f.create_department_chief(department)

    f.DepartmentAdaptationFactory(
        department=department,
        queue='BSTONBOARDING',
        issue_type='development',
    )

    username = 'tesla'
    candidate = f.CandidateFactory(
        login=username,
        first_name='Никола',
        last_name='Тесла',
    )
    offer = f.OfferFactory(
        candidate=candidate,
        status=OFFER_STATUSES.accepted,
        employee_type=EMPLOYEE_TYPES.new,
        username=username,
        full_name=candidate.get_full_name(),
        is_rotation_within_yandex=False,
        department=department,
        join_at=date(2030, 5, 1),
        work_place=WORK_PLACES.office,
        office__name_ru='БЦ Мамонтов',
    )
    f.OfferProfileFactory(
        offer=offer,
        citizenship='RU',
        residence_address='Москва',
        home_email='tesla@ya.ru',
        phone='+79112345678',
    )

    create_adaptation_issue(offer)

    major_fields = dict(
        summary=f'{candidate.get_full_name()} ({username}@)',
        type='development',
        queue='BSTONBOARDING',
        iS2='2030-06-30',
        iS3='2030-07-30',
        iS='2030-05-31',
        start='2030-05-01',
        newDepartment='Команда поддержки бизнеса / Центр открытых разработок (Поддержка бизнеса)',
        legalEntity=offer.org.startrek_id,
        newPosition=offer.position.name_ru,
        office=offer.office.name_ru,
        head=offer.boss.username,
        hrbp=[],
        tags=[],
        address='Москва',
        candidateCitizenship='RU',
        personalEmail='tesla@ya.ru',
        personalPhone='+79112345678',
    )
    minor_fields = (
        'description',
        'formOfWork',
        'formerWorker',
        'hrPl',
        'probationPeriod',
        'recruiter',
        'typeOfEmploymentContract',
        'unique',
        'components',
        'employmentForm',
    )
    minor_fields = dict.fromkeys(minor_fields, ANY)

    mocked_create_issue.assert_called_once_with(
        **major_fields,
        **minor_fields,
    )
    offer.refresh_from_db()
    assert offer.startrek_adaptation_key == 'BSTONBOARDING-1'
