import pytest

from django.core.urlresolvers import reverse
from django.db import reset_queries


@pytest.mark.parametrize("view_name,view_queries", [
    ('controlplan', 12),
    ('controltest', 17),
    ('account', 1),
    ('assertion', 1),
    ('business_unit', 1),
    ('control', 1),
    ('deficiency', 2),
    ('deficiencygroup', 1),
    ('ipe', 3),
    ('legal', 1),
    ('risk', 1),
    ('system', 1),
    ('service', 1),
    ('process', 1),
    ('file', 1),
    ('controlstep', 2),
    ('controltestipe', 4),

])
def test_list_view_num_queries(db, view_name, view_queries, client, default_queries_count,
                               django_assert_num_queries, control_plan, control_test, account,
                               assertion, business_unit, control, deficiency, ipe, legal, risk,
                               service, system, process, file, control_step, controltestipe):
    url = reverse("api_v1:{}".format(view_name))
    with django_assert_num_queries(default_queries_count + view_queries):
        response = client.get(url)
    assert response.status_code == 200
    reset_queries()


@pytest.mark.parametrize("view_name,view_queries", [
    ('controlplan_detail', 12),
    ('controltest_detail', 25),
    ('account_detail', 1),
    ('assertion_detail', 1),
    ('business_unit_detail', 1),
    ('control_detail', 1),
    ('deficiency_detail', 2),
    ('deficiencygroup_detail', 3),
    ('ipe_detail', 24),
    ('legal_detail', 1),
    ('risk_detail', 1),
    ('system_detail', 1),
    ('service_detail', 1),
    ('process_detail', 1),
    ('file_detail', 1),
    ('controlstep_detail', 2),
    ('controltestipe_detail', 5),

])
def test_detail_view_num_queries(db, view_name, view_queries, client, default_queries_count,
                                 django_assert_num_queries, control_plan, control_test, account,
                                 assertion, business_unit, control, deficiency, deficiency_group,
                                 ipe, legal, risk, service, system, process, file,
                                 control_step, controltestipe):
    obj_map = {
        'controlplan_detail': control_plan.id,
        'controltest_detail': control_test.id,
        'account_detail': account.id,
        'assertion_detail': assertion.id,
        'business_unit_detail': business_unit.id,
        'control_detail': control.id,
        'deficiency_detail': deficiency.id,
        'deficiencygroup_detail': deficiency_group.id,
        'ipe_detail': ipe.id,
        'legal_detail': legal.id,
        'risk_detail': risk.id,
        'system_detail': system.id,
        'service_detail': service.id,
        'process_detail': process.id,
        'file_detail': file.id,
        'controlstep_detail': control_step.id,
        'controltestipe_detail': controltestipe.id,
    }
    url = reverse("api_v1:{}".format(view_name), kwargs={'pk': obj_map[view_name]})
    with django_assert_num_queries(default_queries_count + view_queries):
        response = client.get(url)
    assert response.status_code == 200
    reset_queries()


@pytest.mark.parametrize("view_name,view_queries", [
    ('control_plan', 4),
    ('control_test', 1),
    ('account', 1),
    ('assertion', 1),
    ('business_unit', 1),
    ('control', 1),
    ('deficiency', 2),
    ('ipe', 1),
    ('legal', 1),
    ('risk', 1),
    ('system', 1),
    ('service', 1),
    ('process', 1),
    ('file', 1),
    ('control_step', 1),

])
def test_lookup_views_queries(db, view_name, view_queries, client, default_queries_count,
                              django_assert_num_queries, control_plan, control_test, account,
                              assertion, business_unit, control, deficiency, ipe, legal, risk,
                              service, system, sub_process, file, control_step):
    url = reverse('ajax_lookup', kwargs={'channel': view_name})
    with django_assert_num_queries(default_queries_count + view_queries):
        response = client.get(url, {'term': '*', })
    assert response.status_code == 200
    reset_queries()
