import datetime
from decimal import Decimal

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps




client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id,'ur',{'is-partner':'1'})


tag_id = db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']


api.medium().CreateOrUpdateDistributionTag(16571028,
                                               {'TagID': tag_id, 'TagName': 'testTagName',
                                                'ClientID': client_id})