import React from 'react';
import userEvent from '@testing-library/user-event';

import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import catalogOptionsStateMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import catalogSuggestMock from 'auto-core/models/catalogSuggest/mocks';
import complectationMock from 'auto-core/models/catalogSuggest/mocks/complectation.mock';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { AppState } from 'www-poffer/react/store/AppState';

import type { Props } from './OfferFormComplectationField';
import OfferFormComplectationField from './OfferFormComplectationField';

let defaultProps: Props;
let defaultState: Partial<AppState>;

beforeEach(() => {
    const catalogSuggest = catalogSuggestMock.withComplectations([
        complectationMock.withId('01').withName('Prestige').value(),
        complectationMock.withId('02').withName('Elite').value(),
        complectationMock.withId('03').withName('Exclusive').value(),
    ]).value();

    defaultProps = {
        suggest: catalogSuggest?.complectations,
        setCurtainVisibility: jest.fn(),
    };

    defaultState = {
        catalogSuggest: catalogSuggestStateMock.withData(catalogSuggest).value(),
        catalogOptions: catalogOptionsStateMock.value(),
        parsedOptions: parsedOptionsMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
    };
});

it('меняет комплектацию', async() => {
    const initialValues = {
        [FieldNames.COMPLECTATION]: '01',
    };

    const { getByRole } = await renderComponent(<OfferFormComplectationField { ...defaultProps }/>, { initialValues, state: defaultState });

    let eliteComplectation = getByRole('button', { name: /elite/i }) as HTMLInputElement;
    userEvent.click(eliteComplectation);

    eliteComplectation = getByRole('button', { name: /elite/i }) as HTMLInputElement;

    expect(eliteComplectation.className).toContain('Button_checked');
});

it('при выборе кастомной комплектации открывает шторку', async() => {
    const { getByRole } = await renderComponent(<OfferFormComplectationField { ...defaultProps }/>, { state: defaultState });

    const eliteComplectation = getByRole('button', { name: /другая/i }) as HTMLInputElement;
    userEvent.click(eliteComplectation);

    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledTimes(1);
    expect(defaultProps.setCurtainVisibility).toHaveBeenCalledWith(true);
});
