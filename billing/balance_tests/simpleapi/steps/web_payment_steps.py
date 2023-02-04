# coding=utf-8
import time
from contextlib import contextmanager
from collections import defaultdict

from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions

import btestlib.reporter as reporter
from btestlib import utils as butils
from btestlib.constants import Services, Processings, PaystepPaymentResultText
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common import utils
from simpleapi.data import defaults
from simpleapi.data import uids_pool as uids
from simpleapi.pages import payment_pages, binding_pages, paystep_payment_pages
from simpleapi.steps import db_steps as db
from simpleapi.steps import passport_steps as passport
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'


def wait_until_cookies_present(driver, cookie):
    with reporter.step('Ждём пока подгрузятся куки {}...'.format(cookie)):
        return butils.wait_until(lambda: driver.get_cookies(),
                                 success_condition=contains_dicts_with_entries((cookie,), same_length=False),
                                 timeout=60)


@contextmanager
def web_payment_context(driver, payment_form=None, payment_url=None,
                        page_class=None, user=None, wait_for_redirect=True,
                        success_payment_message=None,
                        success_payment_element_xpath=u"//div[@class='b-payment-status_text']", region_id=225,
                        wait_like_in_service_iframe=False):
    """
    Контекст-менеджер оплат через страницу траст
    1) Если передан user - логинимся в паспорте
    2) Если платеж анонимный - получаем куку yandexuid
    3) Если передано success_payment_message - после нажатия на кнопку оплаты ждем повления сообщения об успехе
    4) Если передано wait_for_redirect=True - ожидаем редирект со страницы оплаты после проведения платежа
    """

    if user is not None and user != uids.anonymous:
        passport.auth_via_page(user=user, driver=driver, region=region_id)
    else:
        # если оплата анонимная нужно получить куку yandexuid (путем посещения страницы yandex.ru)
        binding_pages.YandexPage(driver=driver)
        wait_until_cookies_present(driver=driver, cookie={'name': 'yandexuid'})

    payment_page = page_class(driver=driver, payment_form=payment_form, payment_url=payment_url)

    yield payment_page

    if success_payment_message:
        payment_page.wait_for_success_payment_message(success_payment_message, xpath=success_payment_element_xpath)

    elif wait_for_redirect:
        payment_page.wait_for_redirect()

    if wait_like_in_service_iframe:
        # Для сервисов, которые держат нашу платежную форму в своем айфрейме. Пока только Музыка.
        # После оплаты должен покрутиться спинер, чтобы она передалась в траст, но он будет крутиться бесконечно.
        # Так что помимо него сообщения об успехе не будет. Очень странный костыль, но пока так. @slppls
        payment_page.wait_until_spinner_show()


def fill_3ds(page, param_3ds, card, break_3ds, user=None, in_subpage=False, in_iframe=False):
    cvn_3ds = card['cvn'] if param_3ds else 111
    if in_subpage:
        page = page.select_3ds_subpage(user)
    page.wait_until_3ds_redirect(in_iframe=in_iframe)

    if break_3ds:
        page.driver.get('http://yandex.ru')
    else:
        page.fill_3ds_page(cvn_3ds, in_iframe=in_iframe)


class Desktop(object):
    class Default(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None,
                        driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url,
                                     payment_pages.TrustInProcessingPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']") \
                    as trust_page:
                trust_page.select_new_card()
                trust_page.card_form.fill(card, need_cardholder=False)
                trust_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:
                    fill_3ds(trust_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'),
                             in_iframe=True)

        @staticmethod
        def pay_by_linked_card(card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TrustInProcessingPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']") \
                    as trust_page:
                trust_page.select_card(card, checked=True)
                trust_page.card_form.fill_cvn()
                trust_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:
                    fill_3ds(trust_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'),
                             in_iframe=True)

    class Tickets(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage, user,
                                     region_id=region_id) as tickets_page:
                Desktop.Tickets.fill_form_pay_by_card(card, kwargs.get('save_card'), tickets_page, user,
                                                      success_3ds_payment=kwargs.get('success_3ds_payment'),
                                                      break_3ds=kwargs.get('break_3ds'))

        @staticmethod
        def fill_form_pay_by_card(card, save_card, tickets_page, user, success_3ds_payment=None, break_3ds=None):
            tickets_page.select_card()
            tickets_page.wait_until_card_form_spinner_done()
            # чекбокс сохранения карты сделали выбранным по-умолчанию
            # снимаем его, если он есть на странице
            tickets_page.select_save_card(checked=False)
            tickets_page.card_form.fill(card)
            tickets_page.fill_mailto(defaults.email)
            if save_card:
                tickets_page.select_save_card()
            tickets_page.press_pay_button()
            # tickets_page.wait_until_card_form_spinner_done()
            if success_3ds_payment is not None:  # success payment - True, failed payment - False
                fill_3ds(tickets_page,
                         success_3ds_payment,
                         card,
                         break_3ds,
                         user=user,
                         in_subpage=True)

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage,
                                     user, region_id=region_id) as tickets_page:
                tickets_page.select_card(card_id=card_id, card=card, checked=True)
                tickets_page.wait_until_card_form_spinner_done()
                # чекбокс сохранения карты сделали выбранным по-умолчанию
                # снимаем его, если он есть на странице
                tickets_page.select_save_card(checked=False)
                tickets_page.card_form.fill_cvn(cvn=card['cvn'])
                tickets_page.fill_mailto(defaults.email)
                if kwargs.get('save_card'):
                    tickets_page.select_save_card()
                tickets_page.press_pay_button()
                # tickets_page.wait_until_card_form_spinner_done()
                if kwargs.get('success_3ds_payment') is not None:  # success_3ds_payment must be True of False
                    fill_3ds(tickets_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'),
                             user=user,
                             in_subpage=True)

        @staticmethod
        # TODO-slppls: make web promocode great again. when tickets fix their web-page.
        def pay_by_only_promocode(user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     wait_like_in_service_iframe=True) as tickets_page:
                # при загрузке страницы происходит вызов метода preview_payment, если промокод полностью покрывает
                # стоимость заказа, буде автоматически вызываться метод start_payment,
                # без какого-либо пользовательского взаимодействия
                pass

        @staticmethod
        def pay_by_yandex_money(user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     wait_like_in_service_iframe=True) as tickets_page:
                yamoney_subpage = tickets_page.select_yamoney_subpage()
                yamoney_subpage.wait_for_toner_absence()
                time.sleep(1)
                yamoney_subpage.send_sms_code()
                time.sleep(2)  # смс приходит не сразу, надо подождать
                sms_code = trust.get_code_from_sms(user.ym_phones[0])
                yamoney_subpage.fill_sms_code(sms_code)
                yamoney_subpage.press_pay_button()

        @staticmethod
        def pay_by_phone(phone_number, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     wait_like_in_service_iframe=True) as tickets_page:
                phone_subpage = tickets_page.select_phone_subpage()
                phone_subpage.fill_phone_number(phone_number)
                phone_subpage.fill_mailto(defaults.email)
                phone_subpage.press_pay_button()

        @staticmethod
        def unbind_linked_card(card_id=None, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False,
                                     wait_like_in_service_iframe=False) as tickets_page:
                tickets_page.press_unbind_card(card=card)
                tickets_page.submit_unbind_card()

    class EventsTicketsNew(Tickets):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsPaymentPage, user,
                                     wait_for_redirect=False, success_payment_element_xpath=u"//div",
                                     success_payment_message=u"Успешный платеж...", region_id=region_id,
                                     wait_like_in_service_iframe=True) as tickets_page:
                super(Desktop.EventsTicketsNew, Desktop.EventsTicketsNew). \
                    fill_form_pay_by_card(card, kwargs.get('save_card'), tickets_page, user,
                                          success_3ds_payment=kwargs.get('success_3ds_payment'),
                                          break_3ds=kwargs.get('break_3ds'))

    class Realty(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.RealtyPaymentPage,
                                     user, region_id=region_id) as realty_page:
                realty_page.card_form.fill(card)
                realty_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(realty_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_yandex_money(user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.RealtyPaymentPage,
                                     user, region_id=region_id) as realty_page:
                yamoney_subpage = realty_page.select_yamoney_subpage()
                time.sleep(1)
                yamoney_subpage.send_sms_code()
                time.sleep(2)  # смс приходит не сразу, надо подождать
                sms_code = trust.get_code_from_sms(user.ym_phones[0])
                yamoney_subpage.fill_sms_code(sms_code)
                yamoney_subpage.submit_sms_code()

        @staticmethod
        def pay_by_phone(phone_number, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.RealtyPaymentPage,
                                     user, region_id=region_id) as realty_page:
                phone_subpage = realty_page.select_phone_subpage()
                phone_subpage.fill_phone_number(phone_number)
                phone_subpage.press_pay_button()

    class Music(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None,
                        payment_url=None, driver=None, region_id=225,
                        save_card=False, **kwargs):
            if kwargs.get('success_3ds_payment'):
                wait_like_in_service_frame = False
                wait_for_redirect = True
            else:
                wait_like_in_service_frame = True
                wait_for_redirect = False

            with web_payment_context(driver,
                                     payment_form,
                                     payment_url,
                                     payment_pages.MusicPaymentPage,
                                     user,
                                     wait_like_in_service_iframe=True,
                                     wait_for_redirect=False,
                                     region_id=region_id) as music_page:
                music_page.select_card()
                music_page.card_form.fill(card, need_cardholder=False)
                music_page.fill_mailto(defaults.email)
                if not save_card:
                    music_page.select_not_save_card()
                music_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(music_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card=None, user=None,
                               payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            if kwargs.get('success_3ds_payment'):
                wait_like_in_service_frame = False
                wait_for_redirect = True
            else:
                wait_like_in_service_frame = True
                wait_for_redirect = False

            with web_payment_context(driver,
                                     payment_form,
                                     payment_url,
                                     payment_pages.MusicPaymentPage,
                                     user,
                                     wait_like_in_service_iframe=True,
                                     wait_for_redirect=False,
                                     region_id=region_id) as music_page:
                music_page.select_card(card, checked=True)
                music_page.card_form.fill_cvn(cvn=card['cvn'])
                music_page.fill_mailto(defaults.email)
                music_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(music_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_yandex_money(user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MusicPaymentPage,
                                     user, region_id=region_id) as music_page:
                yamoney_subpage = music_page.select_yamoney_subpage()
                time.sleep(1)
                yamoney_subpage.send_sms_code()
                time.sleep(2)  # смс приходит не сразу, надо подождать
                sms_code = trust.get_code_from_sms(user.ym_phones[0])
                yamoney_subpage.fill_sms_code(sms_code)
                yamoney_subpage.submit_sms_code()

    class Disk(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225,
                        save_card=False, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.DiskPaymentPage, user,
                                     success_payment_message=u"Платеж проведен успешно",
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']",
                                     wait_for_redirect=False, region_id=region_id,
                                     ) as disk_page:
                disk_page.select_card()
                disk_page.card_form.fill(card)
                if not save_card:
                    disk_page.select_not_save_card()
                disk_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(disk_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.DiskPaymentPage, user,
                                     success_payment_message=u"Платеж проведен успешно",
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']",
                                     wait_for_redirect=False, region_id=region_id,
                                     ) as disk_page:
                disk_page.select_card(card_id, checked=True)
                disk_page.card_form.fill_cvn(cvn=card['cvn'])
                disk_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(disk_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_yandex_money_web_page(payment_form=None, payment_url=None,
                                         driver=None, user=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form=payment_form, payment_url=payment_url,
                                     page_class=payment_pages.YandexMoneyWebPage,
                                     user=user, success_payment_message=u"Платёж проведен успешно",
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']",
                                     wait_for_redirect=False, region_id=region_id
                                     ) as yandex_money_web_page:
                yandex_money_web_page.press_pay_button()
                time.sleep(2)  # смс приходит не сразу, надо подождать
                sms_code = trust.get_code_from_sms(phone=user.ym_phones[0])
                yandex_money_web_page.fill_sms_code(sms_code)
                yandex_money_web_page.wait_for_pay_button_available()
                yandex_money_web_page.press_pay_button()

    class Store(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.StorePaymentPage, user,
                                     wait_for_redirect=False, region_id=region_id) as store_page:
                store_page.select_card('card-default')
                store_page.card_form.fill(card)
                store_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(store_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

    class Marketplace(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MarketPlacePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_message=u'Платёж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']",
                                     wait_for_redirect=False) as marketplace_page:
                marketplace_page.select_new_card()
                marketplace_page.card_form.fill(card, need_cardholder=False)
                # галка save_card в вебе проставлена по умолчанию :( выглядит отвратительно, но что поделать
                if not kwargs.get('save_card'):
                    marketplace_page.select_save_card()
                marketplace_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(marketplace_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MarketPlacePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_message=u'Платёж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner__title']",
                                     wait_for_redirect=False) as marketplace_page:
                marketplace_page.select_card(card, checked=True)
                marketplace_page.card_form.fill_cvn(cvn=card['cvn'])
                marketplace_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(marketplace_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_yandex_money(user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MarketPlacePaymentPage,
                                     user, region_id=region_id) as marketplace_page:
                yamoney_subpage = marketplace_page.select_yamoney_subpage()
                time.sleep(1)
                yamoney_subpage.send_sms_code()
                time.sleep(2)  # смс приходит не сразу, надо подождать
                sms_code = trust.get_code_from_sms(user.ym_phones[0])
                yamoney_subpage.fill_sms_code(sms_code)
                yamoney_subpage.submit_sms_code()

        @staticmethod
        def pay_by_3ds(card=None, payment_url=None, driver=None, **kwargs):
            driver.get(payment_url)
            driver.find_element(By.ID, "cvc").send_keys(card['cvn'])
            driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]", u"Кнопка оплаты").click()

    class BlueMarket(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BlueMarketPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']",
                                     wait_for_redirect=False) as blue_market_page:
                blue_market_page.select_new_card()
                blue_market_page.card_form.fill(card, need_cardholder=False)
                # галка save_card в вебе проставлена по умолчанию :( выглядит отвратительно, но что поделать
                if not kwargs.get('save_card'):
                    blue_market_page.select_save_card()
                blue_market_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(blue_market_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BlueMarketPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']",
                                     wait_for_redirect=False) as blue_market_page:
                blue_market_page.select_card(card, checked=True)
                blue_market_page.card_form.fill_cvn(cvn=card['cvn'])
                blue_market_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(blue_market_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_3ds(card=None, payment_url=None, driver=None, **kwargs):
            driver.get(payment_url)
            driver.find_element(By.ID, "cvc").send_keys(card['cvn'])
            driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]", u"Кнопка оплаты").click()

    class RedMarket(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.RedMarketPaymentPage,
                                     user, region_id=region_id) as red_market_page:
                # red_market_page.select_new_card()
                red_market_page.card_form.fill(card, need_cardholder=False)
                # галка save_card в вебе проставлена по умолчанию :( выглядит отвратительно, но что поделать
                if not kwargs.get('save_card'):
                    red_market_page.select_save_card()
                red_market_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(red_market_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.RedMarketPaymentPage,
                                     user, region_id=region_id) as red_market_page:
                # red_market_page.select_card(card, checked=True)
                red_market_page.card_form.fill_cvn(cvn=card['cvn'])
                red_market_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(red_market_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_3ds(card=None, payment_url=None, driver=None, **kwargs):
            driver.get(payment_url)
            driver.find_element(By.ID, "cvc").send_keys(card['cvn'])
            driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]", u"Кнопка оплаты").click()

    class Shad(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.ShadPaymentPage, user,
                                     wait_for_redirect=False, region_id=region_id) as shad_page:
                shad_page.card_form.fill(card)
                shad_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:
                    fill_3ds(shad_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_phone(phone_number, user=None, payment_form=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_pages.ShadPaymentPage, user,
                                     wait_for_redirect=False, region_id=region_id) as shad_page:
                phone_subpage = shad_page.select_phone_subpage()
                phone_subpage.fill_phone_number(phone_number)
                phone_subpage.fill_mailto(defaults.email)
                phone_subpage.press_pay_button()

    class YDF(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.YDFPaymentPage,
                                     user, region_id=region_id) as YDF_page:
                YDF_page.card_form.fill(card)
                YDF_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(YDF_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

    class Medicine(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MedicinePaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платёж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as medicine_page:
                medicine_page.card_form.fill(card)
                medicine_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(medicine_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MedicinePaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платёж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as medicine_page:
                medicine_page.select_card(card, checked=True)
                medicine_page.card_form.fill_cvn()
                medicine_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(medicine_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

    class TicketToRide(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None,
                        driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url,
                                     payment_pages.TicketsToRidePaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as ticets_to_ride_page:
                ticets_to_ride_page.select_new_card()
                ticets_to_ride_page.card_form.fill(card, need_cardholder=False)
                ticets_to_ride_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:
                    fill_3ds(ticets_to_ride_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'),
                             in_iframe=True)

        @staticmethod
        def pay_by_linked_card(card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsToRidePaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as ticets_to_ride_page:
                ticets_to_ride_page.select_card(card, checked=True)
                ticets_to_ride_page.card_form.fill_cvn()
                ticets_to_ride_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:
                    fill_3ds(ticets_to_ride_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'),
                             in_iframe=True)

    class Buses(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as buses_page:
                buses_page.select_new_card()
                buses_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    buses_page.select_not_save_card()
                buses_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(buses_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as buses_page:
                buses_page.select_card(card_id, checked=True)
                buses_page.card_form.fill_cvn(cvn=card['cvn'])
                buses_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(buses_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def unbind_linked_card(card_id=None, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as buses_page:
                buses_page.press_unbind_card(card_id=card_id)
                buses_page.submit_unbind_card()

    class Cloud(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, region_id=225, save_card=False,
                        **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.CloudPaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner']") \
                    as cloud_page:
                cloud_page.select_card()
                cloud_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    cloud_page.select_not_save_card()
                cloud_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(cloud_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.CloudPaymentPage,
                                     user, region_id=region_id,
                                     wait_for_redirect=False,
                                     success_payment_message=u'Платеж проведен успешно',
                                     success_payment_element_xpath=u"//div[@class='dialog-inner']") \
                    as cloud_page:
                cloud_page.select_card(card, checked=True)
                cloud_page.card_form.fill_cvn()
                cloud_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(cloud_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

    class AfishaMoviePass(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as afisha_page:
                afisha_page.select_new_card()
                afisha_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    afisha_page.select_not_save_card()
                afisha_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(afisha_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as afisha_page:
                afisha_page.select_card(card_id, checked=True)
                afisha_page.card_form.fill_cvn(cvn=card['cvn'])
                afisha_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(afisha_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def unbind_linked_card(card_id=None, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as afisha_page:
                afisha_page.press_unbind_card(card_id=card_id)
                afisha_page.submit_unbind_card()

    class KinopoiskPlus(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as kinopoisk_page:
                kinopoisk_page.select_new_card()
                kinopoisk_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    kinopoisk_page.select_not_save_card()
                kinopoisk_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(kinopoisk_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusPaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as kinopoisk_page:
                kinopoisk_page.select_card(card_id, checked=True)
                kinopoisk_page.card_form.fill_cvn(cvn=card['cvn'])
                kinopoisk_page.press_pay_button()
                if kwargs.get('success_3ds_payment') is not None:  # success payment - True, failed payment - False
                    fill_3ds(kinopoisk_page,
                             kwargs.get('success_3ds_payment'),
                             card,
                             kwargs.get('break_3ds'))

        @staticmethod
        def unbind_linked_card(card_id=None, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusPaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as kinopoisk_page:
                kinopoisk_page.press_unbind_card(card_id=card_id)
                kinopoisk_page.submit_unbind_card()

    paymethods_by_services = defaultdict(lambda: Desktop.Default)
    paymethods_by_services.update({Services.TICKETS: Tickets,
                                   Services.REALTYPAY: Realty,
                                   Services.MUSIC: Music,
                                   Services.DISK: Disk,
                                   Services.STORE: Store,
                                   Services.MARKETPLACE: Marketplace,
                                   Services.NEW_MARKET: Marketplace,
                                   Services.BLUE_MARKET_PAYMENTS: BlueMarket,
                                   Services.BLUE_MARKET_REFUNDS: BlueMarket,
                                   Services.SHAD: Shad,
                                   Services.EVENTS_TICKETS: Tickets,
                                   Services.EVENTS_TICKETS_NEW: Tickets,
                                   Services.YDF: YDF,
                                   Services.MEDICINE_PAY: Medicine,
                                   Services.UFS: TicketToRide,
                                   Services.BUSES: Buses,
                                   Services.CLOUD_154: Cloud,
                                   Services.RED_MARKET_PAYMENTS: RedMarket,
                                   Services.RED_MARKET_BALANCE: RedMarket,
                                   Services.AFISHA_MOVIEPASS: AfishaMoviePass,
                                   Services.MESSENGER: TicketToRide,
                                   Services.SERVICE_FOR_PROCESSINGS_TESTING: Default,
                                   Services.KINOPOISK_PLUS: KinopoiskPlus,
                                   Services.DIRECT: Default,
                                   })


class Mobile(object):
    class TicketsToRide(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url,
                                     payment_pages.TicketsToRideMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']",
                                     success_payment_message=u'Платеж проведен успешно',
                                     wait_for_redirect=False) \
                    as ticets_to_ride_page:
                ticets_to_ride_page.select_new_card()
                ticets_to_ride_page.card_form.fill(card, need_cardholder=False,
                                                   input_field_xpath="//span[@class='card_number_input']/input")
                if not save_card:
                    ticets_to_ride_page.select_not_save_card()
                ticets_to_ride_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.TicketsToRideMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']",
                                     success_payment_message=u'Платеж проведен успешно',
                                     wait_for_redirect=False) \
                    as ticets_to_ride_page:
                ticets_to_ride_page.select_card(card_id, checked=True)
                ticets_to_ride_page.card_form.fill_cvn(cvn=card['cvn'])
                ticets_to_ride_page.press_pay_button()

    class Marketplace(object):
        @staticmethod
        def pay_by_card(card, user=None, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MarketPlaceMobilePaymentPage,
                                     user, region_id=region_id) as marketplace_page:
                marketplace_page.select_card('card-default')
                marketplace_page.card_form.fill(card)
                # галка save_card в вебе проставлена по умолчанию :( выглядит отвратительно, но что поделать
                if not kwargs.get('save_card'):
                    marketplace_page.select_save_card()
                marketplace_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.MarketPlaceMobilePaymentPage,
                                     user, region_id=region_id) as marketplace_page:
                marketplace_page.select_card(card_id, checked=True)
                marketplace_page.card_form.fill_cvn(cvn=card['cvn'])
                marketplace_page.press_pay_button()

        @staticmethod
        def pay_by_3ds(card=None, payment_url=None, driver=None, **kwargs):
            driver.get(payment_url)
            driver.find_element(By.XPATH, ".//input[@class='form-control']").send_keys(card['cvn'])
            driver.find_element(By.XPATH, u".//button[contains(.,'Confirm')]", u"Кнопка оплаты").click()

    class Buses(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as buses_page:
                buses_page.select_new_card()
                buses_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    buses_page.select_not_save_card()
                buses_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as buses_page:
                buses_page.select_card(card_id, checked=True)
                buses_page.card_form.fill_cvn(cvn=card['cvn'])
                buses_page.press_pay_button()

        @staticmethod
        def unbind_linked_card(card_id, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.BusesMobilePaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as buses_page:
                buses_page.press_unbind_card(card_id)
                buses_page.submit_unbind_card()

    class AfishaMoviePass(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as afisha_movie_pass_page:
                afisha_movie_pass_page.select_new_card()
                afisha_movie_pass_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    afisha_movie_pass_page.select_not_save_card()
                afisha_movie_pass_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as afisha_movie_pass_page:
                afisha_movie_pass_page.select_card(card_id, checked=True)
                afisha_movie_pass_page.card_form.fill_cvn(cvn=card['cvn'])
                afisha_movie_pass_page.press_pay_button()

        @staticmethod
        def unbind_linked_card(card_id, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.AfishaMoviePassMobilePaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as afisha_movie_pass_page:
                afisha_movie_pass_page.press_unbind_card(card_id)
                afisha_movie_pass_page.submit_unbind_card()

    class KinopoiskPlus(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, save_card=False,
                        region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as kinopoisk_page:
                kinopoisk_page.select_new_card()
                kinopoisk_page.card_form.fill(card, need_cardholder=False)
                if not save_card:
                    kinopoisk_page.select_not_save_card()
                kinopoisk_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card_id, card=None, user=None, payment_form=None, payment_url=None, driver=None,
                               region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusMobilePaymentPage,
                                     user, region_id=region_id,
                                     success_payment_element_xpath=u"//div[@class='payment-status-container']") \
                    as kinopoisk_page:
                kinopoisk_page.select_card(card_id, checked=True)
                kinopoisk_page.card_form.fill_cvn(cvn=card['cvn'])
                kinopoisk_page.press_pay_button()

        @staticmethod
        def unbind_linked_card(card_id, card=None, user=None, payment_form=None,
                               payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusMobilePaymentPage,
                                     user, region_id=region_id, wait_for_redirect=False) as kinopoisk_page:
                kinopoisk_page.press_unbind_card(card_id)
                kinopoisk_page.submit_unbind_card()

    paymethods_by_services = defaultdict(lambda: Mobile.Buses)
    paymethods_by_services.update({Services.MARKETPLACE: Marketplace,
                                   Services.NEW_MARKET: Marketplace,
                                   Services.UFS: TicketsToRide,
                                   Services.BUSES: Buses,
                                   Services.KINOPOISK_PLUS: KinopoiskPlus,
                                   Services.AFISHA_MOVIEPASS: AfishaMoviePass,
                                   })


class SmartTV(object):
    class KinopoiskPlus(object):
        @staticmethod
        def pay_by_card(card, user, payment_form=None, payment_url=None, driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusPaymentPageTV,
                                     user, region_id=region_id) \
                    as kinopoiskplus_page:
                kinopoiskplus_page.card_form.fill(card)
                kinopoiskplus_page.press_pay_button()

        @staticmethod
        def pay_by_linked_card(card=None, user=None, payment_form=None, payment_url=None,
                               driver=None, region_id=225, **kwargs):
            with web_payment_context(driver, payment_form, payment_url, payment_pages.KinopoiskPlusPaymentPageTV,
                                     user, region_id=region_id) \
                    as kinopoiskplus_page:
                kinopoiskplus_page.select_card(card, checked=True)
                kinopoiskplus_page.card_form.fill_cvn()
                kinopoiskplus_page.press_pay_button()

    paymethods_by_services = defaultdict(lambda: SmartTV.KinopoiskPlus)


class ProfessionalService(object):
    @staticmethod
    def pay_by_card(driver=None, processing=None, *args, **kwargs):

        if processing == Processings.TRUST and kwargs.get('service') == Services.DIRECT:
            db.bo().add_user_to_trust_as_processing(kwargs.get('user'))
            pass
        elif processing == Processings.ALPHA or kwargs.get('service') == Services.MARKET:
            db.bo().delete_user_from_trust_as_processing(kwargs.get('user'))

        card = kwargs.get('card')
        passport.auth_via_page(user=kwargs.get('user'), driver=driver, region=kwargs.get('region_id'))
        invoice_page = paystep_payment_pages.InvoicePage(driver=driver, payment_url=kwargs.get('payment_url'))
        # Нажимаем на кнопку "Оплатить" на странице счёта, чтобы попасть на страницу платёжной системы
        invoice_page.press_pay_button()

        processing_page = paystep_payment_pages.get_payment_page(driver=driver,
                                                                 processing=processing)
        processing_page.wait_for_present()
        processing_page.check_payment_data(kwargs.get('data_for_checks'))
        if processing == Processings.TRUST:
            processing_page.card_form.fill(card, need_cardholder=False)
        else:
            processing_page.card_form.fill(card)
        processing_page.card_form.press_pay_button()

        if card.get('is_3ds', False):
            processing_page.wait_until_3ds_redirect()
            processing_page.fill_3ds_subpage(card)

        if processing == Processings.TRUST:
            processing_page.wait_until_spinner_hide()
            processing_page.press_ok_button()

        # здесь был еще Processings.BILDERLINGS но уже не надо
        if processing in [Processings.SAFERPAY, ]:
            processing_page.post_3ds_actions()

        invoice_page.wait_for_ci_invoice_page()
        invoice_page.check_payment_status(card.get('payment_result', PaystepPaymentResultText.SUCCESS_PAYMENT))

        if processing == Processings.TRUST and kwargs.get('service') == Services.DIRECT:
            db.bo().delete_user_from_trust_as_processing(kwargs.get('user'))

    @staticmethod
    def pay_by_linked_card(driver=None, processing=None, *args, **kwargs):

        if processing == Processings.TRUST and kwargs.get('service') == Services.DIRECT:
            db.bo().add_user_to_trust_as_processing(kwargs.get('user'))
            pass
        elif processing == Processings.ALPHA or kwargs.get('service') == Services.MARKET:
            db.bo().delete_user_from_trust_as_processing(kwargs.get('user'))

        card = kwargs.get('card')
        passport.auth_via_page(user=kwargs.get('user'), driver=driver, region=kwargs.get('region_id'))
        invoice_page = paystep_payment_pages.InvoicePage(driver=driver, payment_url=kwargs.get('payment_url'))
        # Нажимаем на кнопку "Оплатить" на странице счёта, чтобы попасть на страницу платёжной системы
        invoice_page.press_pay_button()

        processing_page = paystep_payment_pages.get_payment_page(driver=driver,
                                                                 processing=processing)
        processing_page.wait_for_present()
        processing_page.check_payment_data(kwargs.get('data_for_checks'))
        processing_page.select_card(card_id=kwargs.get('card_id'))

        if processing == Processings.TRUST:
            processing_page.card_form.fill_cvn(card['cvn'])
        else:
            processing_page.card_form.fill(card)
        processing_page.card_form.press_pay_button()

        if card.get('is_3ds', False):
            processing_page.wait_until_3ds_redirect()
            processing_page.fill_3ds_subpage(card)

        if processing == Processings.TRUST:
            processing_page.wait_until_spinner_hide()
            processing_page.press_ok_button()

        invoice_page.wait_for_ci_invoice_page()
        invoice_page.check_payment_status(card.get('payment_result', PaystepPaymentResultText.SUCCESS_PAYMENT))

        if processing == Processings.TRUST and kwargs.get('service') == Services.DIRECT:
            db.bo().delete_user_from_trust_as_processing(kwargs.get('user'))

    @staticmethod
    def pay_by_paypal(driver=None, *args, **kwargs):
        passport.auth_via_page(user=kwargs.get('user'), driver=driver, region=kwargs.get('region_id'))

        invoice_page = paystep_payment_pages.InvoicePage(driver=driver, payment_url=kwargs.get('payment_url'))
        invoice_page.press_pay_button()

        paypal_page = paystep_payment_pages.PayPalPaymentPage(driver=driver)
        paypal_page.login()

        invoice_page.wait_for_ci_invoice_page()
        invoice_page.check_payment_status(PaystepPaymentResultText.SUCCESS_PAYMENT)

    @staticmethod
    def pay_by_webmoney(driver=None, *args, **kwargs):
        passport.auth_via_page(user=kwargs.get('user'), driver=driver, region=kwargs.get('region_id'))

        invoice_page = paystep_payment_pages.InvoicePage(driver=driver, payment_url=kwargs.get('payment_url'))
        invoice_page.press_pay_button()

        webmoney_page = paystep_payment_pages.WebMoneyPaymentPage(driver=driver)
        webmoney_page.wait_for_webmoney_form_present()
        webmoney_page.check_payment_data_on_processing_page(kwargs.get('data_for_checks'))


paymethods_by_professional_services = {
    Services.DIRECT: ProfessionalService,
    Services.MARKET: ProfessionalService,
    Services.TOLOKA: ProfessionalService
}

# todo fellow оставлю на влякий случай, но вообще не используется
# для Маркета теримна лвыбирается автоматически, для Директа через конфиг
trust_terminal_by_professional_services = {
    Services.DIRECT: 96001010,
    Services.MARKET: 96111011,
    Services.TOLOKA: 96001010
}


def get_paymethods(service, template_tag=defaults.TemplateTag.desktop):
    mapping = {
        defaults.TemplateTag.desktop: Desktop,
        defaults.TemplateTag.mobile: Mobile,
        defaults.TemplateTag.smarttv: SmartTV,
    }
    return mapping.get(template_tag, Desktop).paymethods_by_services[service]


def fill_emulator_3ds_page(url, cvn):
    driver = utils.WebDriverProvider.get_instance().get_driver()
    driver.get(url)
    locator = (By.XPATH, "//form[contains(@action, '/fake_3ds')]")
    utils.wait_for_ui(driver, expected_conditions.visibility_of_element_located, locator,
                      message=u'Не появилась форма ввода 3ds в течение {} секунд', waiting_time=30)
    driver.find_element(By.XPATH, "//input[@id='3ds_code']").send_keys(cvn)
    driver.find_element(By.XPATH, "//input[@value='Submit']").click()
    utils.WebDriverProvider.get_instance().close_driver()
