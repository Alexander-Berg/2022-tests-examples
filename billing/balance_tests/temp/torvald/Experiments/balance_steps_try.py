# @staticmethod
#     def create_distribution(_type, params, mode='Contract', passport_uid=defaults.PASSPORT_UID):
#         if mode == 'Contract':
#             items_by_type = contract_defaults.contracts_by_type
#             mapping_list = {
#                 'client_id': 'client-id'
#                 , 'person_id': 'person-id'
#                 , 'CURRENCY': 'credit-currency-limit'
#                 , 'dt': 'dt'
#                 , 'FINISH_DT': 'finish-dt'
#                 , 'SERVICES': 'services-'  ##this param should be
#                 , 'is_signed': 'is-signed,is-signed-date,is-signed-dt'  ## several values for single param
#                 , 'is_faxed': 'is-faxed,is-faxed-date,is-faxed-dt'
#                 , 'is_cancelled': 'is-cancelled,is-cancelled-date,is-cancelled-dt'
#                 , 'IS_SUSPENDED': 'is-suspended,is-suspended-date,is-suspended-dt'
#                 , 'SENT_DT': 'sent-dt,sent-dt-date,sent-dt-dt'
#                 , 'IS_BOOKED': 'is-booked'
#                 , 'DISTRIBUTION_TAG': 'distribution-tag'
#                 , 'COMMISSION_TYPE': 'commission-type'
#                 , 'NON_RESIDENT_CLIENTS': 'non-resident-clients'
#                 , 'REPAYMENT_ON_CONSUME': 'repayment-on-consume'
#                 , 'PERSONAL_ACCOUNT': 'personal-account'
#                 , 'LIFT_CREDIT_ON_PAYMENT': 'lift-credit-on-payment'
#                 , 'PERSONAL_ACCOUNT_FICTIVE': 'personal-account-fictive'
#                 , 'CONTRACT_TYPE': 'contract_type'
#             }
#         elif mode == 'Collateral':
#             items_by_type = contract_defaults.collaterals_by_type
#             mapping_list = {
#                 'contract2_id': 'id'
#                 , 'dt': 'col-new-dt'
#                 , 'XXX': 'col-new-print-form-type'
#                 , 'is_signed': 'col-new-is-signed,col-new-is-signed-date,col-new-is-signed-dt'
#                 ## several values for single param
#                 , 'is_faxed': 'col-new-is-faxed,col-new-is-faxed-date,col-new-is-faxed-dt'
#                 # , 'is_cancelled' : 'is-cancelled,is-cancelled-date,is-cancelled-dt'
#                 # , 'IS_SUSPENDED' : 'is-suspended,is-suspended-date,is-suspended-dt'
#                 # , 'SENT_DT'      : 'sent-dt,sent-dt-date,sent-dt-dt'
#                 # , 'IS_BOOKED'    : 'is-booked'
#             }
#         if not items_by_type.has_key(_type):
#             raise Exception('MTestlib exception: No such contract type')
#         else:
#             ignored_keys = []
#             source = items_by_type[_type]
#             ##Convert to list of the tuples ('param', 'value')
#             source_tmp = urlparse.parse_qsl(source, True)
#             ##Convert to dict
#             source_dict = {key: value.decode('utf-8') for (key, value) in source_tmp}
#
#             distr_default = {key: value.decode('utf-8') for (key, value) in
#                              urlparse.parse_qsl(contract_defaults.distr_default.replace('\n', '&').replace(' ', ''),
#                                                 True)}
#             distr_common = {key: value.decode('utf-8') for (key, value) in
#                             urlparse.parse_qsl(contract_defaults.distr_common.replace('\n', '&').replace(' ', ''),
#                                                True)}
#             distr_changable = {key: value.decode('utf-8') for (key, value) in
#                                urlparse.parse_qsl(contract_defaults.distr_changable.replace('\n', '&').replace(' ', ''),
#                                                   True)}
#
#             source_dict.update(distr_default)
#             source_dict.update(distr_common)
#             source_dict.update(distr_changable)
#
#             # ------- OPTIONAL PART----------
#
#             CONTRACT_TYPE_DOWNLOADS_AND_INSTALLS = 2
#             if 'CONTRACT_TYPE' in params:
#                 if params['CONTRACT_TYPE'] == CONTRACT_TYPE_DOWNLOADS_AND_INSTALLS:
#                     distr_installs = {key: value.decode('utf-8') for (key, value) in urlparse.parse_qsl(
#                         contract_defaults.distr_installs.replace('\n', '&').replace(' ', ''), True)}
#                     source_dict.update(distr_installs)
#
#             for key in params:
#                 try:
#                     if key == 'SERVICES':  ##special logic for services
#                         # param_services = int(params[key])
#                         for service_key in source_dict.keys():
#                             if service_key.startswith(mapping_list[key]):
#                                 source_dict.pop(service_key)
#                         for subkey in params[key]:
#                             source_dict[mapping_list[key] + str(subkey)] = subkey
#                     elif key.upper() in ['IS_SIGNED', 'IS_FAXED', 'IS_CANCELLED', 'IS_SUSPENDED', 'SENT_DT',
#                                          'IS_BOOKED']:  ##special logic for dates
#                         keylist = mapping_list[key].split(',')
#                         source_dict[keylist[0]] = ''  ## is-signed - flag
#                         if len(keylist) > 1:
#                             source_dict[keylist[2]] = params[key]  ## is-signed-dt - real date
#                             mn = {1: 'янв', 2: 'фев', 3: 'мар', 4: 'апр', 5: 'май', 6: 'июн', 7: 'июл', 8: 'авг',
#                                   9: 'сен', 10: 'окт', 11: 'ноя', 12: 'дек'}
#                             ## ...-date - date for display in format: 'DD MON YYYY г.'
#                             source_dict[keylist[1]] = '{0} {1} {2} г.'.format(int(params[key][8:10]),
#                                                                               mn[int(params[key][5:7])],
#                                                                               params[key][:4])
#                     elif key in ('NON_RESIDENT_CLIENTS', 'REPAYMENT_ON_CONSUME'):
#                         if mapping_list[key] in source_dict:
#                             source_dict.pop(mapping_list[key])
#                         if params[key] == 1:
#                             source_dict[mapping_list[key]] = ''
#                     else:
#                         source_dict[mapping_list[key]] = params[key] or ''  ##empty string for params with None value
#                 except KeyError:
#                     ignored_keys.append(key)
#                     ## print source_dict
#                     ##        print [(key,source_dict[key]) for key in sorted(source_dict.keys())]
#             print('Next params were ignored: %s') % str(ignored_keys)
#
#             contract = balance_api.Medium().create_contract(passport_uid, source_dict)
#             contract_url = 'https://balance-admin.%s.yandex.ru/contract-edit.xml?contract_id=%s' % (
#                 'greed-tm1f', str(contract['ID']))
#             print 'mode = {0} | Contract_id: {1} (external_id: {2}) url: {3}'.format(mode, contract['ID'],
#                                                                                      repr(contract['EXTERNAL_ID']),
#                                                                                      contract_url)
#
#         return contract['ID'], contract['EXTERNAL_ID']