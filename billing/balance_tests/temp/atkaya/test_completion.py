# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
import urllib

from balance import balance_api as api


def distribution_fixed_data_filter(data, place_id=None):
    splitted = data.split('\n')
    header = tuple(splitted[0].split('\t'))
    filtered = []
    for line in splitted[1:]:
        row = tuple(line.split('\t'))
        if row[1] == place_id:
            filtered.append(dict(zip(header, row)))
    return filtered


def RTBStat():
    start_dt = datetime.datetime(2016, 12, 1, 0, 0, 0)
    end_dt = datetime.datetime(2016, 12, 1, 0, 0, 0)

    # res = rpc.Balance.taxi_stat(start_dt, end_dt)

    # params = urllib.urlencode(
    #     {'starttime': start_dt.strftime('%Y-%m-%dT%H:%M:%S'), 'to_trantime': end_dt.strftime('%Y-%m-%dT%H:%M:%S')})
    full_url = 'http://bssoap.yandex.ru:81/export/export-dsppagestat.cgi?' \
               'starttime=20161201s&amp;stoptime=20161201s&amp;include-extra-fields=1'
    f = urllib.urlopen(full_url)
    splitted = f.split('\n')
    print splitted[0]


    # dr = csv.DictReader(f)
    # header = dr.fieldnames
    # for row in dr.reader:
    #     Print(collections.OrderedDict(zip(header, row)))


def test():
    # f = open('/Users/atkaya/Desktop/rtb20161110.txt', 'r')
    # text = f.read()
    # splitted = text.split('\n')
    splitted = [[123, 2, 33], [123, 2, 22], [123, 7, 66], [144, 4, 10]]
    # print len(splitted)
    i = 0
    # print splitted
    # count = 0
    # i=0
    # splitted.pop()
    # list_new = [{'clid': 0, 'vid': 0, 'count': 1, 'dsp_id': 0, 'dsp_charge': 0, 'shows':0, 'clicks': 0}]
    # list_new = [{'clid': 0, 'vid': 0, 'count': 0, 'sum': 0}]
    list_new = []
    for line in splitted:
        vid = None
        # clid = None
        # print line[0]
        for l in list_new:
            if l['clid'] == line[0]:
                if l['vid'] == line[1]:
                    l['count'] = l['count'] + 1
                    l['sum'] = l['sum'] + line[2]
                    vid = 1
                    # else:
                    #     list_new.append({'clid': line[0], 'vid': line[1], 'count': 1})
                    # else:
                    #     list_new.append({'clid': line[0], 'vid': line[1], 'count': 1})
        if vid is None:
            list_new.append({'clid': line[0], 'vid': line[1], 'count': 1, 'sum': line[2]})

    print list_new


def RTB():
    f = open('/Users/atkaya/Desktop/rtb20161201.txt', 'r')
    # f = open('/Users/atkaya/Desktop/rtbtest.txt', 'r')
    text = f.read()
    splitted = text.split('\n')
    splitted.pop()
    list_new = []
    i = 0
    for line in splitted:
        splitted[i] = tuple(line.split('\t'))
        i = i + 1
    for line in splitted:
        vid = None
        if int(line[3]) <> 0 and int(line[8]) == 1:
            for l in list_new:
                if l['clid'] == line[3]:
                    if l['vid'] == line[4]:
                        # if l['dsp_id']==line[8]:
                        l['count'] = l['count'] + 1
                        l['clicks'] = l['clicks'] + int(line[18])
                        l['shows'] = l['shows'] + int(line[17])
                        l['dsp_charge'] = l['dsp_charge'] + int(line[10])
                        vid = 1
            if vid is None:
                list_new.append({'clid': line[3], 'vid': line[4],
                                 'count': 1, 'dsp_charge': int(line[10]),
                                 'shows': int(line[17]), 'clicks': int(line[18])})
    # print list_new
    print len(list_new)
    print list_new


# f = open('/Users/atkaya/Desktop/api_market.txt', 'r')
# text = f.read()
# splitted = text.split('\n')
# count = 0
# i=0
# splitted.pop()
# for line in splitted:
#     row = splitted[i].split(';')
#     i = i + 1
#     if int(row[3]) < int(row[4]):
#         print row
#         count = count + 1

# print count

# client_id = steps.ClientSteps.create()

# RTB()
start_dt = datetime.datetime(2017, 10, 23, 0, 0, 0)
end_dt = datetime.datetime(2017, 10, 23, 0, 0, 0)
# source = 'rtb_distr'
# source = 'bk'
# source = 'adfox'
# source = 'dsp'
# source = 'taxi'



# source = 'health'
#source =  'd_installs'
#source =  'addapter_ret_ds'
#source =  'addapter_ret_com'
#source = 'addapter_dev_ds'
# source = 'addapter_dev_com'
# source = 'bk'
# source = 'serphits'
# source = 'dsp'
# source = 'rtb_distr'
# source = 'zen'
# source = 'tags3'
# source = 'api_market'
# source = 'advisor_market'
# source = 'rs_market'
# source = 'rs_market_cpa'
# source = 'activations'
# source = 'taxi'
# source = 'taxi_medium'
# source = 'distr_pages'
# source = 'adfox'
# source = 'multiship'
# source = 'avia_chain'
# source = 'avia_order_completions'
# source = 'avia_order_shipment'
# source = 'avia_rs'
# source = 'connect'

source = 'buses2'

# api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})
api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})


# api.medium().GetDistributionRevenueShare(datetime.datetime(2017,1,21,0,0,0), datetime.datetime(2017,1,21,0,0,0))
# inf = steps.DistributionSteps.get_distribution_revenue_share(datetime.datetime(2017,2,16,0,0,0))
# places_full_revshare_info = [row for row in inf if row['PAGE_ID'] == '97587']
# print places_full_revshare_info

# steps.DistributionSteps.get_distribution_revenue_share_full_for_places([1959249], datetime.datetime(2017,2,16,0,0,0))
