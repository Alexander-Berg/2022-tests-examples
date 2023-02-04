import { FlatStatus } from 'types/flat';
import { RentContractStatus } from 'types/contract';

import { ManagerFlatsSearchFilter } from '../index';
import { Fields } from '../types';

import {
    commonFilter,
    filterByContractStatus,
    filterByFlatStatus,
    filterBySigningWindow,
    filterByStatusWithQuery,
} from './mocks';

describe('ManagerFlatsSearchFilter', () => {
    it('Поиск по статусам квартир', () => {
        const body = new ManagerFlatsSearchFilter()
            .in(Fields.OWNER_REQUEST_STATUS, [FlatStatus.RENTED, FlatStatus.DRAFT])
            .build();

        expect(body).toEqual(filterByFlatStatus);
    });

    it('Поиск по статусам и с query-параметром', () => {
        const body = new ManagerFlatsSearchFilter()
            .in(Fields.OWNER_REQUEST_STATUS, [FlatStatus.RENTED, FlatStatus.DRAFT])
            .equal(Fields.QUERY, 'куше')
            .build();

        expect(body).toEqual(filterByStatusWithQuery);
    });

    it('Поиск по сданным квартирам с окном в подписании', () => {
        const body = new ManagerFlatsSearchFilter()
            .in(Fields.OWNER_REQUEST_STATUS, [FlatStatus.RENTED])
            .between(Fields.ACTIVE_CONTRACT_RENT_START_DATE, {
                from: '2022-01-22T00:00:00+03:00',
                to: '2022-01-24T00:00:00+03:00',
            })
            .build();

        expect(body).toEqual(filterBySigningWindow);
    });

    it('Поиск по статусам контракта', () => {
        const body = new ManagerFlatsSearchFilter()
            .in(Fields.ACTIVE_CONTRACT_STATUS, [RentContractStatus.SIGNED, RentContractStatus.ACTIVE])
            .build();

        expect(body).toEqual(filterByContractStatus);
    });

    it('Запрос со всеми операторами фильтра', () => {
        const body = new ManagerFlatsSearchFilter()
            .in(Fields.OWNER_REQUEST_STATUS, [FlatStatus.RENTED])
            .equal(Fields.OWNER_REQUEST_STATUS, FlatStatus.RENTED)
            .match(Fields.OWNER_REQUEST_STATUS, FlatStatus.RENTED)
            .lt(Fields.NEED_VIRTUAL_TOUR_LINK, true, false)
            .gt(Fields.NEED_COPYRIGHTER_REVIEW, true, false)
            .wildcard(Fields.ACTIVE_CONTRACT_STATUS, RentContractStatus.SIGNED)
            .between(Fields.ACTIVE_CONTRACT_RENT_START_DATE, {
                from: 'date_value',
                includeFrom: false,
                to: 'date_value',
                includeTo: false,
            })
            .and(new ManagerFlatsSearchFilter().equal(Fields.QUERY, 'value'))
            .or(new ManagerFlatsSearchFilter().equal(Fields.QUERY, 'value'))
            .not(new ManagerFlatsSearchFilter().equal(Fields.QUERY, 'not'))
            .exist(
                Fields.FLAT_ID,
                new ManagerFlatsSearchFilter().equal(Fields.FLAT_ID, 'flat_id').equal(Fields.CODE, 'code')
            )
            .build();

        expect(body).toEqual(commonFilter);
    });
});
