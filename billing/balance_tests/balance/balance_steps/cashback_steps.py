from balance import balance_db as db


class CashbackSteps(object):
    @staticmethod
    def create(client_id, service_id, currency, bonus):
        next_sequence = db.balance().sequence_nextval('S_CLIENT_CASHBACK_ID')
        query = '''insert into BO.t_client_cashback (ID, CLIENT_ID, SERVICE_ID, ISO_CURRENCY, BONUS)
        values (:id, :client_id, :service_id, :currency, :bonus)'''
        query_params = {'id': next_sequence,
            'client_id': client_id, 
                        'service_id': service_id,
                        'currency': currency, 
                        'bonus': bonus}
        db.balance().execute(query, query_params)
        return next_sequence
