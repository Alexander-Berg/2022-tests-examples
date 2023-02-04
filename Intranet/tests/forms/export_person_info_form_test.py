import pytest

from staff.lib.testing import StaffFactory, verify_forms_error_code

from staff.person.forms import ExportPersonInfoForm


@pytest.mark.django_db
def test_export_person_info_form():
    target = ExportPersonInfoForm(
        data={
            'persons': [
                StaffFactory(intranet_status=0).login,
                StaffFactory(intranet_status=1).login,
            ],
        },
    )

    result = target.is_valid()

    assert result is True, target.errors


@pytest.mark.django_db
def test_export_person_info_form_empty():
    target = ExportPersonInfoForm()

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'required')


@pytest.mark.django_db
def test_export_person_info_form_no_persons():
    target = ExportPersonInfoForm(data={})

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'required')


@pytest.mark.django_db
def test_export_person_info_form_non_existing_login():
    target = ExportPersonInfoForm(
        data={
            'persons': ['non_existing_login'],
        },
    )

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'invalid_logins')


@pytest.mark.django_db
def test_export_person_info_form_max_persons_count():
    target = ExportPersonInfoForm(
        data={
            'persons': [StaffFactory(intranet_status=1).login],
        },
    )

    target.MAX_PERSONS_COUNT = 0
    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'persons', 'too_many')
