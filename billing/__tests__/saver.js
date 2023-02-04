jest.unmock('../saver');
jest.unmock('redux-saga/effects');
jest.mock('admin/api/act');

import { put, call } from 'redux-saga/effects';
import { saveGoodDebt } from '../saver';
import { setGoodDebt } from 'admin/api/act';
import { SAVE_GOOD_DEBT } from '../../actions';

describe('set-good-debt', () => {
    it('set-good-debt bad debt', () => {
        const data = { act_id: 87582042, good_debt_mark: false };
        const gen = saveGoodDebt({ data });

        expect(gen.next().value).toEqual(call(setGoodDebt, data));
        expect(gen.next().value).toEqual(put({ type: SAVE_GOOD_DEBT.RECEIVE }));
    });

    it('set-good-debt good debt', () => {
        const data = { act_id: 87582042, good_debt_mark: true };
        const gen = saveGoodDebt({ data });

        expect(gen.next().value).toEqual(call(setGoodDebt, data));
        expect(gen.next().value).toEqual(put({ type: SAVE_GOOD_DEBT.RECEIVE }));
    });
});
