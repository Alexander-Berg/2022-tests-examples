# -*- coding: utf-8 -*-

"""
This module implements builders for database objects, which are useful
for testing.

Builders can lazily build database objects. They store all data required to
create an object, but only require a session to actually create ("build")
it, so they can be safely modified, serialized, created in setUp etc.

Example:
    >>>invoice = InvoiceBuilder()
    >>>invoice.request.client = Getter(mapper.Client, 299139)
    >>>invoice.request.b.agency_discount_pct = 10
    >>>invoice.request.rows[0][0].product._other.tax = GenericBuilder(mapper.Tax,
    ...    product=invoice.request.rows[0][0].product, nds_pct=18, nsp_pct=0,
    ...    currency=Getter(Currency, 'RUR'))
    >>> # No session is needed until now
    >>>invoice.build(session)
"""

import datetime
import collections
import contextlib
import os
import uuid
import random
import string
import mock
import sqlalchemy as sa
from decimal import Decimal as D
from pycron.sqlalchemy_mappers import JobDescr, JobSchedule

from butils.decimal_unit import DecimalUnit as DU
from billing.contract_iface import contract_meta
from billing.contract_iface import ContractTypeId

from butils.passport import INTRA_MIN, INTRA_MAX

from balance import core
from balance import mapper
from balance import exc
from balance import muzzle_util as ut
from balance.actions.invoice_create import InvoiceFactory
from balance.constants import (
    GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE,
    POSTPAY_PAYMENT_TYPE,
    FirmId,
    ServiceId,
    InvoiceRefundStatus,
    InvoiceTransferStatus,
    NirvanaTaskStatus,
    CONVERT_TYPE_COPY,
    LanguageId,
    OebsOperationType,
    RoleType,
    NirvanaProcessingTaskState,
    PromocodeApplyTypes,
)
from balance.payments import payments

# Fork-compatible random generator
RANDOM = random.SystemRandom()


def get_big_number():
    # Should fit in XMLRPC's int4
    return RANDOM.randint(2 ** 25, 2 ** 30)


def _mk_list(obj):
    return list(obj) if isinstance(obj, collections.Iterable) else [obj]


def generate_numeric_string(length=10):
    return str(generate_int(length))


def generate_character_string(length=10):
    return ''.join(RANDOM.choice(string.letters) for _ in range(length))


def generate_int(length=10):
    """ Generate random int of specified length (in decimal digits) """
    assert length > 0
    min_ = 10 ** (length - 1)
    max_ = min_ * 10
    if length == 1:
        min_ = 0
    result = int((max_ - min_) * RANDOM.random())
    return result + min_


PAYSYS_ID = 1001  # Bank payment


class BuilderContainer(dict):
    """A container for builders.
    Elements can be accessed using container.elem as well as using
    container['elem']. Turns list and tuples in ListBuilders on assignment.
    """

    def transform_seqs(self):
        for key, val in self.iteritems():
            if isinstance(val, (list, tuple)):
                super(self.__class__, self).__setitem__(key, ListBuilder(*val))

    def update(self, *args, **kwargs):
        super(self.__class__, self).update(*args, **kwargs)
        self.transform_seqs()

    def __setattr__(self, key, val):
        self[key] = val  # call __setitem__

    def __getattr__(self, key):
        return self[key]

    def __setitem__(self, key, val):
        super(self.__class__, self).__setitem__(key, val)
        # Transform lists and tuples to ListBuilder's
        self.transform_seqs()

    def __delattr__(self, key):
        del self[key]

    # The dict returned by this method can be passed to database class __init__
    def get_objects(self):
        """Get container content with builders replaced with object they
        have built"""
        return dict((k, (v.obj if isinstance(v, ObjectBuilder) else v))
                    for k, v in self.iteritems())


class ObjectBuilder(object):
    """Base class for database objects buiders.  """

    # Class of object to build
    _class = None
    _factory_class = None

    # Object attributes (simple and builders, no database object). Actual
    # keyword parameters to _class.__init__ are built from it.
    _build_params = None

    # Container for any additional objects we want to use
    # from outside OR we need to build.
    _other = None

    # Stored instance of built object
    _obj = None

    # A flag to indicate that object build process has been started (and maybe
    # completed)
    _build_has_been_started = False

    # If this evaluates to true, ObjectBuilder.build() will generate random
    # unique id and assign it to self._build_params.id.
    _generate_id = False

    # Access to object attributes for external code
    @property
    def b(self):
        return self._build_params

    # Built object getter - redefine in subclasses
    def get_obj(self):
        return self._obj

    # Access to built object for external code
    @property
    def obj(self):
        return self.get_obj()

    # If key is not found in self.__dict__, return value from
    # self.__build_params
    def __getattr__(self, key):
        return self.b.__getattr__(key)

    def __setattr__(self, key, value):
        if not key.startswith('_'):
            raise AttributeError(
                "Assigning attributes to builder is prohibited. "
                "Did you mean to assign to build parameters (.b)?\n"
                ":_class=%s, key=%s, value=%s" % (self._class, key, value))
        self.__dict__[key] = value

    def __init__(self, **params):
        self._build_params = BuilderContainer()
        self._other = BuilderContainer()
        self.prepare(**params)

    def set_build_params(self, **params):
        """Set a batch of attributes"""
        for key, val in params.iteritems():
            self._build_params[key] = val

    def generate_unique_id(self, session, attrib_name, max_attempts=5):
        """Generates unique id"""
        for attempt_num in xrange(max_attempts):
            id = get_big_number()
            id_exists = session.query(sa.literal(True)).filter(
                sa.exists().where(getattr(self._class, attrib_name) == id)
            ).scalar()
            if not id_exists:
                return id
        else:
            raise Exception('Failed {} attempts to generate unique id.'
                            .format(max_attempts))

    # Main method to redefine in subclasses.
    def prepare(self, **params):
        """Set all required attributes and get the object ready for building"""
        for key, val in params.iteritems():
            self._build_params[key] = val

    def postprocess(self):
        """
        update an object after building
        """
        pass

    def build(self, session):
        """Build the object with all parameters and store it internally"""

        # Avoid building more than once
        if self._build_has_been_started:
            return self
        self._build_has_been_started = True

        # Generate unique id if needed
        if self._generate_id:
            self._build_params.id = self.generate_unique_id(session, 'id')

        # Build all the parameters
        for key, val in self._build_params.iteritems():
            if isinstance(val, ObjectBuilder):
                val.build(session)

        # Build the object
        if self._factory_class:
            self._obj = self._factory_class.create(**self._build_params.get_objects())
        else:
            self._obj = self._class(**self._build_params.get_objects())
        if isinstance(self._obj, (mapper.DomainObject, mapper.DeclarativeBase)):
            session.add(self._obj)
            session.flush()
            self.postprocess()
        for key, val in self._other.iteritems():
            if isinstance(val, ObjectBuilder) and not val._obj:
                val.build(session)
        return self

    @classmethod
    def construct(cls, session, **params):
        return cls(**params).build(session).obj

    def reset(self):
        """Reset this builder and all children to state before build"""
        self._obj = None
        self._build_has_been_started = False
        for val in self._build_params.itervalues():
            if isinstance(val, ObjectBuilder) and val._build_has_been_started:
                val.reset()
        for val in self._other.itervalues():
            if isinstance(val, ObjectBuilder) and val._build_has_been_started:
                val.reset()


class ListBuilder(ObjectBuilder):
    """Replacement of python sequences for builders"""

    def __getitem__(self, index):
        return self.b[index]

    def __init__(self, *args):
        super(ListBuilder, self).__init__()
        self._build_params = []
        for elem in args:
            if isinstance(elem, (tuple, list)):
                self._build_params.append(ListBuilder(*elem))
            else:
                self._build_params.append(elem)

    def get_obj(self):
        return [(v.obj if isinstance(v, ObjectBuilder) else v)
                for v in self.b]

    def build(self, session):
        # Avoid building more than once
        if self._build_has_been_started:
            return self
        self._build_has_been_started = True
        for item in self._build_params:
            if isinstance(item, ObjectBuilder):
                item.build(session)
        return self

    def reset(self):
        self._obj = None
        self._build_has_been_started = False
        for val in self._build_params:
            if isinstance(val, ObjectBuilder):
                val.reset()


class Getter(ObjectBuilder):
    """
    Class to get things from database by id
    """

    def __init__(self, _class, _id, **params):
        self._class = _class
        self._id = _id
        super(Getter, self).__init__(**params)

    def build(self, session):
        # Avoid building more than once
        if self._build_has_been_started:
            return self
        self._build_has_been_started = True
        self._obj = session.query(self._class).get(self._id)
        for key, val in self._build_params.iteritems():
            if isinstance(val, ObjectBuilder):
                val.build(session)
        for key, val in self._build_params.get_objects().iteritems():
            self._obj.__setattr__(key, val)
        return self


class GenericBuilder(ObjectBuilder):
    def __init__(self, _class, **params):
        self._class = _class
        if _class == mapper.ProdSeasonCoeff:
            self._generate_id = 1
        super(GenericBuilder, self).__init__(**params)


class ConfigBuilder(ObjectBuilder):
    _class = mapper.Config


class TaxBuilder(ObjectBuilder):
    _class = mapper.Tax
    _generate_id = True

    def prepare(self, **params):
        tax_policy = params.get("tax_policy", None)
        if tax_policy:
            self.b.tax_policy = tax_policy
            self.b.tax_policy_id = tax_policy.id
            self._other.currency = Getter(mapper.Currency, 'RUR')
        else:
            currency = params.get("currency", None)

            if isinstance(currency, mapper.Currency):
                cname = currency.char_code
            elif isinstance(currency, Getter):
                cname = currency._id
            else:
                cname = currency
            if cname:
                assert isinstance(cname, basestring)
                self._other.currency = Getter(mapper.Currency, cname)
                self.b.nds_pct = 0
                self.b.nsp_pct = 0
                if cname == 'RUR':
                    self.b.nds_pct = 20
                if cname == 'UAH':
                    self.b.nds_pct = 20
                    self.b.nsp_pct = 0  # D('0.5')

        self.b.hidden = 0
        params.setdefault('dt', datetime.datetime(2001, 1, 1))

        super(TaxBuilder, self).prepare(**params)

    def build(self, session):
        currency = self._other.currency.build(session).obj
        self.b.iso_currency = currency.iso_code if not self.b.get('iso_currency') else self.b.iso_currency
        self.b.currency = currency
        super(TaxBuilder, self).build(session)
        return self


class TaxesBuilder(ObjectBuilder):
    _class = mapper.Tax
    _generate_id = True

    def build(self, session):
        tax_policies = session.query(mapper.TaxPolicy).filter_by(default_tax=1, hidden=0).all()
        for tax_policy in tax_policies:
            tax = TaxBuilder(tax_policy=tax_policy, product=self.b.product)
            tax.build(session)


class ThirpartyServiceBuilder(ObjectBuilder):
    _class = mapper.ThirdPartyService

    def prepare(self, **params):
        self.b.id = params['id']
        self.b.agent_scheme = params.pop('agent_scheme', 0)


class ServiceBuilder(ObjectBuilder):
    _class = mapper.Service

    def prepare(self, **params):
        self.b.dt = datetime.datetime.now()
        self.b.fiscal_service = mapper.FiscalService()
        self.b.balance_service = mapper.BalanceService()

        self.b.balance_service.contract_needed_agency = params.pop('contract_needed_agency', 0)
        self.b.balance_service.client_only = params.pop('client_only', None)
        self.b.balance_service.unilateral = params.pop('unilateral', None)
        self.b.balance_service.extra_pay = 0
        self.b.balance_service.in_contract = 1
        self.b.balance_service.intern = 0
        self.b.balance_service.is_auto_completion = 0
        self.b.balance_service.media = 1
        self.b.balance_service.contract_needed_client = params.pop('contract_needed_client', 1)
        self.b.balance_service.is_spendable = params.pop('is_spendable', 0)
        self.b.balance_service.url_orders = params.pop('url_orders', None)
        self.b.balance_service.show_to_user = params.pop('show_to_user', 2)
        self.b.balance_service.show_to_external_user = 1
        self.b.balance_service.send_invoices = 1
        self.b.balance_service.partner_income = params.pop('partner_income', 0)
        self._other.allowed_agency_without_contract = params.pop('allowed_agency_without_contract', 0)
        super(ServiceBuilder, self).prepare(**params)

    def build(self, session):
        self.b.allowed_agency_without_contract = self._other.allowed_agency_without_contract
        if 'id' not in self.b:
            self.b.id = self.generate_unique_id(session, 'id')
        return super(ServiceBuilder, self).build(session)


class ThirdpartyServiceBuilder(ObjectBuilder):
    _class = mapper.ThirdPartyService

    def prepare(self, **params):
        self.b.enabled = params.get('enabled', 1)
        super(ThirdpartyServiceBuilder, self).prepare(**params)


class ClientBuilder(ObjectBuilder):
    _class = mapper.Client

    def __init__(self, with_single_account=False, **params):
        super(ClientBuilder, self).__init__(**params)
        self._with_single_account = with_single_account

    def prepare(self, **params):
        self.b.name = 'test'
        self.b.email = "test@test.ru"
        super(ClientBuilder, self).prepare(**params)

    def build(self, session):
        if self._with_single_account:
            self.b.creation_dt = self.b.get('creation_dt', session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT)

        result = super(ClientBuilder, self).build(session)

        if self._with_single_account:
            from balance.actions.single_account.prepare import process_client
            process_client(self.obj)

        return result


class CorrectionTemplateGroupBuilder(ObjectBuilder):
    _class = mapper.CorrectionTemplateGroup

    def prepare(self, **params):
        self.b.id = generate_int(4)
        self.b.title = generate_character_string(16)
        self.b.items = params.get('items') or []
        super(CorrectionTemplateGroupBuilder, self).prepare(**params)


class GroupBuilder(ObjectBuilder):
    _class = mapper.Group


class EmailMessageBuilder(ObjectBuilder):
    _class = mapper.EmailMessage

    def build(self, session):
        return super(EmailMessageBuilder, self).build(session)

    def prepare(self, **params):
        self.b.opcode = GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE
        self.b.object_id = get_big_number()
        self.b.recepient_name = 'recepient_name'
        self.b.recepient_address = 'test@email.ru'
        if 'data' in params:
            self.b.data = params['data']
        super(EmailMessageBuilder, self).prepare(**params)


class PassportBuilder(ObjectBuilder):
    _class = mapper.Passport

    def prepare(self, **params):
        self.b.gecos = "Pupkin pup"
        self.b.email = "test@test.ru"
        self.b.avatar = "0/0-0"
        self.b.client = params.get('client')
        self.b.simple_client = params.get('simple_client')
        super(PassportBuilder, self).prepare(**params)

    def build(self, session):
        if not hasattr(self.b, 'passport_id'):
            self.b.passport_id = self.generate_unique_id(session, 'passport_id')
        if not hasattr(self.b, 'login'):
            self.b.login = "testlogin" + str(self.b.passport_id)
        return super(PassportBuilder, self).build(session)


CURRENCY_TO_TAX_POLICY_ID_MAP = {
    'USD': 21,
    'RUR': 281,
    'EUR': 8,
    'UAH': 5,
    'TRY': 22,
    'CHF': 8
}


class SimplePriceBuilder(ObjectBuilder):
    _class = mapper.Price
    _generate_id = True

    def prepare(self, **params):
        self._other.currency = Getter(mapper.Currency, params.pop('currency_code'))
        self.b.price = 100
        super(SimplePriceBuilder, self).prepare(**params)

    def build(self, session):
        currency = self._other.currency.build(session).obj
        self.b.iso_currency = currency.iso_code
        self.b.currency = currency
        super(SimplePriceBuilder, self).build(session)

        return self


class PlaceBuilder(ObjectBuilder):
    _class = mapper.Place
    _generate_id = True

    def prepare(self, **params):
        self.b.type = 2
        self.b.internal_type = 0
        self.b.url='url_{}'.format(generate_character_string(3))
        super(self.__class__, self).prepare(**params)


class MkbCategoryBuilder(ObjectBuilder):
    _class = mapper.MkbCategory
    _generate_id = True

    def prepare(self, **params):
        self.b.name = generate_character_string(10)
        self.b.price = RANDOM.randint(10, 1000)
        self.b.unit_id = 799
        self.b.nds = 1
        super(self.__class__, self).prepare(**params)


class PriceBuilder(ObjectBuilder):
    """Test price builder"""
    _class = mapper.Price
    _generate_id = True

    def prepare(self, **params):
        self.b.dt = datetime.datetime(2001, 1, 1)
        self.b.tax = 1
        currency_code = params.pop('currency', 'RUR')
        currency = Getter(mapper.Currency, currency_code) if isinstance(currency_code, basestring) else currency_code
        self._other.currency = currency
        self.b.price = params.pop('price', None) or 100
        self.b.product = None

        super(PriceBuilder, self).prepare(**params)

    def build(self, session):
        currency = self._other.currency.build(session).obj
        self.b.iso_currency = currency.iso_code if not self.b.get('iso_currency') else self.b.iso_currency
        self.b.currency = currency
        super(PriceBuilder, self).build(session)
        if self.obj.tax:
            tax_policy_pct_id = CURRENCY_TO_TAX_POLICY_ID_MAP.get(self.b.currency.char_code, 1)
            self.obj.tax_policy_pct = session.query(mapper.TaxPolicyPct).getone(tax_policy_pct_id)

        return self


class ContractBuilder(ObjectBuilder):
    _class = mapper.Contract

    def prepare(self, **params):
        from billing.contract_iface import contract_meta

        ctype = params.pop('ctype', 'GENERAL')
        contract_params = dict(
            client=params.pop('client', ClientBuilder()),
            external_id=params.pop('external_id', 'test/test'),
            is_process_taxi_netting_in_oebs_=params.pop('is_process_taxi_netting_in_oebs_', None),
            cpf_netting_last_dt=params.pop('cpf_netting_last_dt', None),
            ctype=contract_meta.ContractTypes(type=ctype),
        )
        person = params.pop('person', PersonBuilder(client=contract_params['client']))
        if person:
            contract_params['person'] = person

        super(self.__class__, self).prepare(**contract_params)

        if params.get('dt', None) is None:
            # if dt is not present or explicitly None we use default
            params['dt'] = datetime.datetime.now() - datetime.timedelta(10)
        params.setdefault('finish_dt', datetime.datetime.now() + datetime.timedelta(10))
        if ctype == 'GENERAL':
            params.setdefault('commission', ContractTypeId.NON_AGENCY)
        params.setdefault('payment_type', POSTPAY_PAYMENT_TYPE)
        params.setdefault('is_signed', datetime.datetime.now())

        self._collateral_params = params

    def build(self, session):
        import importlib

        try:
            contract_attributes = importlib.import_module(
                'billing.contract_iface.cmeta.%s' % self.obj.type.lower()).contract_attributes
        except (ImportError, AttributeError):
            contract_attributes = None

        super(ContractBuilder, self).build(session)

        if self._collateral_params.get('manager_code') is None:
            manager = SingleManagerBuilder.construct(session, login='login_{}'.format(generate_int(4)))
            self._collateral_params['manager_code'] = manager.manager_code

        if self._collateral_params.get('services') is None:
            service = ServiceBuilder.construct(session)
            self._collateral_params['services'] = service.id

        collateral_column_names = [c.name for c in list(mapper.ContractCollateral.__table__.columns)]

        for attribute, value in self._collateral_params.iteritems():
            if isinstance(value, ObjectBuilder):
                value = value.build(session).obj
            if attribute.lower() == 'collaterals':
                for col in value:
                    dt = col.pop('dt', None) or ut.trunc_date(datetime.datetime.now())
                    collateral_type_id = col.pop('collateral_type_id', None)
                    col_type = contract_meta.collateral_types[self.obj.ctype.type][collateral_type_id]
                    self.obj.append_collateral(dt, col_type, **col)
                continue
            attr_meta = contract_attributes.get(attribute.upper(), None) if contract_attributes else None
            if attribute in collateral_column_names or attr_meta is None:
                setattr(self.obj.col0, attribute, value)
            else:
                attr_meta.todb(self.obj.col0, attribute.upper(), value)

        session.flush()
        return self


class CollateralBuilder(ObjectBuilder):
    _class = mapper.ContractCollateral

    def prepare(self, **params):
        # each collateral must have a dt
        self.b.dt = params.pop('dt', None) or datetime.datetime.now()
        super(CollateralBuilder, self).prepare(**params)


class ContractPDFEmailBuilder(ObjectBuilder):
    _class = mapper.ContractPDFEmail


class CRPaymentReportBuilder(ObjectBuilder):
    _class = mapper.CRPaymentReport

    def prepare(self, **params):
        self.b.contract = params.pop('contract', ContractBuilder())
        self.b.start_dt = params.pop('start_dt', datetime.datetime.now())
        self.b.end_dt = params.pop('end_dt', datetime.datetime.now() + datetime.timedelta(days=7))
        self.b.nach_amount = params.pop('nach_amount', D('1.23'))
        self.b.perech_amount = params.pop('perech_amount', D('6.66'))
        self.b.avans_amount = params.pop('avans_amount', D('9.99'))
        super(CRPaymentReportBuilder, self).prepare(**params)


class YandexMoneyPaymentBuilder(ObjectBuilder):
    """Test payment builder"""
    _class = mapper.YandexMoneyPayment


class WebMoneyPaymentBuilder(ObjectBuilder):
    _class = mapper.WebMoneyPaymasterPayment


class CardPaymentBuilder(ObjectBuilder):
    _class = mapper.CardPayment

    def prepare(self, **params):
        invoice = params.get('invoice', None)
        if not invoice:
            params['invoice'] = InvoiceBuilder()

        super(CardPaymentBuilder, self).prepare(**params)


class TrustApiPaymentBuilder(ObjectBuilder):
    _class = mapper.TrustApiPayment


class CardRegister(ObjectBuilder):
    _class = mapper.CardRegister


class RBSRegisterBuilder(ObjectBuilder):
    _class = mapper.RBSRegister

    def prepare(self, **params):
        self.b.register_dt = datetime.datetime.now()
        self.b.amount = D('1')
        super(RBSRegisterBuilder, self).prepare(**params)


class ActivityBuilder(ObjectBuilder):
    _class = mapper.ActivityType
    _generate_id = True

    def prepare(self, **params):
        self.b.hidden = 0
        self.b.name = 'Test activity'
        super(ActivityBuilder, self).prepare(**params)


class ProductGroupBuilder(ObjectBuilder):
    """Test ProductGroup builder"""
    _class = mapper.ProductGroup
    _generate_id = True

    def prepare(self, **params):
        self.b.discount_id = 0
        super(ProductGroupBuilder, self).prepare(**params)


class ProductBuilder(ObjectBuilder):
    _class = mapper.Product
    _generate_id = True

    def prepare(self, **params):
        params.setdefault('name', 'Test Product')
        params.setdefault('product_group', ProductGroupBuilder(
            name='Test Product Group',
            hidden=0,
            discount_id=0
        ))

        # 'auction' product unit; required when quantity is not integer
        self.b.unit = params.pop('unit', Getter(mapper.ProductUnit, 0))
        self.b.mdh_id = str(uuid.uuid4())
        if 'taxes' in params:
            taxes = _mk_list(params.pop('taxes'))

            def _cr_tax(tax_param):
                tax_param = _mk_list(tax_param)
                if len(tax_param) == 1:
                    policy, = tax_param
                    dt = datetime.datetime(2000, 1, 1)
                else:
                    dt, policy = tax_param

                return TaxBuilder(product=self, dt=dt, tax_policy=policy)

            self._other.tax = ListBuilder(*map(_cr_tax, taxes))
        elif params.pop('create_taxes', True):
            # This tax keeps RequestOrder.update happy
            self._other.tax = TaxesBuilder(product=self)

        if 'prices' in params:
            prices = _mk_list(params.pop('prices'))

            def _cr_price(price_param):
                price_param = _mk_list(price_param)
                if len(price_param) == 2:
                    currency, price = price_param
                    dt = datetime.datetime(2000, 1, 1)
                    tax_pct = None
                elif len(price_param) == 3:
                    dt, currency, price = price_param
                    tax_pct = None
                else:
                    dt, currency, price, tax_pct = price_param

                return PriceBuilder(
                    product=self, currency=currency, dt=dt,
                    price=price, tax_policy_pct=tax_pct, tax=None
                )

            self._other.price = ListBuilder(*map(_cr_price, prices))
        elif params.pop('create_price', True):
            currency = Getter(mapper.Currency, params.get('currency', 'RUR'))
            self._other.price = PriceBuilder(
                product=self,
                currency=currency,
                price=params.pop('price', None)
            )
        self.b.activity_type = ActivityBuilder()

        super(ProductBuilder, self).prepare(**params)


class ProductUnitBuilder(ObjectBuilder):
    _class = mapper.ProductUnit

    def prepare(self, **params):
        self.b.id = get_big_number()
        self.b.name = 'Test product unit'
        self.b.englishname = 'Test product unit en'
        self.b.type_rate = 10
        self.b.product_type = ProductTypeBuilder()
        self.b.precision = 4
        super(ProductUnitBuilder, self).prepare(**params)


class ProductTypeBuilder(ObjectBuilder):
    _class = mapper.ProductType

    def prepare(self, **params):
        self.b.id = get_big_number()
        self.b.cc = generate_character_string()
        self.b.name = generate_character_string()


class PartnerProductBuilder(ObjectBuilder):
    _class = mapper.PartnerProduct

    def prepare(self, **params):
        if 'product' in params:
            product = params.pop('product')
        else:
            raise exc.INVALID_PARAM('product')

        self.b.service_id = product.engine_id
        self.b.product_id = product.id
        super(PartnerProductBuilder, self).prepare(**params)


class MarkupBuilder(ObjectBuilder):
    _class = mapper.Markup

    _generate_id = True

    def prepare(self, **params):
        self.b.code = "Test Markup"
        super(MarkupBuilder, self).prepare(**params)


class ProductMarkupBuilder(ObjectBuilder):
    _class = mapper.ProductMarkup

    _generate_id = True

    def prepare(self, **params):
        self.b.pct = 0
        super(ProductMarkupBuilder, self).prepare(**params)


class SingleManagerBuilder(ObjectBuilder):
    _class = mapper.Manager

    def prepare(self, **params):
        params.setdefault('hidden', 0)
        params.setdefault('is_sales', 0)
        params.setdefault('is_backoffice', 0)
        params.setdefault('manager_type', 0)
        params.setdefault('firm_id', FirmId.YANDEX_OOO)
        params.setdefault('hidden', 0)
        super(SingleManagerBuilder, self).prepare(**params)

    def build(self, session):
        self.b.manager_code = self.generate_unique_id(session, attrib_name='manager_code')
        return super(SingleManagerBuilder, self).build(session)


class ManagerWithChiefsBuilder(ObjectBuilder):
    _class = mapper.Manager

    def _get_manager(self, session, number):
        managers = (
            session.query(mapper.Manager)
        )
        managers_with_chiefs = []
        for m in managers:
            try:
                m.chief
            except exc.NOT_FOUND:
                pass
            else:
                managers_with_chiefs.append(m)
                # m.chief порождает слишком много запросов,
                # а пятерых должно хватить.
                if len(managers_with_chiefs) == 5:
                    break

        return managers_with_chiefs[number]

    def prepare(self, **params):
        bp = self.b
        if 'number' not in params:
            bp.number = 0
        super(ManagerWithChiefsBuilder, self).prepare(**params)

    def build(self, session):
        """
        Trying to fetch manager with chief because of test_contract_notifier.py.
        """
        m = self._get_manager(session, self.b.number)
        self._obj = m
        return self


class OrderBuilder(ObjectBuilder):
    """Test order builder"""
    _class = mapper.Order

    def prepare(self, **params):
        params.setdefault('client', ClientBuilder())
        params.setdefault('manager', SingleManagerBuilder())
        product_id = params.pop('product_id', None)
        if product_id:
            params.setdefault('product', Getter(mapper.Product, product_id))
        else:
            params.setdefault('product', ProductBuilder())
        params.setdefault('service', Getter(mapper.Service, params.get('service_id', ServiceId.DIRECT)))
        super(OrderBuilder, self).prepare(**params)

    def build(self, session):
        self.b.service_order_id = self.generate_unique_id(session,
                                                          'service_order_id')
        self.b.service_order_id_str = str(uuid.uuid4())
        return super(OrderBuilder, self).build(session)


class BasketItemBuilder(ObjectBuilder):
    """Test basket item builder"""
    _class = mapper.BasketItem


class BasketRegisterRowBuilder(ObjectBuilder):
    _class = mapper.BasketRegisterRow

    def prepare(self, **params):
        self.b.ref_invoice = params.pop('ref_invoice')
        self.b.amount = self.b.ref_invoice.amount
        self.b.amount_nds = self.b.ref_invoice.amount_nds
        self.b.amount_nsp = self.b.ref_invoice.amount_nsp
        super(BasketRegisterRowBuilder, self).prepare(**params)


class BasketBuilder(ObjectBuilder):
    """Test basket builder"""
    _class = mapper.Basket

    def prepare(self, **params):
        if 'rows' not in params:
            params.setdefault('client', ClientBuilder())
            product = ProductBuilder()
            params['rows'] = [
                BasketItemBuilder(quantity=2, order=OrderBuilder(client=params['client'], product=product),
                                  desired_discount_pct=0, user_data='1')
            ]

        super(BasketBuilder, self).prepare(**params)


class RequestBuilder(ObjectBuilder):
    """Test request builder"""
    _class = mapper.Request

    def prepare(self, **params):
        if 'basket' in params:
            self.b.basket = params['basket']
        else:
            self.b.basket = BasketBuilder()

        if 'rows' in params:
            raise ut.INVALID_PARAM("""Request "rows" parameter is obsolete.""")
        if 'client' in params:
            raise ut.INVALID_PARAM("""Request "client" parameter is obsolete.""")
        super(RequestBuilder, self).prepare(**params)
        # If 'rows' was in params, client will be set correctly, too
        if 'client' not in self.b.basket.b:
            try:
                self.b.basket.b.client = self.b.basket.b.rows[0].order.client
            except IndexError:
                # If there is no rows, you probably know what you are doing.
                pass


class PersonBuilder(ObjectBuilder):
    """Test person builder"""
    _class = mapper.Person
    _person_params = {}

    def prepare(self, **params):
        self._person_params = params
        super(PersonBuilder, self).prepare(**params)

    def build(self, session):
        from billing.contract_iface.cmeta import person

        params = self._person_params
        self.b.operator_uid = session.oper_id
        self.b.type = params.get('type', 'ph')

        if not {'client', 'client_id'} & set(params.keys()):
            self.b.client = ClientBuilder()
        elif 'client_id' in params:
            self.b.client = Getter(mapper.Client, params.pop('client_id'))

        super(PersonBuilder, self).build(session)

        attributes = person.attribute_batch

        for attr, value in self._person_params.items():
            if isinstance(value, ObjectBuilder):
                value = value.build(session).obj
            attr_meta = attributes.get(attr.upper(), None) if attributes else None

            if attr_meta is not None:
                attr_meta.todb(self.obj, attr.upper(), value)
            else:
                setattr(self.obj, attr, value)

        return self


class EdoOfferBuilder(ObjectBuilder):
    _class = mapper.EdoOffer

    def prepare(self, **params):
        params.setdefault('firm_id', FirmId.YANDEX_OOO)
        params.setdefault('active_start_date', datetime.datetime.now())
        params.setdefault('active_end_date', None)
        params.setdefault('status', 'FRIENDS')
        params.setdefault('blocked', False)
        params.setdefault('default_flag', True)
        params.setdefault('enabled_flag', True)
        params.setdefault('org_orarowid', str(generate_int(25)))
        params.setdefault('inv_orarowid', str(generate_int(25)))
        super(EdoOfferBuilder, self).prepare(**params)


class InvoiceBuilder(ObjectBuilder):
    _class = mapper.Invoice
    _factory_class = InvoiceFactory

    def prepare(self, **params):
        client_params = dict()
        if 'client' in params:
            client_params['client'] = params['client']
        elif 'client_id' in params:
            client_params['client_id'] = params['client_id']

        params.setdefault('request', RequestBuilder(basket=BasketBuilder(**client_params)))
        params.setdefault('paysys', Getter(mapper.Paysys, params.get('paysys_id', PAYSYS_ID)))
        params.setdefault('status_id', 0)
        params.setdefault('credit', 0)
        params.setdefault('temporary', False)
        super(InvoiceBuilder, self).prepare(**params)

    def build(self, session):
        if self.b.get('person') is None:
            if isinstance(self.b.paysys, mapper.Paysys):
                paysys = self.b.paysys
            else:
                paysys = self.b.paysys.build(session).obj

            if isinstance(self.b.request, mapper.Request):
                client = self.b.request.client
            else:
                client = self.b.request.b.basket.b.client

            self.b.person = PersonBuilder(client=client, type=paysys.category,
                                          person_category=paysys.person_category)
        return super(InvoiceBuilder, self).build(session)


class OebsCashPaymentFactBuilder(ObjectBuilder):
    _class = mapper.OebsCashPaymentFact
    _generate_id = True

    def prepare(self, **params):
        bp = self.b

        bp.amount = params['amount']
        bp.operation_type = params.get('operation_type', OebsOperationType.INSERT)
        invoice = params.get('invoice')
        bp.receipt_number = invoice and invoice.external_id or params['receipt_number']

        bp.created_by = bp.last_updated_by = -1
        dt = params.pop('dt') if 'dt' in params else None
        dt = dt or datetime.datetime.now()
        bp.creation_date = bp.last_update_date = bp.receipt_date = dt

        super(OebsCashPaymentFactBuilder, self).prepare(**params)


class PayOnCreditCase(object):
    def __init__(self, session):
        self.session = session

    def pay_on_credit(self, b_basket, contract, paysys=None, with_paystep=False):
        if not isinstance(b_basket, BasketBuilder):
            raise ut.INVALID_PARAM('BasketBuilder expected')

        b_request = RequestBuilder(basket=b_basket)
        req = b_request.build(self.session).obj

        coreobj = core.Core(self.session)
        if with_paystep:
            invoices = coreobj.create_invoice(
                request_id=req.id,
                paysys_id=paysys.id if paysys else 1000,
                person_id=contract.person.id,
                contract_id=contract.id,
                credit=1)
        else:
            invoices = coreobj.pay_on_credit(
                request_id=req.id,
                paysys_id=paysys.id if paysys else 1000,
                person_id=contract.person.id,
                contract_id=contract.id)

        self.session.flush()
        return invoices

    def get_contract(self, **params):
        b_client = ClientBuilder()
        b_person = PersonBuilder(client=b_client, type='ph').build(self.session)

        if 'firm' not in params:
            params['firm'] = 1
        if 'currency' not in params:
            params['currency'] = self.session.query(mapper.Currency).getone(
                char_code=self.session.query(mapper.Firm).getone(params['firm']).default_currency).num_code

        params['is_signed'] = datetime.datetime.now() if params.get('is_signed') else None
        _params = {'client': b_client, 'person': b_person}
        _params.update(params)
        b_contract = ContractBuilder(
            **_params
        )
        cont = b_contract.build(self.session).obj

        cont.col0.is_signed = datetime.datetime.now()

        for p, v in params.iteritems():
            if p in ['person_id', 'client_id']:
                setattr(cont, p, v)
            else:
                setattr(cont.col0, p, v)
        self.session.flush()

        return cont

    def get_product_hierarchy(self, **params):
        '''activity type hierarchy tree:
              p[0]
               |  \
              p[1] p[2]
               |
              p[3]
        '''
        p = [ProductBuilder(**params) for x in xrange(4)]
        for x in [(3, 1), (1, 0), (2, 0)]:
            p[x[0]].b.activity_type.b.parent = p[x[1]].b.activity_type

        # Pre-build activity_types to generate their IDs.
        [product.b.activity_type.build(self.session) for product in p]

        return p

    def get_credits_available(self, b_basket, cont):
        if not isinstance(b_basket, BasketBuilder):
            raise ut.INVALID_PARAM('BasketBuilder expected')

        b_request = RequestBuilder(basket=b_basket)
        b_invoice = InvoiceBuilder(request=b_request, person=cont.person, contract=cont)
        inv = b_invoice.build(self.session).obj

        return inv.get_credit_available()


class JobDescrBuilder(ObjectBuilder):
    _class = JobDescr

    def prepare(self, **params):
        self.b.name = "name"
        self.b.command = "command"
        super(JobDescrBuilder, self).prepare(**params)


class JobScheduleBuilder(ObjectBuilder):
    _class = JobSchedule

    def prepare(self, **params):
        self.b.name = "name"
        self.b.crontab = "* * * * *"
        super(JobScheduleBuilder, self).prepare(**params)


class PromoCodeBuilder(ObjectBuilder):
    _class = mapper.PromoCode


class PromoCodeGroupBuilder(ObjectBuilder):
    _class = None

    def prepare(self, **params):
        calc_class_name = params.pop('calc_class_name', 'FixedDiscountPromoCodeGroup')
        self._class = getattr(mapper, calc_class_name)
        self.b.calc_class_name = calc_class_name
        self.b.service_ids = params.get('service_ids', [7])
        self.b._product_ids = params.pop('product_ids', None)

        promocode_info_list = params.pop(
            'promocode_info_list',
            [{'code': str(generate_numeric_string(16)), 'client_id': None}]
        )
        self.b.promocodes = [
            PromoCodeBuilder(code=promocode_info['code'], client_id=promocode_info['client_id'])
            for promocode_info in promocode_info_list
        ]

        event = params.get('event', None)
        if 'event_name' in params:
            event = mapper.PromoCodeEvent(event=params['event_name'])
        self.b.event = event

        self.b.start_dt = datetime.datetime.now() - datetime.timedelta(days=1)
        self.b.end_dt = datetime.datetime.now() + datetime.timedelta(days=7)
        self.b.firm_id = FirmId.YANDEX_OOO

        calc_params = params.pop('calc_params', {u"discount_pct": u"66"})
        if 'apply_on_create' not in calc_params:
            calc_params['apply_on_create'] = params.pop('apply_on_create', False)
        self.b.calc_params = calc_params

        super(PromoCodeGroupBuilder, self).prepare(**params)


class BankBuilder(ObjectBuilder):
    _class = mapper.Bank
    _generate_id = True

    def prepare(self, **params):
        self.b.name = 'Test Bank'
        self.b.bik = '012345678'
        self.b.city = u'Москва'
        self.b.hidden = 0
        super(BankBuilder, self).prepare(**params)


class BankIntBuilder(ObjectBuilder):
    _class = mapper.BankIntClass

    def prepare(self, **params):
        self.b.bictypeint = params.pop('bictypeint', 'SWIFT')
        self.b.bicint = params.pop('bicint', '0123456789A')
        self.b.name = 'Test Bank'
        self.b.rplstatus = 1
        self.b.client = 0
        self.b.update_dt = datetime.datetime.now()
        super(BankIntBuilder, self).prepare(**params)


class ExportBuilder(ObjectBuilder):
    _class = mapper.Export


class CountryBuilder(ObjectBuilder):
    _class = mapper.Country

    def prepare(self, **params):
        self.b.region_id = get_big_number()
        self.b.region_name = u'Новое Кукуево'
        self.b.region_name_en = u'New Kukuevo'
        super(CountryBuilder, self).prepare(**params)


class PersonCategoryBuilder(ObjectBuilder):
    _class = mapper.PersonCategory

    def prepare(self, **params):
        self.b.country = params.pop('country', Getter(mapper.Country, 225))
        self.b.ur = params.pop('ur', 1)
        self.b.resident = params.pop('resident', 1)
        self.b.is_default = params.pop('is_default', 1)
        self.b.auto_only = 0
        self.b.category = '%s_%s' % (params.get('cc', 'cat'), get_big_number())

        from balance.person import mandatory_fields
        mandatory_fields[self.b.category] = tuple()

        self.b.oebs_country_code = params.pop('oebs_country_code', 66)  # 6
        super(PersonCategoryBuilder, self).prepare(**params)


class FirmBuilder(ObjectBuilder):
    _class = mapper.Firm

    def prepare(self, **params):
        self.b.id = random.randint(2000, 9999)
        self.b.email = 'test@balance.yandex'
        self.b.payment_invoice_email = 'test@balance.yandex'
        self.b.phone = '-6(666)666-66-66'
        self.b.country = params.pop('country', Getter(mapper.Country, 225))
        self.b.default_iso_currency = 'RUB'
        super(FirmBuilder, self).prepare(**params)


class FirmInterbranchBuilder(ObjectBuilder):
    _class = mapper.FirmInterbranch

    def prepare(self, **params):
        self.b.id = get_big_number()
        self.b.firm = params.get('firm') or FirmBuilder()
        self.b.root_firm = params.get('root_firm') or FirmBuilder()
        self.b.contract = params.get('contract') or ContractBuilder()
        self.b.invoice_paysys_id = PAYSYS_ID
        super(FirmInterbranchBuilder, self).prepare(**params)


class YadocFirmBuilder(ObjectBuilder):
    _class = mapper.YadocFirm

    def prepare(self, **params):
        if not params.get('firm'):
            params.setdefault('firm_id', FirmId.YANDEX_OOO)
        params.setdefault('last_closed_dt', None)
        super(YadocFirmBuilder, self).prepare(**params)


class TaxPolicyPctBuilder(ObjectBuilder):
    _class = mapper.TaxPolicyPct

    def prepare(self, **params):
        self.b.id = get_big_number()
        self.b.hidden = 0
        super(TaxPolicyPctBuilder, self).prepare(**params)


class TaxPolicyBuilder(ObjectBuilder):
    _class = mapper.TaxPolicy

    def prepare(self, **params):
        self.b.id = get_big_number()
        self.b.name = 'some_tax'
        self.b.resident = params.pop('resident', 1)
        self.b.region_id = params.pop('region_id', 225)
        self.b.default_tax = 1
        self.b.hidden = 0

        tax_pcts = params.pop('tax_pcts', [18])

        def _cr_tax_pct(pct_param):
            pct_param = _mk_list(pct_param)

            if len(pct_param) == 1:
                nds_pct, = pct_param
                nsp_pct = 0
                dt = datetime.datetime(2000, 1, 1)
            elif len(pct_param) == 2:
                dt, nds_pct = pct_param
                nsp_pct = 0
            else:
                dt, nds_pct, nsp_pct = pct_param

            return TaxPolicyPctBuilder(
                tax_policy_id=self.b.id,
                dt=dt,
                nds_pct=nds_pct,
                nsp_pct=nsp_pct
            )

        self.b.taxes = map(_cr_tax_pct, tax_pcts)

        super(TaxPolicyBuilder, self).prepare(**params)


class FiasCity(ObjectBuilder):
    _class = mapper.fias.FiasCity


class PaysysBuilder(ObjectBuilder):
    _class = mapper.Paysys
    _generate_id = True

    def prepare(self, category='ur', **kwargs):
        self.b.category = category
        super(PaysysBuilder, self).prepare(**kwargs)


class FiasBuilder(ObjectBuilder):
    _class = mapper.Fias

    KLADR_CODE_LENGTH = 11

    def prepare(self, with_kladr=True, parent_fias=None, formal_name='formal_name', short_name=u'к.',
                postcode='123456', **kwargs):
        self.b.guid = str(uuid.uuid4())
        self.b.obj_level = 1
        self.b.live_status = 1
        self.b.center_status = 0
        self.b.formal_name = formal_name
        self.b.short_name = short_name
        self.b.postcode = postcode
        if with_kladr:
            self.b.kladr_code = generate_numeric_string(length=FiasBuilder.KLADR_CODE_LENGTH)
        if parent_fias:
            self.b.parent_guid = parent_fias.guid
        super(FiasBuilder, self).prepare(**kwargs)


class CurrencyBuilder(ObjectBuilder):
    _class = mapper.Currency

    def prepare(self, **kwargs):
        self.b.char_code = generate_character_string(length=4).upper()
        self.b.iso_code = generate_character_string(length=4).upper()
        self.b.num_code = get_big_number()
        self.b.weight = RANDOM.randint(1, 200)
        super(CurrencyBuilder, self).prepare(**kwargs)


class IsoCurrencyBuilder(ObjectBuilder):
    _class = mapper.IsoCurrency

    def prepare(self, **kwargs):
        self.b.alpha_code = generate_character_string(length=3).upper()
        super(IsoCurrencyBuilder, self).prepare(**kwargs)


class CurrencyRateBuilder(ObjectBuilder):
    _class = mapper.CurrencyRate


class DistributionTagBuilder(ObjectBuilder):
    _class = mapper.DistributionTag

    def prepare(self, client_id, tag_id=None, name='test_tag'):
        self.b.client_id = client_id
        self.b.name = name
        self.b.id = tag_id

    def build(self, session):
        if self.b.id is None:
            self.b.id = session.execute(
                "SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual"
            ).fetchone()['tag_id']
        return super(DistributionTagBuilder, self).build(session)


class PageDataBuilder(ObjectBuilder):
    _class = mapper.PageData

    def prepare(self, page_id=None, dt=None, desc=None, nds=None, **kwargs):
        self.b.page_id = page_id
        self.b.dt = dt or datetime.datetime.now()
        self.b.desc = desc or 'test page data'
        self.b.nds = nds or 0

    def build(self, session):
        if self.b.page_id is None:
            self.b.page_id = session.execute(
                "select max(page_id)+1 as page_id from T_PAGE_DATA"
            ).fetchone()['page_id']
        return super(PageDataBuilder, self).build(session)


class VerificationCodeBuilder(ObjectBuilder):
    _class = mapper.VerificationCode

    def prepare(self, **params):
        self.b.dt = datetime.datetime.now()
        super(VerificationCodeBuilder, self).prepare(**params)


class OverdraftParamsBuilder(ObjectBuilder):
    _class = mapper.OverdraftParams
    _generate_id = True

    def prepare(self, **params):
        client = ClientBuilder()
        self.b.client = client
        self.b.service_id = ServiceId.DIRECT
        self.b.person = PersonBuilder(client=client, type='ph')
        self.b.payment_method_cc = 'card'
        self.b.iso_currency = 'RUB'
        self.b.client_limit = 100
        super(OverdraftParamsBuilder, self).prepare(**params)


class ClientCashbackBuilder(ObjectBuilder):
    _class = mapper.ClientCashback

    def prepare(self, **params):
        if 'client_id' in params:
            self.b.client_id = params['client_id']
        else:
            self.b.client = params.pop('client', ClientBuilder())
        self.b.service_id = params.pop('service_id', ServiceId.DIRECT)
        self.b.iso_currency = params.pop('iso_currency', 'RUB')
        self.b.bonus = params.pop('bonus', D('1'))
        self.b.start_dt = params.pop('start_dt', None)
        self.b.finish_dt = params.pop('finish_dt', None)
        super(ClientCashbackBuilder, self).prepare(**params)


class CashbackUsageBuilder(ObjectBuilder):
    _class = mapper.CashbackUsage


class CashbackSettingsBuilder(ObjectBuilder):
    _class = mapper.ClientCashbackSettings


class PaymentBankBuilder(ObjectBuilder):
    _class = mapper.PaymentBank
    _generate_id = True

    def prepare(self, **params):
        self.b.name = 'ЗАО "Банка"'
        super(PaymentBankBuilder, self).prepare(**params)


class BankDetailsBuilder(ObjectBuilder):
    _class = mapper.BankDetails
    _generate_id = True

    def prepare(self, **params):
        self.b.payment_bank = PaymentBankBuilder()
        self.b.bank = params.pop('bank', 'ЗАО БАНК "Мамой клянусь"')
        self.b.iso_currency = 'RUR'
        super(BankDetailsBuilder, self).prepare(**params)


class ClientBankBuilder(ObjectBuilder):
    _class = mapper.ClientBank
    _generate_id = True

    def prepare(self, **params):
        self.b.is_alien_repr = params.pop('is_alien_repr', False)
        self.b.currency_code = params.pop('currency_code', 'RUR')
        self.b.iso_currency = mapper.Currency.fix_iso_code(self.b.currency_code)
        super(ClientBankBuilder, self).prepare(**params)


class TrustPaymentBuilder(ObjectBuilder):
    _class = payments.TrustPayment

    def prepare(self, **params):
        invoice = params.get('invoice', None)
        if not invoice:
            params['invoice'] = InvoiceBuilder()
        super(TrustPaymentBuilder, self).prepare(**params)


class SidePaymentBuilder(ObjectBuilder):
    _class = mapper.SidePayment

    def prepare(self, **params):
        if 'transaction_dt' not in params:
            self.b.transaction_dt = datetime.datetime.now()
        super(SidePaymentBuilder, self).prepare(**params)


class FiscalReceiptBuilder(ObjectBuilder):
    _class = mapper.FiscalReceipt

    def prepare(self, **params):
        params.setdefault('fiscal_receipt', {
            'dt': datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'id': str(generate_int(6)),
            'fp': str(generate_int(10)),
            'fn': {
                'sn': str(generate_int(16))
            },
            'kkt': {
                'rn': str(generate_int(13))
            },
            'receipt_content': {
                'receipt_type': params.pop('receipt_type', 'return_income')
            },
            'receipt_calculated_content': {
                'total': str(generate_int(3))
            },
            'email': generate_character_string(25),
            'invoice_id': str(generate_int(10)),
            'service_id': str(generate_int(10))
        })
        super(FiscalReceiptBuilder, self).prepare(**params)


class RoleBuilder(ObjectBuilder):
    _class = mapper.Role

    def prepare(self, **params):
        self.b.name = 'test_role_name'
        super(self.__class__, self).prepare(**params)


class RoleClientGroupBuilder(ObjectBuilder):
    _class = mapper.RoleClientGroup

    def prepare(self, **params):
        self._clients = params.pop('clients', [])
        super(RoleClientGroupBuilder, self).prepare(**params)

    def build(self, session):
        if self.b.get('client_batch_id') is None:
            self.b.client_batch_id = session.execute(sa.func.next_value(sa.Sequence('s_client_batch_id'))).scalar()

        if self.b.get('external_id') is None:
            self.b.external_id = get_big_number()

        refresh_dt = self.b.get('refresh_dt')
        for client in self._clients:
            params = {
                'client': client,
                'client_batch_id': self.b.client_batch_id,
            }
            if refresh_dt:
                params['update_dt'] = refresh_dt
            RoleClientBuilder.construct(session, **params)

        super(RoleClientGroupBuilder, self).build(session)
        return self


class RoleClientBuilder(ObjectBuilder):
    _class = mapper.RoleClient

    def prepare(self, **params):
        self._group_id = params.pop('group_id', None)

        if not (params.get('client') or params.get('client_id')):
            params['client'] = ClientBuilder()

        super(RoleClientBuilder, self).prepare(**params)

    def build(self, session):
        if self.b.get('client_batch_id') is None:
            if self._group_id:
                self.b.client_batch_id = RoleClientGroupBuilder.construct(session,
                                                                          external_id=self._group_id).client_batch_id
            else:
                self.b.client_batch_id = session.execute(sa.func.next_value(sa.Sequence('s_client_batch_id'))).scalar()

        super(RoleClientBuilder, self).build(session)
        return self


class RoleClientPassportBuilder(ObjectBuilder):
    _class = mapper.RoleClientPassport

    def prepare(self, **params):
        if not (params.get('role') or params.get('role_id')):
            self.b.role_id = RoleType.REPRESENTATIVE
        if not (params.get('client') or params.get('client_id')):
            self.b.client = ClientBuilder()
        if not (params.get('passport') or params.get('passport_id')):
            self.b.passport = PassportBuilder()
        super(RoleClientPassportBuilder, self).prepare(**params)


class TariffGroupBuilder(ObjectBuilder):
    _class = mapper.TariffGroup

    def prepare(self, **params):
        self.b.cc = 'test_tariff_group_cc'
        self.b.service_id = params.pop('service_id', ServiceId.APIKEYS)
        super(TariffGroupBuilder, self).prepare(**params)


class TariffBuilder(ObjectBuilder):
    _class = mapper.Tariff

    def prepare(self, **params):
        self.b.cc = 'test_tariff_cc'
        self.b.tariff_group = TariffGroupBuilder()
        super(TariffBuilder, self).prepare(**params)


class ProdSeasonCoeffBuilder(ObjectBuilder):
    _class = mapper.ProdSeasonCoeff


class PartnerBalanceCache(ObjectBuilder):
    _class = mapper.PartnerBalanceCache


class CartItemBuilder(ObjectBuilder):
    _class = mapper.CartItem

    def prepare(self, **params):
        client = params.get('client', None)
        client_id = params.get('client_id', None)
        if not (client or client_id):
            params['client'] = ClientBuilder()

        order = params.get('order', None)
        service_id = params.get('service_id', None)
        service_order_id = params.get('service_order_id', None)
        if not (order or (service_id and service_order_id)):
            params['order'] = OrderBuilder()

        params.setdefault('quantity', D('12.34'))

        super(CartItemBuilder, self).prepare(**params)


class ServiceNotifyParamsBuilder(ObjectBuilder):
    _class = mapper.ServiceNotifyParams

    def prepare(self, **params):
        from notifier.data_objects import JSON_REST

        service = params.get('service', None)
        service_id = params.get('service_id', None)
        if not (service or service_id):
            params['service'] = ServiceBuilder()

        url_scheme = params.pop('url_scheme', 'https')
        params.setdefault('url', '{}://service.yandex.net/api'.format(url_scheme))
        params.setdefault('test_url', '{}://service-dev.yandex.net/api'.format(url_scheme))

        params.setdefault('hidden', False)
        params.setdefault('version', 1)
        params.setdefault('protocol', JSON_REST)
        params.setdefault('iface_version', 3)
        params.setdefault('tvm_alias', generate_character_string(10))

        super(ServiceNotifyParamsBuilder, self).prepare(**params)


class RefundBuilder(ObjectBuilder):
    _class = mapper.Refund


class TerminalBuilder(ObjectBuilder):
    _class = mapper.Terminal
    _generate_id = True

    def prepare(self, **params):
        self.b.dt = datetime.datetime.now()
        self.b.payment_method_id = 1001
        self.b.currency = 'RUB'
        super(TerminalBuilder, self).prepare(**params)


class ProcessingBuilder(ObjectBuilder):
    _class = mapper.Processing
    _generate_id = True

    def prepare(self, **params):
        self.b.dt = datetime.datetime.now()
        self.b.cc = 'TEST'
        self.b.name = 'Test_Name'
        self.b.payment_method_id = 1000
        super(ProcessingBuilder, self).prepare(**params)


class InvoiceRefundBuilder(ObjectBuilder):
    _class = mapper.OEBSInvoiceRefund

    def prepare(self, **params):
        self.b.status_code = InvoiceRefundStatus.not_exported
        super(InvoiceRefundBuilder, self).prepare(**params)

    def postprocess(self):
        self.b.invoice.notify_unused_funds()


class TrustInvoiceRefundBuilder(ObjectBuilder):
    _class = mapper.TrustApiCPFInvoiceRefund

    def prepare(self, **params):
        self.b.status_code = InvoiceRefundStatus.uninitialized
        super(TrustInvoiceRefundBuilder, self).prepare(**params)


class InvoiceTransferBuilder(ObjectBuilder):
    _class = mapper.InvoiceTransfer

    def prepare(self, **params):
        self.b.status_code = InvoiceTransferStatus.not_exported
        super(InvoiceTransferBuilder, self).prepare(**params)


class NirvanaClonedWorkflowBuilder(ObjectBuilder):
    _class = mapper.NirvanaClonedWorkflow

    def prepare(self, **params):
        current_dt = datetime.datetime.now()
        self.b.dt = datetime.datetime(current_dt.year, current_dt.month, 1)
        self.b.mnclose_task = 'mnclose_task'
        self.b.original_id = 'workflow-instance-to-be-cloned'
        self.b.instance_id = 'cloned-instance-id'

        super(NirvanaClonedWorkflowBuilder, self).prepare(**params)


class NirvanaMnCloseSyncBuilder(ObjectBuilder):
    _class = mapper.NirvanaMnCloseSync

    def prepare(self, **kwargs):
        self.b.task_id = kwargs.pop('task_id', 'mnclose_task_id')
        current_dt = datetime.datetime.now()
        self.b.dt = kwargs.pop('dt', datetime.datetime(current_dt.year, current_dt.month, 1))
        self.b.status = kwargs.pop('status', NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE)
        self.b.changed_dt = current_dt

        super(NirvanaMnCloseSyncBuilder, self).prepare(**kwargs)


class ProductNameBuilder(ObjectBuilder):
    _class = mapper.ProductName

    def prepare(self, **params):
        params.setdefault('product', ProductBuilder())
        params.setdefault('lang_id', LanguageId.RU)
        if 'product_name' not in params:
            product_name = params['product'].name
            if isinstance(product_name, unicode):
                format_string = u'{}_{}'
            else:
                format_string = '{}_{}'
            params['product_name'] = format_string.format(product_name, params['lang_id'])
        super(ProductNameBuilder, self).prepare(**params)


class ThirdPartyTransactionBuilder(ObjectBuilder):
    _class = mapper.ThirdPartyTransaction
    _generate_id = True

    def prepare(self, **kwargs):
        current_dt = datetime.datetime.now()
        self.b.dt = datetime.datetime(current_dt.year, current_dt.month, 1)
        self.b.contract = kwargs.get('contract')
        super(ThirdPartyTransactionBuilder, self).prepare(**kwargs)


class NirvanaBlockBuilder(ObjectBuilder):
    _class = mapper.NirvanaBlock

    def prepare(self, **kwargs):
        current_dt = datetime.datetime.now()
        self.b.dt = datetime.datetime(current_dt.year, current_dt.month, 1)
        self.b.operation = 'run_mnclose_task'
        self.b.instance_id = str(uuid.uuid4())
        self.b.terminate = 0
        self.b.request = kwargs.pop('request', {'data': {'options': {}}})
        self.b.response = {}
        self.b.status = self._class.Status.RUNNING
        self.b.pid = os.getpid()

        super(NirvanaBlockBuilder, self).prepare(**kwargs)

    def add_input(self, name, data_type='text', download_url=None):
        inputs = self.b.request['data'].setdefault('inputs', dict())
        input_ = inputs.setdefault(name, dict(name=name, type='INPUT', items=list()))

        input_items = input_['items']
        current_index = len(input_items)

        if download_url is None:
            download_url = 'http://localhost/nirvana/api/storage/{}/data'.format(current_index)

        input_['items'].append({
            'dataType': data_type,
            'downloadURL': download_url,
            'fileName': '{}_{}'.format(current_index, name),
        })
        return self

    def add_output(self, name, data_type='text', upload_url=None):
        outputs = self.b.request['data'].setdefault('outputs', dict())
        output = outputs.setdefault(name, dict(name=name, type='OUTPUT', items=list()))

        output_items = output['items']
        current_index = len(output_items)

        if upload_url is None:
            upload_url = 'mds-s3://localhost/s3/nirvana/{}'.format(current_index)

        output['items'].append({
            'dataType': data_type,
            'uri': upload_url,
            'fileName': '{}_{}'.format(current_index, name)
        })
        return self


class ReportBuilder(ObjectBuilder):
    _class = mapper.Report

    def prepare(self, **kwargs):
        self.b.request_dt = datetime.datetime.now()
        self.b.create_dt = datetime.datetime.now()
        self.b.params = {}
        super(ReportBuilder, self).prepare(**kwargs)

    def build(self, session):
        if self.b.get('key') is None:
            self.b.key = ('unittest666/'
                          + str(session.execute(sa.sql.functions.next_value(sa.Sequence('s_mds_id'))).scalar())
                          + '.xls')

        if self.b.get('passport_id') is None:
            self.b.passport_id = PassportBuilder.construct(session).passport_id

        return super(ReportBuilder, self).build(session)


class TVMACLAppBuidler(ObjectBuilder):
    _class = mapper.TVMACLApp

    def prepare(self, **params):
        params.setdefault('env', 'test')
        super(TVMACLAppBuidler, self).prepare(**params)


class TVMACLAllowedServiceBuilder(ObjectBuilder):
    _class = mapper.TVMACLAllowedServiceTable


class TVMACLPermissionBuilder(ObjectBuilder):
    _class = mapper.TVMACLPermissionTable


class TVMACLGroupBuilder(ObjectBuilder):
    _class = mapper.TVMACLGroup


class TVMACLGroupMethodBuilder(ObjectBuilder):
    _class = mapper.TVMACLGroupMethod


class TVMACLGroupPermissionBuilder(ObjectBuilder):
    _class = mapper.TVMACLGroupPermission


class RestrictedDomainBuilder(ObjectBuilder):
    _class = mapper.RestrictedDomain


class RestrictedPersonParamBuilder(ObjectBuilder):
    _class = mapper.RestrictedPersonParams


def create_client_service_data(migrate_to_currency_dt=datetime.datetime.now(), currency='RUB',
                               currency_convert_type=CONVERT_TYPE_COPY, service_id=ServiceId.DIRECT):
    client_service_data = mapper.ClientServiceData(service_id)
    client_service_data.iso_currency = currency
    client_service_data.migrate_to_currency = migrate_to_currency_dt
    client_service_data.convert_type = currency_convert_type
    return client_service_data


def create_pay_policy_service(session, service_id, firm_id, paymethods_params=None, category=None, legal_entity=None,
                              is_atypical=False,
                              **kwargs):
    pay_policy_service_id = get_big_number()
    session.execute('''insert into bo.t_pay_policy_service (id, service_id, firm_id, legal_entity, category, is_atypical)
                       values (:id, :service_id, :firm_id, :legal_entity, :category, :is_atypical)''',
                    {'id': pay_policy_service_id, 'service_id': service_id, 'firm_id': firm_id,
                     'legal_entity': legal_entity, 'category': category, 'is_atypical': int(is_atypical)})
    if paymethods_params:
        for currency, pm_id in paymethods_params:
            create_pay_policy_payment_method(session, pay_policy_service_id=pay_policy_service_id, iso_currency=currency,
                                             payment_method_id=pm_id, hidden=0, **kwargs)
    session.flush()
    return pay_policy_service_id


def create_pay_policy_payment_method(session, pay_policy_service_id, iso_currency, payment_method_id, paysys_group_id=0,
                                     hidden=0, **kwargs):
    payment_method = Getter(mapper.PaymentMethod, payment_method_id).build(session).obj
    pay_policy_payment_method_id = get_big_number()
    pp = mapper.PayPolicyPaymentMethod(id=pay_policy_payment_method_id, pay_policy_service_id=pay_policy_service_id,
                                       iso_currency=iso_currency, payment_method=payment_method,
                                       paysys_group_id=paysys_group_id, hidden=hidden)
    session.add(pp)
    session.flush()
    return pp


def create_pay_policy_region(session, pay_policy_service_id, region_id=None, region_group_id=None, is_contract=None,
                             is_agency=None, hidden=0, **kwargs):
    if is_contract is not None:
        is_contract = int(is_contract)
    if is_agency is not None:
        is_agency = int(is_agency)
    assert region_id is None or region_group_id is None
    pay_policy_region_id = get_big_number()
    if region_group_id:
        session.execute('''
        insert into bo.t_pay_policy_region (id, region_group_id, is_agency, is_contract, pay_policy_service_id, hidden)
        values (:id, :region_group_id, :is_agency, :is_contract, :pay_policy_service_id, :hidden)
        ''', {'id': pay_policy_region_id, 'pay_policy_service_id': pay_policy_service_id,
              'region_group_id': region_group_id, 'is_contract': is_contract, 'is_agency': is_agency, 'hidden': 0})
    if region_id:
        session.execute('''
        insert into bo.t_pay_policy_region (id, region_id, is_agency, is_contract, pay_policy_service_id, hidden)
        values (:id, :region_id, :is_agency, :is_contract, :pay_policy_service_id, :hidden)
        ''', {'id': pay_policy_region_id, 'pay_policy_service_id': pay_policy_service_id, 'region_id': region_id,
              'is_contract': is_contract, 'is_agency': is_agency, 'hidden': hidden})


def create_pay_policy_region_group(session, region_group_id, regions):
    region_group_name = generate_character_string()
    session.execute('''
            insert into bo.t_pay_policy_region_group_name (region_group_id, region_group_name)
            values (:region_group_id, :region_group_name)
            ''', {'region_group_id': region_group_id, 'region_group_name': region_group_name})
    for region in regions:
        pay_policy_region_group_id = get_big_number()
        session.execute('''
        insert into bo.t_pay_policy_region_group (id, region_group_id, region_id)
        values (:id, :region_group_id, :region_id)
        ''', {'id': pay_policy_region_group_id, 'region_group_id': region_group_id, 'region_id': region})


def create_permission(session, perm_code):
    permission = mapper.Permission(
        code=perm_code,
    )
    session.add(permission)
    session.flush()
    return permission


def create_role(session, *permissions, **kwargs):
    role = mapper.Role(
        id=get_big_number(),
        name=kwargs.get('name', str(get_big_number()))
    )
    session.add(role)
    if any(permissions):
        for perm_info in permissions:
            if not isinstance(perm_info, basestring) and isinstance(perm_info, collections.Iterable):
                perm_code, constraints = perm_info
            else:
                perm_code, constraints = perm_info, None

            if isinstance(perm_code, basestring):
                perm = create_permission(session, perm_code)
            else:
                perm = perm_code

            role_perm = mapper.RolePermission(
                role=role,
                permission=perm,
                constraints=constraints
            )
            session.add(role_perm)
    session.flush()
    return role


def get_domain_uid_value():
    return random.randint(INTRA_MIN(), INTRA_MAX())


def create_passport(session, *roles, **kwargs):
    patch_session = kwargs.get('patch_session', False)
    passport = PassportBuilder(**kwargs).build(session).obj
    for role_info in roles:
        if role_info is None:
            continue

        if not isinstance(role_info, collections.Iterable):
            role_info = (role_info,)

        role, firm_id, client_batch_id = (role_info + (None, None))[:3]

        passport_role = mapper.RealRolePassport(
            session,
            passport=passport,
            role=role,
            firm_id=firm_id,
            client_batch_id=client_batch_id,
        )
        session.add(passport_role)

    session.flush()
    if patch_session:
        session.oper_id = passport.passport_id
        try:
            del session.oper_perms
        except AttributeError:
            pass
        session._passport = passport
    return passport


def create_passport_manager(passport):
    return SingleManagerBuilder(
        passport_id=passport.passport_id,
        domain_login=passport.login
    ).build(passport.session).obj


def create_credit_contract(session, client=None, person=None, **kwargs):
    pc = PayOnCreditCase(session)
    client = client or ClientBuilder(is_agency=True)
    person = person or PersonBuilder(client=client, type='ur')

    params = dict(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        commission=ContractTypeId.COMMISSION,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=52,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=1,
        firm=1,
    )
    params.update(kwargs)
    return pc.get_contract(client=client, person=person, **params)


def create_correction_payment(invoice):
    invoice.session.execute(
        'insert into bo.t_correction_payment (dt, doc_date, sum, memo, invoice_eid) '
        'VALUES (to_date(:paymentDate,\'DD.MM.YYYY HH24:MI:SS\'),'
        'to_date(:paymentDate,\'DD.MM.YYYY HH24:MI:SS\'),'
        ':total_sum,'
        '\'Testing\','
        ':external_id)',
        {'paymentDate': invoice.dt,
         'total_sum': invoice.total_sum.as_decimal(),
         'external_id': invoice.external_id})


def create_comsn_type_discount_types(session, commission_type_id, discount_type_ids):
    for discount_type_id in discount_type_ids:
        session.execute(
            """insert into bo.t_comsn_type_discount_types (contract_comsn_type_id, discount_type_id)
            VALUES (:commission_type_id, :discount_type_id)""",
            {'commission_type_id': commission_type_id,
             'discount_type_id': discount_type_id})


def create_contract_commission_type(session):
    seq = session.execute(sa.Sequence('S_CONTRACT_COMSN_TYPE_ID'))
    session.execute(
        """insert into bo.T_CONTRACT_COMSN_TYPE (id, name)
        VALUES (:id, :name)""",
        {'name': 'comsn_name' + str(get_big_number()),
         'id': seq})
    return seq


def set_roles(session, passport=None, roles=None):
    roles = roles or []
    passport = passport or session.passport

    with mock.patch('butils.passport.passport_admsubscribe'):  # не ходим в апи паспорта
        passport.set_roles(roles)

    # Clean up permissions cache
    def delattr_safe(o, name):
        if hasattr(o, name):
            delattr(o, name)

    delattr_safe(passport, '_perms_cache')
    delattr_safe(session, 'oper_perms')


def set_repr_client(session, passport, client):
    RoleClientPassportBuilder.construct(
        session,
        passport=passport,
        client=client,
    )
    session.flush()


class BadDebtActBuilder(ObjectBuilder):
    _class = mapper.BadDebtAct

    def prepare(self, **kwargs):
        self.b.act = kwargs['act']
        self.b.oper_uid = None
        self.b.commentary = None
        self.b.our_fault = False

        super(BadDebtActBuilder, self).prepare(**kwargs)


class YTLogLoadTypeBuilder(ObjectBuilder):
    _class = mapper.YtLogLoadType
    _generate_id = True

    def prepare(self, **kwargs):
        self.b.cc = kwargs.pop('cc', generate_character_string())
        self.b.table_name = kwargs.pop('table_name', 'bo.t_log_tariff_act_row')
        self.b.partition_col_name = kwargs.pop('partition_col_name', 'log_type_id')
        super(YTLogLoadTypeBuilder, self).prepare(**kwargs)


class YTLogLoadTaskBuilder(ObjectBuilder):
    _class = mapper.YtLogLoadTask

    def prepare(self, **kwargs):
        self.b.external_id = generate_character_string()
        self.b.task_type = kwargs.pop('task_type', YTLogLoadTypeBuilder())
        self.b.state = kwargs.pop('state', NirvanaProcessingTaskState.IN_PROGRESS)
        self.b.cluster_name = kwargs.pop('cluster_name', 'some_cluster')
        self.b.table_path = kwargs.pop('table_path', 'some_table')
        super(YTLogLoadTaskBuilder, self).prepare(**kwargs)


class LogTariffTypeBuilder(ObjectBuilder):
    _class = mapper.LogTariffType

    def prepare(self, **kwargs):
        self.b.id = kwargs.pop('id', generate_character_string())
        super(LogTariffTypeBuilder, self).prepare(**kwargs)


class LogTariffTaskBuilder(ObjectBuilder):
    _class = mapper.LogTariffTask

    def prepare(self, **kwargs):
        self.b.task_type = kwargs.pop('task_type', LogTariffTypeBuilder())
        self.b.metadata = kwargs.pop('metadata', {})
        self.b.state = NirvanaProcessingTaskState.IN_PROGRESS
        super(LogTariffTaskBuilder, self).prepare(**kwargs)


class LogTariffOrderBuilder(ObjectBuilder):
    _class = mapper.LogTariffOrder

    def prepare(self, **kwargs):
        self.b.task = kwargs.pop('task', None) or LogTariffTaskBuilder()
        super(LogTariffOrderBuilder, self).prepare(**kwargs)


class LogTariffConsumeBuilder(ObjectBuilder):
    _class = mapper.LogTariffConsume

    def prepare(self, **kwargs):
        self.b.consume_qty = kwargs.get('qty')
        self.b.consume_sum = kwargs.get('sum')
        super(LogTariffConsumeBuilder, self).prepare(**kwargs)


class LogTariffMigrationOrderLoadBuilder(ObjectBuilder):
    _class = mapper.LogTariffMigrationOrderLoad


class LogTariffMigrationOrderBuilder(ObjectBuilder):
    _class = mapper.LogTariffMigrationOrder

    def prepare(self, order, **kwargs):
        self.b.task = kwargs.pop('task', LogTariffTaskBuilder())
        self.b.service_id = order.service_id
        self.b.service_order_id = order.service_order_id
        super(LogTariffMigrationOrderBuilder, self).prepare(**kwargs)


class LogTariffMigrationInputBuilder(ObjectBuilder):
    _class = mapper.LogTariffMigrationInput


class LogTariffMigrationUntariffedBuilder(ObjectBuilder):
    _class = mapper.LogTariffMigrationUntariffed

    def prepare(self, order, **kwargs):
        self.b.task_id = kwargs.pop('task', LogTariffTaskBuilder()).id
        migration_order = kwargs.pop('migration_order', LogTariffMigrationOrderBuilder(order=order))
        self.b.migration_order = migration_order
        self.b.service_id = order.service_id
        self.b.service_order_id = order.service_order_id
        self.b.tariff_dt = kwargs.pop('tariff_dt', datetime.datetime.now())
        self.b.product_id = order.service_code
        self.b.overcompletion_qty = kwargs.pop('overcompletion_qty', 0)
        super(LogTariffMigrationUntariffedBuilder, self).prepare(**kwargs)


class LogTariffMigrationConsumeBuilder(ObjectBuilder):
    _class = mapper.LogTariffMigrationConsume

    def prepare(self, consume, **kwargs):
        self.b.task_id = kwargs.pop('task', LogTariffTaskBuilder()).id
        self.b.consume_id = consume.id
        order = consume.order
        self.b.service_id = order.service_id
        self.b.service_order_id = order.service_order_id
        self.b.migration_order = kwargs.pop('migration_order', LogTariffMigrationOrderBuilder(order=order))
        self.b.tariff_dt = kwargs.pop('tariff_dt', datetime.datetime.now())
        self.b.qty = kwargs.pop('qty', 0)
        self.b.sum = kwargs.pop('sum', 0)
        self.b.consume_qty = kwargs.pop('consume_qty', 0)
        self.b.consume_sum = kwargs.pop('consume_sum', 0)
        super(LogTariffMigrationConsumeBuilder, self).prepare(**kwargs)


class DailyActRequestBuilder(ObjectBuilder):
    _class = mapper.DailyActRequest


class ContractPrintFormRuleBuilder(ObjectBuilder):
    _class = mapper.ContractPrintFormRules


class FPSBankBuilder(ObjectBuilder):
    _class = mapper.FPSBank


class IsoCurrencyRateBuilder(ObjectBuilder):
    _class = mapper.IsoCurrencyRate

    def prepare(self, **kwargs):
        self.b.id = kwargs.pop('rate_id')
        self.b.src_cc = kwargs.pop('src_cc')
        self.b.dt = kwargs.pop('dt')
        self.b.iso_currency_from = kwargs.pop('iso_currency_from')
        self.b.rate_from = kwargs.pop('rate_from')
        self.b.iso_currency_to = kwargs.pop('iso_currency_to')
        self.b.rate_to = kwargs.pop('rate_to')
        super(IsoCurrencyRateBuilder, self).prepare(**kwargs)


class ReconciliationRequestBuilder(ObjectBuilder):
    _class = mapper.ReconciliationRequest

    def prepare(self, **kwargs):
        now = datetime.datetime.now()
        self.b.external_id = kwargs.pop('external_id', 'super-reconciliation-id-%s' % get_big_number())
        self.b.dt_from = ut.trunc_date(now - datetime.timedelta(days=30))
        self.b.dt_to = ut.trunc_date(now)
        self.b.dt = now
        self.b.status = 'NEW'
        super(ReconciliationRequestBuilder, self).prepare(**kwargs)

    def build(self, session):
        if self.b.get('client') is None:
            self.b.client = ClientBuilder.construct(session)

        if self.b.get('person') is None:
            self.b.person = PersonBuilder.construct(session, client=self.b.client, type='ur')

        if (self.b.get('firm') or self.b.get('firm_id')) is None:
            firm = FirmBuilder.construct(session)
            oebs_org_id = get_big_number()
            session.execute(
                '''insert into bo.t_firm_export (firm_id, export_type, oebs_org_id) values (:firm_id, 'OEBS', :oebs_org_id)''',
                {'firm_id': firm.id, 'oebs_org_id': oebs_org_id},
            )
            self.b.firm = firm

        return super(ReconciliationRequestBuilder, self).build(session)


class NirvanaTaskTypeBuilder(ObjectBuilder):
    _class = mapper.NirvanaTaskType

    def prepare(self, **kwargs):
        self.b.id = kwargs.pop('id', generate_character_string())
        super(NirvanaTaskTypeBuilder, self).prepare(**kwargs)


class NirvanaTaskBuilder(ObjectBuilder):
    _class = mapper.NirvanaTask

    def prepare(self, **kwargs):
        self.b.task_type = kwargs.pop('task_type', NirvanaTaskTypeBuilder())
        self.b.metadata = kwargs.pop('metadata', None)
        self.b.state = NirvanaProcessingTaskState.NEW
        super(NirvanaTaskBuilder, self).prepare(**kwargs)


class NirvanaTaskItemBuilder(ObjectBuilder):
    _class = mapper.NirvanaTaskItem

    def prepare(self, **kwargs):
        self.b.task = kwargs.pop('task', NirvanaTaskBuilder())
        self.b.metadata = kwargs.pop('metadata', None)
        self.b.output = kwargs.pop('output', None)
        self.b.processed = 0
        super(NirvanaTaskItemBuilder, self).prepare(**kwargs)


def create_brand(session, dt2clients, finish_dt=None, brand_type=7):
    from_dt, clients = dt2clients[0]
    main_client = next(iter(clients))
    contract = ContractBuilder(
        client=main_client,
        person=None,
        commission=ContractTypeId.ADVERTISING_BRAND,
        firm=FirmId.YANDEX_OOO,
        dt=ut.trunc_date(from_dt),
        payment_type=None,
        brand_type=brand_type,
        brand_clients={cl.id: 1 for cl in clients}
    ).build(session).obj

    col_type = mapper.contract_meta.collateral_types['GENERAL'][1026]
    col = contract.col0
    for from_dt, clients in dt2clients[1:]:
        col = contract.append_collateral(
            ut.trunc_date(from_dt),
            col_type,
            is_signed=from_dt,
            brand_clients={cl.id: 1 for cl in clients}
        )
    col.finish_dt = finish_dt and ut.trunc_date(finish_dt)
    session.flush()

    return contract


def add_dynamic_discount(consume, dynamic_discount_pct):
    consume.consume_qty = ut.round(ut.radd_percent(consume.consume_qty, -dynamic_discount_pct), 6)
    delta_qty = ut.round(ut.radd_percent(consume.current_qty, -dynamic_discount_pct), 6) - consume.current_qty
    consume.current_qty += delta_qty
    consume.order.consume_qty += delta_qty
    consume.order.dynamic_bonus_qty = delta_qty

    consume.completion_sum = ut.round00(ut.add_percent(consume.completion_qty * 30, -dynamic_discount_pct))
    consume.discount_pct = ut.round00(ut.mul_discounts(consume.discount_pct, dynamic_discount_pct))
    consume.session.flush()


@contextlib.contextmanager
def patched_currency(dts_rates):
    def _extract_dt_rate(dt_rates):
        if isinstance(dt_rates, (list, tuple)):
            from_dt, rates = dt_rates
        else:
            from_dt = datetime.datetime(1980, 1, 1)
            rates = dt_rates
        return from_dt, rates

    def _patched(session, iso_cc, dat, *args, **kwargs):
        prev_dt, prev_rates = _extract_dt_rate(dts_rates[0])
        for dt_rates in dts_rates[1:] + [(datetime.datetime(3000, 1, 1), {})]:
            cur_dt, rates = _extract_dt_rate(dt_rates)
            if prev_dt <= dat < cur_dt:
                return ut.Struct(rate=prev_rates[iso_cc])
            else:
                prev_dt, prev_rates = cur_dt, rates

    patcher_real = mock.patch(
        'balance.mapper.common.CurrencyRate.get_real_currency_rate_by_date',
        staticmethod(_patched)
    )
    patcher = mock.patch(
        'balance.mapper.common.CurrencyRate.get_currency_rate_by_date',
        staticmethod(_patched)
    )
    with patcher, patcher_real:
        yield


class NDSOperationCodeBuilder(ObjectBuilder):
    _class = mapper.NDSOperationCode
