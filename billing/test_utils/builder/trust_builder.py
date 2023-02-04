from datetime import datetime
from decimal import Decimal
from typing import Any, Optional

import arrow

from billing.library.python.calculator.values import PaymentMethodID

_dt_with_tz = arrow.get(datetime(2020, 1, 1, 10, 54, 0)).datetime


def gen_fiscal_info(
    fiscal_agent_type: Optional[str] = None,
    fiscal_doc_type: Optional[str] = None,
    fiscal_partner_inn: Optional[str] = None,
    fiscal_partner_phone: Optional[str] = None,
    fiscal_status: Optional[str] = None,
    fiscal_taxation_type: Optional[str] = None,
    fiscal_title: Optional[str] = None,
) -> dict:

    return {
        'fiscal_agent_type': fiscal_agent_type,
        'fiscal_doc_type': fiscal_doc_type,
        'fiscal_partner_inn': fiscal_partner_inn,
        'fiscal_partner_phone': fiscal_partner_phone,
        'fiscal_status': fiscal_status,
        'fiscal_taxation_type': fiscal_taxation_type,
        'fiscal_title': fiscal_title,
    }


def gen_order(
    dt: datetime = _dt_with_tz,
    service_id: int = 118,
    service_order_id: str = '123',
    service_order_id_number: int = 123,
    # update_dt: datetime = _dt_with_tz,

    clid: Optional[str] = None,
    commission_category: Optional[str] = None,
    # contract_id: Optional[str] = None,
    developer_payload: Optional[Any] = None,
    passport_id: Optional[int] = None,
    # price: Optional[Decimal] = None,
    # region_id: Optional[int] = None,
    service_product_external_id: Optional[str] = None,
    service_product_id: Optional[int] = None,
    # start_dt_offset: Optional[int] = None,
    # start_dt_utc: Optional[datetime] = None,
    # text: Optional[str] = None,
) -> dict:

    return {
        'dt': dt,
        'service_id': service_id,
        'service_order_id': service_order_id,
        'service_order_id_number': service_order_id_number,
        # 'update_dt': update_dt,

        'clid': clid,
        'commission_category': commission_category,
        # 'contract_id': contract_id,
        'developer_payload': developer_payload,
        'passport_id': passport_id,
        # 'price': price,
        # 'region_id': region_id,
        'service_product_external_id': service_product_external_id,
        'service_product_id': service_product_id,
        # 'start_dt_offset': start_dt_offset,
        # 'start_dt_utc': start_dt_utc,
        # 'text': text,
    }


def gen_payment_row(
    amount: Decimal,
    id: int = 1,
    order: Optional[dict] = None,
    # quantity: Decimal = Decimal(0.0),

    cancel_dt: Optional[datetime] = None,
    # fiscal_agent_type: Optional[str] = None,
    # fiscal_inn: Optional[str] = None,
    # fiscal_item_code: Optional[str] = None,
    fiscal_nds: Optional[str] = 'nds_0',
    # fiscal_title: Optional[str] = None,
    # payment_id: Optional[int] = None,
    # price: Optional[Decimal] = None,
) -> dict[str, Any]:

    return {
        'amount': amount,
        'id': id,
        'order': order or gen_order(),
        # 'quantity': quantity,

        'cancel_dt': cancel_dt,
        # 'fiscal_agent_type': fiscal_agent_type,
        # 'fiscal_inn': fiscal_inn,
        # 'fiscal_item_code': fiscal_item_code,
        'fiscal_nds': fiscal_nds,
        # 'fiscal_title': fiscal_title,
        # 'payment_id': payment_id,
        # 'price': price,
    }


def gen_service_product(
    external_id: str = '6109807978868302650',
    id: int = 1,
    name: str = 'Super Product',
    service_id: int = 118,

    # active_until_dt: Optional[datetime] = None,
    # fiscal_nds: Optional[str] = None,
    # fiscal_title: Optional[str] = None,
    # hidden: Optional[int] = None,
    # inapp_name: Optional[str] = None,
    # package_name: Optional[str] = None,
    # parent_id: Optional[int] = None,
    partner_id: Optional[int] = None,
    # product_type: Optional[str] = None,
    service_fee: Optional[int] = None,
    # single_purchase: Optional[bool] = None,
    # subs_period: Optional[str] = None,
    # subs_trial_period: Optional[str] = None,
) -> dict:

    return {
        'external_id': external_id,
        'id': id,
        'name': name,
        'service_id': service_id,

        # 'active_until_dt': active_until_dt,
        # 'fiscal_nds': fiscal_nds,
        # 'fiscal_title': fiscal_title,
        # 'hidden': hidden,
        # 'inapp_name': inapp_name,
        # 'package_name': package_name,
        # 'parent_id': parent_id,
        'partner_id': partner_id,
        # 'product_type': product_type,
        'service_fee': service_fee,
        # 'single_purchase': single_purchase,
        # 'subs_period': subs_period,
        # 'subs_trial_period': subs_trial_period,
    }


def gen_partner(
    dt: datetime = _dt_with_tz,
    id: int = 1353409549,

    city: Optional[str] = None,
    email: Optional[str] = None,
    fax: Optional[str] = None,
    fullname: Optional[str] = None,
    name: Optional[str] = None,
    passport_id: Optional[int] = None,
    phone: Optional[str] = None,
    region_id: Optional[int] = None,
    url: Optional[str] = None,
) -> dict:

    return {
        'dt': dt,
        'id': id,

        'city': city,
        'email': email,
        'fax': fax,
        'fullname': fullname,
        'name': name,
        'passport_id': passport_id,
        'phone': phone,
        'region_id': region_id,
        'url': url
    }


def gen_fraud_status(
    afs_action: Optional[str] = None,
    afs_additional_info: Optional[str] = None,
    afs_check_method: Optional[str] = None,
    afs_resp_desc: Optional[str] = None,
    afs_risk_score: Optional[int] = None,
    afs_rule_score: Optional[int] = None,
    afs_status: Optional[str] = None,
    afs_tags: Optional[str] = None,
    afs_tx_id: Optional[str] = None,
    partition_dt: Optional[datetime] = None,
    payment_id: Optional[int] = None,
    trust_payment_id: Optional[str] = None,
) -> dict:

    return {
        'afs_action': afs_action,
        'afs_additional_info': afs_additional_info,
        'afs_check_method': afs_check_method,
        'afs_resp_desc': afs_resp_desc,
        'afs_risk_score': afs_risk_score,
        'afs_rule_score': afs_rule_score,
        'afs_status': afs_status,
        'afs_tags': afs_tags,
        'afs_tx_id': afs_tx_id,
        'partition_dt': partition_dt,
        'payment_id': payment_id,
        'trust_payment_id': trust_payment_id,
    }


def gen_refund(
    amount: Decimal,
    currency: str = 'RUB',
    description: str = 'cancel payment',
    dt: datetime = _dt_with_tz,
    is_reversal: int = 0,
    # refund_to: str = 'paysys',
    rows: Optional[list[dict]] = None,
    service_id: int = 118,
    trust_refund_id: str = '615c678a5b095cb6e55af28e',
    #  # type: str = 'REFUND',

    cancel_dt: Optional[datetime] = None,
    # passport_id: Optional[int] = None,
    payment_dt: Optional[datetime] = None,
    # resp_code: Optional[str] = None,
    # resp_desc: Optional[str] = None,
    # resp_dt: Optional[datetime] = None,
    terminal_id: Optional[int] = None,
    trust_group_id: Optional[str] = None,
) -> dict[str, Any]:

    return {
        'amount': amount,
        'currency': currency,
        'description': description,
        'dt': dt,
        'is_reversal': is_reversal,
        # 'refund_to': refund_to,
        'rows': rows or [],
        'service_id': service_id,
        'trust_refund_id': trust_refund_id,
        # 'type': type,

        'cancel_dt': cancel_dt,
        # 'passport_id': passport_id,
        'payment_dt': payment_dt,
        # 'resp_code': resp_code,
        # 'resp_desc': resp_desc,
        # 'resp_dt': resp_dt,
        'terminal_id': terminal_id,
        'trust_group_id': trust_group_id,
    }


def gen_payment(
    amount: Decimal,
    currency: str = 'RUB',
    dt: datetime = _dt_with_tz,
    # fiscal_info: Optional[dict] = None,
    # fraud_status: Optional[dict] = None,
    # partners: Optional[list[dict]] = None,
    payment_method: str = 'card-x04f1ec28585ff787eb9cd9ea',
    payment_method_id: int = PaymentMethodID.CARD,
    payment_status: str = 'postauthorized',
    payment_type: str = 'TRUST_PAYMENT',
    products: Optional[list[dict]] = None,
    purchase_token: str = 'e3f9654b438f860927a5ab02a70c412c',
    rows: Optional[list[dict]] = None,
    service_id: int = 118,
    trust_payment_id: str = '615c677fb955d7f0c0cf5720',
    # user_ip: str = '127.0.0.1',

    # acs_redirect_url: Optional[str] = None,
    # application: Optional[str] = None,
    # approval_code: Optional[str] = None,
    # binding_result: Optional[str] = None,
    cancel_dt: Optional[datetime] = None,
    # card_id: Optional[str] = None,
    # cardholder: Optional[str] = None,
    # cashback_amount: Optional[Decimal] = None,
    # cashback_parent_id: Optional[int] = None,
    # cashback_trust_payment_id: Optional[str] = None,
    composite_components: Optional[list[dict]] = None,
    # composite_payment_id: Optional[int] = None,
    description: Optional[str] = None,
    developer_payload: Optional[Any] = None,
    firm_id: Optional[int] = None,
    # force_3ds: Optional[int] = None,
    # notify_url: Optional[str] = None,
    # passport_id: Optional[int] = None,
    payment_dt: Optional[datetime] = None,
    # payment_mode: Optional[str] = None,
    # paysys_partner_id: Optional[int] = None,
    # platform: Optional[str] = None,
    postauth_amount: Optional[Decimal] = None,
    postauth_dt: Optional[datetime] = None,
    processing_id: Optional[int] = None,
    # real_postauth_dt: Optional[datetime] = None,
    # refund_groups: Optional[list[dict]] = None,
    refunds: Optional[list[dict]] = None,
    # resp_code: Optional[str] = None,
    # resp_desc: Optional[str] = None,
    # resp_dt: Optional[datetime] = None,
    # rrn: Optional[str] = None,
    # start_dt: Optional[datetime] = None,
    terminal_id: Optional[int] = None,
    transaction_id: Optional[str] = None,
    transaction_request_id: Optional[int] = None,
    trust_group_id: Optional[str] = None,
    # user_account: Optional[str] = None,
    # user_email: Optional[str] = None,
    # user_phone: Optional[str] = None,
    # version: Optional[int] = None,
    payload: Optional[dict] = None,
    tariffer_payload: Optional[dict] = None,
) -> dict[str, Any]:
    return {
        'amount': amount,
        'currency': currency,
        'dt': dt,
        # 'fiscal_info': fiscal_info or gen_fiscal_info(),
        # 'fraud_status': fraud_status or gen_fraud_status(),
        # 'partners': partners or [],
        'payment_method': payment_method,
        'payment_status': payment_status,
        'payment_type': payment_type,
        'products': products or [gen_service_product()],
        'purchase_token': purchase_token,
        'rows': rows or [],
        'service_id': service_id,
        'trust_payment_id': trust_payment_id,
        # 'user_ip': user_ip,

        # 'acs_redirect_url': acs_redirect_url,
        # 'application': application,
        # 'approval_code': approval_code,
        # 'binding_result': binding_result,
        'cancel_dt': cancel_dt,
        # 'card_id': card_id,
        # 'cardholder': cardholder,
        # 'cashback_amount': cashback_amount,
        # 'cashback_parent_id': cashback_parent_id,
        # 'cashback_trust_payment_id': cashback_trust_payment_id,
        'composite_components': composite_components or [],
        # 'composite_payment_id': composite_payment_id,
        'description': description,
        'developer_payload': developer_payload,
        'firm_id': firm_id,
        # 'force_3ds': force_3ds,
        # 'notify_url': notify_url,
        # 'passport_id': passport_id,
        'payment_dt': payment_dt,
        'payment_method_id': payment_method_id,
        # 'payment_mode': payment_mode,
        # 'paysys_partner_id': paysys_partner_id,
        # 'platform': platform,
        'postauth_amount': postauth_amount,
        'postauth_dt': postauth_dt,
        'processing_id': processing_id,
        # 'real_postauth_dt': real_postauth_dt,
        # 'refund_groups': refund_groups or [],
        'refunds': refunds or [],
        # 'resp_code': resp_code,
        # 'resp_desc': resp_desc,
        # 'resp_dt': resp_dt,
        # 'rrn': rrn,
        # 'start_dt': start_dt,
        'terminal_id': terminal_id,
        'transaction_id': transaction_id,
        'transaction_request_id': transaction_request_id,
        'trust_group_id': trust_group_id,
        # 'user_account': user_account,
        # 'user_email': user_email,
        # 'user_phone': user_phone,
        # 'version': version,
        'payload': payload or {},
        'tariffer_payload': tariffer_payload or {},
    }
