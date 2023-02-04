import datetime

from balance import balance_db as db

f = open('/Users/aikawa/Work/SQL/request.tsv', 'r')

SERVICES_allowed_agency_wo_contract = (7, 11, 77, 37)
SERVICES_restrict_client = (750, 114, 111, 11, 102, 132, 90, 135, 42, 137)

for request in f:
    request = request.split('||+|')
    request_id = request[0]
    client_id = request[12]
    request_dt = request[1]

    # print request_id

    request_with_sub_client_non_resident = set()
    count_of_agencies = 0

    request_orders = db.balance().execute('select * from t_request_order where request_id = :request_id',
                                          {'request_id': request_id})
    request_orders_ids = [request_order['parent_order_id'] for request_order in request_orders]
    services_from_orders = set()
    for order_id in request_orders_ids:
        service = db.balance().execute('select * from t_order where id = :order_id', {'order_id': order_id})[0][
            'service_id']
        services_from_orders.add(service)
    # print request_id, services_from_request_orders
    client_from_request = \
        db.balance().execute('select * from t_client where id = :client_id', {'client_id': client_id})[0]
    if client_from_request['is_agency']:
        if services_from_orders <= SERVICES_allowed_agency_wo_contract:
            all_contracts = db.balance().execute('select * from t_contract2 where client_id = :client_id',
                                                 {'client_id': client_id})

            if all_contracts:
                print 'I have contracts'
                for contract in all_contracts:
                    contract2_id = contract['id']
                    services_from_contract = set()
                    all_collaterals = db.balance().execute(
                        'select * from t_contract_collateral where contract2_id = :contract2_id',
                        {'contract2_id': contract2_id})
                    contract_is_matched = False
                    for collateral in all_collaterals:
                        if collateral['num'] is None:
                            if collateral['is_signed'] is not None:
                                if collateral['is_signed'] <= datetime.datetime.strptime(request_dt,
                                                                                         '%Y-%m-%d %H:%M:%S') and \
                                                collateral['is_cancelled'] is None:
                                    contract_is_matched = True
                                else:
                                    break
                            else:
                                break
                        services_from_collateral = db.balance().execute(
                            "select key_num from t_contract_attributes where code = 'SERVICES' and collateral_id = :collateral_id",
                            {'collateral_id': collateral['id']})
                        services = tuple(
                            service_from_collateral['key_num'] for service_from_collateral in services_from_collateral)
                        services_from_contract.update(services)
                    if contract_is_matched and services_from_orders <= services_from_contract:
                        print 'I have active contracts!'
            else:
                print 'im here because of SERVICES_allowed_agency_wo_contract'
    else:
        print 'im client'
        if services_from_orders.intersection(SERVICES_restrict_client):
            print 'im here because of SERVICES_restrict_client'
        else:
            all_contracts = db.balance().execute('select * from t_contract2 where client_id = :client_id',
                                                 {'client_id': client_id})

            if all_contracts:
                print 'I have contracts'
                for contract in all_contracts:
                    contract2_id = contract['id']
                    services_from_contract = set()
                    all_collaterals = db.balance().execute(
                        'select * from t_contract_collateral where contract2_id = :contract2_id',
                        {'contract2_id': contract2_id})
                    contract_is_matched = False
                    for collateral in all_collaterals:
                        if collateral['num'] is None:
                            if collateral['is_signed'] is not None:
                                if collateral['is_signed'] <= datetime.datetime.strptime(request_dt,
                                                                                         '%Y-%m-%d %H:%M:%S') and \
                                                collateral['is_cancelled'] is None:
                                    contract_is_matched = True
                                else:
                                    break
                            else:
                                break
                        services_from_collateral = db.balance().execute(
                            "select key_num from t_contract_attributes where code = 'SERVICES' and collateral_id = :collateral_id",
                            {'collateral_id': collateral['id']})
                        services = tuple(
                            service_from_collateral['key_num'] for service_from_collateral in services_from_collateral)
                        services_from_contract.update(services)
                    if contract_is_matched and services_from_orders <= services_from_contract:
                        print 'I have active contracts!'



                        # if client_from_request:
                        #     print 'Im agency'
                        #     count_of_agencies += 1

    clients_from_orders = [order['client_id'] for order in request_orders]
    for client_from_order in clients_from_orders:
        client = db.balance().execute('select * from t_client where id = :client_id', {'client_id': client_from_order})[
            0]
        if client['is_non_resident']:
            request_with_sub_client_non_resident.add(request_id)
            print 'Im nonresident'
            break

            # print request_with_sub_client_non_resident
