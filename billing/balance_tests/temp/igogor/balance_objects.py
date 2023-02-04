# coding: utf-8

from decimal import Decimal

import attr

from btestlib.constants import *
from btestlib.data.simpleapi_defaults import ThirdPartyData


@attr.s
class Client(object):
    id = attr.ib()
    is_agency = attr.ib(default=False)
    agency = attr.ib(default=None)

    region = attr.ib(default=None)
    currency = attr.ib(default=None)
    service = attr.ib(default=None)
    migrate_to_currency = attr.ib(default=None)

    # то что дефолты прописаны прямо здесь усложняет создание пустого объекта, но пока необходимости не было
    name = attr.ib(default=attr.Factory(lambda: 'balance_test {}'.format(datetime.now())))
    email = attr.ib(default='client@in-fo.ru')
    phone = attr.ib(default='+7 (911) 111-22-33')
    fax = attr.ib(default='+7 (911) 444-55-66')
    url = attr.ib(default='http://client.info/')
    city = attr.ib(default='Some City')

    @classmethod
    def for_creation(cls, context, is_agency=False, agency=None):
        # todo-igogor здесь имя будет повторяющееся
        return attr.assoc(context.client_template, is_agency=is_agency, agency=agency)


class ClientTemplates(utils.ConstantsContainer):
    constant_type = Client

    NON_CURRENCY = Client(id=None)
    DIRECT_CURRENCY_RUB = Client(id=None, region=Regions.RU, currency=Currencies.RUB, service=Services.DIRECT,
                                 migrate_to_currency=datetime(2000, 1, 1))
    DIRECT_CURRENCY_USD = attr.assoc(DIRECT_CURRENCY_RUB, region=Regions.US, currency=Currencies.USD)


@attr.s(frozen=True)
class QtyMoney(object):
    qty = attr.ib()
    money = attr.ib()


@attr.s
class Order(object):
    id = attr.ib()
    client = attr.ib()
    service = attr.ib()
    service_order_id = attr.ib()
    product = attr.ib()
    currency = attr.ib(default=None)
    manager = attr.ib(default=None)
    agency = attr.ib(default=None)
    consumes = attr.ib(default=attr.Factory(list))
    completions = attr.ib(default=attr.Factory(list))
    group_order = attr.ib(default=None)
    group_members = attr.ib(default=attr.Factory(list))
    is_converted = attr.ib(default=False)

    @classmethod
    def for_creation(cls, context, client, agency=None, service_order_id=None):
        return cls(id=None,
                   client=client,
                   service=context.service,
                   service_order_id=service_order_id,
                   product=context.product,
                   currency=context.currency,
                   manager=context.manager,
                   agency=agency)

    @property
    def consume_qty(self):
        return sum([consume.qty for consume in self.consumes])

    @property
    def consume_money(self):
        return sum([consume.money for consume in self.consumes])

    @property
    def consume_money_rounded(self):
        # #balance_logic округление
        return sum([round(consume.money, 2) for consume in self.consumes])

    @property
    def completion_qty(self):
        return sum([completion.qty for completion in self.completions])

    @property
    def completion_money(self):
        return sum([completion.money for completion in self.completions])

    @property
    def group(self):
        return [self.group_order] + self.group_members

    @property
    def group_consume_qty(self):
        return sum([member.consume_qty for member in self.group])

    @property
    def group_consume_money(self):
        return sum([member.consume_money for member in self.group])

    @property
    def is_multicurrency(self):
        return self.product.type == ProductTypes.MONEY

    @property
    def is_main_order(self):
        return self.group_order is not None and self.group_order.id == self.id

    @property
    def notifications_count(self):
        # отправляется нотификация на каждый консьюм, на главный заказ при объединении, на начало и конец конвертации
        return len(self.consumes) + int(self.is_main_order) + (2 if self.is_converted else 0)

    def on_consume(self, qty, money):
        self.consumes.append(QtyMoney(qty=qty, money=money))

    def on_completion(self, qty, money):
        self.completions.append(QtyMoney(qty=qty, money=money))

    def on_transfer(self, to_order, transfer_consume, to_order_consume=None):
        transfer_consume = QtyMoney(qty=-abs(transfer_consume.qty), money=-abs(transfer_consume.money))
        if not to_order_consume:
            to_order_consume = QtyMoney(qty=abs(transfer_consume.qty), money=abs(transfer_consume.money))
        # todo-igogor не нравится мне здесь использование astuple не нужно оно
        self.on_consume(*attr.astuple(transfer_consume))
        to_order.on_consume(*attr.astuple(to_order_consume))

    def on_group_transfer(self):
        for member in self.group_members:
            if member.consume_qty:
                member.on_transfer(to_order=self,
                                   transfer_consume=QtyMoney(qty=member.consume_qty - member.completion_qty,
                                                             money=member.consume_money - member.completion_money))

    def on_conversion(self):
        self.is_converted = True

    def on_grouping(self, parent, members):
        self.group_order = parent
        self.group_members = members

    def __str__(self):
        return 'Order(id={}, soid={}-{})'.format(self.id, self.service.id, self.service_order_id)


@attr.s()
class Context(object):
    # Минимальный набор атрибутов контекста, которые необходимо инициализировать в каждом контексте
    # Расширять контексты можно через метод new,
    # через него в контексте можно переопределять значения существующих атрибутов
    # и задавать любой другой атрибут с произвольным именем
    name = attr.ib()
    service = attr.ib()
    client_template = attr.ib()
    person_type = attr.ib()
    paysys = attr.ib()
    product = attr.ib()
    price = attr.ib()
    manager = attr.ib()

    def money(self, qty):
        return qty * self.price

    def qty(self, money):
        # #balance_logic округление
        return utils.dround(money / self.price, 6)

    @property
    def currency(self):
        return self.currency_ if 'currency_' in attr.asdict(self).keys() else self.paysys.currency

    def new(self, **kwargs):
        if 'currency' in kwargs:
            kwargs['currency_'] = kwargs['currency']
            del kwargs['currency']
        attrs_from_self_instance = attr.asdict(self, recurse=False)
        attrs_all = utils.merge_dicts([attrs_from_self_instance, kwargs])
        attrs_names_from_context_class = [f.name for f in attr.fields(Context)]
        attrs_names_to_extend_context = list(set(attrs_all.keys()) - set(attrs_names_from_context_class))
        ContextNew = attr.make_class('ContextNew', attrs_names_to_extend_context, bases=(Context,))
        return ContextNew(**attrs_all)

    def __str__(self):
        return self.name


# todo-igogor это надо бы перенести в отдельный модуль. balance/templates
class Contexts(object):
    DIRECT_FISH_RUB_CONTEXT = Context(name='DIRECT_FISH_RUB',
                                      service=Services.DIRECT,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                      product=Products.DIRECT_FISH,
                                      price=Decimal('30.0'),
                                      manager=Managers.SOME_MANAGER)

    REALTY_RUB_CONTEXT = Context(name='REALTY_RUB',
                                      service=Services.REALTY,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                      product=Products.REALTY,
                                      price=Decimal('30.0'),
                                      manager=Managers.SOME_MANAGER)

    NAVI_RUB_CONTEXT = Context(name='NAVI_RUB',
                                 service=Services.NAVI,
                                 client_template=ClientTemplates.NON_CURRENCY,
                                 person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                 product=Products.NAVI,
                                 price=Decimal('30.0'),
                                 manager=Managers.SOME_MANAGER)

    OFD_RUB_CONTEXT = Context(name='OFD_RUB',
                               service=Services.OFD,
                               client_template=ClientTemplates.NON_CURRENCY,
                               person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                               product=Products.OFD_YEAR,
                               price=Decimal('30.0'),
                               manager=Managers.SOME_MANAGER)

    GEO_RUB_CONTEXT = Context(name='GEO_RUB',
                              service=Services.GEO,
                              client_template=ClientTemplates.NON_CURRENCY,
                              person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                              product=Products.GEO,
                              price=Decimal('30.0'),
                              manager=Managers.SOME_MANAGER)

    DIRECT_FISH_YT_RUB_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_YT_RUB',
                                                             person_type=PersonTypes.YT,
                                                             paysys=Paysyses.BANK_YT_RUB,
                                                             price=Decimal('30.0') / Decimal('1.20'))

    # todo fix price
    DIRECT_FISH_UAH_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_UAH',
                                                          person_type=PersonTypes.UA,
                                                          paysys=Paysyses.BANK_UA_UR_UAH)

    # todo fix price
    DIRECT_FISH_KZ_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_KZ',
                                                         person_type=PersonTypes.KZU,
                                                         paysys=Paysyses.BANK_KZ_UR_TG)

    # todo fix price
    DIRECT_BYN_BYU_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_BYN_BYU_CONTEXT',
                                                         person_type=PersonTypes.BYU,
                                                         paysys=Paysyses.BANK_BY_UR_BYN)

    DIRECT_FISH_USD_CONTEXT = Context(name='DIRECT_FISH_USD',
                                      service=Services.DIRECT,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.USU, paysys=Paysyses.BANK_US_UR_USD,
                                      product=Products.DIRECT_FISH,
                                      price=Decimal('0.41'),
                                      manager=Managers.SOME_MANAGER)

    # todo fix price
    DIRECT_FISH_SW_EUR_CONTEXT = Context(name='DIRECT_FISH_SW_EUR',
                                         service=Services.DIRECT,
                                         client_template=ClientTemplates.NON_CURRENCY,
                                         person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_EUR,
                                         product=Products.DIRECT_FISH,
                                         price=Decimal('0.41'),
                                         manager=Managers.SOME_MANAGER)

    DIRECT_FISH_SW_CHF_YT_CONTEXT = Context(name='DIRECT_FISH_SW_CHF_YT',
                                         service=Services.DIRECT,
                                         client_template=ClientTemplates.NON_CURRENCY,
                                         person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_CHF,
                                         product=Products.DIRECT_FISH,
                                         price=Decimal('0.41'),
                                         manager=Managers.SOME_MANAGER)

    # todo fix price
    DIRECT_FISH_TRY_CONTEXT = Context(name='DIRECT_FISH_TRY',
                                      service=Services.DIRECT,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.TRU, paysys=Paysyses.BANK_TR_UR_TRY,
                                      product=Products.DIRECT_FISH,
                                      price=Decimal('0.41'),
                                      manager=Managers.SOME_MANAGER)

    DIRECT_MONEY_RUB_CONTEXT = Context(name='DIRECT_MONEY_RUB',
                                       service=Services.DIRECT,
                                       client_template=ClientTemplates.DIRECT_CURRENCY_RUB,
                                       person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                       product=Products.DIRECT_RUB,
                                       price=Decimal('1'),
                                       manager=Managers.SOME_MANAGER)

    DIRECT_MONEY_USD_CONTEXT = Context(name='DIRECT_MONEY_USD',
                                       service=Services.DIRECT,
                                       client_template=ClientTemplates.DIRECT_CURRENCY_USD,
                                       person_type=PersonTypes.USU, paysys=Paysyses.BANK_US_UR_USD,
                                       product=Products.DIRECT_USD,
                                       price=Decimal('1'),
                                       manager=Managers.SOME_MANAGER)

    MARKET_RUB_CONTEXT = Context(name='MARKET_RUB',
                                 service=Services.MARKET,
                                 client_template=ClientTemplates.NON_CURRENCY,
                                 person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                 product=Products.MARKET,
                                 price=Decimal('30.0'),
                                 manager=Managers.SOME_MANAGER)

    MARKET_BLUE_CONTEXT = Context(name='MARKET_BLUE',
                                  service=[Services.BLUE_MARKET_PAYMENTS, Services.BLUE_MARKET],
                                  client_template=ClientTemplates.NON_CURRENCY,
                                  person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                  product=Products.MARKET_BLUE_PAYMENTS,
                                  price=Decimal('100'),
                                  manager=Managers.SOME_MANAGER)

    DIRECT_TUNING_FISH_RUB_CONTEXT = Context(name='DIRECT_TUNING_FISH_RUB',
                                             service=Services.DIRECT_TUNING,
                                             client_template=ClientTemplates.NON_CURRENCY,
                                             person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                             product=Products.DIRECT_TUNING_1,
                                             price=Decimal('0.41'),
                                             manager=Managers.SOME_MANAGER)

    BAYAN_FISH_RUB = Context(name='BAYAN_FISH_RUB',
                    service=Services.BAYAN,
                    client_template=ClientTemplates.NON_CURRENCY,
                    person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                    product=Products.VENDOR,
                    price=Decimal('30.0'),
                    manager=Managers.SOME_MANAGER)

    MEDIA_70_SHOWS_RUB = Context(name='MEDIA_70_SHOWS_RUB',
                             service=Services.MEDIA_70,
                             client_template=ClientTemplates.NON_CURRENCY,
                             person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                             product=Products.MEDIA,
                             price=Decimal('30.0'),
                             manager=Managers.SOME_MANAGER)

    MEDIA_70_USD_SW = MEDIA_70_SHOWS_RUB.new(
                            name='MEDIA_70_UZB_SW',
                            person_type=PersonTypes.SW_YT,
                            paysys=Paysyses.BANK_SW_YT_USD,
                            product=Products.MEDIA_UZ,
                            currency=Currencies.USD,
                            firm=Firms.EUROPE_AG_7,
                            contract_template=ContractCommissionType.SW_OPT_AGENCY,
                            )

    CATALOG1_CONTEXT = Context(name='CATALOG1_CONTEXT',
                               service=Services.CATALOG1,
                               client_template=ClientTemplates.NON_CURRENCY,
                               person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                               product=Products.CATALOG1,
                               price=Decimal('30.0'),
                               manager=Managers.SOME_MANAGER)

    # todo fix price
    ADFOX_CONTEXT = Context(name='ADFOX_CONTEXT',
                            service=Services.ADFOX,
                            client_template=ClientTemplates.NON_CURRENCY,
                            person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                            product=Products.ADFOX,
                            price=Decimal('30.0'),
                            manager=Managers.SOME_MANAGER)

    # todo fix price
    SPEECHKIT_CONTEXT = Context(name='SPEECHKIT_CONTEXT',
                                service=Services.SHOP,
                                client_template=ClientTemplates.NON_CURRENCY,
                                person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                product=Products.SPEECHKIT,
                                price=Decimal('30.0'),
                                manager=Managers.SOME_MANAGER)
    SHOP_UAE_CONTEXT = Context(
        name="SHOP_UAE",
        service=Services.SHOP,
        client_template=None,
        person_type=PersonTypes.UAE_UR,
        paysys=Paysyses.BANK_UAE_UR_AED,
        # person_type=PersonTypes.UAE_YTUR,
        # paysys=Paysyses.BANK_UAE_UR_YT_AED,
        product=Products.PRAKTICUM_INFORMATION_PROGRAMS,
        manager=Managers.SOME_MANAGER,
        price=None,
    ).new(
        region=Regions.US,
        currency=Currencies.AED,
        firm=Firms.DIRECT_CURSUS_1101
    )

    SHOP_HK_CONTEXT = Context(
        name="SHOP_HK",
        service=Services.SHOP,
        client_template=None,
        person_type=PersonTypes.HK_YT,
        paysys=Paysyses.BANK_HK_YT_SPB_SOFTWARE_EUR_GENERATED,
        product=Products.HK_TEST_PRODUCT,
        manager=Managers.SOME_MANAGER,
        price=None,
    ).new(
        region=Regions.US,
        currency=Currencies.EUR,
        firm=Firms.SPB_SOFTWARE_1483
    )

    SHOP_HK_PH_CONTEXT = SHOP_HK_CONTEXT.new(
        person_type=PersonTypes.HK_YTPH,
        paysys=Paysyses.BANK_HK_YTPH_SPB_SOFTWARE_EUR
    )

    TAXI_FISH_RUB_CONTEXT = Context(name='TAXI_FISH_RUB',
                                    service=Services.TAXI,
                                    client_template=ClientTemplates.NON_CURRENCY,
                                    person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                    product=Products.DIRECT_FISH,
                                    price=Decimal('30.0'),
                                    manager=Managers.SOME_MANAGER)

    TOLOKA_FISH_USD_CONTEXT = Context(name='TOLOKA_FISH_USD',
                                      service=Services.TOLOKA,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_USD,
                                      product=Products.TOLOKA,
                                      price=Decimal('30.0'),
                                      manager=Managers.SOME_MANAGER)

    VENDORS_FISH_RUB_CONTEXT = Context(name='VENDORS_FISH_RUB',
                                       service=Services.VENDORS,
                                       client_template=ClientTemplates.NON_CURRENCY,
                                       person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_USD,
                                       product=Products.VENDOR,
                                       price=Decimal('30.0'),
                                       manager=Managers.SOME_MANAGER)

    VENDORS_FISH_UR_RUB_CONTEXT = Context(name='VENDORS_FISH_RUB',
                                          service=Services.VENDORS,
                                          client_template=ClientTemplates.NON_CURRENCY,
                                          person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                          product=Products.VENDOR,
                                          price=Decimal('30.0'),
                                          manager=Managers.SOME_MANAGER)

    APIKEYS_CONTEXT = Context(name='APIKEYS_RUB',
                              service=Services.APIKEYS,
                              client_template=ClientTemplates.NON_CURRENCY,
                              person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                              product=Products.APIKEYS_TEST,
                              price=Decimal('30.0'),
                              manager=Managers.SOME_MANAGER)

    # TAXI
    TAXI_RU_CONTEXT = Context(
        name='TAXI_RU_CONTEXT',
        service=Services.TAXI,
        client_template=None,
        person_type=PersonTypes.UR,
        paysys=Paysyses.BANK_UR_RUB_TAXI,
        product=None,
        price=None,
        manager=None
    ).new(
        firm=Firms.TAXI_13,
        currency=Currencies.RUB,
        payment_currency=Currencies.RUB,
        region=Regions.RU,
        nds=NdsNew.DEFAULT,
        third_party_data=ThirdPartyData.TAXI,
        services=[Services.TAXI, Services.UBER, Services.UBER_ROAMING,
                  Services.TAXI_111, Services.TAXI_128, Services.TAXI_SVO],
        commission_pct=Decimal('0'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.CBR,
        precision=2
    )

    TAXI_BV_ARM_USD_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_BV_ARM_CONTEXT',
        person_type=PersonTypes.EU_YT,
        firm=Firms.TAXI_BV_22,
        currency=Currencies.USD,
        payment_currency=Currencies.AMD,
        region=Regions.ARM,
        nds=NdsNew.NOT_RESIDENT,
        paysys=Paysyses.BANK_UR_USD_TAXI_BV,
        services=[Services.TAXI, Services.TAXI_111, Services.TAXI_128],
        third_party_data=ThirdPartyData.TAXI_BV,
        commission_pct=Decimal('3.5'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.CBA,
        precision=2
    )

    TAXI_UBER_BY_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_UBER_BY_CONTEXT',
        person_type=PersonTypes.EU_YT,
        currency=Currencies.BYN,
        payment_currency=Currencies.BYN,
        firm=Firms.UBER_115,
        region=Regions.BY,
        paysys=Paysyses.BANK_UR_UBER_BYN,
        third_party_data=ThirdPartyData.TAXI_UBER_BY,
        nds=NdsNew.NOT_RESIDENT,
        services=[Services.TAXI, Services.UBER, Services.UBER_ROAMING, Services.TAXI_111, Services.TAXI_128],
        commission_pct=Decimal('2.44'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.NBRB,
        precision=2
    )

    TAXI_UBER_AZ_CONTEXT = TAXI_UBER_BY_CONTEXT.new(
        name='TAXI_UBER_AZ_CONTEXT',
        currency=Currencies.USD,
        payment_currency=Currencies.AZN,
        region=Regions.AZ,
        paysys=Paysyses.BANK_UR_UBER_USD,
        third_party_data=ThirdPartyData.TAXI_UBER_AZ,
        services=[Services.UBER, Services.UBER_ROAMING, Services.TAXI_111, Services.TAXI_128],
        currency_rate_source=CurrencyRateSource.CBAR
    )


    MUSIC_CONTEXT = TAXI_RU_CONTEXT.new(
        name='MUSIC_CONTEXT',
        person_type=PersonTypes.PH,
        firm=Firms.MEDIASERVICES_121,
        third_party_data=ThirdPartyData.MUSIC,
        service=Services.MUSIC,
        contracts=[
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.RUB,
                'nds': NdsNew.YANDEX_RESIDENT,
                'product': Products.MUSIC,
                'paysys': Paysyses.BANK_PH_RUB_MEDIASERVICES
            }
        ]
    )

    # PARTNERS
    PARTNERS_DEFAULT = Context(
        name='PARTNERS_RU',
        service=None,
        client_template=None,
        person_type=PersonTypes.UR,
        paysys=None,
        product=None,
        price=None,
        manager=Managers.NIGAI
    ).new(
        firm=Firms.YANDEX_1,
        currency=Currencies.RUB,
        nds=NdsNew.DEFAULT,
        default_partner_pct=43,
        default_market_api_pct=50,
        default_payment_type=2,
        default_open_date=0,
        default_search_forms=0,
        default_reward_type=1,
        create_offer_params={'ctype': 'PARTNERS', 'currency': Currencies.RUB.iso_code,
                             'firm_id': Firms.YANDEX_1.id, 'manager_uid': Managers.NIGAI.uid}
    )

    # Practicum
    PRACTICUM_US_YT_UR = Context(
        name='Practicum_US_YT',
        service=Services.PRACTICUM,
        product=Products.PRACTICUM,
        person_type=PersonTypes.US_YT,
        paysys=Paysyses.CC_US_YT_UR_USD,
        client_template=None,
        price=None,
        manager=Managers.SOME_MANAGER,
    ).new(
        region=Regions.SW,
        currency=Currencies.USD,
        firm=Firms.YANDEX_INC_4,
    )

    PRACTICUM_US_YT_PH = PRACTICUM_US_YT_UR.new(
        person_type=PersonTypes.US_YT_PH,
        paysys=Paysyses.CC_US_YT_PH_USD
    )

    REALTY_RUB_CONTEXT = Context(name='REALTY_RUB',
                                 service=Services.REALTY,
                                 client_template=ClientTemplates.NON_CURRENCY,
                                 person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                 product=Products.REALTY,
                                 price=Decimal('30.0'),
                                 manager=Managers.SOME_MANAGER)

    NAVI_RUB_CONTEXT = Context(name='NAVI_RUB',
                               service=Services.NAVI,
                               client_template=ClientTemplates.NON_CURRENCY,
                               person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                               product=Products.NAVI,
                               price=Decimal('30.0'),
                               manager=Managers.SOME_MANAGER)

    OFD_RUB_CONTEXT = Context(name='OFD_RUB',
                              service=Services.OFD,
                              client_template=ClientTemplates.NON_CURRENCY,
                              person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                              product=Products.OFD_YEAR,
                              price=Decimal('30.0'),
                              manager=Managers.SOME_MANAGER)

    DIRECT_FISH_RUB_PH_CONTEXT = Context(name='DIRECT_FISH_RUB_PH',
                                      service=Services.DIRECT,
                                      client_template=ClientTemplates.NON_CURRENCY,
                                      person_type=PersonTypes.PH, paysys=Paysyses.CC_PH_RUB,
                                      product=Products.DIRECT_FISH,
                                      price=Decimal('30.0'),
                                      manager=Managers.SOME_MANAGER)
