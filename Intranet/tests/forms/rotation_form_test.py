import pytest

from staff.preprofile.forms.rotation_form import RotationForm


@pytest.mark.django_db
def test_rotation_form_has_validation_errors():
    target = RotationForm(data={})

    result = target.is_valid()

    assert result is False
    assert 'login' not in target.cleaned_data
