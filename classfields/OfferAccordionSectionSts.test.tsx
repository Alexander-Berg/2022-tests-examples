import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { OfferAccordionSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { AppState } from 'www-poffer/react/store/AppState';

import OfferAccordionSectionSts from './OfferAccordionSectionSts';

let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultState = {
        equipmentDictionary: equipmentDictionaryMock,
        catalogOptions: catalogOptionsMock.value(),
        offerDraft: offerDraftMock.value(),
        parsedOptions: parsedOptionsMock.value(),
    };
});

it('показывает модал с подсказкой при клике на ссылку', async() => {
    expect.assertions(2);

    const { getByRole, queryByRole } = await renderComponent(
        <OfferAccordionSectionSts id={ OfferAccordionSectionId.STS } initialIsCollapsed={ false } isVisible/>,
        { state: defaultState },
    );

    expect(queryByRole('dialog')).toBeNull();

    const link = getByRole('button', { name: /где найти vin и номер стс/i });
    userEvent.click(link);

    expect(queryByRole('dialog')).not.toBeNull();
});

it('сбрасывает значения госномера и СТС при установке галки "не рашка"', async() => {
    const initialValues = {
        [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
        [OfferFormFieldNames.GOS_NUMBER]: 'AA 123 77',
        [OfferFormFieldNames.STS]: '2222222222',
    };
    const { getByRole } = await renderComponent(
        <OfferAccordionSectionSts id={ OfferAccordionSectionId.STS } initialIsCollapsed={ false } isVisible/>,
        { state: defaultState, initialValues },
    );

    let licensePlateInput = getByRole('textbox', { name: /госномер/i }) as HTMLInputElement;
    expect(licensePlateInput.value).toBe('AA 123 77');

    let stsInput = getByRole('textbox', { name: /стс/i }) as HTMLInputElement;
    expect(stsInput.value).toBe('2222222222');

    const notRegisteredCheckbox = getByRole('checkbox', { name: /Автомобиль не на учёте в РФ/i }) as HTMLInputElement;
    userEvent.click(notRegisteredCheckbox);

    licensePlateInput = getByRole('textbox', { name: /госномер/i }) as HTMLInputElement;
    expect(licensePlateInput.value).toBe('');

    stsInput = getByRole('textbox', { name: /стс/i }) as HTMLInputElement;
    expect(stsInput.value).toBe('');
});
