import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import { SECOND, MINUTE, HOUR, DAY } from 'auto-core/lib/consts';

import DaysLeft, { TYPES } from './DaysLeft';

it('не должен отрендерить компонент, когда timestamp < now', () => {
    const tree = shallow(<DaysLeft timestamp={ Date.now() - 3 * HOUR } type={ TYPES.WORKS }/>);
    expect(tree).toBeEmptyRender();
});

describe('TYPES.LEFT', () => {
    it.each<[string, number]>([
        [ 'осталась 1\u00a0секунда', SECOND ],
        [ 'осталось 3\u00a0секунды', 3 * SECOND ],
        [ 'осталось 5\u00a0секунд', 5 * SECOND ],
        [ 'осталась 31\u00a0секунда', 31 * SECOND ],

        [ 'осталась 1\u00a0минута', MINUTE ],
        [ 'осталось 3\u00a0минуты', 3 * MINUTE ],
        [ 'осталось 5\u00a0минут', 5 * MINUTE ],
        [ 'осталась 41\u00a0минута', 41 * MINUTE ],

        [ 'остался 1\u00a0час', HOUR ],
        [ 'осталось 3\u00a0часа', 3 * HOUR ],
        [ 'осталось 5\u00a0часов', 5 * HOUR ],
        [ 'остался 21\u00a0час', 21 * HOUR ],
        [ 'осталось 23\u00a0часа', 23 * HOUR ],
        [ 'остался 1\u00a0день', 24 * HOUR ],
        [ 'остался 1\u00a0день', 25 * HOUR ],

        [ 'остался 1\u00a0день', DAY ],
        [ 'осталось 3\u00a0дня', 3 * DAY ],
        [ 'осталось 5\u00a0дней', 5 * DAY ],
        [ 'остался 51\u00a0день', 51 * DAY ],
    ])('%s', (expected, timestamp) => {
        const tree = shallow(<DaysLeft timestamp={ Date.now() + timestamp } type={ TYPES.LEFT }/>);
        expect(tree).toHaveText(expected);
    });
});

describe('TYPES.WORKS', () => {
    it.each<[string, number]>([
        [ 'Действует ещё 1\u00a0секунду', SECOND ],
        [ 'Действует ещё 3\u00a0секунды', 3 * SECOND ],
        [ 'Действует ещё 5\u00a0секунд', 5 * SECOND ],
        [ 'Действует ещё 31\u00a0секунду', 31 * SECOND ],

        [ 'Действует ещё 1\u00a0минуту', MINUTE ],
        [ 'Действует ещё 3\u00a0минуты', 3 * MINUTE ],
        [ 'Действует ещё 5\u00a0минут', 5 * MINUTE ],
        [ 'Действует ещё 41\u00a0минуту', 41 * MINUTE ],

        [ 'Действует ещё 1\u00a0час', HOUR ],
        [ 'Действует ещё 3\u00a0часа', 3 * HOUR ],
        [ 'Действует ещё 5\u00a0часов', 5 * HOUR ],
        [ 'Действует ещё 21\u00a0час', 21 * HOUR ],
        [ 'Действует ещё 23\u00a0часа', 23 * HOUR ],
        [ 'Действует ещё 1\u00a0день', 24 * HOUR ],
        [ 'Действует ещё 1\u00a0день', 25 * HOUR ],

        [ 'Действует ещё 1\u00a0день', DAY ],
        [ 'Действует ещё 3\u00a0дня', 3 * DAY ],
        [ 'Действует ещё 5\u00a0дней', 5 * DAY ],
        [ 'Действует ещё 51\u00a0день', 51 * DAY ],
    ])('%s', (expected, timestamp) => {
        const tree = shallow(<DaysLeft timestamp={ Date.now() + timestamp } type={ TYPES.WORKS }/>);
        expect(tree).toHaveText(expected);
    });
});

describe('TYPES.NO_PREFIX', () => {
    it.each<[string, number]>([
        [ '1\u00a0секунда', SECOND ],
        [ '3\u00a0секунды', 3 * SECOND ],
        [ '5\u00a0секунд', 5 * SECOND ],
        [ '31\u00a0секунда', 31 * SECOND ],

        [ '1\u00a0минута', MINUTE ],
        [ '3\u00a0минуты', 3 * MINUTE ],
        [ '5\u00a0минут', 5 * MINUTE ],
        [ '41\u00a0минута', 41 * MINUTE ],

        [ '1\u00a0час', HOUR ],
        [ '3\u00a0часа', 3 * HOUR ],
        [ '5\u00a0часов', 5 * HOUR ],
        [ '21\u00a0час', 21 * HOUR ],
        [ '23\u00a0часа', 23 * HOUR ],
        [ '1\u00a0день', 24 * HOUR ],
        [ '1\u00a0день', 25 * HOUR ],

        [ '1\u00a0день', DAY ],
        [ '3\u00a0дня', 3 * DAY ],
        [ '5\u00a0дней', 5 * DAY ],
        [ '51\u00a0день', 51 * DAY ],
    ])('%s', (expected, timestamp) => {
        const tree = shallow(<DaysLeft timestamp={ Date.now() + timestamp } type={ TYPES.NO_PREFIX }/>);
        expect(tree).toHaveText(expected);
    });
});
