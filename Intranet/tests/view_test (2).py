import pytest

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory

from staff.person.models import Staff

from staff.survey.views import with_department, with_chief


@pytest.mark.django_db()
def test_survey_w_department(rf, company_with_module_scope):
    company = company_with_module_scope

    request = rf.get(
        reverse('survey-api:with_department'),
        data={'survey_ru_id': '123', 'survey_en_id': '124', 'persons_limit': '10'}
    )

    request.user = StaffFactory(department=company.dep111).user
    result = with_department(request)
    assert result.status_code == 302
    assert result['location'] == (
        'https://forms.yandex.net/surveys/123/?department=yandex_dep1&utm_source=unknown'
    )


@pytest.mark.django_db()
def test_survey_w_chief(rf, company_with_module_scope):
    company = company_with_module_scope

    request = rf.get(
        reverse('survey-api:with_chief'),
        data={'survey_ru_id': '123', 'survey_en_id': '124'}
    )

    request.user = StaffFactory(department=company.dep111).user
    result = with_chief(request)
    assert result.status_code == 302
    assert result['location'] == (
        'https://forms.yandex.net/surveys/123/?chief=dep111-chief&utm_source=unknown'
    )


@pytest.mark.django_db()
def test_survey_w_chief_for_chief(rf, company_with_module_scope):
    request = rf.get(
        reverse('survey-api:with_chief'),
        data={'survey_ru_id': '123', 'survey_en_id': '124'}
    )

    person = Staff.objects.get(login='dep111-chief')

    request.user = person.user
    result = with_chief(request)
    assert result.status_code == 302
    assert result['location'] == (
        'https://forms.yandex.net/surveys/123/?chief=dep11-chief&utm_source=unknown'
    )


@pytest.mark.django_db()
def test_survey_w_custom_host(rf, company_with_module_scope):
    company = company_with_module_scope

    request = rf.get(
        reverse('survey-api:with_chief'),
        data={'survey_ru_id': '123', 'survey_en_id': '124', 'host': 'smrof.yandex.net'}
    )

    request.user = StaffFactory(department=company.dep111).user
    result = with_chief(request)
    assert result.status_code == 302
    assert result['location'] == (
        'https://smrof.yandex.net/surveys/123/?chief=dep111-chief&utm_source=unknown'
    )


@pytest.mark.django_db()
def test_survey_w_utm_source(rf, company_with_module_scope):
    company = company_with_module_scope

    request = rf.get(
        reverse('survey-api:with_chief'),
        data={'survey_ru_id': '123', 'survey_en_id': '124', 'utm_source': 'balalayka-rpc'}
    )

    request.user = StaffFactory(department=company.dep111).user
    result = with_chief(request)
    assert result.status_code == 302
    assert result['location'] == (
        'https://forms.yandex.net/surveys/123/?chief=dep111-chief&utm_source=balalayka-rpc'
    )
