# coding: utf-8


import json
from random import randrange

from django.db import transaction

from review.lib import encryption
from review.staff.models import Person

from review.bi.models import BIPersonIncome, BIPersonVesting
from review.bi.testing_sync_data import INCOME_DATA_MOCKS, VESTING_DATA_MOCKS


@transaction.atomic
def fill_income_with_mocks():
    BIPersonIncome.objects.all().delete()

    to_create = [
        BIPersonIncome(
            person_id=person_id,
            data=encryption.encrypt(json.dumps(INCOME_DATA_MOCKS[randrange(2)])),
            hash=0,
        )
        for person_id in Person.objects.values_list('id', flat=True)
    ]

    BIPersonIncome.objects.bulk_create(to_create)


@transaction.atomic
def fill_vesting_with_mocks():
    BIPersonVesting.objects.all().delete()

    to_create = [
        BIPersonVesting(
            person_id=person_id,
            data=encryption.encrypt(json.dumps(VESTING_DATA_MOCKS[randrange(2)])),
            hash=0,
        )
        for person_id in Person.objects.values_list('id', flat=True)
    ]

    BIPersonVesting.objects.bulk_create(to_create)
