from django.contrib.admin import AdminSite
from django.test import SimpleTestCase as TestCase

from billing_app.admin import BillingLogAdmin, BillingLog, BillingInfoAdmin, changes_amount
from billing_app.models import BillingInfo

FIELD_NAME = "changes_amount"


class TestBillingLogAdmin(TestCase):
    def test__changes_formatting(self):
        _, dec = changes_amount(BillingLog(changes_amount=59.99999)).split(".")
        self.assertTrue(len(dec) == 2)
        _, dec = changes_amount(BillingLog(changes_amount=59)).split(".")
        self.assertTrue(len(dec) == 2)

    def test_right_name_in_list_display(self):
        self.assertEqual(BillingLog._meta.get_field(FIELD_NAME).verbose_name, changes_amount.short_description)

    def test_sortable(self):
        self.assertEqual(FIELD_NAME, changes_amount.admin_order_field)

    def test__changes_amount_applied_on_billinglog(self):
        ma = BillingLogAdmin(BillingLog, AdminSite())
        self.assertIn(changes_amount, ma.list_display)

    def test__changes_amount_applied_on_billinginfo(self):
        ma = BillingInfoAdmin(BillingInfo, AdminSite())
        self.assertIn(changes_amount, ma.list_display)
