const mockSendFrontLogOnInputBlur = jest.fn();

jest.mock('www-poffer/react/hooks/useSendFormLogOnInputBlur', () => () => mockSendFrontLogOnInputBlur);

import React, { createRef } from 'react';
import { fireEvent, act } from '@testing-library/react';

import { Availability } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import '@testing-library/jest-dom';

import type { FormValidationResult, FormContext } from 'auto-core/react/components/common/Form/types';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { AppState } from 'www-poffer/react/store/AppState';

import OfferFormVinField from './OfferFormVinField';

type State = Partial<AppState>

let state: State;

beforeEach(() => {
    state = {
        offerDraft: offerDraftMock.value(),
    };
});

it('на blur посылает стату', async() => {
    expect.assertions(1);

    await renderComponent(<OfferFormVinField/>, { state });

    const input = document.querySelector('input');

    if (!input) {
        return;
    }

    await act(async() => {
        fireEvent.blur(input);
    });

    expect(mockSendFrontLogOnInputBlur).toHaveBeenCalledWith(OfferFormFieldNames.VIN);
});

describe('валидация', () => {
    it('обязательно, если тачка зарегана в рашке, после 1997, с левым рулем, не на дальнем востоке', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 1997,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '0' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(FieldErrors.REQUIRED);
    });

    it('обязательно для частника если авто не зарегистрировано в РФ', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 2015,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '73' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(FieldErrors.REQUIRED);
    });

    it('не обязательно для дилера если авто не зарегистрировано в РФ', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 2015,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '73' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state, offerFormContext: {
            ...offerFormPageContextMock,
            isDealer: true,
        } });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(undefined);
    });

    it('не обязательно если авто не в наличии', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 2015,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.AVAILABILITY]: Availability.ON_ORDER,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '73' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(undefined);
    });

    it('НЕ обязательно, если тачка зарегана в рашке, после 1997, с левым рулем, НА дальнем востоке', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 1997,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '73' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(undefined);
    });

    it('НЕ обязательно, если тачка зарегана в рашке, после 1997, с ПРАВЫМ рулем, не на дальнем востоке', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 1997,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: true,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '0' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(undefined);
    });

    it('НЕ обязательно, если тачка зарегана в рашке, ДО 1997, с левым рулем, не на дальнем востоке', async() => {
        const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: false,
            [OfferFormFieldNames.PURCHASE_DATE]: {
                year: 1996,
            },
            [FieldNames.RIGHT_STEERING_WHEEL]: false,
            [OfferFormFieldNames.LOCATION]: {
                parentRegionIds: [ '0' ],
            },
        };
        await renderComponent(<OfferFormVinField/>, { formApi, initialValues, state });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.VIN);
        });

        expect(error?.type).toEqual(undefined);
    });
});
