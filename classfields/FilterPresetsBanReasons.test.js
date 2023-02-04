import React from 'react';

import '@testing-library/jest-dom';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import FilterPresetsBanReasons from './FilterPresetsBanReasons';

it('должен нарисовать 3 отдельных пресета', async() => {
    const { findAllByRole } = await renderComponent(<FilterPresetsBanReasons
        banReasons={ [
            { ban_reason: 'user_banned', count: '1', title: 'Кабинет заблокирован' },
            { ban_reason: 'user_banned', count: '2', title: 'Кабинет заблокирован 2' },
            { ban_reason: 'user_banned', count: '3', title: 'Кабинет заблокирован 3' },
        ] }
        routeParams={{ all: '1', category: 'moto' }}
    />);
    const buttons = await findAllByRole('button');
    expect(buttons).toHaveLength(3);
});

it('должен нарисовать 1 пресет, если причин блокировки больше 3', async() => {
    const { findAllByRole } = await renderComponent(<FilterPresetsBanReasons
        banReasons={ [
            { ban_reason: 'user_banned', count: '1', title: 'Кабинет заблокирован' },
            { ban_reason: 'user_banned', count: '2', title: 'Кабинет заблокирован 2' },
            { ban_reason: 'user_banned', count: '3', title: 'Кабинет заблокирован 3' },
            { ban_reason: 'user_banned', count: '3', title: 'Кабинет заблокирован 4' },
        ] }
        routeParams={{ all: '1', category: 'moto' }}
    />);
    const preset = await findAllByRole('button');
    expect(preset).toHaveLength(1);
});
