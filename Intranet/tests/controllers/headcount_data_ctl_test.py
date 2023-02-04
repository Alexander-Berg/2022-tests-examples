import pytest
from django.contrib.auth.models import Permission

from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.oebs.constants import PERSON_POSITION_STATUS

from staff.proposal.controllers.department import order_field
from staff.proposal.controllers.headcount import HeadcountDataCtl


@pytest.mark.django_db
def test_as_form_data(company):
    # given
    hp1 = HeadcountPositionFactory(status=PERSON_POSITION_STATUS.VACANCY_PLAN, department=company.dep1)
    hp2 = HeadcountPositionFactory(status=PERSON_POSITION_STATUS.VACANCY_PLAN, department=company.dep1)
    codes = [hp1.code, hp2.code]

    observer = company.persons['yandex-chief']
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    # when
    data_ctl = HeadcountDataCtl(codes, observer)

    # than
    form_data = [data_ctl.as_form_data(hc_code) for hc_code in codes]
    for item in form_data:
        assert item['headcount_code'] in codes
        assert item['department'] == ''  # не подставляем подразделение
        assert item['fake_department'] == ''
        assert item['action_id'] == ''


@pytest.mark.django_db
def test_as_meta(company):
    hp1 = HeadcountPositionFactory(status=PERSON_POSITION_STATUS.VACANCY_PLAN, department=company.dep1)
    hp2 = HeadcountPositionFactory(status=PERSON_POSITION_STATUS.VACANCY_PLAN, department=company.dep1)
    codes = {
        hp1.code: hp1,
        hp2.code: hp2,
    }

    observer = company.persons['yandex-chief']
    observer.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))

    data_ctl = HeadcountDataCtl(list(codes.keys()), observer)

    meta_data = [data_ctl.as_meta(code) for code in codes]

    for item in meta_data:
        assert item['headcount_code'] is not None
        hp = codes[item['headcount_code']]

        assert item['name'] == hp.name
        assert item['id'] == hp.id
        assert item['status'] == hp.status
        assert item['department_order_field'] == order_field(hp.department.tree_id, hp.department.lft)
        assert item['department_id'] == hp.department.id
        assert item['department_level'] == hp.department.level
        assert item['department_name'] == hp.department.name
        assert item['department_url'] == hp.department.url
