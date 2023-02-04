from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint


# create an instance of the API class
api_configuration = swagger_client.Configuration()
api_configuration.debug = True
api_client = swagger_client.ApiClient(configuration=api_configuration)
api_instance = swagger_client.DefaultApi(api_client=api_client)
tags = swagger_client.Tags(itype=['mailpostgresql'])
alert = swagger_client.Alert(
    name='paysys.trust-posrtgres-prod2',
    signal='postgresql_log-log_db-trust_payments_prod-errors_mmmm',
    tags=tags,
    warn=[0, 10],
    crit=[10, 9999999],
    mgroups=['CON'],
)
body = alert # Body | Alert object to be created

try:
    api_instance.create_alert(body)
except ApiException as e:
    print("Exception when calling DefaultApi->create_alert: %s\n" % e)

try:
    api_instance.get_alert(name='paysys.trust-posrtgres-prod', with_checks='false')
except ApiException as e:
    print("Exception when calling DefaultApi->create_alert: %s\n" % e)


try:
    api_instance.list_alerts(name_prefix='paysys', with_checks='false')
except ApiException as e:
    print("Exception when calling DefaultApi->create_alert: %s\n" % e)

try:
    api_instance.delete_alert(name='paysys.trust-posrtgres-prod1')
except ApiException as e:
    print("Exception when calling DefaultApi->create_alert: %s\n" % e)

try:
    api_instance.list_alerts(name_prefix='paysys', with_checks='false')
except ApiException as e:
    print("Exception when calling DefaultApi->create_alert: %s\n" % e)

