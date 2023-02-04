import React from 'react';
import userEvent from '@testing-library/user-event';

import { renderComponent } from 'www-poffer/react/utils/testUtils';

import type { Props } from './EquipmentSelectorCurtain';
import EquipmentSelectorCurtain from './EquipmentSelectorCurtain';

let defaultProps: Props;

beforeEach(() => {
    const dictionary = {
        abs: { code: 'abs', group: 'Безопасность', name: 'Антиблокировочная система (ABS)' },
        'airbag-driver': { code: 'airbag-driver', group: 'Безопасность', name: 'Подушка безопасности водителя' },
        aux: { code: 'aux', group: 'Мультимедиа', name: 'AUX' },
        esp: { code: 'esp', group: 'Безопасность', name: 'Система стабилизации (ESP)' },
    };

    defaultProps = {
        dictionary,
        options: [ 'abs', 'airbag-driver', 'aux', 'esp' ],
        isOpened: true,
        addMode: true,
        onClose: jest.fn(),
        onSave: jest.fn(),
    };
});

it('клик на "добавить" передает все выбранные опции в коллбэк', async() => {
    const { getByRole } = await renderComponent(<EquipmentSelectorCurtain { ...defaultProps }/>);

    const espCheckbox = getByRole('checkbox', { name: /esp/i }) as HTMLInputElement;
    userEvent.click(espCheckbox);

    const buttonSave = getByRole('button', { name: /добавить/i });
    userEvent.click(buttonSave);

    expect(defaultProps.onSave).toHaveBeenCalledTimes(1);
    expect(defaultProps.onSave).toHaveBeenCalledWith([ 'abs', 'airbag-driver', 'aux' ]);
});

it('клик на "сбросить" сбросит все чекбоксы', async() => {
    const { getAllByRole, getByRole } = await renderComponent(<EquipmentSelectorCurtain { ...defaultProps }/>);

    const buttonReset = getByRole('button', { name: /сбросить/i });
    userEvent.click(buttonReset);

    const checkedCheckboxes = (getAllByRole('checkbox') as Array<HTMLInputElement>).filter(({ checked }) => checked);

    expect(checkedCheckboxes).toHaveLength(0);
});
