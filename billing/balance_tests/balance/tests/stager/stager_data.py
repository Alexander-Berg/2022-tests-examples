# coding: utf-8

__author__ = 'a-vasin'

from dateutil.relativedelta import relativedelta as rd
from btestlib.constants import Services, StagerProject, Products, TransactionType
from stager_data_steps import *

MISSING_PROJECT_ID = 'missing_project_id'

INPUT_DATA = {
    StagerProject.EDA: (lambda c: {
        'billing_export_commissions': [
            make_eda_input(t_id=1, client_id=1, service_id=Services.FOOD_SERVICES.id, amount=123.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=1, client_id=1, service_id=Services.FOOD_SERVICES.id, amount=132.46, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            # сервис 9999 не учитывается
            make_eda_input(t_id=2, client_id=1, service_id=9999, amount=101.43, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),

            make_eda_input(t_id=3, client_id=2, service_id=Services.FOOD_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=4, client_id=2, service_id=Services.FOOD_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt='2019-07-15 10:55:42'),

            make_eda_input(t_id=666, client_id=666, service_id=Services.FOOD_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt='2019-07-15 10:55:42'),
            # строки для проверки агрегации по датам
            make_eda_input(t_id=667, client_id=3, service_id=Services.FOOD_SERVICES.id, amount=10.55, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=668, client_id=3, service_id=Services.FOOD_SERVICES.id, amount=-5.45, currency=Currencies.RUB, t_type="1", dt='2019-07-14 10:55:42'),

            # лавка
            make_eda_input(t_id=1001, client_id=1, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=123.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=1001, client_id=1, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=132.46, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),

            make_eda_input(t_id=1003, client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=1004, client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt='2019-07-15 10:55:42'),

            make_eda_input(t_id=1666, client_id=666, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=1667, client_id=3, service_id=Services.FOOD_PHARMACY_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="goods", dt='2019-07-15 10:55:42'),

            make_eda_input(t_id=2661, client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=1666.66, currency=Currencies.RUB, t_type="third_party_order_processing", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=2662, client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="third_party_order_processing", dt='2019-07-15 10:55:42'),
            make_eda_input(t_id=2663, client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="corporate", dt='2019-07-15 10:55:42'),
        ]
    }),

    StagerProject.DRIVE: (lambda c: {
        'aggregations': [
            # order_type should be from T_PARTNER_PRODUCT.ORDER_TYPE
            make_drive_input(order_type='carsharing', amount=42.42, currency=Currencies.RUB, promo_amount=10.41),
            make_drive_input(order_type='carsharing', amount=45.43, currency=Currencies.RUB, promo_amount=33.12),
            make_drive_input(order_type='carsharing', amount=45.44, currency=Currencies.USD, promo_amount=32.12),

            make_drive_input(order_type='fine', amount=12.31, currency=Currencies.RUB, promo_amount=1.32),
            make_drive_input(order_type='fine', amount=42.33, currency=Currencies.USD, promo_amount=5.65),

            make_drive_input(order_type='toll_road', amount=112, currency=Currencies.RUB, promo_amount=0),
            make_drive_input(order_type='toll_road', amount=112, currency=Currencies.RUB, promo_amount=0),
            make_drive_input(order_type='toll_road', amount=112, currency=Currencies.RUB, promo_amount=0),
        ]
    }),

    StagerProject.CLOUD: (lambda c: {
        'marketplace_in': [
            make_cloud_input_marketplace(client_id=1, date=c['str_dt'], amount=123.32),
            make_cloud_input_marketplace(client_id=1, date=c['str_dt'], amount=42.23),
            make_cloud_input_marketplace(client_id=1, date=WRONG_STR_DT, amount=123.32),

            make_cloud_input_marketplace(client_id=2, date=c['str_dt'], amount=123.32),
        ],
        'events_src': [
            make_cloud_input_event(Products.CLOUD, c['project_id'], amount=45.797485324, str_dt=c['str_dt']),
            make_cloud_input_event(Products.CLOUD, c['project_id'], amount=17.888888888, str_dt=c['str_dt']),
            make_cloud_input_event(Products.CLOUD, MISSING_PROJECT_ID, amount=888.888888888, str_dt=c['str_dt'])
        ]
    }),

    StagerProject.HOSTING_SERVICE: (lambda c: {
        'events_src': [
            make_hosting_service_input_event(Products.HOSTING_SERVICE, c['project_id'], amount=45.797485324, str_dt=c['str_dt']),
            make_hosting_service_input_event(Products.HOSTING_SERVICE, c['project_id'], amount=17.888888888, str_dt=c['str_dt']),
            make_hosting_service_input_event(Products.HOSTING_SERVICE, MISSING_PROJECT_ID, amount=888.888888888, str_dt=c['str_dt'])
        ]
    }),

    StagerProject.AVIA: (lambda c: {
        'show_log': [
            make_avia_input_show_log(c['client_ids'][0], row_id=1),
            make_avia_input_show_log(c['client_ids'][0], row_id=2),

            make_avia_input_show_log(c['client_ids'][1], row_id=3),

            make_avia_input_show_log(c['client_id_wo_contract'], row_id=4),
        ],

        'redir_log': [
            make_avia_input_redir_log(c['client_ids'][0], row_id=1, price=127.1),
            make_avia_input_redir_log(c['client_ids'][0], row_id=2, price=88.42),
            make_avia_input_redir_log(c['client_ids'][0], row_id=5, price=31.22, national_version=NatVer.COM),

            make_avia_input_redir_log(c['client_ids'][1], row_id=3, price=32.16),
            make_avia_input_redir_log(c['client_ids'][1], row_id=3, price=32.16),

            make_avia_input_redir_log(c['client_id_wo_contract'], row_id=4, price=11.23),
        ]
    }),

    StagerProject.TAXI: (lambda c: {
        'tl_revenues_in': [
            make_taxi_input(transaction_id=1, client_id=1, clid='1', currency=Currencies.RUB,
                            transaction_type=TransactionType.PAYMENT, amount=42.7569, str_dt=c['str_dt']),
            make_taxi_input(transaction_id=2, client_id=1, clid='1', currency=Currencies.USD,
                            transaction_type=TransactionType.REFUND, amount=1.3423, str_dt=c['str_dt'],
                            aggregation_sign=-1),
            make_taxi_input(transaction_id=3, client_id=1, clid='2', currency=Currencies.RUB,
                            transaction_type=TransactionType.PAYMENT, amount=17.1819, str_dt=c['str_dt']),
            make_taxi_input(transaction_id=4, client_id=1, clid='4', currency=Currencies.RUB,
                            transaction_type=TransactionType.PAYMENT, product=TaxiOrderType.subsidy_tlog,
                            aggregation_sign=-1,
                            amount=1.00, str_dt=c['str_dt']),

            make_taxi_input(transaction_id=5, client_id=2, clid='1', currency=Currencies.RUB,
                            transaction_type=TransactionType.PAYMENT, amount=31.0234, str_dt=c['str_dt']),
            make_taxi_input(transaction_id=6, client_id=2, clid=None, currency=Currencies.RUB,
                            transaction_type=TransactionType.REFUND, amount=15.8763, str_dt=c['str_dt'],
                            aggregation_sign=-1),

            make_taxi_input(transaction_id=7, client_id=3, clid='5', currency=Currencies.RON,
                            transaction_type=TransactionType.PAYMENT, amount=32.11, str_dt=c['str_dt']),

            make_taxi_input(transaction_id=666, client_id=666, clid=None, currency=Currencies.RUB,
                            transaction_type=TransactionType.PAYMENT, amount=666.66, str_dt=c['str_dt'],
                            ignore_in_balance=True),
        ],
    }),
    StagerProject.BLUE_MARKET: (lambda c: {
        'tl_revenues_in': [
            make_blue_market_input(
                # tt < border, et = mig +
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'] - rd(days=1), 3],
                transaction_id=1, amount=10, client_id=1, ),
            make_blue_market_input(
                # tt < border, et > mig +
                event_time_tz=[c['tlog_start_dt'] + rd(days=1), 3],
                transaction_time_tz=[c['filter_border'] - rd(days=1), 3],
                transaction_id=2, amount=10, client_id=1, ),
            make_blue_market_input(
                # tt = border, et < mig +
                event_time_tz=[c['tlog_start_dt'] - rd(days=1), 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=3, amount=10, client_id=1, ),
            make_blue_market_input(
                # tt > border (tz), et < mig +
                event_time_tz=[c['tlog_start_dt'] - rd(days=1), 3],
                transaction_time_tz=[c['filter_border'].replace(hour=23)-rd(days=1), 1],
                transaction_id=4, amount=10, client_id=1, ),
            make_blue_market_input(
                # tt < border, et < mig -
                event_time_tz=[c['tlog_start_dt'] - rd(days=1), 3],
                transaction_time_tz=[c['filter_border'] - rd(days=1), 3],
                transaction_id=5, amount=666666, client_id=1, ),
            make_blue_market_input(
                # tt < border, et < mig (tz) -
                event_time_tz=[c['tlog_start_dt'] + rd(days=-1), 3],
                transaction_time_tz=[c['filter_border'].replace(hour=4), 9],
                transaction_id=6, amount=666666, client_id=1, ),

            # next 2 - another product +;
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=7, amount=11.115, client_id=1, product=BlueMarketOrderType.sorting),
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=8, amount=11.115, client_id=1, product=BlueMarketOrderType.sorting),

            # ignore in balance -
            make_blue_market_input(
                # tt < border, et < mig (tz) +
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=9, amount=666666, client_id=1, product=BlueMarketOrderType.sorting, ignore_in_balance=True),

            #another currency +
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=10, amount=11.115, client_id=1, currency=Currencies.USD),
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=11, amount=11.115, client_id=1, currency=Currencies.ILS),
            # another service +
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=12, amount=11.115, client_id=1, service=Services.RED_MARKET_BALANCE),
            # another client +
            make_blue_market_input(
                event_time_tz=[c['tlog_start_dt'], 3],
                transaction_time_tz=[c['filter_border'], 3],
                transaction_id=13, amount=11.115, client_id=2),
        ],
    }),
    StagerProject.BLUE_MARKETING_SERVICES: (lambda c: {
        'tl_revenues_in': [
            make_blue_marketing_services_input(
                transaction_id=1,
                event_time_tz=[c['dt'], 3],
                client_id=1, amount=10),
            make_blue_marketing_services_input(
                transaction_id=2,
                event_time_tz=[c['dt'], 3],
                client_id=1, amount=10),
            make_blue_marketing_services_input(
                transaction_id=3,
                event_time_tz=[c['dt'], 3],
                client_id=1, amount=10,
                product=BlueMarketingServicesOrderType.marketing_promo_mailing),
            make_blue_marketing_services_input(
                transaction_id=4,
                event_time_tz=[c['dt'], 3],
                client_id=1, amount=10,
                product=BlueMarketingServicesOrderType.marketing_promo_mailing),
            make_blue_marketing_services_input(
                transaction_id=5,
                event_time_tz=[c['dt'], 3],
                client_id=1, amount=10),
        ]
    }),
}

EXPECTED_DATA = {
    StagerProject.EDA: (lambda c: {
        'completions': [
            make_eda_output(client_id=1, service_id=Services.FOOD_SERVICES.id, amount=255.59, currency=Currencies.RUB, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=2, service_id=Services.FOOD_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=2, service_id=Services.FOOD_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt=c['str_dt']),
            make_eda_output(client_id=666, service_id=Services.FOOD_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=3, service_id=Services.FOOD_SERVICES.id, amount=5.10, currency=Currencies.RUB, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=1, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=255.59, currency=Currencies.RUB, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt=c['str_dt']),
            make_eda_output(client_id=666, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt=c['str_dt']),
            make_eda_output(client_id=3, service_id=Services.FOOD_PHARMACY_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="goods", dt=c['str_dt']),
            make_eda_output(client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=2387.78, currency=Currencies.RUB, t_type="third_party_order_processing", dt=c['str_dt']),
            make_eda_output(client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="corporate", dt=c['str_dt']),

        ]
    } if not c['dt_aggregation'] else {
        'completions': [
            make_eda_output(client_id=1, service_id=Services.FOOD_SERVICES.id, amount=255.59, currency=Currencies.RUB, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=2, service_id=Services.FOOD_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=2, service_id=Services.FOOD_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt='2019-07-15'),
            make_eda_output(client_id=666, service_id=Services.FOOD_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=3, service_id=Services.FOOD_SERVICES.id, amount=10.55, currency=Currencies.RUB, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=3, service_id=Services.FOOD_SERVICES.id, amount=-5.45, currency=Currencies.RUB, t_type="1", dt='2019-07-14'),
            make_eda_output(client_id=1, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=255.59, currency=Currencies.RUB, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=321.13, currency=Currencies.RUB, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=2, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=421.00, currency=Currencies.RUB, t_type="2", dt='2019-07-15'),
            make_eda_output(client_id=666, service_id=Services.FOOD_SHOPS_SERVICES.id, amount=666.66, currency=Currencies.KZT, t_type="1", dt='2019-07-15'),
            make_eda_output(client_id=3, service_id=Services.FOOD_PHARMACY_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="goods", dt='2019-07-15'),
            make_eda_output(client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=2387.78, currency=Currencies.RUB, t_type="third_party_order_processing", dt='2019-07-15'),
            make_eda_output(client_id=2666, service_id=Services.REST_SITES_SERVICES.id, amount=721.12, currency=Currencies.RUB, t_type="corporate", dt='2019-07-15'),
        ]
    }),

    StagerProject.DRIVE: (lambda c: {
        'calculations': [
            make_drive_output(order_type='carsharing', amount=44.32, currency=Currencies.RUB,
                              product=Products.CARSHARING_WITH_NDS_1),
            make_drive_output(order_type='toll_road', amount=336.0, currency=Currencies.RUB,
                              product=Products.CARSHARING_WITH_NDS_1)
        ]
    }),

    StagerProject.CLOUD: (lambda c: {
        'completions': [
            make_cloud_output_completion(Products.CLOUD, c['contract_id'], c['project_id'], 63.686374)
        ],
        'errors': [
            make_cloud_output_error(MISSING_PROJECT_ID, c['str_dt']),
        ],
        'marketplace_out': [
            make_cloud_output_marketplace(client_id=1, amount=165.55),
            make_cloud_output_marketplace(client_id=2, amount=123.32)
        ]
    }),

    StagerProject.HOSTING_SERVICE: (lambda c: {
        'completions': [
            make_hosting_service_output_completion(
                Products.HOSTING_SERVICE, c['contract_id'], c['project_id'], 63.686374)
        ],
        'errors': [
            make_hosting_service_output_error(MISSING_PROJECT_ID, c['str_dt']),
        ]
    }),

    StagerProject.AVIA: (lambda c: {
        'errors': [
            make_avia_output_error_no_contract(c['client_id_wo_contract'], clicks=1, amount=11.23)
        ],
        'completions': [
            make_avia_output_completion(c['client_ids'][0], c['contract_ids'][0], clicks=2, amount=215.52),
            make_avia_output_completion(c['client_ids'][1], c['contract_ids'][1], clicks=1, amount=32.16),
            make_avia_output_completion(c['client_ids'][0], c['contract_ids'][0], clicks=1, national_version=NatVer.COM,
                                        amount=utils.dround(Decimal(31.22) * c['eur_rate'], 6)),
        ],
        'distribution': [
            make_avia_output_distribution(c['client_ids'][0], clicks=2, amount=calc_avia_bucks(215.52, c['dt'])),
            make_avia_output_distribution(c['client_ids'][1], clicks=1, amount=calc_avia_bucks(32.16, c['dt'])),
            make_avia_output_distribution(c['client_id_wo_contract'], clicks=1, amount=calc_avia_bucks(11.23, c['dt'])),
            make_avia_output_distribution(c['client_ids'][0], clicks=1, national_version=NatVer.COM,
                                          amount=calc_avia_bucks(Decimal(31.22) * c['eur_rate'], c['dt'])),
        ],
        'fraud': [
            make_avia_output_fraud(c['client_ids'][1], filter_name=u"count_rasp_redir")
        ],
    }),

    StagerProject.TAXI: (lambda c: {
        'tl_revenues_copy': INPUT_DATA[StagerProject.TAXI](c)['tl_revenues_in'],
        'tl_revenues_out': [
            make_taxi_output_revenue(client_id=1, currency=Currencies.RUB, last_transaction_id=3,
                                     amount=59.9388, str_dt=c['str_dt']),
            make_taxi_output_revenue(client_id=1, currency=Currencies.USD, last_transaction_id=2,
                                     amount=-1.3423, str_dt=c['str_dt']),
            make_taxi_output_revenue(client_id=2, currency=Currencies.RUB, last_transaction_id=6,
                                     amount=15.1471, str_dt=c['str_dt']),
            make_taxi_output_revenue(client_id=1, currency=Currencies.RUB, last_transaction_id=4,
                                     amount=-1.0, str_dt=c['str_dt'], product=TaxiOrderType.subsidy_tlog),
            make_taxi_output_revenue(client_id=3, currency=Currencies.RON, last_transaction_id=7,
                                     amount=32.11, str_dt=c['str_dt']),
        ],
        'tl_distrib_out': [
            make_taxi_output_distr(clid='1', count=3, amount=-0.436845),
            make_taxi_output_distr(clid='2', count=1, amount=0.675821),
            make_taxi_output_distr(clid='4', count=1, amount=-0.039333),
            make_taxi_output_distr(clid='5', count=1, amount=19.925488),
        ],
        'errors': []
    }),

    StagerProject.BLUE_MARKET: (lambda c: {
        'tl_revenues_copy': INPUT_DATA[StagerProject.BLUE_MARKET](c)['tl_revenues_in'],
        'tl_revenues_out': [
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=1, amount='10.0', last_transaction_id=1),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'] + rd(days=1), client_id=1, amount='10.0', last_transaction_id=2),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'] - rd(days=1), client_id=1, amount='20.0', last_transaction_id=4),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=1, amount='22.23', last_transaction_id=8,
                product=BlueMarketOrderType.sorting),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=1, amount='11.115', last_transaction_id=10,
                currency=Currencies.USD),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=1, amount='11.115', last_transaction_id=11,
                currency=Currencies.ILS),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=1, amount='11.115', last_transaction_id=12,
                service=Services.RED_MARKET_BALANCE),
            make_blue_market_output_revenue(
                event_time_msk=c['tlog_start_dt'], client_id=2, amount='11.115', last_transaction_id=13),
        ],
        'errors': []
    }),
    StagerProject.BLUE_MARKETING_SERVICES: (lambda c: {
        'tl_revenues_copy': INPUT_DATA[StagerProject.BLUE_MARKETING_SERVICES](c)['tl_revenues_in'],
        'tl_revenues_out': [
            make_blue_marketing_services_output_revenue(
                event_time_msk=c['dt'], client_id=1, amount='20.0', last_transaction_id=4,
                product=BlueMarketingServicesOrderType.marketing_promo_mailing),
            make_blue_marketing_services_output_revenue(
                event_time_msk=c['dt'], client_id=1, amount='30.0', last_transaction_id=5),
        ],
        'errors': []
    }),
}
