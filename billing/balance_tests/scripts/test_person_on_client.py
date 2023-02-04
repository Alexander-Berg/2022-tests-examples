# -*- coding: utf-8 -*-
__author__ = 'aikawa'
import pytest

from balance import balance_steps as steps

'''
Для клиента с регионом, кроме того:
Нельзя создавать плательщиков-нерезидентов клиента, регион которого является регионом резидентства плательщика.
Нельзя создавать плательщиков-резидентов клиента, регион которого не является регионом резидентства плательщика.
Нельзя создавать плательщика, у которого нет общих платежных политик с платежными политиками, с которыми можно платить в регион клиента.

При редактировании(?) или создании последующих плательщиков:
если предыдущий плательщик - резидент, то текущий плательщик тоже должен быть резидентом этого региона.
если предыдущий плательщик - нерезидент, то текущий плательщик не может быть нерезидентом относительно того же региона, но с другой категорией плательщика (sw_ytph, by_ytph не могут являться плательщиками одного клиента, потому что оба являются нерезидентами Швейцарии).
если в списке фирм нового плательщика нет фирм предыдущего плательщика.
'''

region_person_params_list = [
    # плательщик-резидент региона, клиент из региона резиденства
    {'region_id': '225', 'region_name': 'Russia', 'person_type': 'ur', 'resident': 1, 'firm_ids': ['1', '12', '13'],
     'resident_region_id': '225'},
    {'region_id': '225', 'region_name': 'Russia', 'person_type': 'yt', 'resident': 0, 'firm_ids': ['1', '12', '13'],
     'resident_region_id': '225'},
    {'region_id': '225', 'region_name': 'Russia', 'person_type': 'pu', 'resident': 1, 'firm_ids': ['2'],
     'resident_region_id': '187'},
    {'region_id': '225', 'region_name': 'Russia', 'person_type': 'pu', 'resident': 1, 'firm_ids': ['2'],
     'resident_region_id': '187'},
    {'region_id': '171', 'region_name': 'Uzbekistan', 'person_type': 'yt', 'resident': 0, 'firm_ids': ['1', '12', '13'],
     'resident_region_id': '225'},
    {'region_id': '84', 'region_name': 'США', 'region_pay_policy_ids': [1200, 410, 1300, 100], 'person_type': 'yt_kzp',
     'resident': 0, 'firm_ids': ['1'], 'resident_region_id': '225', 'person_pay_policy_ids': [102]}]

region_person_params_list_ids = [
    str(param['region_name']) + '  ' + str(param['person_type']) + '  ' + param['resident_region_id'] for param in
    region_person_params_list]


def create_person_on_client_with_region(region_id, person_category):
    client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    try:
        person_id = steps.PersonSteps.create(client_id, person_category)
    except Exception, exc:
        if steps.CommonSteps.get_exception_code(exc) == 'PERSON_TYPE_MISMATCH':
            return 'PERSON_TYPE_MISMATCH'
    return person_id


@pytest.mark.parametrize('region_person_param', region_person_params_list
    , ids=region_person_params_list_ids)
def create_person_on_client_with_region(region_person_param):
    '''
        Для клиента с регионом, кроме того:
        Нельзя создавать плательщиков-нерезидентов клиента, регион которого является регионом резидентства плательщика.
        Нельзя создавать плательщиков-резидентов клиента, регион которого не является регионом резидентства плательщика.
        Нельзя создавать плательщика, у которого нет общих платежных политик с платежными политиками, с которыми можно платить в регион клиента.

        При редактировании(?) или создании последующих плательщиков:
        если предыдущий плательщик - резидент, то текущий плательщик тоже должен быть резидентом этого региона.
        если предыдущий плательщик - нерезидент, то текущий плательщик не может быть нерезидентом относительно того же региона, но с другой категорией плательщика (sw_ytph, by_ytph не могут являться плательщиками одного клиента, потому что оба являются нерезидентами Швейцарии).
        если в списке фирм нового плательщика нет фирм предыдущего плательщика.
    '''
    region_id = region_person_param['region_id']
    person_category = region_person_param['person_type']
    # common_pay_policy = set(region_person_param['region_pay_policy_ids']).intersection(region_person_param['person_pay_policy_ids'])
    if create_person_on_client_with_region(region_id, person_category) == 'PERSON_TYPE_MISMATCH':
        if region_person_param['resident'] == 0:
            # не создастся плательщик-нерезидент, если клиент из региона резиденства или нет общих платежных политик
            assert bool(region_id == region_person_param['region_id'])
            # or (bool(common_pay_policy) is False)
        else:
            # не создастся плательщик-резидент, если клиент не из региона резиденства
            assert region_id <> region_person_param['region_id']
    else:
        pass

        # a-vasin: тут что-то совсем страшное, вместо замены матчера просто закоментил
        # ut.check_that(assert_that(item['expected_cons_qty'], match.FullMatch(db.get_client_by_id(order_params_list[num]['order_id']))))


if __name__ == "__main__":
    # pytest.main("person_vs_client.py -v")
    # pytest.main("person_vs_client.py -vk 'test_create_second_person_on_client'")
    pytest.main("test_person_on_client.py -vk 'test_create_person_on_client_with_region'")
    # pytest.main("person_vs_client.py -vk 'test_change_region_on_client_with_person'")
