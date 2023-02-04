import userEvent from '@testing-library/user-event';
import { render } from '@testing-library/react';
import React from 'react';

import { Damage_DamageType, Damage_CarPart } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { Damage } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import DamageDot, { DAMAGE_DOT_MODE } from './DamageDot';

const onItemClick = jest.fn();

const DAMAGE_INDEX = 0;

const damage = {
    car_part: Damage_CarPart.FRONT_BUMPER,
    type: [ Damage_DamageType.DENT, Damage_DamageType.DYED ],
    description: '',
} as Damage;

describe(`MODE=${ DAMAGE_DOT_MODE.VIEW }`, () => {
    it('не отображаем, если нет повреждения', async() => {
        const { queryAllByRole } = await render(<DamageDot mode={ DAMAGE_DOT_MODE.VIEW } index={ DAMAGE_INDEX }/>);
        const dot = queryAllByRole('button');

        expect(dot).toHaveLength(0);
    });

    it('отображаем, если есть повреждение', async() => {
        const { getByRole } = await render(<DamageDot mode={ DAMAGE_DOT_MODE.VIEW } damage={ damage } index={ DAMAGE_INDEX }/>);
        const dot = getByRole('button');

        expect(dot).not.toBeNull();
    });
});

describe(`MODE=${ DAMAGE_DOT_MODE.ADD }`, () => {
    it('отображаем кнопку с +', async() => {
        const { getByRole } = await render(<DamageDot mode={ DAMAGE_DOT_MODE.ADD } index={ DAMAGE_INDEX } onItemClick={ onItemClick }/>);
        const dot = getByRole('button', { name: '+' });

        userEvent.click(dot);

        expect(onItemClick).toHaveBeenCalledTimes(1);
        expect(onItemClick.mock.calls[0][0]).toBe(DAMAGE_INDEX);

        expect(dot).not.toBeNull();
    });
});

describe(`MODE=${ DAMAGE_DOT_MODE.EDIT }`, () => {
    it('отображаем кнопку в режиме редактирования, при клике вызываем метод onItemClick', async() => {
        const { getByRole } = await render(<DamageDot damage={ damage } mode={ DAMAGE_DOT_MODE.EDIT } index={ DAMAGE_INDEX } onItemClick={ onItemClick }/>);
        const dot = getByRole('button');
        userEvent.click(dot);

        expect(onItemClick).toHaveBeenCalledTimes(1);
        expect(onItemClick.mock.calls[0][0]).toBe(DAMAGE_INDEX);

        expect(dot).not.toBeNull();
    });
});
