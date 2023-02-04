from decimal import Decimal as D
import datetime
import copy
from btestlib.utils import aDict

__author__ = 'kostya-karpus'

shift = datetime.timedelta
BASE_DT = datetime.datetime.utcnow().replace(hour=5, minute=0, second=0, microsecond=0)

STATS = {'processed': {'completions': None, 'dt': BASE_DT - shift(hours=3)}, 'raw': None}


def split_by_keys(stats, keys_count):
    """split statistics by keys"""
    stats = aDict(stats)
    stats_by_key = []
    for keys_number in xrange(keys_count):
        out_stats = {}
        if hasattr(stats, 'raw'):
            raw_stats = []
            for stat in stats.raw['stats']:
                raw_stat = {}
                for key, value in stat['completions'].items():
                    if keys_number < keys_count - 1:
                        raw_stat.update({key: int(value / keys_count)})
                    else:
                        raw_stat.update({key: int(value - (value / keys_count * keys_number))})
                raw_stats.append({'completions': raw_stat})
            out_stats.update({'raw': {'stats': raw_stats}})

        if hasattr(stats, 'processed'):
            processed_stats = []
            for stat in stats.processed['stats']:
                processed_stat = {}
                for key, value in stat['completions'].items():
                    if keys_number < keys_count - 1:
                        processed_stat.update({key: int(value / keys_count)})
                    else:
                        processed_stat.update({key: int(value - (value / keys_count * keys_number))})
                processed_stats.append({'dt': stat['dt'], 'completions': processed_stat})
            out_stats.update({'processed': {'stats': processed_stats, 'base_dt': stats.processed['base_dt']}})

        stats_by_key.append(out_stats)
    return stats_by_key


def zero_in_one_key(stats, keys_count):
    """0 in one key, stats to other keys, for more than one key """
    if keys_count == 1:
        return [stats]
    else:
        stats = aDict(stats)
        stats_by_key = []
        out_stats = {}

        if hasattr(stats, 'raw'):
            raw_stats = []
            for stat in stats.raw['stats']:
                raw_stat = {}
                for key, value in stat['completions'].items():
                    raw_stat.update({key: 0})
                raw_stats.append({'completions': raw_stat})
            out_stats.update({'raw': {'stats': raw_stats}})

        if hasattr(stats, 'processed'):
            processed_stats = []
            for stat in stats.processed['stats']:
                processed_stat = {}
                for key, value in stat['completions'].items():
                    processed_stat.update({key: 0})
                processed_stats.append({'dt': stat['dt'], 'completions': processed_stat})
            out_stats.update({'processed': {'stats': processed_stats, 'base_dt': stats.processed['base_dt']}})

        stats_by_key.append(out_stats)

        for _ in xrange(keys_count - 1):
            out_stats = {}
            if hasattr(stats, 'raw'):
                raw_stats = []
                for stat in stats.raw['stats']:
                    raw_stat = {}
                    for key, value in stat['completions'].items():
                        raw_stat.update({key: int(value / (keys_count - 1))})
                    raw_stats.append({'completions': raw_stat})
                out_stats.update({'raw': {'stats': raw_stats}})

            if hasattr(stats, 'processed'):
                processed_stats = []
                for stat in stats.processed['stats']:
                    processed_stat = {}
                    for key, value in stat['completions'].items():
                        processed_stat.update({key: int(value / (keys_count - 1))})
                    processed_stats.append({'dt': stat['dt'], 'completions': processed_stat})
                out_stats.update({'processed': {'stats': processed_stats, 'base_dt': stats.processed['base_dt']}})

            stats_by_key.append(out_stats)
        return stats_by_key


# def zero_in_one_key_limit_in_other(tariff_config, keys_count):
#     test_case = []
#     key_stats = []
#     stats = copy.deepcopy(STATS)
#     stats['processed']['completions'] = {}
#     stats['raw'] = {}
#     for key in tariff_config:
#         stats['processed']['completions'].update({key: 0})
#         stats['raw'].update({key: 0})
#     key_stats.append(stats)
#     test_case.append(key_stats)
#
#     for _ in xrange(keys_count - 1):
#         key_stats = []
#         stats = copy.deepcopy(STATS)
#         stats['processed']['completions'] = {}
#         stats['raw'] = {}
#         for key in tariff_config:
#             processed = (float(tariff_config[key]) * 0.5) / (keys_count - 1)
#             processed += 0 if processed % 1 == 0 else 1
#             stats['processed']['completions'].update({key: int(processed)})
#             stats['raw'].update({key: int((float(tariff_config[key]) * 0.5) / (keys_count - 1))})
#         key_stats.append(stats)
#         test_case.append(key_stats)
#     return test_case


if __name__ == '__main__':
    import pprint

    tariff_config = {'tner': 3000, 'hits': 200}
    stats = [{'processed': {'completions': {'hits': 99, 'tner': 750},
                            'dt': datetime.datetime(2017, 3, 17, 2, 0)},
              'raw': {'hits': 100, 'tner': 750}},
             {'processed': {'completions': {'hits': 50, 'tner': 1499},
                            'dt': datetime.datetime(2017, 3, 17, 2, 0)},
              'raw': {'hits': 50, 'tner': 1500}}]

    print 'null_in_each_unit'
    pprint.pprint(zero_in_one_key(stats, 3))
