# -*- coding: utf-8 -*-
__author__ = 'aikawa'

from balance import balance_steps as steps

# PERSON_TYPE = 'sw_yt'
PERSON_TYPE = 'sw_ph'

client_id = steps.ClientSteps.create({'IS_AGENCY': '1'}, passport_uid=327224994)
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params={'vip':'1', 'verified-docs':'0', 'name': u'Ваня', 'region': '225', 'longname': u'Иван','legaladdress': 'street'}, passport_uid = 327224994)
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params={'person_id': person_id, 'vip':'0', 'verified-docs':'1', 'name': u'Ваня1', 'region': '187', 'longname': u'Иван1','legaladdress': 'street1'}, passport_uid = 327224994)