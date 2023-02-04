from balance import balance_api as api
from balance import balance_db as db
from btestlib.constants import Users


class RefundSteps(object):

    @staticmethod
    def create_refund(cpf_id, amount, payload=None, user_uid=Users.YB_ADM.uid):
        refund_id = api.test_balance().CreateInvoiceRefund(user_uid, cpf_id, amount)
        return refund_id

    @staticmethod
    def check_invoice_refund_status(refund_id):
        refund_id = api.test_balance().CheckInvoiceRefundStatus(refund_id)
        return refund_id

    @staticmethod
    def set_properties(refund_id, status=None, descr=None, payload=None):
        db.balance().execute('''update (select * from t_invoice_refund where id=:refund_id) set status_code=:status, 
        status_descr=:status_descr, payload=:payload''',
                             {'status': status, 'refund_id': refund_id, 'status_descr': descr,
                              'payload': payload})

    @staticmethod
    def get(refund_id):
        return db.balance().execute("select * from bo.t_invoice_refund where id = :id", {'id': refund_id})[0]
