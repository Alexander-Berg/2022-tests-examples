from staff.lib.testing import verify_forms_error_code

from staff.umbrellas.forms import ExportUmbrellasForm


def test_export_umbrellas_form():
    target = ExportUmbrellasForm(
        data={
            'continuation_token': 3,
        },
    )

    result = target.is_valid()

    assert result is True, target.errors


def test_export_umbrellas_form_empty():
    target = ExportUmbrellasForm()

    result = target.is_valid()

    assert result is True, target.errors


def test_export_umbrella_assignments_form_non_int():
    target = ExportUmbrellasForm(data={
        'continuation_token': 'test'
    })

    result = target.is_valid()

    assert result is False
    verify_forms_error_code(target.errors, 'continuation_token', 'invalid')
