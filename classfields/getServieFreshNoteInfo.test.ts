import MockDate from 'mockdate';

import dayjs from 'auto-core/dayjs';

import getServieFreshNoteInfo from './getServieFreshNoteInfo';

it('возвращает объект для serviceFresh, если объявление поднято сегодня', () => {
    MockDate.set('2022-05-30T13:00:00.000+0300');
    const currentDate = String(dayjs().unix() * 1000);
    expect(getServieFreshNoteInfo(currentDate)).toEqual({
        info: 'в 13:00',
        note: 'Подключено в 13:00',
    });
});

it('возвращает объект для serviceFresh, если объявление поднято вчера', () => {
    MockDate.set('2022-05-30T13:00:00.000+0300');
    const currentDate = String(dayjs().add(-1, 'days').unix() * 1000);

    expect(getServieFreshNoteInfo(currentDate)).toEqual({
        info: '29.05',
        note: 'Подключено вчера в 13:00',
    });
});

it('возвращает объект для serviceFresh, если объявление поднято больше 2х дней назад', () => {
    MockDate.set('2022-05-30T13:00:00.000+0300');
    const currentDate = String(dayjs().add(-10, 'days').unix() * 1000);

    expect(getServieFreshNoteInfo(currentDate)).toEqual({
        info: '20.05',
        note: 'Подключено 20.05 в 13:00',
    });
});
