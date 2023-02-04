# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import btestlib.utils as utils
from balance import balance_web as web


def temp_ui():
    invoice_id = 53205741

    my_driver = web.Driver()
    with my_driver as driver:
        web.Invoice.open(driver, invoice_id)
        from selenium.common.exceptions import TimeoutException
        return_button = driver.find_element(*web.Invoice.RETURN_ON_CREDIT)
        return_button.click()
        try:
            alert = utils.Web.switch_to_alert()
            alert.accept()
            print "ok"
        except TimeoutException:
            print "no alert"


if __name__ == "__main__":
    temp_ui()
