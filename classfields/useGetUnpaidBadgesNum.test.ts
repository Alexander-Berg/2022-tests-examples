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

import { renderHook } from '@testing-library/react-hooks';
import { useSelector } from 'react-redux';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import configMock from 'auto-core/react/dataDomain/config/mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';

import useGetUnpaidBadgesNum from './useGetUnpaidBadgesNum';

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;

it('правильно считает кол-во неоплаченных бейджей при размещении', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: OfferFormFieldNamesType) => {
            return fieldName === OfferFormFieldNames.BADGES ? [ 'one', 'two', 'three' ] as any : undefined;
        }),
    });
    const state = {
        config: configMock.withPageParams({ form_type: 'add' }).value(),
        offerDraft: offerDraftMock.withOfferMock(cloneOfferWithHelpers(offerMock).withBadges([ 'three' ])),
    };

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    const { result } = renderHook(() => useGetUnpaidBadgesNum());

    expect(result.current).toBe(3);
});

it('правильно считает кол-во неоплаченных бейджей при редактировании', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: OfferFormFieldNamesType) => {
            return fieldName === OfferFormFieldNames.BADGES ? [ 'one', 'two', 'three' ] as any : undefined;
        }),
    });
    const state = {
        config: configMock.withPageParams({ form_type: 'edit' }).value(),
        offerDraft: offerDraftMock.withOfferMock(cloneOfferWithHelpers(offerMock).withBadges([ 'three' ])),
    };

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    const { result } = renderHook(() => useGetUnpaidBadgesNum());

    expect(result.current).toBe(2);
});

it('правильно считает кол-во неоплаченных бейджей при редактировании, если часть бейджей отжали', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: OfferFormFieldNamesType) => {
            return fieldName === OfferFormFieldNames.BADGES ? [ 'one' ] as any : undefined;
        }),
    });
    const state = {
        config: configMock.withPageParams({ form_type: 'edit' }).value(),
        offerDraft: offerDraftMock.withOfferMock(cloneOfferWithHelpers(offerMock).withBadges([ 'one', 'two' ])),
    };

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(state));

    const { result } = renderHook(() => useGetUnpaidBadgesNum());

    expect(result.current).toBe(0);
});
