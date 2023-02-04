import { renderHook, act } from '@testing-library/react-hooks';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import configMock from 'auto-core/react/dataDomain/config/mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { AppState } from 'www-poffer/react/store/AppState';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { OfferAccordionSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';

import { useContextValue, FormFeatures } from './offerFormPage';

let sendParamsMock: (params: Array<string>) => void;
let state: Partial<AppState>;

beforeEach(() => {
    sendParamsMock = jest.fn();

    state = {
        config: configMock.value(),
        offerDraft: offerDraftMock.value(),
    };
});

it('правильно обрабатывает сабмит и роллбэк формы', () => {
    applyUseSelectorMock().mockUseSelector(state);
    const { result } = renderHook(() => useContextValue(sendParamsMock));

    expect(result.current.isPending).toBe(false);
    expect(result.current.cart).toEqual([]);

    const cart = [ { name: TOfferVas.TURBO, count: 1 }, { name: TOfferVas.BADGE, count: 2 } ];

    act(() => {
        result.current.submitForm(cart);
    });

    expect(result.current.isPending).toBe(true);
    expect(result.current.cart).toEqual(cart);

    act(() => {
        result.current.rollbackForm();
    });

    expect(result.current.isPending).toBe(false);
    expect(result.current.cart).toEqual([]);
});

it('устанавливает ошибку в форме', () => {
    applyUseSelectorMock().mockUseSelector(state);
    const { result } = renderHook(() => useContextValue(sendParamsMock));

    const error = 'some error';
    act(() => {
        result.current.setFormError(error);
    });

    expect(result.current.formError).toBe(error);
});

describe('getMetrikaParams', () => {
    it('правильно формирует параметры для тача', () => {
        const initialState = {
            ...state,
            config: configMock.withBrowser({ isMobile: true }).value(),
        };

        applyUseSelectorMock().mockUseSelector(initialState);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ level_3: 'foo' });

        expect(params).toEqual([ 'M_FORM_EDIT', 'CARS', 'FOO' ]);
    });

    it('правильно формирует параметры если передан level_3', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ level_3: 'foo' });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'FOO' ]);
    });

    it('правильно формирует параметры для событий навигации', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ type: 'navigation', block: OfferAccordionSectionId.TECH });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'NAVIGATION', 'TECH' ]);
    });

    it('правильно формирует параметры для событий в блоке', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ block: OfferAccordionSectionId.TECH, event: 'click' });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'TECH', 'CLICK' ]);
    });

    it('правильно формирует параметры для событий в филде', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ field: FieldNames.BODY_TYPE, event: 'success', level_6: 'sedan' });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'TECH', 'BODY_TYPE', 'SUCCESS', 'SEDAN' ]);
    });

    it('правильно формирует параметры для кастомного события в филде', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ field: FieldNames.BODY_TYPE, level_5: 'foo', level_6: 'sedan' });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'TECH', 'BODY_TYPE', 'FOO', 'SEDAN' ]);
    });

    it('правильно формирует параметры для фичи', () => {
        applyUseSelectorMock().mockUseSelector(state);
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ feature: FormFeatures.VIN_OPTIONS, level_5: 'save' });

        expect(params).toEqual([ 'FORM_EDIT', 'CARS', 'FEATURE_VIN_OPTIONS', 'SAVE' ]);
    });

    it('правильно формирует параметры для формы размещения с пустым драфтом', () => {
        applyUseSelectorMock().mockUseSelector({
            config: configMock.withPageParams({ form_type: 'add' }),
            offerDraft: offerDraftMock.withOfferMock(cloneOfferWithHelpers(offerMock).withMarkInfo({})),
        });
        const { result } = renderHook(() => useContextValue(sendParamsMock));

        const params = result.current.getMetrikaParams({ level_3: 'foo' });

        expect(params).toEqual([ 'FORM_ADD_NEW', 'CARS', 'FOO' ]);
    });
});

it('отправляет лог в метрику', () => {
    applyUseSelectorMock().mockUseSelector(state);
    const { result } = renderHook(() => useContextValue(sendParamsMock));

    result.current.sendFormLog({ level_3: 'foo' });

    expect(sendParamsMock).toHaveBeenCalledTimes(1);
    expect(sendParamsMock).toHaveBeenCalledWith([ 'FORM_EDIT', 'CARS', 'FOO' ]);
});
