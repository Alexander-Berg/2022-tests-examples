jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});
jest.mock('lodash', () => ({
    ...jest.requireActual('lodash'),
    debounce: (fn: any) => {
        fn.cancel = jest.fn();
        return fn;
    },
}));

import { renderHook } from '@testing-library/react-hooks';
import fetchMock from 'jest-fetch-mock';
import flushPromises from 'jest/unit/flushPromises';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import optionsMock from 'auto-core/models/equipment/mocks/option.mock';

import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import useParseOptions from './useParseOptions';

const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;

let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultState = {
        parsedOptions: parsedOptionsMock.value(),
        catalogOptions: catalogOptionsMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
    };
});

afterEach(() => {
    fetchMock.resetMocks();
    jest.restoreAllMocks();
});

it('запросит ресурс и после отфильтрует распарсенные опции и добавить их в поле формы', async() => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: OfferFormFieldNamesType) => {
            switch (fieldName) {
                case FieldNames.EQUIPMENT:
                    return {
                        gbo: true,
                        'passenger-airbag': false,
                    } as any;
                default:
                    return;
            }
        }),
    });
    fetchMock.mockResponse((req) => {
        return req.url === '/-/ajax/jest/parseOptions/' ?
            Promise.resolve(JSON.stringify({ options: [
                { value: optionsMock.withCode('panorama-roof').value() },
                { value: optionsMock.withCode('alarm').value() },
                { value: optionsMock.withCode('usb').value() },
                { value: optionsMock.withCode('foo').value() },
            ] })) :
            Promise.resolve(JSON.stringify({}));
    });
    const store = mockStore(defaultState);

    mockUseSelector(defaultState);
    mockUseDispatch(store);

    const { result } = renderHook(() => useParseOptions());
    result.current('some text');

    await flushPromises();

    expect(fetchMock).toHaveBeenCalledTimes(1);

    const url = fetchMock.mock.calls[0][0];
    const text = JSON.parse(fetchMock.mock.calls[0][1]?.body?.toString() || '').description;
    expect(url).toBe('/-/ajax/jest/parseOptions/');
    expect(text).toBe('some text');

    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledTimes(1);
    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledWith(FieldNames.EQUIPMENT, {
        alarm: true,
        gbo: true,
        'panorama-roof': true,
        'passenger-airbag': false,
    });
});
