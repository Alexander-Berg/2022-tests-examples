import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';

import { Damage_CarPart, Damage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

const Context = createContextProvider(contextMock);

import DamagePopup from './DamagePopup';

const damage = {
    car_part: Damage_CarPart.FRONT_BUMPER,
    type: [ Damage_DamageType.DENT ],
    description: 'description',
};

const onDamageChange = jest.fn();
const onDamageRemove = jest.fn();

it('если не передали повреждение, то все поля в DamagePopup будут пустыми', () => {
    const { getAllByRole, getByRole } = render(
        <DamagePopup
            anchorRef={ null }
            isVisible
            damage={ null }
            hidePopup={ jest.fn() }
            onDamageChange={ jest.fn() }
            onDamageRemove={ jest.fn() }
            index={ 0 }
        />,
    );

    const checkboxes = (getAllByRole('checkbox') as Array<HTMLInputElement>).every((value) => value.checked);
    const textArea = getByRole('textbox');
    expect(checkboxes).toBe(false);
    expect(textArea).toHaveValue('');
});

it('если передали повреждение, то поля в DamagePopup будут заполнены из повреждения', () => {
    const { getAllByRole, getByRole } = render(
        <DamagePopup
            anchorRef={ null }
            isVisible
            damage={ damage }
            hidePopup={ jest.fn() }
            onDamageChange={ jest.fn() }
            onDamageRemove={ jest.fn() }
            index={ 0 }
        />,
    );
    const checkboxes = (getAllByRole('checkbox') as Array<HTMLInputElement>).filter((value) => value.checked);
    const textArea = getByRole('textbox');

    expect(checkboxes).toHaveLength(damage.type.length);
    expect(textArea).toHaveValue(damage.description);
});

it('при клике на удалить и сохранить вызываются нужные методы', () => {
    const { getByRole } = render(
        <Context >
            <DamagePopup
                anchorRef={ null }
                isVisible
                damage={ null }
                hidePopup={ jest.fn() }
                onDamageChange={ onDamageChange }
                onDamageRemove={ onDamageRemove }
                index={ 0 }
            />
        </Context>,
    );

    const saveButton = getByRole('button', { name: 'Сохранить' });
    const removeButton = getByRole('button', { name: 'Удалить' });

    userEvent.click(saveButton);

    expect(onDamageChange).toHaveBeenCalledTimes(1);

    userEvent.click(removeButton);

    expect(onDamageRemove).toHaveBeenCalledTimes(1);
});
