import pytest
from factory.django import mute_signals
from django.db.models import signals
from staff.person_profile.forms.contacts import ContactForm, ContactType
from staff.lib.testing import StaffFactory
from staff.person.models import Contact, VALIDATION_TYPES


@pytest.fixture()
def create_contact(db):
    with mute_signals(signals.pre_save, signals.post_save):
        staff = StaffFactory(login='tamirok')
        telegram = ContactType(name_en='Telegram', validation_type=VALIDATION_TYPES.LOGIN)
        telegram.save()
        return {
            'telegram': telegram,
            'staff': staff,
        }


def test_telegram_alias(create_contact):
    with mute_signals(signals.pre_save, signals.post_save):
        data = create_contact
        telegram, staff = data['telegram'], data['staff']
        form = ContactForm(data={
            'account_id': '@tamirok',
            'contact_type': telegram.id
        }
        )
        assert form.is_valid()
        data = form.cleaned_data
        contact = Contact(
            person_id=staff.id,
            contact_type_id=data['contact_type'].id,
            account_id=data['account_id'],
        )
        contact.save()
        assert contact.account_id == 'tamirok'

        form = ContactForm(data={
            'account_id': ' @ tamirok ',
            'contact_type': telegram.id
        }
        )
        assert form.is_valid()
        data = form.cleaned_data
        contact = Contact(
            person_id=staff.id,
            contact_type_id=data['contact_type'].id,
            account_id=data['account_id'],
        )
        contact.save()
        assert contact.account_id == 'tamirok'
