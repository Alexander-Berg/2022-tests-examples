# -*- coding: utf-8 -*-
__author__ = 'aikawa'

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

# # -------- firm_id = 1 -------
person_type = 'ur'  # ЮЛ резидент РФ

client_id = mtl.create_client({'IS_AGENCY': 0})

person_id = mtl.create_person(client_id, person_type)

print test_rpc.InvalidatePersonBankProps(person_id)

print test_rpc.InvalidatePersonBankProps(person_id)
