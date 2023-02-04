from decimal import Decimal

from balance.balance_steps import (
    ActsSteps,
    CommonPartnerSteps,
    ContractSteps,
    ExportSteps,
    InvoiceSteps,
)
from balance.balance_steps.new_taxi_steps import TaxiData
from btestlib.constants import NdsNew, Services, SpendablePaymentType
from temp.allista.report import Report

PAYMENT_AMOUNT = Decimal("100.1")
REFUND_AMOUNT = Decimal("93.13")
TOTAL_AMOUNT = PAYMENT_AMOUNT - REFUND_AMOUNT


def export_context_to_oebs(
    context,
    start_dt,
    end_dt,
    completions=tuple(),
    invoices_for=("YANDEX_SERVICE",),
    spendable_contexts=None,
):
    additional_params = {"start_dt": start_dt}
    (
        client_id,
        person_id,
        contract_id,
        contract_eid,
    ) = ContractSteps.create_partner_contract(
        context, is_postpay=0, is_offer=1, additional_params=additional_params
    )

    compls_data_3rd_month = TaxiData.generate_default_oebs_compls_data(
        start_dt, context.currency.iso_code, start_dt
    ) + [
        {
            "service_id": Services.TAXI.id,
            "amount": (PAYMENT_AMOUNT - REFUND_AMOUNT),
            "product_id": product,
            "dt": start_dt,
            "transaction_dt": start_dt,
            "currency": context.currency.iso_code,
            "accounting_period": start_dt,
        }
        for product in completions
    ]

    CommonPartnerSteps.create_partner_oebs_completions(
        contract_id, client_id, compls_data_3rd_month
    )

    CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, end_dt
    )

    invoices = [
        InvoiceSteps.get_invoice_by_service_or_service_code(
            contract_id, service_code=service_code
        )
        for service_code in invoices_for
    ]
    act_data = ActsSteps.get_all_act_data(client_id, dt=end_dt)

    client_log = ExportSteps.get_oebs_api_response("Client", client_id)
    person_log = ExportSteps.get_oebs_api_response("Person", person_id)
    contract_log = ExportSteps.get_oebs_api_response("Contract", contract_id)
    pa_log = {
        pa_id: ExportSteps.get_oebs_api_response("Invoice", pa_id)
        for pa_id, pa_eid, service_code in invoices
    }
    act_log = {
        act["id"]: ExportSteps.get_oebs_api_response("Act", act["id"])
        for act in act_data
    }

    with Report(context, u"Projects/balance/reports") as report:
        report.write_log(
            ("Client", client_id, client_log),
            ("Person", person_id, person_log),
            ("Main Contract", contract_id, contract_log),
            ("Invoices", pa_log),
            ("Acts", act_log),
        )
        # handle spendable contexts, if any
        if not spendable_contexts:
            return
        additional_params.update(
            {
                "nds": NdsNew.ZERO.nds_id,
                "payment_type": SpendablePaymentType.MONTHLY,
                "link_contract_id": contract_id,
            }
        )
        for ctx in spendable_contexts:
            (
                _,
                spendable_person_id,
                spendable_contract_id,
                spendable_contract_eid,
            ) = ContractSteps.create_partner_contract(
                ctx,
                client_id=client_id,
                unsigned=False,
                additional_params=additional_params,
            )
            spendable_person_log = ExportSteps.get_oebs_api_response(
                "Person", spendable_person_id
            )
            spendable_contract_log = ExportSteps.get_oebs_api_response(
                "Contract", spendable_contract_id
            )
            report.write_log(
                (u"\n\n{ctx}".format(ctx=ctx.name),),
                ("Person", spendable_person_id, spendable_person_log),
                ("Contract", spendable_contract_id, spendable_contract_log),
            )
