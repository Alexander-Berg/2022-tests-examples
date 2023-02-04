import json

from balance import balance_steps as steps

client_id = 10
value_json_dict = {'Clients': [client_id]}

json_string = json.dumps(value_json_dict)
json_acceptable_string = json_string.replace("'", "\"")
steps.ConfigSteps.set_config_value('UA_PROCESS_BY_ORDERS', value_json=json_acceptable_string)
