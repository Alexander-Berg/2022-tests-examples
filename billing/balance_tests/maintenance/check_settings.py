# coding=utf-8

import balance.balance_db as db
from btestlib.constants import Services

DIRECT = Services.DIRECT.id
VENDORS = Services.VENDORS.id
RABOTA = Services.RABOTA.id
TOLOKA = Services.TOLOKA.id
MARKET = Services.MARKET.id
ADFOX = Services.ADFOX.id
KUPIBILET = Services.KUPIBILET.id
MEDIASELLING = Services.BAYAN.id
BAYAN = Services.MEDIA_BANNERS.id
MEDIA = Services.MEDIA_70.id
GEO = Services.GEO.id
APIKEYS = Services.APIKEYS.id
DSP = Services.DSP.id
MEDIABANNERS = Services.MEDIA_BANNERS_167.id
TRANSLATE = Services.TRANSLATE.id
CLOUD = Services.CLOUD_143.id
PUBLIC = Services.PUBLIC.id
DOSTAVKA = Services.DOSTAVKA.id
BANKI = Services.BANKI.id
OFD = Services.OFD.id
SPAMDEF = Services.SPAMDEF.id
TAXI = Services.TAXI_111.id
CORP_TAXI = Services.TAXI_CORP.id
TAXI_DONATE = Services.TAXI_DONATE.id
RIT = Services.RIT.id

# _СЕРВИСЫ_________________________________________________________________________________________________________________________

# сервисы, по которым клиенты не могут выставляться без договора при наличии договора
SERVICES_WITH_RESTRICT_CLIENT_FLAG = {MARKET, TOLOKA, RIT, RABOTA, ADFOX, TAXI, KUPIBILET, VENDORS, CORP_TAXI, TAXI_DONATE}

# сервисы, по которым агенства могут выставляться без договора при наличии договора
SERVICES_ALLOWED_AGENCY_WITHOUT_CONTRACT_FLAG = {DIRECT, MEDIASELLING, MARKET, GEO}

# сервисы, по которым агенства не могут выставляться без договора
SERVICES_WITH_CONTRACT_NEEDED_FLAG = {DIRECT, MARKET, MEDIASELLING, VENDORS, APIKEYS, DSP,
                                      BAYAN, GEO, MEDIABANNERS, TOLOKA, TRANSLATE, CLOUD,
                                      PUBLIC, DOSTAVKA, KUPIBILET, BANKI, SPAMDEF, OFD}


def test_check_service_with_restrict_client_flag():
    services = db.balance().execute('''select id from v_service where restrict_client = 1''')
    services_ids = {service['id'] for service in services}
    assert services_ids == SERVICES_WITH_RESTRICT_CLIENT_FLAG


def test_check_service_with_allowed_agency_without_contract_flag():
    services = db.balance().execute('''select OBJECT_ID from T_EXTPROPS where classname = 'Service' and ATTRNAME = 'allowed_agency_without_contract' and VALUE_NUM = 1''')
    services_ids = {service['object_id'] for service in services}
    assert services_ids == SERVICES_ALLOWED_AGENCY_WITHOUT_CONTRACT_FLAG


def test_check_service_with_contact_needed_flag():
    services = db.balance().execute('''select id from v_service where contract_needed = 1''')
    services_ids = {service['id'] for service in services}
    assert services_ids == SERVICES_WITH_CONTRACT_NEEDED_FLAG
