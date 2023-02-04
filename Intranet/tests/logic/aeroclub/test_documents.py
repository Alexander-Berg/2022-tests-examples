import pytest

from intranet.trip.src.logic.aeroclub.documents import (
    mask_passport_data,
    mask_traveller_document,
)


@pytest.mark.parametrize('documents, mask_documents', (
    ([{'series': '1111', 'number': '222222'}], [{'series': '****', 'number': '**2222'}]),
    ([{'series': None, 'number': '222222'}], [{'series': '', 'number': '**2222'}]),
))
def test_mask_passport_data(documents, mask_documents):
    mask_passport_data(documents)
    assert documents == mask_documents


@pytest.mark.parametrize('documents, mask_documents', (
    ([{'document_number': '1111 222222'}], [{'document_number': '**** **2222'}]),
    ([{'document_number': '1111222222'}], [{'document_number': '******2222'}]),
))
def test_mask_traveller_document(documents, mask_documents):
    mask_traveller_document(documents)
    assert documents == mask_documents
