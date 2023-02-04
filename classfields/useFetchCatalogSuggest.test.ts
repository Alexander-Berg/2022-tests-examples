jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/catalogSuggest/actions/fetch');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import { renderHook } from '@testing-library/react-hooks';
import { useDispatch } from 'react-redux';

import type { CarSuggest } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_helper_model';
import { Car_BodyType, Car_EngineType, Car_GearType, Car_Transmission } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import fetchCatalogSuggest from 'auto-core/react/dataDomain/catalogSuggest/actions/fetch';
import { showAutoclosableErrorMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';
import type { Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { formContextMock } from 'auto-core/react/components/common/Form/contexts/FormContext.mock';

import useFetchCatalogSuggest from './useFetchCatalogSuggest';

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<FieldNames, Fields, FieldErrors>>;
const fetchCatalogSuggestMock = fetchCatalogSuggest as jest.MockedFunction<typeof fetchCatalogSuggest>;
const showAutoclosableErrorMessageMock = showAutoclosableErrorMessage as jest.MockedFunction<typeof showAutoclosableErrorMessage>;

const coreFormContextMock = {
    ...formContextMock,
    requiredErrorType: FieldErrors.REQUIRED,
};
const initialFields = {
    [FieldNames.MARK]: {
        data: 'KIA',
        text: 'Kia',
    },
    [FieldNames.MODEL]: {
        data: 'RIO',
        text: 'Rio',
    },
    [FieldNames.YEAR]: 2019,
    [FieldNames.BODY_TYPE]: Car_BodyType.SEDAN,
    [FieldNames.SUPER_GEN]: {
        data: '21028015',
        text: 'IV',
    },
    [FieldNames.ENGINE_TYPE]: Car_EngineType.DIESEL,
    [FieldNames.GEAR_TYPE]: Car_GearType.REAR_DRIVE,
    [FieldNames.TRANSMISSION]: Car_Transmission.AUTOMATIC,
};

it('сделает запрос с правильными параметрами', async() => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValues: jest.fn(() => initialFields),
    });
    fetchCatalogSuggestMock.mockReturnValue(() => Promise.resolve({} as CarSuggest));
    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockImplementation(() => (fn: any) => fn());

    const { result: fetchCatalogSuggestCaller } = renderHook(() => useFetchCatalogSuggest());

    await fetchCatalogSuggestCaller?.current(FieldNames.GEAR_TYPE);
    expect(fetchCatalogSuggestMock).toHaveBeenCalledTimes(1);
    expect(fetchCatalogSuggestMock).toHaveBeenCalledWith({
        mark: initialFields[FieldNames.MARK].data,
        model: initialFields[FieldNames.MODEL].data,
        year: initialFields[FieldNames.YEAR],
        body_type: initialFields[FieldNames.BODY_TYPE],
        engine_type: initialFields[FieldNames.ENGINE_TYPE],
        super_gen: initialFields[FieldNames.SUPER_GEN].data,
        gear_type: undefined,
        tech_param_id: undefined,
        transmission: undefined,
    });
});

it('при ошибке покажет нотификацию', async() => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValues: jest.fn(() => initialFields),
    });
    fetchCatalogSuggestMock.mockReturnValue(() => Promise.resolve({ error: 'UNKNOWN_ERROR' }));
    showAutoclosableErrorMessageMock.mockReturnValue(() => { });
    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockImplementation(() => (fn: any) => fn());

    const { result: fetchCatalogSuggestCaller } = renderHook(() => useFetchCatalogSuggest());

    await fetchCatalogSuggestCaller?.current(FieldNames.GEAR_TYPE);
    expect(showAutoclosableErrorMessageMock).toHaveBeenCalledTimes(1);
});
