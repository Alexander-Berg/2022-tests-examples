import pytest

from staff.keys.forms import BannedSSHAdminForm


GOOD_KEY = (
    'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDmCvbWwxazuJx+P82dyNOvHo9RTJd/NPsuUFJy6qdqE6Pp2/NI/Sasjmp6IFNG'
    '+4DtKMkwFx1h9LNnmJux+1wS44liQoyLbeYa55pbTKt0VP+CRXREMdYkAD1mASokENtKeTurWVQCvnPzV2ErISs+UEMkFHOsEL2CURM'
    '/0GvPZgBs9cI7VT43k2K4S2K6XqbBlcRShU8U7cRwMdcKnklNhbl00mm9AMYhkn76vnrgOfgMB'
    '+D8Mr6E2C243kvH2Mkgr6qvL6RA1NlSq9DL5OsXCosGDXEh3NRir8rl0qeDmE9TAbhwYmkbXJAP708n9fm6TfU6SI5OfGcli3KGRxnD '
    'you@example.com '
)


@pytest.mark.django_db
def test_wrong_key_will_be_rejected():
    form_data = {'description': 'вуглускр', 'key': 'wrong key'}
    form = BannedSSHAdminForm(form_data)
    assert not form.is_valid(), 'Form should be invalid for wrong key'


@pytest.mark.django_db
def test_right_key_will_be_accepted():
    form_data = {'description': 'вуглускр', 'key': GOOD_KEY}
    form = BannedSSHAdminForm(form_data)
    assert form.is_valid(), 'Form should be valid for good key'


@pytest.mark.django_db
def test_fingerprint_will_be_filled_for_right_key():
    form_data = {'description': 'вуглускр', 'key': GOOD_KEY}
    form = BannedSSHAdminForm(form_data)
    assert form.is_valid() and form.cleaned_data['fingerprint_sha256'], 'Form should have fingerprint for good key'
