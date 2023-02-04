import React from 'react';
import userEvent from '@testing-library/user-event';

import selectItemInSelect from 'autoru-frontend/jest/unit/selectItemInSelect';

import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import optionsMock from 'auto-core/models/equipment/mock';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';

import type { Props } from './OfferFormEquipmentFieldCurtain';
import OfferFormEquipmentFieldCurtain from './OfferFormEquipmentFieldCurtain';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        complectations: [],
        selectedComplectation: '',
        options: optionsMock,
        selectedOptions: {},
        isOpened: true,
        onClose: jest.fn(),
        onComplectationChange: jest.fn(),
        onOptionChange: jest.fn(),
    };
});

it('позволяет пользователю переключаться между популярными и всеми опциями', async() => {
    const { getByRole, queryByLabelText } = await renderComponent(<OfferFormEquipmentFieldCurtain { ...defaultProps }/>);
    let absCheckbox = queryByLabelText('ABS');
    let espCheckbox = queryByLabelText('ESP');

    expect(absCheckbox).not.toBeNull();
    expect(espCheckbox).toBeNull();

    const searchInput = getByRole('textbox', { name: /поиск/i });
    userEvent.type(searchInput, 'abs');

    let toggler = getByRole('button', { name: /показать все опции/i });
    userEvent.click(toggler);

    absCheckbox = queryByLabelText('ABS');
    espCheckbox = queryByLabelText('ESP');

    expect(absCheckbox).not.toBeNull();
    expect(espCheckbox).not.toBeNull();
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { field: FieldNames.EQUIPMENT, level_5: 'curtain', level_6: 'show_all' });

    toggler = getByRole('button', { name: /показать только популярные/i });
    userEvent.click(toggler);

    absCheckbox = queryByLabelText('ABS');
    espCheckbox = queryByLabelText('ESP');

    expect(absCheckbox).not.toBeNull();
    expect(espCheckbox).toBeNull();
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog)
        .toHaveBeenNthCalledWith(2, { field: FieldNames.EQUIPMENT, level_5: 'curtain', level_6: 'show_popular' });
});

it('оставляет в списке непопулярную опцию, если пользователь ее нажал, а потом отжал', async() => {
    const { getByLabelText, getByRole } = await renderComponent(<OfferFormEquipmentFieldCurtain { ...defaultProps }/>);

    let toggler = getByRole('button', { name: /показать все опции/i });
    userEvent.click(toggler);

    let espCheckbox = getByLabelText('ESP');
    userEvent.click(espCheckbox);

    toggler = getByRole('button', { name: /показать только популярные/i });
    userEvent.click(toggler);

    espCheckbox = getByLabelText('ESP');
    userEvent.click(espCheckbox);

    expect(espCheckbox).not.toBeNull();
});

it('строка поиска фильтрует список', async() => {
    const { getAllByRole, getByRole } = await renderComponent(<OfferFormEquipmentFieldCurtain { ...defaultProps }/>);

    let checkboxes = getAllByRole('checkbox') as Array<HTMLInputElement>;

    expect(checkboxes).toHaveLength(1);
    expect(checkboxes[0].value).toBe('abs');

    const searchInput = getByRole('textbox', { name: /поиск/i });
    userEvent.type(searchInput, 'esp');

    checkboxes = getAllByRole('checkbox') as Array<HTMLInputElement>;

    expect(checkboxes).toHaveLength(1);
    expect(checkboxes[0].value).toBe('esp');
});

describe('вызывает onOptionChange', () => {
    it('при изменении чекбокса', async() => {
        const { getByLabelText } = await renderComponent(<OfferFormEquipmentFieldCurtain { ...defaultProps }/>);

        const absCheckbox = getByLabelText('ABS');
        userEvent.click(absCheckbox);

        expect(defaultProps.onOptionChange).toHaveBeenCalledTimes(1);
        expect(defaultProps.onOptionChange).toHaveBeenNthCalledWith(1, { abs: true });
    });

    it('при изменении селекта', async() => {
        const { getByRole } = await renderComponent(<OfferFormEquipmentFieldCurtain { ...defaultProps }/>);

        const airBagSelect = getByRole('button', { name: /подушки безопасности/i });
        await selectItemInSelect(airBagSelect, /водителя/i);

        expect(defaultProps.onOptionChange).toHaveBeenCalledTimes(1);
        expect(defaultProps.onOptionChange).toHaveBeenNthCalledWith(1, { 'airbag-driver': true, 'airbag-passenger': false });
    });
});
