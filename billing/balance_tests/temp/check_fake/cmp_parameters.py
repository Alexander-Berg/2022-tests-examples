# -*- coding: utf-8 -*-



def param(compare_type):
    ################################################# iob
    if compare_type == 'iob':
        print u'Запуск сверки dcs_iob_no'
        paysys_id = 1001
        person_type = 'ph'
        contract_type = 'comm'
        cmp_name = 'dcs_iob'
        cmp_type = 'iob'
        code_name = 'compare_iob.sh'

    if compare_type == 'iob_us':
        print u'Запуск сверки dcs_iob_us'
        paysys_id = 1029
        person_type = 'usp'
        contract_type = 'usa'
        cmp_name = 'dcs_iob_us'
        cmp_type = 'iob'
        code_name = 'compare_iob_us.sh'

    ################################################# aob
    if compare_type == 'aob':
        print u'Запуск сверки dcs_aob_nomnc'
        paysys_id = 1001
        person_type = 'ph'
        contract_type = 'comm'
        cmp_name = 'dcs_aob_nomnc'
        cmp_type = 'aob'
        code_name = 'compare_aob.sh'

    if compare_type == 'aob_us':
        print u'Запуск сверки dcs_aob_us'
        paysys_id = 1029
        person_type = 'usp'
        contract_type = 'usa'
        cmp_name = 'dcs_aob_us'
        cmp_type = 'aob'
        code_name = 'compare_aob_us.sh'

    if compare_type == 'aob_tr':
        print u'Запуск сверки dcs_aob_tr'
        paysys_id = 1051
        person_type = 'trp'
        contract_type = 'tr'
        cmp_name = 'dcs_aob_tr'
        cmp_type = 'aob'
        code_name = 'compare_aob_tr.sh'

    return paysys_id, person_type, contract_type, cmp_name, cmp_type, code_name
