import MockDate from 'mockdate';

import dayjs from 'auto-core/dayjs';

import getVasActivateTimeText from './getVasActivateTimeText';

beforeEach(() => {
    MockDate.reset();
});

it('должен вернуть время, если ВАС был подключен сегодня', () => {
    MockDate.set('2021-09-03T18:00:00.000+0300');

    const activateDate = dayjs('2021-09-03T10:00:00.000+0300').unix() * 1000;
    expect(getVasActivateTimeText(activateDate)).toEqual('Подключено в 10:00');
});

it('должен вернуть "Вчера", если ВАС был подключен вчера', () => {
    MockDate.set('2021-09-03T18:00:00.000+0300');

    const activateDate = dayjs('2021-09-02T10:00:00.000+0300').unix() * 1000;
    expect(getVasActivateTimeText(activateDate)).toEqual('Подключено вчера');
});

it('должен вернуть дату, если ВАС был подключен раньше, чем вчера', () => {
    MockDate.set('2021-09-03T18:00:00.000+0300');

    const activateDate = dayjs('2021-09-01T10:00:00.000+0300').unix() * 1000;
    expect(getVasActivateTimeText(activateDate)).toEqual('Подключено 01.09');
});
