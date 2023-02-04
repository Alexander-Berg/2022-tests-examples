# coding=utf-8
data = '''|| t_person.inn|endbuyer_ur, am_jp, ur, tru, ur_autoru| 'I' + t_person.inn | hz_parties.orig_system_reference| oebs_id||
|| t_person.kpp|ua| 'I' + t_person.kpp | hz_parties.orig_system_reference| oebs_id ||
|| t_person.id|yt, endbuyer_yt, usu, sw_ur, sw_yt, eu_yt, yt_kzp, yt_kzu, kzu| 'P' + t_person.id | hz_parties.orig_system_reference| oebs_id ||
|| t_person.name|endbuyer_ur, am_jp, ur, tru, ur_autoru| -- | hz_parties.party_name| NAME||
|| t_person.name|ua| -- | hz_parties.party_name| NAME||
|| t_person.name|yt, endbuyer_yt, usu, sw_ur, sw_yt, eu_yt, yt_kzp, yt_kzu, kzu| -- | hz_parties.party_name| YT_NAME||
|| t_person.inn|endbuyer_ur, am_jp, ur, tru, ur_autoru| -- | hz_parties.jgzz_fiscal_code| INN ||
|| t_person.inn|ua| -- | hz_parties.jgzz_fiscal_code| INN ||
|| t_person.inn_doc_details|endbuyer_ur, am_jp, ur, tru, ur_autoru| -- | hz_parties.tax_reference| INN_DOC_DETAILS||
|| t_person.kpp or t_person.inn|ua| -- | hz_parties.tax_reference | KPP_UA||
|| -- |endbuyer_ur, am_jp, ur, tru, ur_autoru| 'ORGANIZATION' | hz_parties.party_type| UR_TYPE ||
|| -- |ua| 'ORGANIZATION' | hz_parties.party_type| UR_TYPE ||
|| -- |yt, endbuyer_yt, usu, sw_ur, sw_yt, eu_yt, yt_kzp, yt_kzu, kzu| 'ORGANIZATION' | hz_parties.party_type| YT_UR_TYPE ||'''

headers = ['billing_data', 'categories_list', 'rule', 'oebs_data', 'test_data']


def f1(data):
    return iter(data.splitlines())


data_list = []
for f in f1(data):
    data_list.append(f)

formatted_list = []
for line in data_list:
    line = line.split('|')[2:-2]
    dictionary = dict(zip(headers, line))
    dictionary['categories_list'] = dictionary['categories_list'].split(',')
    dictionary['categories_list'] = [category.strip() for category in dictionary['categories_list']]
    formatted_list.append(dictionary)
    # print line
# print formatted_list
from collections import defaultdict

result_list = []
result_dict = defaultdict(list)

for line in formatted_list:
    for category in line['categories_list']:
        if line['test_data'] in (' oebs_id', ' oebs_id '):
            result_dict[category].append('id:{0}'.format(line['rule']))
        else:
            result_dict[category].append('{0}'.format(line['test_data']))
        # result_list.append(result_dict)

for k, v in result_dict.iteritems():
    print k, v
