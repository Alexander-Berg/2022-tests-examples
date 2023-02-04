# -*- encoding: utf-8 -*-
from datetime import (
    datetime,
    timedelta
)
from copy import deepcopy
import json
import random
import string

import pytest
from butils.application import getApplication

from yb_darkspirit import scheme
from yb_darkspirit import interactions


@pytest.fixture
def receipt_template(cr_wrapper):
    return {
        "id": 12345,
        "dt": datetime(2017, 11, 15, 16, 03, 00),
        "fp": 856605666,
        "document_index": 822,
        "shift_number": 267,
        "fn": {
            "sn": cr_wrapper.fiscal_storage.serial_number,
        },
        "receipt_content": {
            "firm_inn": "7736207543",
            "taxation_type": "OSN",
            "client_email_or_phone": "test@test.test",
            "receipt_type": "income",
            "additional_user_requisite": {
                "name": "trust_purchase_token",
                "value": "4d49gc3924e58fe36531d49b09bd5ceb"
            },
        },
        "receipt_calculated_content": {
            "total": "199.00",
        },
    }


@pytest.fixture
def receipts(test_client, receipt_template):
    receipts_number = 3
    purchase_token_len = 32
    purchase_tokens = [
        ''.join(
            random.choice(string.ascii_lowercase + string.digits)
            for _ in range(purchase_token_len)
        )
        for i in range(receipts_number)
    ]

    receipts = []
    for i, purchase_token in enumerate(purchase_tokens):
        receipt = deepcopy(receipt_template)
        receipt["id"] += i
        receipt["dt"] += timedelta(minutes=i)
        receipt["receipt_content"]["additional_user_requisite"]["value"] = purchase_token
        receipt["document_type"] = "Receipt"
        if i == 0:
            receipt["receipt_content"]["composite_event_id"] = {
                "payment_id_type": receipt["receipt_content"]["additional_user_requisite"]["name"],
                "payment_id": receipt["receipt_content"]["additional_user_requisite"]["value"],
                "event_id": "an_event_id",
            }
        receipts.append(receipt)

    mds_s3_client = interactions.ReceiptsClient.from_app(getApplication())
    for receipt in receipts:
        # TODO: replace/extend with moto
        try:
            existing_receipt = mds_s3_client.get_receipt(fn_sn=receipt["fn"]["sn"], dn=receipt["id"], do_retry=False)
        except mds_s3_client.exceptions.NoSuchKey:
            pass
        else:
            mds_s3_client.delete_receipt(fn_sn=receipt["fn"]["sn"], dn=receipt["id"])

        test_client.post(
            "/v1/receipts/",
            data=json.dumps(receipt, default=str),
            content_type="application/json"
        )

    return receipts


@pytest.mark.parametrize("is_confirmed", [False, True])
@pytest.mark.parametrize("doc_type,event_id", [
    ("Receipt", None),
    ("BSO", None),
    ("Receipt", "an_event_id"),
])
def test_create_receipt(test_client, session, receipt_template, is_confirmed, doc_type, event_id):
    """
    1. Ссылка, по которой можно забрать на чек, формируется правильно.
    2. Чек сохраняется в s3/базу (t_document) с отброшенными секундами.
    3. В зависимости от клиента (pull_documents или сервис) в базу
       сохраняется подтверждённый или неподтверждённый (is_confirmed) чек.
    4. В базу тело чека не сохраняется.
    """
    receipt_template["document_type"] = doc_type
    if event_id:
        receipt_template["receipt_content"]["composite_event_id"] = {
            "payment_id_type": receipt_template["receipt_content"]["additional_user_requisite"]["name"],
            "payment_id": receipt_template["receipt_content"]["additional_user_requisite"]["value"],
            "event_id": event_id,
        }

    mds_s3_client = interactions.ReceiptsClient.from_app(getApplication())
    try:
        mds_s3_client.get_receipt(fn_sn=receipt_template["fn"]["sn"], dn=receipt_template["id"], do_retry=False)
    except mds_s3_client.exceptions.NoSuchKey:
        pass
    else:
        mds_s3_client.delete_receipt(fn_sn=receipt_template["fn"]["sn"], dn=receipt_template["id"])

    response = test_client.post(
        "/v1/receipts/",
        query_string="is_confirmed=false" if not is_confirmed else None,
        data=json.dumps(receipt_template, default=str),
        content_type="application/json"
    )
    assert response.status_code == 200
    assert "v1/fiscal_storages/{fs_sn}/documents/{fs_dn}/{fp}".format(
        fs_sn=receipt_template["fn"]["sn"],
        fs_dn=receipt_template["id"],
        fp=receipt_template["fp"]
    ) in json.loads(response.get_data())["receipt_url"]

    dt_without_seconds = receipt_template["dt"].replace(second=0)
    receipt_with_dt_without_seconds = deepcopy(receipt_template)
    receipt_with_dt_without_seconds["dt"] = dt_without_seconds

    raw_receipt = mds_s3_client.get_receipt(
        fn_sn=receipt_template["fn"]["sn"],
        dn=receipt_template["id"],
    )["Body"].read()
    assert json.dumps(receipt_with_dt_without_seconds, default=str) == raw_receipt

    fs = (
        session.query(scheme.FiscalStorage)
        .filter_by(serial_number=receipt_template["fn"]["sn"])
    ).one()
    document = (
        session.query(scheme.Document)
        .filter_by(fiscal_storage=fs, fiscal_storage_number=receipt_template["id"])
    ).one()
    assert document.dt == dt_without_seconds
    assert document.is_confirmed is is_confirmed
    assert document.fiscal_storage_number == receipt_template["id"]
    assert document.fiscal_storage_sign == receipt_template["fp"]
    assert document.document_type == doc_type
    assert document.raw_document is None
    assert document.event_id == event_id


@pytest.mark.parametrize("doc_type,event_id", [
    ("Receipt", None),
    ("BSO", None),
    ("Receipt", "an_event_id"),
])
def test_create_receipt_by_service_after_pd(test_client, session,
                                            receipt_template, doc_type, event_id):
    """
    Проверка сценария досылки сервисом чека после того,
    как его уже забрал pull_documents.
    Дата должна остаться из ФНа.
    """
    receipt_template["document_type"] = doc_type
    if event_id:
        receipt_template["receipt_content"]["composite_event_id"] = {
            "payment_id_type": receipt_template["receipt_content"]["additional_user_requisite"]["name"],
            "payment_id": receipt_template["receipt_content"]["additional_user_requisite"]["value"],
            "event_id": event_id,
        }

    mds_s3_client = interactions.ReceiptsClient.from_app(getApplication())
    try:
        mds_s3_client.get_receipt(fn_sn=receipt_template["fn"]["sn"], dn=receipt_template["id"], do_retry=False)
    except mds_s3_client.exceptions.NoSuchKey:
        pass
    else:
        mds_s3_client.delete_receipt(fn_sn=receipt_template["fn"]["sn"], dn=receipt_template["id"])

    response = test_client.post(
        "/v1/receipts/",
        query_string="is_confirmed=false",
        data=json.dumps(receipt_template, default=str),
        content_type="application/json"
    )
    assert response.status_code == 200

    dt = receipt_template["dt"]
    receipt_template["dt"] = receipt_template["dt"] - timedelta(minutes=1)

    response = test_client.post(
        "/v1/receipts/",
        data=json.dumps(receipt_template, default=str),
        content_type="application/json"
    )
    assert response.status_code == 200

    fs = (
        session.query(scheme.FiscalStorage)
        .filter_by(serial_number=receipt_template["fn"]["sn"])
    ).one()
    document = (
        session.query(scheme.Document)
        .filter_by(fiscal_storage=fs, fiscal_storage_number=receipt_template["id"])
    ).one()
    assert document.dt == dt.replace(second=0)
    assert document.document_type == doc_type
    assert document.event_id == event_id


def test_get_receipt(test_client, receipt_template):
    mds_s3_client = interactions.ReceiptsClient.from_app(getApplication())
    fs_sn, dn, fp = receipt_template["fn"]["sn"], receipt_template["id"], receipt_template["fp"]
    try:
        mds_s3_client.get_receipt(fn_sn=fs_sn, dn=dn)
        mds_s3_client.delete_receipt(fn_sn=fs_sn, dn=dn)
    except mds_s3_client.exceptions.NoSuchKey:
        pass
    mds_s3_client.put_receipt(fs_sn, dn, json.dumps(receipt_template, default=str))

    receipt_url = "/v1/fiscal_storages/{sn}/documents/{dn}/{fp}".format(sn=fs_sn, dn=dn, fp=fp)
    response = test_client.get(receipt_url)
    assert "2017-11-15 16:03:00" in response.data


def test_receipts_search_by_user_requisites(test_client, session, receipts):

    receipt = receipts[0]
    response = test_client.post(
        "/v1/receipts/search-by-payment-ids",
        data=json.dumps(
            {
                "user_requisites": [
                    {
                        "name": "trust_purchase_token",
                        "value": receipt["receipt_content"]["additional_user_requisite"]["value"]
                    }
                ]
            },
            default=str
        ),
        content_type="application/json"
    )
    fetched_receipt = json.loads(response.data)["items"][0]
    assert fetched_receipt["id"] == receipt["id"]
    assert fetched_receipt["fn"] == receipt["fn"]
    assert fetched_receipt["fp"] == receipt["fp"]
    assert fetched_receipt["receipt_content"]["additional_user_requisite"] == \
           receipt["receipt_content"]["additional_user_requisite"]


def test_receipts_search_by_event_id(test_client, session, receipts):
    receipt = receipts[0]
    composite_event_id = receipt["receipt_content"]["composite_event_id"]
    response = test_client.post(
        "/v1/receipts/search-by-event-id",
        data=json.dumps(
            {
                "payment_id_type": composite_event_id["payment_id_type"],
                "payment_id": composite_event_id["payment_id"],
                "event_id": composite_event_id["event_id"]
            },
        ),
        content_type="application/json"
    )

    resp = json.loads(response.data)
    assert len(resp) > 0
    fetched_receipt = resp[0]
    assert fetched_receipt["id"] == receipt["id"]
    assert fetched_receipt["fn_sn"] == receipt["fn"]["sn"]
    assert fetched_receipt["fp"] == receipt["fp"]
    assert fetched_receipt["is_confirmed"]
    assert fetched_receipt["receipt_dt"] == receipt["dt"].isoformat()
