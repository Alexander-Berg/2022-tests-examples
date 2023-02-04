# coding: utf-8

import decimal
import ssl
import xmlrpclib

from btestlib import utils_tvm
import btestlib.environments as env
import btestlib.utils as utils

# Adds support for decimal and long ints in xmlrpc
import butils.xmlrpc_common


def ssl_context(certfile, keyfile=None, cafile=None, password=None):
    certfile = utils.project_file(certfile)
    if keyfile:
        keyfile = utils.project_file(keyfile)

    context = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
    context.load_cert_chain(certfile, keyfile=keyfile, password=password)
    if cafile:
        cafile = utils.project_file(cafile)
        context.load_verify_locations(cafile=cafile)
    return context


class Medium(utils.XmlRpc.ReportingServerProxy):
    def __init__(self, url, namespace, context=None, transport=None):
        # Context argument temporary removed.
        # apikeys-dev1f host has 2.7.3 python version installed.
        # Context argument is available from 2.7.9
        # super(Medium, self).__init__(url, namespace, context)
        super(Medium, self).__init__(url, namespace, context=context, transport=transport)


@utils.cached
def medium():
    env.balance_env().log_env_name()
    return Medium(url=env.balance_env().medium_url, namespace='Balance', transport=env.balance_env().transport)


def medium_tvm(ticket=None):
    env.balance_env().log_env_name()
    return utils.XmlRpc.ReportingServerProxy(url=env.balance_env().medium_tvm_url,
                                             namespace='Balance',
                                             transport=utils_tvm.TvmTransport(tvm_ticket=ticket))


@utils.cached
def test_balance():
    env.balance_env().log_env_name()
    return utils.XmlRpc.ReportingServerProxy(url=env.balance_env().test_balance_url, namespace='TestBalance',
                                             transport=env.balance_env().transport)


@utils.cached
def simple():
    return utils.XmlRpc.ReportingServerProxy(url=env.simpleapi_env().simple_url, namespace='BalanceSimple')


@utils.cached
def simpleapi():
    return utils.XmlRpc.ReportingServerProxy(url=env.simpleapi_env().simple_url_old, namespace='TODO')


@utils.cached
def coverage():
    return utils.XmlRpc.ReportingServerProxy(url=env.balance_env().coverage_url, namespace='Coverage')


@utils.cached
def oebs_gate():
    return utils.XmlRpc.ReportingServerProxy(url=env.balance_env().oebs_gate_url, namespace=None)


class FailedXmlRpcCall(utils.ServiceError):
    pass


ALL_METHODS = ['EstimateDiscount',
               'CreateOrUpdateOrdersBatch',
               'CreateRequest2',
               'CreateRequest',
               'TurnOnRequest',
               'CreateTransfer',
               'CreateTransferMultiple',
               'CreateCommonContract',
               'UpdateContract',
               'CreateOffer',
               'CreateOperation',
               'CreateClient',
               'CreatePerson',
               'CreateInvoice',
               'CreateYaMoneyInvoice',
               'GetOrdersDirectPaymentNumber',
               'CreateInvoice2',
               'GetBank',
               'CreateUserClientAssociation',
               'HasRepresentative',
               'RemoveUserClientAssociation',
               'MergeClients',
               'FindClient',
               'GetClientPersons',
               'GetClientByIdBatch',
               'GetInvoice',
               'GetUpdatedOrders',
               'GetEqualClients',
               'GetClientCreditLimits',
               'GetContractCreditsDetailed',
               'CreateContract',
               'GetClientContracts',
               'GetContractCredits',
               'GetWorkingCalendar',
               'GetClientAgencies',
               'GetClientActs',
               'GetProducts',
               'SetClientProductDiscount',
               'UpdateCampaigns',
               'UpdateProjects',
               'DailyShipments',
               'CreateOrUpdatePlace',
               'CreateServiceProduct',
               'GetClientPublicKey',
               'SignClientMessage',
               'GetOrderInfoForIntel',
               'GetPassportByUid',
               'GetPassportByLogin',
               'ListClientPassports',
               'GetClientBrand',
               'GetClientDiscountsAll',
               'EditPassport',
               'RollbackInvoice',
               'GetDirectDiscount',
               'GetDirectProducts',
               'GetDirectBudget',
               'GetDirectBrand',
               'GetAllEqualClients',
               'GetClientNDS',
               'GetFirmCountryCurrency',
               'GetClientCurrencies',
               'GetCurrencyRate',
               'GetClientTaxForDirect',
               'CreateFastInvoice',
               'GetRequestChoices',
               'CreateFastPayment',
               'AddClientDomain',
               'ValidateAppStoreReceipt',
               'ValidateGoogleInAppPurchase',
               'ValidateWinStoreReceipt',
               'CheckInAppSubscription',
               'StoreImhoAdditionalCommissionInfo',
               'EstimateBudgetDiscounts',
               'QueryCatalog',
               'ListPaymentMethodsSimple',
               'GetPurchasedServiceProducts',
               'GetOwnedOrders',
               'CreateOrderSimple',
               'PayOrderSimple',
               'CheckOrderSimple',
               'RefundOrderSimple',
               'ConsumeItemSimple',
               'StopSubscriptionSimple',
               'GetCompletionHistory',
               'GetMixedNDSClients',
               'GetDirectBalance',
               'LinkDspToClient',
               'GetClientDsp',
               'GetCurrencyProducts',
               'GetNDSInfo',
               'GetOrdersInfo',
               'CreateOrUpdateDistributionTag',
               'GetTaxiBalance',
               'GetPartnerBalance',
               'GetDspStat',
               'GetDistributionActed',
               'GetPartnerOrderCompletion',
               'AcceptTaxiOffer',
               'GetManagersInfo',
               'GetMncloseStatus',
               'ResolveMncloseTask',
               'UpdatePayment',
               'GetContractPrintForm',
               ]
