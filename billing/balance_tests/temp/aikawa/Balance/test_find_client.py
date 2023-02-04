# -*- coding: utf-8 -*-

from balance import balance_steps as steps

client_id = steps.ClientSteps.create({'EMAIL': 'rgr@rtrg.tt'})
steps.ClientSteps.link(client_id, 'aikawa-test-0')
print steps.ClientSteps.find_client({'ClientID': client_id})
print steps.ClientSteps.find_client({'Login': 'aikawa-test-0'})
print steps.ClientSteps.find_client({'PassportID': 327224994})
print steps.ClientSteps.find_client({'Email': 'rgr@rtrg.tt', 'ClientID': client_id})
