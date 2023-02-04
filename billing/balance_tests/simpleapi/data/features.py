# coding: utf-8
__author__ = 'fellow'


class PCIDSS(object):
    KeyKeeper = 'KeyKeeper'
    ConfPatch = 'ConfPatch'
    KeyApi = 'KeyApi'
    Scheduler = 'Scheduler'


class Service(object):
    Store = 'Store'
    Disk = 'Disk'
    Tickets = 'Tickets'
    EventsTickets = 'Events Tickets'
    Realty = 'Realty'
    Direct = 'Direct'
    Marketplace = 'Marketplace'
    Taxi = 'Taxi'
    TaxiDonate = 'Taxi Donate'
    TaxiCorp = 'Taxi Corporate'
    Music = 'Music'
    Dostavka = 'Dostavka'
    Passport = 'Passport'
    TicketToRide = 'Ticket to ride'
    Medicine = 'Telemedicine'
    Buses = 'Buses'
    Translator = 'Translator'
    KinopoiskPlus = 'Kinopoisk.Plus'
    Cloud = 'Cloud'
    Uber = 'Uber'
    Carsharing = 'Carsharing'
    Quasar = 'Quasar'
    Afisha = 'Afisha Movie PASS'
    Zapravki = 'Zapravki'
    Messenger = 'Messenger'

class Methods(object):
    CheckPayment = 'CheckPayment'
    SupplyPaymentData = 'SupplyPaymentData'
    CheckCard = 'CheckCard'
    GetServiceProductPublicKey = 'GetServiceProductPublicKey'
    SignServiceProductMessage = 'SignServiceProductMessage'
    ListPaymentMethods = 'ListPaymentMethods'
    BindApplePayToken = 'BindApplePayToken'
    BindGooglePayToken = 'BindGooglePayToken'
    CreatePromoseries = 'CreatePromoseries'
    CreatePromocode = 'CreatePromocode'
    FindClient = 'FindClient'
    CreateOrderOrSubscription = 'CreateOrderOrSubscription'
    CreateBasket = 'CreateBasket'


class General(object):
    CardsOperations = 'Cards Operations'
    Subscriptions = 'Subscriptions'
    Promocodes = 'Promocodes'
    ServiceProduct = 'Service Product'
    Partner = 'Partner'
    Terminal = 'Terminal'
    Export = 'Export'
    ClearingQueue = 'Clearing Queue'
    GroupRefund = 'GroupRefund'
    FailurePayments = 'Failure Payments'
    PaymentsAPI = 'Payments API'
    Registers = 'Registers'
    With3DS = '3DS'
    Paystep = 'Paystep'
    Fiscal = 'Fiscal'
    AFS = 'Anti Fraud System'
    NewBindingApi = 'New Binding Api'


class Rules(object):
    Payment = 'Payment Rules'
