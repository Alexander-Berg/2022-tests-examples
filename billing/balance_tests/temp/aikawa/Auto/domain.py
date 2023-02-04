import datetime

import balance.balance_db as db
import btestlib.utils as utils


class Domain(object):

    def __init__(self, client_id, url_source='text', active = 1, domain = None, removed=0):
        if not domain:
            domain = utils.generate_alfanumeric_string(5)
        self.id = db.balance().execute('''select S_CLIENT_DOMAIN.nextval from dual''')[0]['nextval']
        self.client_id = client_id
        self.sync_id = db.balance().execute('''select max(sync_id) from T_CLIENT_DOMAIN2''')[0]['MAX(SYNC_ID)']
        self.log_date = datetime.datetime.now()
        self.url_source = url_source
        self.active = active
        self.removed = removed
        self.domain = domain
        db.balance().execute(
            '''INSERT INTO T_CLIENT_DOMAIN2 VALUES (:id, :client_id, :sync_id, :domain, :log_date, :url_source, :removed, :active)''',
            {'id': self.id,
                                'client_id': self.client_id,
                                'sync_id': self.sync_id,
                                'log_date': self.log_date,
                                'url_source': self.url_source,
                                'removed': self.removed,
                                'active': self.active,
                                'domain': self.domain
                                })



