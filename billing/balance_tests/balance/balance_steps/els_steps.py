import balance.balance_db as db
import balance.balance_api as api


class ElsSteps(object):
    @staticmethod
    def get_ls_by_person_id_and_els_number(person_id, els_number):
        invoices = db.balance().execute('''select i.id, i.external_id from t_invoice i join t_extprops e on i.id = e.object_id
        where i.person_id = :person_id and e.classname='PersonalAccount' 
        and e.attrname = 'single_account_number' and e.value_num = :els_number''',
                                        {'person_id': person_id, 'els_number': els_number})
        assert len(invoices) <= 1
        if invoices:
            return invoices[0].get('id'), invoices[0].get('external_id')
        else:
            return None, None

    @staticmethod
    def get_els_number_from_client(client_id):
        return db.balance().execute('''select single_account_number from t_client where id = :client_id''',
                                    {'client_id': client_id})[0]['single_account_number']

    @staticmethod
    def create_els(client_id):
        single_account_number = api.test_balance().SingleAccountProcessClient(client_id)
        return single_account_number
