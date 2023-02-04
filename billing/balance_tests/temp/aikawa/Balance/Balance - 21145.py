# coding=utf-8

import datetime

from balance import balance_api as api
from balance import balance_steps as steps

after = datetime.datetime(2015, 6, 24, 11, 0, 0)

dt = after

service_id = 7
product_id = 1475
person_type = 'ur'
paysys_id = 1003

qty = 100


def set_high_priority_to_acts():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)
    api.test_balance().export_object('OEBS', 'Person', person_id)


if __name__ == "__main__":
    set_high_priority_to_acts()
