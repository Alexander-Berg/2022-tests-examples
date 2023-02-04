import datetime

import pytest
from hamcrest import none, equal_to

import btestlib.utils as utils
from balance import balance_db as db
from balance import balance_steps as steps

dt = datetime.datetime.now()
contract_finish_dt = (datetime.datetime.now()+datetime.timedelta(days=1))

PERSON_TYPE_LIST = [
    'ur',
    'ur_autoru',
    'ua',
]

INN_DICT = {'inn_starts_with_99': '998724554655',
            'inn_correct': '318290305927'}


@pytest.mark.parametrize('PERSON_TYPE',
                         PERSON_TYPE_LIST)
@pytest.mark.parametrize('INN',
                         [inn for inn in INN_DICT.itervalues()])
def test_create_person_with_nonres_inn(PERSON_TYPE, INN):
    client_id = steps.ClientSteps.create()
    person_id = None
    if INN.startswith('99') and PERSON_TYPE in ['ur', 'ur_autoru']:
            try:
                person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params=dict(inn=INN))
            except Exception, exc:
                utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to('INVALID_INN'))
            utils.check_that(person_id, none())

    else:
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params=dict(inn=INN))
        person = db.get_person_by_id(person_id)
        utils.check_that(person['inn'], equal_to(INN))


@pytest.mark.parametrize('INN', [inn for inn in INN_DICT.itervalues()])
def test_create_person_with_nonres_inn_endbuyer(INN):
    contract_type = 'comm'
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': utils.Date.date_to_iso_format(dt),
                                                                         'FINISH_DT': utils.Date.date_to_iso_format(
                                                                             contract_finish_dt),
                                                                         'IS_SIGNED': utils.Date.date_to_iso_format(dt),
                                                                         'SERVICES': [7]})
    endbuyer_id = steps.PersonSteps.create(client_id, 'endbuyer_ur', params=dict(inn=INN))
    endbuyer = db.get_person_by_id(endbuyer_id)
    utils.check_that(endbuyer['inn'], equal_to(INN))

if __name__ == "__main__":
    # pytest.main("Balance22238_inn_starts_with_99.py -vk 'test_create_person_with_nonres_inn'")
    pytest.main("Balance22238_inn_starts_with_99.py -vk 'test_create_person_with_nonres_inn_endbuyer'")
