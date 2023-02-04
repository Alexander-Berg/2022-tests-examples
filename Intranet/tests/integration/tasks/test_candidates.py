from datetime import timedelta

import pytest

from django.db.models import Q
from django.utils import timezone

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.candidates.models import DuplicationCase, CandidateContact, Candidate
from intranet.femida.src.candidates.tasks import run_duplicates_search

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db

two_days_ago = timezone.now() - timedelta(days=2)


def _create_simple_candidate_with_phone(last_name, phone, modified=None, **kwargs):
    cand = f.CandidateFactory.create(last_name=last_name, **kwargs)
    f.CandidateContactFactory.create(
        type=CONTACT_TYPES.phone,
        candidate=cand,
        account_id=phone,
    )
    if modified is not None:
        CandidateContact.objects.filter(candidate_id=cand.id).update(modified=modified)
        Candidate.unsafe.filter(id=cand.id).update(modified=modified)
    return cand


test_data = [
    {  # 0. Похожие кандидаты
        'cand1': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
        },
        'cand2': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
        },
        'is_duplicate_created': True,
    },
    {  # 1. Похожие кандидаты, c разными логинами
        'cand1': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
            'login': 'ivanov',
        },
        'cand2': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
            'login': 'ivan',
        },
        'is_duplicate_created': False,
    },
    {  # 2. Похожие кандидаты, измененные давно
        'cand1': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
            'modified': two_days_ago,
        },
        'cand2': {
            'last_name': 'Ivanov',
            'phone': '+77001112233',
            'modified': two_days_ago,
        },
        'is_duplicate_created': False,
    },
]


@pytest.mark.parametrize('data', test_data)
def test_run_duplicates_search(data):
    cand1 = _create_simple_candidate_with_phone(**data['cand1'])
    cand2 = _create_simple_candidate_with_phone(**data['cand2'])
    run_duplicates_search()
    assert (
        DuplicationCase.unsafe
        .filter(
            Q(first_candidate=cand1, second_candidate=cand2)
            | Q(first_candidate=cand2, second_candidate=cand1)
        ).exists() == data['is_duplicate_created']
    )
