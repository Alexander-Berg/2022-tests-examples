#Необходимые предустановки для pythin консоли

from apikeys.apikeys_api import UI2, BO2, TEST, Questionary, ST_ISSUE, API
from apikeys.tests_by_object_model.conftest import db_connection
from apikeys.apikeys_defaults import ADMIN, APIKEYS_SERVICE_ID
from balance.balance_steps import invoice_steps
from apikeys.apikeys_steps_new import clean_up
from datetime import datetime as DT, timedelta as shift


db = db_connection()
