jest.unmock('../act');
jest.unmock('redux-saga/effects');
jest.mock('admin/api/act');

import { put, call } from 'redux-saga/effects';
import { fetchAct } from '../act';
import { getAct } from 'admin/api/act';
import {
    getActData,
    getOrderData,
    getSaverData,
    getPrintFormData,
    getMemo,
    getStatus
} from 'admin/api/data-processors/act';
import { ACT } from '../../actions';

const data = { act_id: 87582042 };

describe('fetch act', () => {
    it('fetch act', () => {
        const gen = fetchAct({ data });

        const act = {};

        expect(gen.next().value).toEqual(call(getAct, data));
        expect(gen.next(act).value).toEqual(
            put({
                type: ACT.RECEIVE,
                actTableData: { items: [getActData(act)] },
                orderTableData: { items: getOrderData(act) },
                saver: getSaverData(act),
                printFormData: getPrintFormData(act),
                memo: getMemo(act),
                status: getStatus(act)
            })
        );
    });
});
