# coding: utf-8
__author__ = 'fellow'


class General(object):
    Success = 'Success'
    Failed = 'Failed'
    Payment = 'Payment'
    Refund = 'Refund'
    Discount = 'Discount'
    Subscription = 'Subscription'
    PhantomSubscription = 'Phantom Subscription'
    BindingCard = 'Binding Card'
    Autopayment = 'Autopayment'
    Compensation = 'Compensation'
    BonusPayment = 'Bonus Payment'
    Promocodes = 'Promocodes'
    NotUniquePromocodes = 'NotUniquePromocodes'
    PaymentLink = 'Payment Link'
    BaseCycle = 'BaseCycle'
    PaymentStatus = 'Payment Status'
    RBS = 'RBS'
    Rules = 'General Rules'
    TrustAsProcessing = 'Trust as Processing'


class CardsOperations(object):
    CardsBinding = 'Cards Binding'
    CardsLabels = 'Cards Labels'
    CardsMasking = 'Cards masking'
    CardsUpdating = 'Cards Updating'
    LinkedUsers = 'Linked Users'


class ServiceProduct(object):
    CreationRules = 'Creation Rules'
    GeneralRules = 'General Rules'
    ExportBsBo = 'Export from BS to BO'
    IntroductorySubsRules = 'Introductory Subscriptions Rules'


class Partner(object):
    Load = 'Load'
    Create = 'Create'


class Methods(object):
    Call = 'Call'
    SpecialRules = 'Special Rules'


class Terminal(object):
    Autocreation = 'Autocreation'


class Export(object):
    AfterPayment = 'After Payment'
    AfterPostauth = 'After Postauth'
    AfterRefund = 'After Refund'
    Terminal = 'Terminal'
    Subscription = 'subscription'


class Clearing(object):
    Clear = 'Clear'
    Reversal = 'Reversal'


class Rules(object):
    Qty = 'Order Qty Rules'


class AFS(object):
    General = 'General'
    SendingErrors = 'Sending Errors'
    Whitelist = 'Whitelist'


class Notifications(object):
    BindingNotifies = 'Binding Notifications'


class Subscriptions(object):
    SpecialRules = 'Special Rules'
    Phantom = 'Phantom Subscriptions'
    AggregatedCharging = 'Aggregated Charging'
