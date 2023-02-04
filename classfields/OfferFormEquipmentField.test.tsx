import React from 'react';
import userEvent from '@testing-library/user-event';
jest.mock('www-poffer/react/utils/storeDeselectedOptions');
jest.mock('auto-core/react/dataDomain/parsedOptions/actions/updatedDeselectedOptions');

import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import catalogOptionsStateMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import updatedDeselectedOptions from 'auto-core/react/dataDomain/parsedOptions/actions/updatedDeselectedOptions';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import complectationMock from 'auto-core/models/catalogSuggest/mocks/complectation.mock';
import catalogSuggestMock from 'auto-core/models/catalogSuggest/mocks';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import storeDeselectedOptions from 'www-poffer/react/utils/storeDeselectedOptions';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';

import type { Props } from './OfferFormEquipmentField';
import OfferFormEquipmentField from './OfferFormEquipmentField';

let defaultProps: Props;
let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultProps = {
        isCurtainOpened: false,
        setCurtainVisibility: jest.fn(),
    };

    defaultState = {
        catalogSuggest: catalogSuggestStateMock.value(),
        catalogOptions: catalogOptionsStateMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
        offerDraft: offerDraftMock.value(),
        parsedOptions: parsedOptionsMock.value(),
    };
});

it('открывает шторку', async() => {
    const { getByRole } = await renderComponent(<OfferFormEquipmentField { ...defaultProps }/>, { state: defaultState });
    const cutLink = getByRole('button', { name: /показать опции/i });
    userEvent.click(cutLink);

    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledTimes(1);
    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledWith(true);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: FieldNames.COMPLECTATION, level_5: 'show_more' });
});

it('закрывает шторку', async() => {
    const props = { ...defaultProps, isCurtainOpened: true };
    const { getByRole } = await renderComponent(<OfferFormEquipmentField { ...props }/>, { state: defaultState });
    const buttonSave = getByRole('button', { name: /сохранить/i });
    userEvent.click(buttonSave);

    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledTimes(1);
    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledWith(false);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: FieldNames.EQUIPMENT, level_5: 'curtain', level_6: 'close' });
});

describe('изменение опций', () => {
    it('меняет опции при выборе', async() => {
        const state = {
            ...defaultState,
            catalogSuggest: catalogSuggestStateMock.withData(
                catalogSuggestMock.withComplectations([]).value(),
            ).value(),
        };
        const initialValues = {
            [FieldNames.EQUIPMENT]: {},
        };
        const { getByRole } = await renderComponent(<OfferFormEquipmentField { ...defaultProps }/>, { state, initialValues });

        let espCheckbox = getByRole('button', { name: /esp/i }) as HTMLInputElement;
        userEvent.click(espCheckbox);
        expect(espCheckbox.className).toContain('Button_checked');

        espCheckbox = getByRole('button', { name: /esp/i }) as HTMLInputElement;
        userEvent.click(espCheckbox);
        expect(espCheckbox.className).not.toContain('Button_checked');
    });

    it('если комплектация не выбрана при выборе опции выберет кастомную комплектацию', async() => {
        const props = { ...defaultProps, isCurtainOpened: true };
        const { getByLabelText, getByRole } = await renderComponent(<OfferFormEquipmentField { ...props }/>, { state: defaultState });

        const absCheckbox = getByLabelText(/abs/i) as HTMLInputElement;
        userEvent.click(absCheckbox);

        const customComplectation = getByRole('button', { name: /другая/i }) as HTMLInputElement;
        expect(customComplectation.className).toContain('Button_checked');
    });

    it('если комплектация выбрана при выборе опции не выберет другую комплектацию', async() => {
        const props = { ...defaultProps, isCurtainOpened: true };
        const initialValues = {
            [FieldNames.COMPLECTATION]: '21042883',
        };
        const { getByLabelText, getByRole } = await renderComponent(<OfferFormEquipmentField { ...props }/>, { state: defaultState, initialValues });

        const absCheckbox = getByLabelText(/abs/i) as HTMLInputElement;
        userEvent.click(absCheckbox);

        const prestigeComplectation = getByRole('button', { name: /prestige/i }) as HTMLInputElement;
        const customComplectation = getByRole('button', { name: /другая/i }) as HTMLInputElement;

        expect(customComplectation.className).not.toContain('Button_checked');
        expect(prestigeComplectation.className).toContain('Button_checked');
    });
});

it('если в выбранных опциях есть те которые распарсили, обновит их список в ЛС и сторе', async() => {
    const state = {
        ...defaultState,
        catalogSuggest: catalogSuggestStateMock.withData(
            catalogSuggestMock.withComplectations([]).value(),
        ).value(),
    };
    const initialValues = {
        [FieldNames.EQUIPMENT]: {
            esp: true,
        },
    };
    const { getByRole } = await renderComponent(<OfferFormEquipmentField { ...defaultProps }/>, { state, initialValues });

    const espCheckbox = getByRole('button', { name: /esp/i }) as HTMLInputElement;
    userEvent.click(espCheckbox);
    expect(storeDeselectedOptions).toHaveBeenCalledTimes(1);
    expect(storeDeselectedOptions).toHaveBeenNthCalledWith(1, { esp: false }, 'draft_id');
    expect(updatedDeselectedOptions).toHaveBeenCalledTimes(1);
    expect(updatedDeselectedOptions).toHaveBeenNthCalledWith(1, { esp: false });

    const usbCheckbox = getByRole('button', { name: /usb/i }) as HTMLInputElement;
    userEvent.click(usbCheckbox);
    expect(storeDeselectedOptions).toHaveBeenCalledTimes(2);
    expect(storeDeselectedOptions).toHaveBeenNthCalledWith(2, { esp: false, usb: true }, 'draft_id');
    expect(updatedDeselectedOptions).toHaveBeenCalledTimes(2);
    expect(updatedDeselectedOptions).toHaveBeenNthCalledWith(2, { esp: false, usb: true });
});

it('меняет комплектацию', async() => {
    const initialValues = {
        [FieldNames.COMPLECTATION]: '01',
    };
    const state = {
        ...defaultState,
        catalogSuggest: catalogSuggestStateMock.withData(
            catalogSuggestMock.withComplectations([
                complectationMock.withId('01').withName('Prestige').value(),
                complectationMock.withId('02').withName('Elite').value(),
                complectationMock.withId('03').withName('Exclusive').value(),
            ]).value(),
        ).value(),
    };
    const props = { ...defaultProps, isCurtainOpened: true };

    const { getByRole } = await renderComponent(<OfferFormEquipmentField { ...props }/>, { state, initialValues });

    let eliteComplectation = getByRole('button', { name: /elite/i }) as HTMLInputElement;
    userEvent.click(eliteComplectation);

    eliteComplectation = getByRole('button', { name: /elite/i }) as HTMLInputElement;

    expect(eliteComplectation.className).toContain('Button_checked');
});
