import unittest
from datetime import date, timedelta
from os import path
from unittest.mock import MagicMock, patch

from dwh.grocery.billing import (
    BILLING_RESOURCES_IN_DWH,
    BILLING_YQL_LIB,
    NewBillingBaseComparisons,
    NewBillingBaseComparisonsWithAccruals,
    NewBillingBaseComparisonsWithActs,
    NewBillingBaseComparisonsWithTrustPayments,
)


class TestNewBillingBaseComparisons(unittest.TestCase):
    def _new_task(self, cls=NewBillingBaseComparisons, **kwargs):
        kwargs.setdefault("yql_name_template", "test_{namespace}_template")
        kwargs.setdefault("query_file", "events_comparison.sql")
        kwargs.setdefault("namespace", "non_existing_service_namespace")
        return cls(**kwargs)

    def test_default_params(self):
        task = self._new_task()
        default_date = "{0:%Y-%m-%d}".format(date.today() - timedelta(days=1))
        assert task.output_folder == "//tmp"
        assert task.from_date == default_date
        assert task.to_date == default_date

    def test_prepare_run(self):
        # prepare with non-existing namespace
        task = self._new_task(
            output_folder="//tmp/dwh/test",
            from_date="2022-05-01",
            to_date="2022-05-31",
        )
        task.prepare_run()
        # check basic parameters
        assert task.yql_name == f"test_{task.namespace!s}_template"
        assert task.yql_path == path.join(
            BILLING_RESOURCES_IN_DWH, f"{task.namespace}", f"{task.query_file}"
        )
        assert len(task.parameters) == 3
        assert task.parameters["$yt_output_folder"].value == f"{task.output_folder}"
        assert task.parameters["$from"].value == f"{task.from_date}"
        assert task.parameters["$to"].value == f"{task.to_date}"
        # attachments should not include the implementation.sql
        basic_attachments = (
            {
                "content": path.join(
                    BILLING_YQL_LIB, "compare_input_with_accounts.sql"
                ),
                "name": "./lib/compare_input_with_accounts.sql",
            },
        )
        assert task.attachments == basic_attachments
        # no re-prepare while mocking isfile return value
        with patch("dwh.grocery.billing.path.isfile", MagicMock(return_value=True)):
            task = self._new_task()
            task.prepare_run()
            assert task.attachments == (
                basic_attachments
                + (
                    {
                        "content": path.join(
                            BILLING_RESOURCES_IN_DWH,
                            f"{task.namespace}",
                            "implementation.sql",
                        ),
                        "name": "./implementation.sql",
                    },
                )
            )

    def test_accruals_attachments(self):
        task = self._new_task(
            NewBillingBaseComparisonsWithAccruals,
            query_file="accruals_comparison.sql",
        )
        task.prepare_run()
        assert task.attachments[-1] == {
            "content": path.join(BILLING_YQL_LIB, "accruals_analysis.sql"),
            "name": "./lib/accruals_analysis.sql",
        }

    def test_trust_payments_attachments(self):
        task = self._new_task(
            NewBillingBaseComparisonsWithTrustPayments,
            query_file="trust_payments_comparison.sql",
        )
        task.prepare_run()
        assert task.attachments[-1] == {
            "content": path.join(BILLING_YQL_LIB, "trust_payments_input.sql"),
            "name": "./lib/trust_payments_input.sql",
        }

    def test_acts_attachments(self):
        task = self._new_task(
            NewBillingBaseComparisonsWithActs,
            query_file="balance_acts_comparison.sql",
        )
        task.prepare_run()
        assert task.attachments[-1] == {
            "content": path.join(BILLING_YQL_LIB, "compare_balance.sql"),
            "name": "./lib/compare_balance.sql",
        }

    def test_acts_parameters(self):
        task = self._new_task(
            NewBillingBaseComparisonsWithActs,
            query_file="balance_acts_comparison.sql",
            namespace="some_namespace",
        )
        task.prepare_run()

        assert len(task.parameters) == 4
        assert task.parameters["$yt_output_folder"].value == f"{task.output_folder}"
        assert task.parameters["$from"].value == f"{task.from_date}"
        assert task.parameters["$to"].value == f"{task.to_date}"
        assert task.parameters["$namespace"].value == f"{task.namespace}"
