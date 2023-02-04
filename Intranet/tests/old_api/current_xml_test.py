from datetime import datetime

import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-current-xml')
    response = client.get(url)
    assert response.status_code == 200
    assert response.content.decode('utf-8') == (
        '<?xml version="1.0" encoding="windows-1251"?><planner><absents/><soon/></planner>'
    )


@pytest.mark.django_db
def test_just_not_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = datetime(2050, 1, 1, 12, 30)
    base_gap['date_to'] = datetime(2050, 1, 2, 13, 55)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?encoding=utf8' % reverse('gap:api-current-xml')
    response = client.get(url)
    assert response.status_code == 200
    assert response.content.decode('utf-8') == (
       '<?xml version="1.0" encoding="utf8"?><planner>'
       '<absents/>'
       '<soon>'
       '<absent color="#ffc136" end="01.01.2050" id="%s" login="%s" mail="%s@yandex-team.ru" '
       'start="01.01.2050" subject="Отсутствие" subject_id="absence" '
       'url="https://staff.test.yandex-team.ru/gap/%s" user_id="%s">Отсутствие</absent>'
       '</soon>'
       '</planner>'
    ) % (
       gap_test.DEFAULT_GAP_ID,
       gap_test.test_person.login,
       gap_test.test_person.login,
       gap_test.DEFAULT_GAP_ID,
       gap_test.test_person.id,
    )
