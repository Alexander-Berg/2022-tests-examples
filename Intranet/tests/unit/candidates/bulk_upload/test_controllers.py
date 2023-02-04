import pytest

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.candidates.bulk_upload.controllers import (
    _get_missing_contacts,
)

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_get_missing_contacts():
    candidate = f.CandidateFactory()
    conflict_contact = f.CandidateContactFactory(candidate=candidate)
    f.CandidateContactFactory(
        candidate=candidate,
        type=CONTACT_TYPES.email,
        is_main=True,
    )

    contacts = [
        {  # Должен проигнориться, потому что это копия
            'account_id': conflict_contact.account_id,
            'type': conflict_contact.type,
            'is_main': True,
        },
        {  # Должен стать is_main = False, потому что основной email уже есть
            'account_id': 'ya@ya.ru',
            'type': CONTACT_TYPES.email,
            'is_main': True,
        },
        {  # Должен остаться без изменений
            'account_id': '+77021123419',
            'type': CONTACT_TYPES.phone,
            'is_main': True,
        },
    ]

    expected = [
        {
            'account_id': 'ya@ya.ru',
            'normalized_account_id': 'ya@ya.ru',
            'type': CONTACT_TYPES.email,
            'is_main': False,
        },
        {
            'account_id': '+77021123419',
            'normalized_account_id': '+7 702 112 3419',
            'type': CONTACT_TYPES.phone,
            'is_main': True,
        },
    ]

    result = _get_missing_contacts(candidate, contacts)
    assert result == expected
