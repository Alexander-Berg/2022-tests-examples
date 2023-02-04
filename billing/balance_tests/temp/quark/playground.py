# -*- coding: utf-8 -*-

from balance import balance_steps as steps
from  btestlib.data import person_defaults
from balance import balance_db as db


def test_person_passport_birthplace():

    client_id= steps.ClientSteps.create()
    person_id= steps.PersonSteps.create(client_id, 'ph', full=True)
    passport_birthplace = person_defaults.get_details('ph', full=True)['passport_birthplace']

    query = """select ca.value_str from bo.t_person p
join bo.t_contract_attributes ca on p.attribute_batch_id = ca.attribute_batch_id
where p.id = :person_id and code = 'PASSPORT_BIRTHPLACE'"""

    params = {'person_id': person_id}
    passport_birthplace_new = db.balance().execute(query, params)[0]['value_str']

    assert passport_birthplace == passport_birthplace_new
