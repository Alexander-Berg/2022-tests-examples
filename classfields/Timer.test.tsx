import MockDate from 'mockdate';
import React from 'react';
import { shallow } from 'enzyme';

import Timer from './Timer';

afterEach(() => {
    MockDate.reset();
});

it('если осталось меньше дня, показывает часы:минуты:секунды', () => {
    MockDate.set('2020-03-18T13:00:00Z');
    const props = {
        expiresDate: '2020-03-18T15:21:35Z',
        collapseIfMoreThanOneDayLeft: true,
    };

    const page = shallow(<Timer { ...props }/>);

    expect(page.text()).toBe('02:21:35');
});

it('если осталось больше дня, показывает дни и часы', () => {
    MockDate.set('2020-03-18T13:00:00Z');
    const props = {
        expiresDate: '2020-03-20T15:21:35Z',
        collapseIfMoreThanOneDayLeft: true,
    };

    const page = shallow(<Timer { ...props }/>);

    expect(page.text()).toBe('2 дня, 2 часа');
});

it('если осталось больше дня, но выключен режим свёртки, показывает дни:часы:минуты:секунды', () => {
    MockDate.set('2020-03-18T13:00:00Z');
    const props = {
        expiresDate: '2020-03-20T15:21:35Z',
        collapseIfMoreThanOneDayLeft: true,
        shortModeForDays: false,
    };

    const page = shallow(<Timer { ...props }/>);

    expect(page.text()).toBe('02:02:21:35');
});

it('использует метод для постобработки строки, если он передан', () => {
    MockDate.set('2020-03-18T13:00:00Z');
    const props = {
        expiresDate: '2020-03-20T15:21:35Z',
        collapseIfMoreThanOneDayLeft: true,
        shortModeForDays: false,
        renderTime: (str: string) => str.replace(/:/g, ' * '),
    };

    const page = shallow(<Timer { ...props }/>);

    expect(page.text()).toBe('02 * 02 * 21 * 35');
});
