jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});

import { renderHook } from '@testing-library/react-hooks';

import { Car_BodyType, Car_EngineType, Car_GearType, Car_Transmission } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import { nbsp } from 'auto-core/react/lib/html-entities';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import catalogSuggestMock from 'auto-core/models/catalogSuggest/mocks';

import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';

import { sections } from '../sections/sections';

import useGetFieldsToAutoSelect from './useGetFieldsToAutoSelect';

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<FieldNames, Fields, FieldErrors>>;

it('правильно формирует поля для автоселекта когда все параметры имеют один вариант', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValues: jest.fn(() => {
            return {
                [FieldNames.YEAR]: 2019,
                [FieldNames.BODY_TYPE]: Car_BodyType.SEDAN,
            };
        }),
    });
    const catalogSuggest = catalogSuggestMock
        .withEngineTypes([ Car_EngineType.DIESEL ])
        .withGearTypes([ Car_GearType.FORWARD_CONTROL ])
        .withTransmissionTypes([ Car_Transmission.MECHANICAL ])
        .value();

    const { result: getFieldsToAutoSelect } = renderHook(() => useGetFieldsToAutoSelect(sections));
    const result = getFieldsToAutoSelect?.current(catalogSuggest);

    expect(result).toEqual([
        { fieldName: FieldNames.SUPER_GEN, newValue: { data: '21028015', text: 'IV' } },
        { fieldName: FieldNames.ENGINE_TYPE, newValue: Car_EngineType.DIESEL },
        { fieldName: FieldNames.GEAR_TYPE, newValue: Car_GearType.FORWARD_CONTROL },
        { fieldName: FieldNames.TRANSMISSION, newValue: Car_Transmission.MECHANICAL },
        { fieldName: FieldNames.TECH_PARAM, newValue: { data: '21028062', text: `100${ nbsp }л.с. (1.4 MT)` } },
    ]);
});

it('правильно формирует поля для автоселекта когда один из параметров имеет множественный выбор', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValues: jest.fn(() => {
            return {
                [FieldNames.YEAR]: 2019,
                [FieldNames.BODY_TYPE]: Car_BodyType.SEDAN,
            };
        }),
    });
    const catalogSuggest = catalogSuggestMock
        .withEngineTypes([ Car_EngineType.DIESEL ])
        .withGearTypes([ Car_GearType.FORWARD_CONTROL, Car_GearType.REAR_DRIVE ])
        .withTransmissionTypes([ Car_Transmission.MECHANICAL ])
        .value();

    const { result: getFieldsToAutoSelect } = renderHook(() => useGetFieldsToAutoSelect(sections));
    const result = getFieldsToAutoSelect?.current(catalogSuggest);

    expect(result).toEqual([
        { fieldName: FieldNames.SUPER_GEN, newValue: { data: '21028015', text: 'IV' } },
        { fieldName: FieldNames.ENGINE_TYPE, newValue: Car_EngineType.DIESEL },
    ]);
});
