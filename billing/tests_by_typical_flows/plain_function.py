from decimal import Decimal as D
import datetime
import copy
from math import ceil

shift = datetime.timedelta
BASE_DT = datetime.datetime.utcnow().replace(hour=5, minute=0, second=0, microsecond=0)

STATS = {'processed': {'completions': None, 'dt': BASE_DT - shift(hours=3)}, 'raw': None}


def null_in_each_unit(tariff_config):
    stats = copy.deepcopy(STATS)
    stats['processed']['completions'] = {key: 0 for key in tariff_config}
    stats['raw'] = {key: 0 for key in tariff_config}
    return [stats]


def below_limit_in_each_unit_over_limit_in_sum(tariff_config):
    stats = copy.deepcopy(STATS)
    processed_part = D('0.9')
    raw_part = D('0.1')
    stats['processed']['completions'] = {key: int((tariff_config[key] - 1) * processed_part) for key in tariff_config}
    stats['raw'] = {key: int((tariff_config[key] - 1) * raw_part) for key in tariff_config}
    return [stats]


def below_limit_in_each_unit_multi_processed(tariff_config, hours=3):
    stats = copy.deepcopy(STATS)
    processed_part = D('0.9')
    raw_part = D('0.1')
    processed_list = []
    for hour in xrange(hours):
        completions = {key: int((tariff_config[key] - 1) * processed_part) / hours for key in tariff_config}
        dt = BASE_DT - shift(hours=(3 + hour))
        processed_list.append({'completions': completions, 'dt': dt})
    stats['processed'] = processed_list
    stats['raw'] = {key: int((tariff_config[key] - 1) * raw_part) for key in tariff_config}
    return [stats]


def equal_to_limit_in_every_unit_PR_below_limit_in_others_PR(tariff_config):
    return limit_in_every_unit_below_limit_in_others(tariff_config,
                                                     processed_part=D('0.9'),
                                                     raw_part=D('0.1'))


def equal_to_limit_in_every_unit_P_zero_in_others_R(tariff_config):
    return limit_in_every_unit_zero_in_others(tariff_config,
                                              processed_part=D('1'),
                                              raw_part=D('0'))


def equal_to_limit_in_every_unit_R_zero_in_others_P(tariff_config):
    return limit_in_every_unit_zero_in_others(tariff_config,
                                              processed_part=D('0'),
                                              raw_part=D('1'))


def limit_minus_one_in_every_unit_PR_below_limit_in_others_PR(tariff_config):
    return limit_minus_one_in_every_unit_below_limit_in_others(tariff_config,
                                                               processed_part=D('0.9'),
                                                               raw_part=D('0.1'))


def limit_minus_one_in_every_unit_P_zero_in_others_R(tariff_config):
    return limit_minus_one_in_every_unit_zero_in_others(tariff_config,
                                                        processed_part=D('1'),
                                                        raw_part=D('0'))


def limit_minus_one_in_every_unit_R_zero_in_others_P(tariff_config):
    return limit_minus_one_in_every_unit_zero_in_others(tariff_config,
                                                        processed_part=D('0'),
                                                        raw_part=D('1'))


def limit_in_every_unit_below_limit_in_others(limit_config, processed_part=0.5, raw_part=0.5):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['processed']['completions'] = {}
        stats['raw'] = {}
        for key in limit_config:
            processed = float(limit_config[key]) * float(processed_part)
            processed += 0 if processed % 1 == 0 else 1
            if key == unit:
                stats['processed']['completions'].update({key: int(processed)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
            else:
                stats['processed']['completions'].update({key: int(processed * 0.5)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part) * 0.5)})
        test_cases.append(stats)
    return test_cases


def limit_in_every_unit_zero_in_others(limit_config, processed_part=0.5, raw_part=0.5):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['processed']['completions'] = {}
        stats['raw'] = {}
        for key in limit_config:
            if key == unit:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else 1
                stats['processed']['completions'].update({key: int(processed)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
            else:
                stats['processed']['completions'].update({key: 0})
                stats['raw'].update({key: 0})
        test_cases.append(stats)
    return test_cases


def limit_minus_one_in_every_unit_below_limit_in_others(limit_config, processed_part=0.5, raw_part=0.5):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['processed']['completions'] = {}
        stats['raw'] = {}
        for key in limit_config:
            processed = float(limit_config[key]) * float(processed_part)
            processed += 0 if processed % 1 == 0 else 1
            if key == unit:
                stats['processed']['completions'].update({key: int(processed - 1)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
            else:
                stats['processed']['completions'].update({key: int(processed * 0.5)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part) * 0.5)})
        test_cases.append(stats)
    return test_cases


def limit_minus_one_in_every_unit_zero_in_others(limit_config, processed_part=0.5, raw_part=0.5):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['processed']['completions'] = {}
        stats['raw'] = {}
        for key in limit_config:
            if key == unit:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else 1
                stats['processed']['completions'].update({key: int(processed - 1)})
                stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
            else:
                stats['processed']['completions'].update({key: 0})
                stats['raw'].update({key: 0})
        test_cases.append(stats)
    return test_cases


def limit_in_each_unit_multi_processed_zero_in_others(limit_config, hours=3, processed_part=1, raw_part=0):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['raw'] = {}
        processed_list = []
        for hour in xrange(hours):
            for key in limit_config:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else 1
                hourly_stat = processed / float(hours)
                if hour == 0:
                    hourly_stat += 0 if hourly_stat % 1 == 0 else hours - 1
                dt = BASE_DT - shift(hours=(3 + hour))
                if key == unit:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat)})
                else:
                    if hour == 0:
                        stats['raw'].update({key: 0})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: 0}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: 0})
        stats['processed'] = processed_list
        test_cases.append(stats)
    return test_cases


def limit_minus_one_in_each_unit_multi_processed_zero_in_others(limit_config, hours=3, processed_part=1, raw_part=0):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['raw'] = {}
        processed_list = []
        for hour in xrange(hours):
            for key in limit_config:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else 1
                hourly_stat = (processed - 1) / float(hours)
                if hour == 0:
                    hourly_stat += 0 if hourly_stat % 1 == 0 else hours - 1
                dt = BASE_DT - shift(hours=(3 + hour))
                if key == unit:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat)})
                else:
                    if hour == 0:
                        stats['raw'].update({key: 0})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: 0}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: 0})
        stats['processed'] = processed_list
        test_cases.append(stats)
    return test_cases


def limit_in_each_unit_multi_processed_below_limit_in_others(limit_config, hours=3, processed_part=1, raw_part=0):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['raw'] = {}
        processed_list = []
        for hour in xrange(hours):
            for key in limit_config:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else 1
                hourly_stat = processed / float(hours)
                if hour == 0:
                    hourly_stat += 0 if hourly_stat % 1 == 0 else hours - 1
                dt = BASE_DT - shift(hours=(3 + hour))
                if key == unit:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat)})
                else:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part) * 0.5)})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat * 0.5)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat * 0.5)})
        stats['processed'] = processed_list
        test_cases.append(stats)
    return test_cases


def limit_minus_one_in_each_unit_multi_processed_below_limit_in_others(limit_config, hours=5, processed_part=1,
                                                                       raw_part=0):
    test_cases = []
    for unit in limit_config:
        stats = copy.deepcopy(STATS)
        stats['raw'] = {}
        processed_list = []
        for hour in xrange(hours):
            for key in limit_config:
                processed = float(limit_config[key]) * float(processed_part)
                processed += 0 if processed % 1 == 0 else processed % 1
                hourly_stat = (processed - 1) / float(hours)
                if hour == 0:
                    hourly_stat += 0 if hourly_stat % 1 == 0 else hours - 1
                dt = BASE_DT - shift(hours=(3 + hour))
                if key == unit:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part))})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat)})
                else:
                    if hour == 0:
                        stats['raw'].update({key: int(float(limit_config[key]) * float(raw_part) * 0.5)})
                    if len(processed_list) <= hour:
                        processed_list.append({'completions': {key: int(hourly_stat * 0.5)}, 'dt': dt})
                    else:
                        processed_list[hour]['completions'].update({key: int(hourly_stat * 0.5)})
        stats['processed'] = processed_list
        test_cases.append(stats)
    return test_cases


def limit_minus_one_in_each_unit_PR_multi_processed_below_limit_in_others_PR(limit_config):
    return limit_minus_one_in_each_unit_multi_processed_below_limit_in_others(limit_config, raw_part=0.2,
                                                                              processed_part=0.8)


def limit_in_each_unit_PR_multi_processed_below_limit_in_others_PR(limit_config):
    return limit_in_each_unit_multi_processed_below_limit_in_others(limit_config, raw_part=0.2,
                                                                    processed_part=0.8)


def calculate_expected_postpay(stats, tariff):
    tariff_free_call = tariff.free_call
    completions = 0
    for row in stats:
        daily_stat = sum(row['completions'].values()) - tariff_free_call
        completions += 0 if daily_stat < 0 else daily_stat
    tariff_min = tariff.min_quantum
    completions = completions if completions >= tariff_min else tariff_min
    tariff_scale = tariff.price
    return completions * tariff_scale


def calculate_expected_postpay_two_months(stats, tariff):
    months_stats = {}
    for row in stats:
        if months_stats.get(row['dt'].month):
            months_stats.get(row['dt'].month).append(row)
        else:
            months_stats.update({row['dt'].month: [row]})

    expected = 0
    for month, stat in months_stats.items():
        expected += calculate_expected_postpay(stat, tariff)

    print '....... [DEBUG] ...... TWO MONTHS EXPECTED: {}'.format(expected)
    return expected


def calculate_expected_multikeys(key_stats, price):
    tariff_free_call = price['free_call']
    completions = 0
    for stats in key_stats:
        for row in stats:
            daily_stat = sum(row['completions'].values()) - tariff_free_call
            completions += 0 if daily_stat < 0 else daily_stat
    tariff_min = price['min_quantum']
    completions = completions if completions >= tariff_min else tariff_min
    tariff_scale = price['price']
    return completions * tariff_scale


def calculate_over_limit_expected(stats, free_day_limit, price_over_limit, part=1000):
    completions = 0
    daily_stats = {row['dt'].day: 0 for row in stats}
    for row in stats:
        daily_stats.update({row['dt'].day: sum(row['completions'].values()) + daily_stats[row['dt'].day]})

    for stat in daily_stats.itervalues():
        daily_over_limit_stat = stat - free_day_limit
        completions += 0 if daily_over_limit_stat < 0 else ceil(float(daily_over_limit_stat) / part)

    return '{:.1f}'.format(completions * price_over_limit)


def calculate_total_expected_overlimit(stats, tariff):
    completions = 0
    for row in stats:
        month_stat = sum(row['completions'].values())
        completions += 0 if month_stat < 0 else month_stat
    if tariff.month_limit < completions:
        expected_money = D(tariff.month_price + (
                tariff.price_over_limit_per_1 * (completions - tariff.month_limit))).quantize(D('.00'))
    else:
        expected_money = D(tariff.month_price)
    return expected_money


def completions_zero(tariff_config, dt):
    return {'completions': {key: 0 for key in tariff_config}, 'dt': dt}


def completions_limit_minus_one(tariff_config, dt):
    return {'completions': {key: tariff_config[key] - 1 for key in tariff_config}, 'dt': dt}


def completions_equal_limit(tariff_config, dt):
    return {'completions': tariff_config, 'dt': dt}


def completions_limit_plus_one(tariff_config, dt):
    return {'completions': {key: tariff_config[key] + 1 for key in tariff_config}, 'dt': dt}


def completions_shift_limit(stats, limit, counters):
    for row in stats:
        if limit is -1:
            for counter in counters:
                row['completions'][counter] = row['completions'].pop('shift_limit')
        else:
            shift_total = row['completions'].pop('shift_limit')
            shift_total = -limit if shift_total is 'to_zero' else shift_total
            counters_qty = len(counters)
            total_stat = (limit + shift_total)
            counters_stat = [total_stat / counters_qty] * counters_qty
            counters_stat[-1] = counters_stat[-1] + (total_stat - sum(counters_stat))
            for counter in counters:
                row['completions'][counter] = counters_stat.pop()
    return stats


if __name__ == '__main__':
    import pprint

    tariff_config = {'geocoder_hits': 25000, 'router_hits': 25000}

    print 'null_in_each_unit'
    pprint.pprint(null_in_each_unit(tariff_config))
    print '\n\nbelow_limit_in_each_unit_over_limit_in_sum'
    pprint.pprint(below_limit_in_each_unit_over_limit_in_sum(tariff_config))
    print '\n\nbelow_limit_in_each_unit_multi_processed'
    pprint.pprint(below_limit_in_each_unit_multi_processed(tariff_config))
    print '\n\nequal_to_limit_in_every_unit_PR_below_limit_in_others_PR'
    pprint.pprint(equal_to_limit_in_every_unit_PR_below_limit_in_others_PR(tariff_config))
    print '\n\nequal_to_limit_in_every_unit_P_below_limit_in_others_R'
    pprint.pprint(equal_to_limit_in_every_unit_P_zero_in_others_R(tariff_config))
    print '\n\nequal_to_limit_in_every_unit_R_below_limit_in_others_P'
    pprint.pprint(equal_to_limit_in_every_unit_R_zero_in_others_P(tariff_config))
    print '\n\nlimit_in_every_unit_below_limit_in_others'
    pprint.pprint(limit_in_every_unit_below_limit_in_others(tariff_config))
    print '\n\nlimit_in_every_unit_zero_in_others'
    pprint.pprint(limit_in_every_unit_zero_in_others(tariff_config))
    print '\n\nlimit_minus_one_in_every_unit_below_limit_in_others'
    pprint.pprint(limit_minus_one_in_every_unit_below_limit_in_others(tariff_config))
    print '\n\nlimit_minus_one_in_every_unit_zero_in_others'
    pprint.pprint(limit_minus_one_in_every_unit_zero_in_others(tariff_config))
    print '\n\nlimit_in_each_unit_multi_processed_zero_in_others'
    pprint.pprint(limit_in_each_unit_multi_processed_zero_in_others(tariff_config))
    print '\n\nlimit_minus_one_in_each_unit_multi_processed_zero_in_others'
    pprint.pprint(limit_minus_one_in_each_unit_multi_processed_zero_in_others(tariff_config))
    print '\n\nlimit_in_each_unit_multi_processed_below_limit_in_others'
    pprint.pprint(limit_in_each_unit_multi_processed_below_limit_in_others(tariff_config))
    print '\n\nlimit_minus_one_in_each_unit_multi_processed_below_limit_in_others'
    pprint.pprint(limit_minus_one_in_each_unit_multi_processed_below_limit_in_others(tariff_config))
