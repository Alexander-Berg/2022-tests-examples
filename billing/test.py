# -*- coding: utf-8 -*-

from balance.printform.rules import *
from balance.printform.attribute import PrintFormAttributes as pf
import balance.constants as cst
import time
from balance import application


def reformat(dict_, pfattrs):
    attrs = set()
    context = dict_[0]
    for k, v in dict_[1].iteritems():
        attr_type = pfattrs.get_attr_type(k)
        if attr_type == 'SINGLE':
            attrs.add(str(context + ':' + k + ':' + str(v)))
        elif attr_type == 'BOOL':
            attrs.add(str(context + ':' + k + ':' + 'ON'))
        elif attr_type == 'MULTIPLE':
            for i in v:
                attrs.add(str(context + ':' + k + ':' + str(i)))
    return attrs


def test_1(pfattrs):
    c = ['c', {
        'COMMISSION': cst.ContractTypeId.NON_AGENCY,
        'FIRM': cst.FirmId.UBER_KZ,
        'PAYMENT_TYPE': cst.PREPAY_PAYMENT_TYPE,
        'COUNTRY': cst.RegionId.RUSSIA
    }]
    attrs = reformat(c, pfattrs)
    rule = Rule('Такси нерезиденты (KAZ), корпоративное, предоплата', [cst.ContractPrintTpl.TAXI_TAXINONR_CORP_KAZ_1])
    rule.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.UBER_KZ),
        Terminal('c', 'PAYMENT_TYPE', cst.PREPAY_PAYMENT_TYPE)
    ]))
    return rule.eval(attrs)


def test_2(pfattrs):
    c = ['c', {
        'COMMISSION': cst.ContractTypeId.NON_AGENCY,
        'FIRM': cst.FirmId.TAXI,
        'PAYMENT_TYPE': cst.POSTPAY_PAYMENT_TYPE,
        'COUNTRY': cst.RegionId.RUSSIA,
        'SERVICES': [cst.ServiceId.ONE_TIME_SALE, cst.ServiceId.TAXI_BRANDING]
    }]
    attrs = reformat(c, pfattrs)
    rule = Rule('Такси, расширенное сотрудничество', [cst.ContractPrintTpl.TAXI_EXTCOOP_2])
    rule.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.TAXI),
        Terminal('c', 'PAYMENT_TYPE', cst.POSTPAY_PAYMENT_TYPE),
        Terminal('c', 'SERVICES', cst.ServiceId.ONE_TIME_SALE),
        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_BRANDING)
    ]))
    return rule.eval(attrs)


def test_3(pfattrs):
    c = ['c', {
        'COMMISSION': cst.ContractTypeId.NON_AGENCY,
        'FIRM': cst.FirmId.MARKET,
        'PAYMENT_TYPE': cst.POSTPAY_PAYMENT_TYPE,
        'COUNTRY': cst.RegionId.RUSSIA,
        'SERVICES': [cst.ServiceId.ONE_TIME_SALE],
        'CREDIT_TYPE': cst.CreditType.PO_SROKU,
        'PAYMENT_TERM': 30,
        'PARTNER_CREDIT': True
    }]
    attrs = reformat(c, pfattrs)
    rule = Rule('Маркет, Бренд-зона', [cst.ContractPrintTpl.MARKET_BRAND_ZONE_1])
    rule.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.MARKET),
        Terminal('c', 'PAYMENT_TYPE', cst.POSTPAY_PAYMENT_TYPE),
        Terminal('c', 'SERVICES', cst.ServiceId.ONE_TIME_SALE),
        Terminal('c', 'CREDIT_TYPE', cst.CreditType.PO_SROKU),
        Terminal('c', 'PAYMENT_TERM', 30),
        Terminal('c', 'PARTNER_CREDIT', 'ON')
    ]))
    return rule.eval(attrs)


def test_4(pfattrs):  # тестируем движок в связке с печатными атрибутами, правило – выдуманное
    c = ['c', {
        'COMMISSION': list(pfattrs.get_attribute('COMMISSION').values)[0],
        'FIRM': list(pfattrs.get_attribute('FIRM').values)[0],
        'COUNTRY': cst.RegionId.RUSSIA,
        'SERVICES': [
            list(pfattrs.get_attribute('SERVICES').values)[0], list(pfattrs.get_attribute('SERVICES').values)[1]
        ],
        'PAYMENT_TERM': 30,
        'PARTNER_CREDIT': True,
        'FINISH_DT': '02-02-1998'
    }]
    attrs = reformat(c, pfattrs)
    rule = Rule('Выдуманное правило', ['Выдуманная ссылка'])
    rule.fill(Interleave([
        Terminal('c', 'COMMISSION', list(pfattrs.get_attribute('COMMISSION').values)[0]),
        Terminal('c', 'FIRM', list(pfattrs.get_attribute('FIRM').values)[0]),
        Terminal('c', 'SERVICES', list(pfattrs.get_attribute('SERVICES').values)[0]),
        Terminal('c', 'SERVICES', list(pfattrs.get_attribute('SERVICES').values)[1]),
        Terminal('c', 'PAYMENT_TERM', 30),
        Terminal('c', 'PARTNER_CREDIT', 'ON'),
        Terminal('c', 'FINISH_DT', 'ON')
    ]))
    return rule.eval(attrs)


def get_print_template_items(c, rules_):
    print_template_items = []
    log = 'log: '

    for i in rules_:
        if i.eval(c):
            log += ('Rule {caption} - OK, '.format(caption=i.caption))
            print_template_items.extend(i.print_forms)
        else:
            log += ('Rule {caption} - FAIL, '.format(caption=i.caption))

    return [log, print_template_items]


def test_5(pfattrs):
    rule_1 = Rule('Единый договор', [cst.ContractPrintTpl.UNIFIED_CORP_CONTRACT])
    rule_1.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.TAXI),
        # MoreThanOne('c', 'SERVICES', cst.UNIFIED_SERVICES_EXCEPT_TAXI_CORP)
        Alternation([
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS),
                        Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP)]),
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP),
                        Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B)]),
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B),
                        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS)])
        ])
    ]))

    rule_2 = Rule('Корпоративное такси, постоплата', [cst.ContractPrintTpl.TAXI_CORP_CUST_1,
                                                       cst.ContractPrintTpl.TAXI_CORP_CUST_4,
                                                       cst.ContractPrintTpl.TAXI_CORP_POSTPAY_NEW_ITEM])
    rule_2.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.TAXI),
        Terminal('c', 'PAYMENT_TYPE', cst.POSTPAY_PAYMENT_TYPE),
        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP),
        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS),
        # OneOf('SERVICES', cst.UNIFIED_SERVICES_EXCEPT_TAXI_CORP),
        Not(Alternation([Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP),
                         Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B)]))
    ]))

    rule_3 = Rule('Корпоративное такси, предоплата', [cst.ContractPrintTpl.TAXI_CORP_CUST_2,
                                                       cst.ContractPrintTpl.TAXI_CORP_PREPAY_NEW_ITEM])
    rule_3.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.TAXI),
        Terminal('c', 'PAYMENT_TYPE', cst.PREPAY_PAYMENT_TYPE),
        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP),
        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS),
        # OneOf('SERVICES', cst.UNIFIED_SERVICES_EXCEPT_TAXI_CORP),
        Not(Alternation([Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP),
                         Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B)]))
    ]))

    rules_ = [rule_1, rule_2, rule_3]
    c = ['c', {
        'COMMISSION': cst.ContractTypeId.NON_AGENCY,
        'FIRM': cst.FirmId.TAXI,
        'PAYMENT_TYPE': cst.PREPAY_PAYMENT_TYPE,
        'SERVICES': [cst.ServiceId.TAXI_CORP, cst.ServiceId.TAXI_CORP_CLIENTS]
    }]
    return get_print_template_items(reformat(c, pfattrs), rules_)


def test_6(pfattrs):
    start_time = time.time()
    for i in range(100000):
        test_5(pfattrs)
    return '--- %s seconds ---' % (time.time() - start_time)


def test_7(pfattrs):
    rule = Rule(u'Единый договор', [cst.ContractPrintTpl.UNIFIED_CORP_CONTRACT, cst.ContractPrintTpl.TAXI_CORP_CUST_1])
    rule.fill(Interleave([
        Terminal('c', 'COMMISSION', cst.ContractTypeId.NON_AGENCY),
        Terminal('c', 'FIRM', cst.FirmId.TAXI),
        Alternation([
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS),
                        Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP)]),
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.FOOD_CORP),
                        Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B)]),
            Interleave([Terminal('c', 'SERVICES', cst.ServiceId.DRIVE_B2B),
                        Terminal('c', 'SERVICES', cst.ServiceId.TAXI_CORP_CLIENTS)])
        ]),
        Not(Terminal('c', 'FIRM', cst.FirmId.MARKET))
    ]))
    frozen = rule.encode()
    copy_ = decode(frozen)
    return rule.caption == copy_.caption


def main():
    app = application.Application(database_id='balance')
    session = app.new_session()

    pfattrs = pf(session=session, contract_type='GENERAL')
    all_tests = ['test_1(pfattrs)', 'test_2(pfattrs)', 'test_3(pfattrs)', 'test_4(pfattrs)', 'test_5(pfattrs)',
                 'test_6(pfattrs)', 'test_7(pfattrs)']
    blocked_tests = ['test_4(pfattrs)']
    working_tests = sorted(set(all_tests).difference(set(blocked_tests)))
    ans = []
    for i in working_tests:
        test_ans = [i]
        exec 'test_ans.append({test})'.format(test=i)
        ans.append(test_ans)
    for i in ans:
        print i[0] + ': '
        if isinstance(i[1], list):
            for j in i[1]:
                print '\t',
                print j
        else:
            print '\t',
            print i[1]
        print '\n'
