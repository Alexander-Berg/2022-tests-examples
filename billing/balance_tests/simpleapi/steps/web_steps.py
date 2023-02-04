from simpleapi.pages import binding_pages


class Direct(object):
    @staticmethod
    def bind_card(card, purchase_token, from_yandex=False):
        if from_yandex:
            binding_pages.YandexPage()

        direct_binding_page = binding_pages.DirectBindingPage(purchase_token=purchase_token)
        direct_binding_page.card_form.fill(card)
        direct_binding_page.press_link_button()


class Passport(object):
    @staticmethod
    def bind_card(card, purchase_token, from_yandex=False):
        if from_yandex:
            binding_pages.YandexPage()

        passport_binding_page = binding_pages.PassportBindingPage(purchase_token=purchase_token)
        passport_binding_page.card_form.fill(card)
        passport_binding_page.press_link_button()


class Medicine(object):
    @staticmethod
    def bind_card(card, binding_url):
        binding_pages.YandexPage()
        medicine_binding_page = binding_pages.MedicineBindingPage(binding_url=binding_url)
        medicine_binding_page.card_form.fill(card)
        medicine_binding_page.press_link_button()


class Cloud(object):
    @staticmethod
    def bind_card(card, binding_url):
        binding_pages.YandexPage()
        medicine_binding_page = binding_pages.CloudBindingPage(binding_url=binding_url)
        medicine_binding_page.card_form.fill(
            card,
            need_cardholder=False,
            input_field_xpath="//span[@class='card_number']//input",
            extra_input_field_xpath="//input[@id='card_number-input']"
        )
        medicine_binding_page.press_link_button()
