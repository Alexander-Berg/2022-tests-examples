import json

import hamcrest
import mock


def check_export_obj(export_obj, state, output, error, input, next_export=None, rate=0):
    hamcrest.assert_that(
        export_obj,
        hamcrest.has_properties(
            state=state,
            rate=rate,
            output=output,
            input=input,
            next_export=next_export,
            error=error
        )
    )


def mock_post(answer, status_code=200):
    return_value = mock.MagicMock(
        status_code=status_code,
        text=json.dumps(answer),
        json=lambda: answer
    )

    return mock.patch('requests.post', return_value=return_value)


def patch_oebs():
    patch_contract_dao = mock.patch('balance.processors.oebs.dao.contract.ContractDao')
    patch_customer_dao = mock.patch('balance.processors.oebs.dao.customer.CustomerDao')
    patch_transaction_dao = mock.patch('balance.processors.oebs.dao.transaction.TransactionDao')
    patch_out_cursor = mock.MagicMock()
    patch_out_cursor.execute.return_value = 1
    return patch_contract_dao, patch_customer_dao, patch_transaction_dao, patch_out_cursor
