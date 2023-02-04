from balance import balance_steps as steps
from btestlib import utils as utils
import btestlib.constants as constants


def test_collateral_do_prepay():
    from btestlib.data.defaults import Date
    from temp.igogor.balance_objects import Contexts

    UR_CTX = Contexts.DIRECT_FISH_RUB_CONTEXT

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, UR_CTX.person_type.code)

    QTY = 100
    YESTERDAY_ISO = utils.Date.to_iso(utils.Date.shift_date(Date.TODAY, days=-1))
    contract_id, _ = steps.ContractSteps.create_contract_new(constants.ContractCommissionType.OPT_CLIENT, {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': constants.ContractPaymentType.POSTPAY,
        'SERVICES': [UR_CTX.service.id],
        'DT': YESTERDAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': YESTERDAY_ISO,
        'CURRENCY': UR_CTX.currency.num_code,
        'CREDIT_TYPE': constants.ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': QTY * UR_CTX.price * 10
    })

    campaigns_list = [
        {'client_id': client_id, 'service_id': UR_CTX.service.id, 'product_id': UR_CTX.product.id, 'qty': QTY},
    ]

    steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                            person_id=person_id,
                                            campaigns_list=campaigns_list,
                                            paysys_id=UR_CTX.paysys.id,
                                            credit=1,
                                            contract_id=contract_id)

    steps.ContractSteps.create_collateral(100, {'CONTRACT2_ID': contract_id,
                                                'DT': Date.TODAY_ISO,
                                                'IS_SIGNED': Date.TODAY_ISO})
    # #
    # steps.ContractSteps.create_collateral(100, {'CONTRACT2_ID': contract_id,
    #                                             'DT': Date.TODAY_ISO,
    #                                             'IS_SIGNED': Date.TODAY_ISO})

    steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                            person_id=person_id,
                                            campaigns_list=campaigns_list,
                                            paysys_id=UR_CTX.paysys.id,
                                            credit=1,
                                            contract_id=contract_id)
