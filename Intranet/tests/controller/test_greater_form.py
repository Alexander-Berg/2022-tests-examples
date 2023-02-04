from staff.person_filter.controller.f_110_last_activity import GreaterForm, INTERVALS


def test_greater_form_for_last_activity():
    form = GreaterForm()
    assert form.is_valid()
    assert form.cleaned_data['interval'] == INTERVALS.MIN15
