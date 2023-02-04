import pytest

from intranet.femida.src.candidates import choices
from intranet.femida.tests import factories as f


# TODO: Добавить аттачи
@pytest.fixture
def dd_dataset():
    ivan = f.create_candidate_with_consideration(
        first_name='Ivan',
        middle_name='Ivanovich',
        last_name='Ivanov',
        country='Russian Federation',
        city='Moscow',
    )
    petr = f.create_candidate_with_consideration(
        first_name='Petr',
        middle_name='Petrovich',
        last_name='Petrov',
        country='Belarus',
        city='Minsk',
    )
    ivan_too = f.create_candidate_with_consideration(
        first_name='Vanya',
        middle_name='Ivanovich',
        last_name='ivanov',
        country='Russia',
        city='moscow',
    )
    contacts_data = [
        (ivan, choices.CONTACT_TYPES.email, 'ivanov@yandex.ru'),
        (ivan, choices.CONTACT_TYPES.skype, 'ivanov'),
        (petr, choices.CONTACT_TYPES.email, 'petrov@yandex.ru'),
        (petr, choices.CONTACT_TYPES.phone, '71234567890'),
        (ivan_too, choices.CONTACT_TYPES.email, 'ivanov@yandex.ru'),
        (ivan_too, choices.CONTACT_TYPES.phone, '71234567899'),
    ]
    for cand, contact_type, account_id in contacts_data:
        f.CandidateContactFactory.create(
            candidate=cand,
            type=contact_type,
            account_id=account_id,
        )

    ivan_submission = f.create_submission(
        candidate_data={
            'cand_name': 'Ivan',
            'cand_surname': 'Ivanov',
            'cand_phone': '71234567899',
            'cand_email': 'ivanov@yandex.ru',
            'contest_id': 1,
            'login': 'login',
        }
    )
    ivan_dict = {
        'first_name': 'Ivan',
        'middle_name': 'Ivanovich',
        'last_name': 'Ivanov',
        'country': 'Russian Federation',
        'city': 'Moscow',
        'contacts': [
            {
                'type': choices.CONTACT_TYPES.email,
                'account_id': 'ivanov@yandex.ru',
            },
            {
                'type': choices.CONTACT_TYPES.skype,
                'account_id': 'ivanov',
            }
        ]
    }

    return {
        'ivan': ivan,
        'petr': petr,
        'ivan_too': ivan_too,
        'ivan_submission': ivan_submission,
        'ivan_dict': ivan_dict,
    }
