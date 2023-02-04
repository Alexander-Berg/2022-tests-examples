import MockDate from 'mockdate';

import { MONTH, DAY } from 'auto-core/lib/consts';

import ls from 'auto-core/react/lib/localstorage';

import localStorageSearchPosition from './localStorageSearchPosition';

const LS_KEY = 'autoru_search_position';

beforeEach(() => {
    MockDate.set('2021-06-06');
    ls.removeItem(LS_KEY);
});

it('если ls пустой, то добавит в него значение и вернет 0', () => {
    const diff = localStorageSearchPosition({ offerId: '1', position: 12 });

    expect(diff).toBe(0);

    expect(ls.getItem(LS_KEY)).toEqual('[{"offerId":"1","position":12,"ts":1622937600000}]');
});

it('если в ls есть другие офферы, то добавит текущий в начало и вернет 0', () => {
    ls.setItem(LS_KEY, '[{"offerId":"1","position":12,"ts":1622937600000}]');
    const diff = localStorageSearchPosition({ offerId: '2', position: 13 });

    expect(diff).toBe(0);

    expect(ls.getItem(LS_KEY)).toEqual('[{"offerId":"2","position":13,"ts":1622937600000},{"offerId":"1","position":12,"ts":1622937600000}]');
});

it('если в ls уже есть этот оффер, то обновит его значение и вернет разницу', () => {
    ls.setItem(LS_KEY, '[{"offerId":"1","position":12,"ts":1622937600000}]');
    const diff = localStorageSearchPosition({ offerId: '1', position: 22 });

    expect(diff).toBe(-10);

    expect(ls.getItem(LS_KEY)).toEqual('[{"offerId":"1","position":22,"ts":1622937600000}]');
});

it('если в ls есть значения старше месяца, то удалит их', () => {
    const oldItems = Array(10).fill(null).map((item, index) => ({ offerId: index + 1, position: index + 1, ts: Date.now() - 2 * MONTH }));
    ls.setItem(LS_KEY, JSON.stringify(oldItems));
    localStorageSearchPosition({ offerId: '1', position: 22 });

    const lsArr = JSON.parse(ls.getItem(LS_KEY) || '[]');

    expect(lsArr).toHaveLength(1);
});

it('если превышаем лимит (200 значений), то обрезаем число значений и добавляем новое в начало', () => {
    const oldItems = Array(210).fill(null).map((item, index) => ({ offerId: index + 2, position: index + 2, ts: Date.now() - 2 * DAY }));
    ls.setItem(LS_KEY, JSON.stringify(oldItems));
    localStorageSearchPosition({ offerId: '1', position: 22 });

    const lsArr = JSON.parse(ls.getItem(LS_KEY) || '[]');
    expect(lsArr[0]).toEqual({ offerId: '1', position: 22, ts: 1622937600000 });
    expect(lsArr).toHaveLength(200);
});
