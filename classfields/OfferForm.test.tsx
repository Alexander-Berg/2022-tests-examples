jest.mock('lodash', () => ({
    ...jest.requireActual('lodash'),
    debounce: (fn: any) => {
        fn.cancel = jest.fn(); return fn;
    },
}));

jest.mock('react-scroll', () => ({
    ...jest.requireActual('react-scroll'),
    scroller: {
        scrollTo: jest.fn(),
    },
}));
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/formVas/actions/submitVas');

import React from 'react';
import { Provider } from 'react-redux';
import { scroller } from 'react-scroll';
import { render, act } from '@testing-library/react';
import fetchMock from 'jest-fetch-mock';

import '@testing-library/jest-dom';

import { TradeInType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { ResponseStatus, ErrorCode } from '@vertis/schema-registry/ts-types-snake/auto/api/response_model';

import MockInputComponentGenerator from 'autoru-frontend/jest/unit/MockInputComponentGenerator';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import configMock from 'auto-core/react/dataDomain/config/mock';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { actionQueueContextMock } from 'auto-core/react/components/common/ActionQueue/contexts/ActionQueueContext.mock';
import { ActionQueueProvider } from 'auto-core/react/components/common/ActionQueue/contexts/ActionQueueContext';
import AccordionContext from 'auto-core/react/components/common/Accordion/AccordionContext';
import { AccordionContextMock } from 'auto-core/react/components/common/Accordion/AccordionContext.mock';
import getMileAge from 'auto-core/react/lib/offer/getMileAge';
import getYear from 'auto-core/react/lib/offer/getYear';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import submitVas from 'auto-core/react/dataDomain/formVas/actions/submitVas';
import useActionQueueCallback from 'auto-core/react/components/common/ActionQueue/hooks/useActionQueueCallback';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import YearField from 'auto-core/react/components/common/Form/fields/YearField/YearField';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TBillingFrom, TBillingPlatform } from 'auto-core/types/TBilling';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormPageContextProvider } from 'www-poffer/react/contexts/offerFormPage';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import OfferFormVinField from 'www-poffer/react/components/common/OfferForm/fields/OfferFormVinField/OfferFormVinField';
import OfferFormPurchaseDateField from 'www-poffer/react/components/desktop/OfferForm/fields/OfferFormPurchaseDateField/OfferFormPurchaseDateField';
import OfferFormWarrantyField from 'www-poffer/react/components/desktop/OfferForm/fields/OfferFormWarrantyField/OfferFormWarrantyField';
import OfferFormAddressLocationField from 'www-poffer/react/components/desktop/OfferForm/fields/OfferFormLocationField/OfferFormLocationField';

import OfferForm from './OfferForm';

type HandlerTypes = 'change' | 'select';
jest.mock('www-poffer/react/components/desktop/MapLocationPoffer/MapLocationPoffer',
    () => MockInputComponentGenerator<HandlerTypes>('MapLocationPoffer'));

const submitSuccessCallback = jest.fn();

const ComponentWithActionQueue = () => {
    useActionQueueCallback(submitSuccessCallback);

    return <div>FOO</div>;
};

type State = Partial<AppState>;

type Props = {
    apiRef: OfferFormRef;
}
let defaultState: State;
let defaultProps: Props;
type OfferFormContext = FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>;
type OfferFormRef = React.RefObject<OfferFormContext>;

beforeEach(() => {
    defaultState = {
        config: configMock.value(),
        offerDraft: offerDraftMock.value(),
    };

    defaultProps = {
        apiRef: React.createRef<OfferFormContext>(),
    };

    jest.spyOn(global.location, 'assign').mockImplementation(() => { });
});

afterEach(() => {
    fetchMock.resetMocks();
    jest.restoreAllMocks();
});

describe('при маунте', () => {
    it('логирует показ формы', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'show' });
    });

    it('отправляет цель если форма пустая', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(1, 'START_FORMS_ADD_OFFERS');
    });

    it('не отправляет цель если форма не пустая', async() => {
        await renderComponent({ state: defaultState, props: defaultProps, pageContext: { ...offerFormPageContextMock, isEmptyForm: false } });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(0);
    });
});

describe('сохранение драфт при изменении формы', () => {
    it('отправит запрос с задержкой, если поменяли значение в обычном инпуте', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        act(() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.MILEAGE, 1111);
        });

        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(1);

        const url = fetchMock.mock.calls[0][0];
        const mileage = getMileAge(JSON.parse(fetchMock.mock.calls[0][1]?.body?.toString() || '').draft);

        expect(url).toBe('/-/ajax/jest/saveDraftFormsToPublicApi/');
        expect(mileage).toBe(1111);
    });

    it('сохранит значение составного инпута', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        act(() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.MARK, { data: 'FERRARI', text: 'Феррари' });
        });

        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(1);

        const url = fetchMock.mock.calls[0][0];
        const mark = JSON.parse(fetchMock.mock.calls[0][1]?.body?.toString() || '').draft.car_info.mark;

        expect(url).toBe('/-/ajax/jest/saveDraftFormsToPublicApi/');
        expect(mark).toBe('FERRARI');
    });

    it('сбросит значение филда в драфте, если передали null', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        act(() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, null);
        });

        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(1);

        const url = fetchMock.mock.calls[0][0];
        const year = getYear(JSON.parse(fetchMock.mock.calls[0][1]?.body?.toString() || '').draft);

        expect(url).toBe('/-/ajax/jest/saveDraftFormsToPublicApi/');
        expect(year).toBeNull();
    });

    it('не сбросит значение в драфте, если сбросили значение составного инпута', async() => {
        await renderComponent({ state: defaultState, props: defaultProps });

        act(() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.MARK, { text: '' });
        });

        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(1);

        const url = fetchMock.mock.calls[0][0];
        const mark = JSON.parse(fetchMock.mock.calls[0][1]?.body?.toString() || '').draft.car_info.mark;

        expect(url).toBe('/-/ajax/jest/saveDraftFormsToPublicApi/');
        expect(mark).toBe('FORD');
    });
});

describe('при сабмите формы с ошибкой', () => {
    describe('обычный сценарий', () => {
        beforeEach(async() => {
            await renderComponent({
                state: defaultState,
                props: defaultProps,
                children: (
                    <>
                        <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                        <OfferFormVinField/>
                    </>
                ),
            });
            (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

            await act(async() => {
                defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, null);
                defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.VIN, undefined);

                await flushPromises();

                defaultProps?.apiRef?.current?.submitForm();

                await flushPromises();
            });
        });
        it('отправит лог ошибок полей и формы', async() => {
            expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(3);
            expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { event: 'error_save', field: FieldNames.YEAR });
            expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { event: 'error_save', field: OfferFormFieldNames.VIN });
            expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { level_3: 'SAVE_ERROR' });
        });

        it('подскроллит к первому блоку с ошибкой', () => {
            expect(scroller.scrollTo).toHaveBeenCalledTimes(1);
            expect(scroller.scrollTo).toHaveBeenCalledWith(FieldNames.YEAR, { duration: 200, offset: -150, smooth: true });
        });

        it('откатит состояние формы', () => {
            expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(1);
        });

        it('не сохранит и не будет публиковать драфт после валидации', () => {
            expect(fetchMock).toHaveBeenCalledTimes(2); // поменяли 2 поля, 2 раза вызвали сохранение драфта
        });
    });

    it('правильно отправляет лог для полей "дата покупки", "гарантия", "место осмотра"', async() => {
        await renderComponent({
            state: defaultState,
            props: defaultProps,
            children: (
                <>
                    <OfferFormPurchaseDateField/>
                    <OfferFormWarrantyField/>
                    <OfferFormAddressLocationField/>
                </>
            ),
        });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.PURCHASE_DATE, { year: 2019 });
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.WARRANTY, { warranty: true, year: 2030 });
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.LOCATION, undefined);

            await flushPromises();

            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(4);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { event: 'error_save', field: OfferFormFieldNames.PURCHASE_DATE + '_month' });
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { event: 'error_save', field: OfferFormFieldNames.WARRANTY + '_month' });
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { event: 'error_save', field: OfferFormFieldNames.LOCATION + '_city' });
    });
});

describe('при сабмите формы без ошибок на десктопе', () => {
    beforeEach(async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({ status: ResponseStatus.SUCCESS, offer_id: 'offer_hash-offer_id' })) :
                Promise.resolve(JSON.stringify({}));
        });

        await renderComponent({
            state: defaultState,
            props: defaultProps,
            children: (
                <>
                    <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                    <OfferFormVinField/>
                    <ComponentWithActionQueue/>
                </>
            ),
        });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();
        contextMock.metrika.reachGoal.mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, 2020);
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.FINGERPRINT, 'fingerprint-mock');

            await flushPromises();
        });
    });

    it('сохранит драфт и опубликует его', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(fetchMock).toHaveBeenCalledTimes(4); // поменяли 2 поля, 2 раза вызвали сохранение драфта + 1 раз + публикация

        const urls = fetchMock.mock.calls.map(([ url ]) => url);

        expect(urls).toEqual([
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/publishDraftPublicApi/',
        ]);

        const publishDraftParams = JSON.parse(fetchMock.mock.calls[3][1]?.body?.toString() || '');

        expect(publishDraftParams).toEqual({
            fingerprint: 'fingerprint-mock',
            offer_id: 'draft_id',
            parent_category: 'cars',
            postTradeIn: false,
        });
    });

    it('отправит голзы в метрику', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(3);
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(1, 'FORM_OFFER_READY', { draft_id: '1085562758-1970f439' });
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(2, 'CARS_NEW_FORM_ADD');
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(3, 'USER_CARS_FORM_ADD');
    });

    it('отправит лог формы', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'SAVE_SUCCESS' });
    });

    it('вызовет коллбэки из очереди', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(submitSuccessCallback).toHaveBeenCalledTimes(1);
    });

    it('если ничего не нужно оплачивать, сделает редирект', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith(
            'link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=offer_hash&sale_hash=offer_id&page_from=add-page',
        );
    });

});

describe('при сабмите формы без ошибок на таче', () => {
    beforeEach(async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({ status: ResponseStatus.SUCCESS, offer_id: 'offer_hash-offer_id' })) :
                Promise.resolve(JSON.stringify({}));
        });
        const state = {
            ...defaultState,
            config: configMock.withBrowser({ isMobile: true }).value(),
        };

        await renderComponent({
            state,
            props: defaultProps,
            children: (
                <>
                    <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                    <OfferFormVinField/>
                    <ComponentWithActionQueue/>
                </>
            ),
        });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();
        contextMock.metrika.reachGoal.mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, 2020);
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.FINGERPRINT, 'fingerprint-mock');

            await flushPromises();
        });
    });

    it('сохранит драфт и опубликует его', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(fetchMock).toHaveBeenCalledTimes(4); // поменяли 2 поля, 2 раза вызвали сохранение драфта + 1 раз + публикация

        const urls = fetchMock.mock.calls.map(([ url ]) => url);

        expect(urls).toEqual([
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/saveDraftFormsToPublicApi/',
            '/-/ajax/jest/publishDraftPublicApi/',
        ]);

        const publishDraftParams = JSON.parse(fetchMock.mock.calls[3][1]?.body?.toString() || '');

        expect(publishDraftParams).toEqual({
            fingerprint: 'fingerprint-mock',
            offer_id: 'draft_id',
            parent_category: 'cars',
            postTradeIn: false,
        });
    });

    it('отправит голзы в метрику для тача', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(3);
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(1, 'M_FORM_OFFER_READY', { draft_id: '1085562758-1970f439' });
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(2, 'M_CARS_NEW_FORM_ADD');
        expect(contextMock.metrika.reachGoal).toHaveBeenNthCalledWith(3, 'M_USER_CARS_FORM_ADD');
    });

    it('отправит лог формы', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'SAVE_SUCCESS' });
    });

    it('вызовет коллбэки из очереди', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(submitSuccessCallback).toHaveBeenCalledTimes(1);
    });

    it('если ничего не нужно оплачивать, сделает редирект', async() => {
        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith(
            'link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=offer_hash&sale_hash=offer_id&page_from=add-page',
        );
    });

});

describe('если нужна оплата, откроет модал', () => {
    it('и засабмитит вас на десктопе', async() => {
        const cart = [ { name: TOfferVas.PLACEMENT, count: 1 } ];
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({ status: ResponseStatus.SUCCESS, offer_id: 'offer_hash-offer_id' })) :
                Promise.resolve(JSON.stringify({}));
        });
        offerFormPageContextMock.cart = cart;

        await renderComponent({
            state: defaultState,
            props: defaultProps,
            children: (
                <>
                    <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                    <OfferFormVinField/>
                    <ComponentWithActionQueue/>
                </>
            ),
        });

        await act(async() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, 2020);
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.FINGERPRINT, 'fingerprint-mock');

            await flushPromises();
        });

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpen).toHaveBeenCalledWith({
            offerId: 'offer_hash-offer_id',
            category: 'cars',
            section: 'used',
            product: cart,
            platform: TBillingPlatform.DESKTOP,
            from: TBillingFrom.FORM_BETA_ADD,
            host: 'auto.ru',
        });
        expect(submitVas).toHaveBeenCalledTimes(1);
    });

    it('не будет сабмитить на мобилке', async() => {
        const state = {
            ...defaultState,
            config: configMock.withBrowser({ isMobile: true }).value(),
        };

        const cart = [ { name: TOfferVas.PLACEMENT, count: 1 } ];
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({ status: ResponseStatus.SUCCESS, offer_id: 'offer_hash-offer_id' })) :
                Promise.resolve(JSON.stringify({}));
        });
        offerFormPageContextMock.cart = cart;

        await renderComponent({
            state: state,
            props: defaultProps,
            children: (
                <>
                    <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                    <OfferFormVinField/>
                    <ComponentWithActionQueue/>
                </>
            ),
        });

        await act(async() => {
            defaultProps?.apiRef?.current?.setFieldValue(FieldNames.YEAR, 2020);
            defaultProps?.apiRef?.current?.setFieldValue(OfferFormFieldNames.FINGERPRINT, 'fingerprint-mock');

            await flushPromises();
        });

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpen).toHaveBeenCalledWith({
            offerId: 'offer_hash-offer_id',
            category: 'cars',
            section: 'used',
            product: cart,
            platform: TBillingPlatform.MOBILE,
            from: TBillingFrom.FORM_BETA_ADD,
            host: 'auto.ru',
        });
        expect(submitVas).toHaveBeenCalledTimes(0);
    });

});

describe('при ошибке публикации драфта OFFER_NOT_VALID', () => {
    beforeEach(async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({
                    status: ResponseStatus.ERROR,
                    error: ErrorCode.OFFER_NOT_VALID,
                    validation_errors: [
                        {
                            description: 'Неверный формат VIN. Должно быть 17 знаков. Букв O, Q и I не бывает.',
                            error_code: 'wrong.vin',
                            field: 'vin',
                        },
                        {
                            description: 'Слишком высокая цена',
                            error_code: 'wrong.price',
                            field: 'price',
                        },
                        {
                            description: 'Размещение по четвергам запрещено',
                            error_code: 'wrong.weekday',
                            field: 'weekday',
                        },
                    ],
                })) :
                Promise.resolve(JSON.stringify({}));
        });

        await renderComponent({
            state: defaultState,
            props: defaultProps,
            children: (
                <>
                    <YearField onFieldChange={ jest.fn } suggest={ [ 2020, 2021 ] }/>
                    <OfferFormVinField/>
                    <ComponentWithActionQueue/>
                </>
            ),
        });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });
    });

    it('для известных полей, расставит ошибки и отправит лог', async() => {
        expect(defaultProps?.apiRef?.current?.getFieldError(OfferFormFieldNames.VIN)).toEqual({
            type: FieldErrors.INCORRECT_VALUE,
            text: 'Неверный формат VIN. Должно быть 17 знаков. Букв O, Q и I не бывает.',
        });
        expect(defaultProps?.apiRef?.current?.getFieldError(OfferFormFieldNames.PRICE)).toEqual({
            type: FieldErrors.INCORRECT_VALUE,
            text: 'Слишком высокая цена',
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(3);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { event: 'error_save', field: OfferFormFieldNames.VIN });
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { event: 'error_save', field: OfferFormFieldNames.PRICE + '_input' });
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { level_3: 'SAVE_ERROR' });
    });

    it('ошибки в неизвестных полях покажет во всплывашке', () => {
        expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.setFormError).toHaveBeenNthCalledWith(1, 'Размещение по четвергам запрещено');
    });

    it('подскроллит к первому блоку с ошибкой', () => {
        expect(scroller.scrollTo).toHaveBeenCalledTimes(1);
        expect(scroller.scrollTo).toHaveBeenCalledWith(OfferFormFieldNames.VIN, { duration: 200, offset: -150, smooth: true });
    });

    it('откатит состояние формы', () => {
        expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(1);
    });
});

describe('при ошибке публикации драфта DRAFT_NOT_FOUND', () => {
    beforeEach(async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({ status: ResponseStatus.ERROR, error: ErrorCode.DRAFT_NOT_FOUND })) :
                Promise.resolve(JSON.stringify({}));
        });
    });

    it('на странице редактирования покажет ошибку формы', async() => {
        const state = {
            ...defaultState,
            config: configMock.withPageParams({ form_type: 'edit' }).value(),
        };
        await renderComponent({ state, props: defaultProps });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.setFormError).toHaveBeenNthCalledWith(1, 'Ошибка обработки запроса. Пожалуйста, перезагрузите страницу.');

        expect(scroller.scrollTo).toHaveBeenCalledTimes(0);
        expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(1);

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'SAVE_ERROR' });
    });

    it('на странице добавления сделает редирект в лк', async() => {
        const state = {
            ...defaultState,
            config: configMock.withPageParams({ form_type: 'add' }).value(),
        };
        await renderComponent({ state, props: defaultProps });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith('link/my-main/?');

        expect(scroller.scrollTo).toHaveBeenCalledTimes(0);
        expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(0);
        expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(0);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });
});

describe('при ошибке публикации драфта FORBIDDEN_REQUEST', () => {
    it('если в описании ошибки есть что-то про трейд-ин, сбросит поле трейд-ина в форме', async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({
                    status: ResponseStatus.ERROR,
                    error: ErrorCode.FORBIDDEN_REQUEST,
                    detailed_error: 'trade-id is NOT available for you',
                })) :
                Promise.resolve(JSON.stringify({}));
        });
        const state = {
            ...defaultState,
            offerDraft: offerDraftMock.withOfferMock(
                cloneOfferWithHelpers(offerMock).withTradeInInfo({
                    trade_in_type: TradeInType.FOR_MONEY,
                    trade_in_price_range: {
                        currency: 'RUR',
                        from: '517000',
                        to: '569000',
                    },
                }),
            ).value(),
        };

        await renderComponent({ state, props: defaultProps });
        (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        const tradeInValue = defaultProps?.apiRef?.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

        expect(tradeInValue).toBe(null);

        expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.setFormError).toHaveBeenNthCalledWith(1, 'Произошла ошибка. Попробуйте еще раз.');

        expect(scroller.scrollTo).toHaveBeenCalledTimes(0);
        expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(1);

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'SAVE_ERROR' });
    });

    it('если в описании ошибки ничего нет про трейд-ин, не будет сбрасывать поле трейд-ина в форме', async() => {
        fetchMock.mockResponse((req) => {
            return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
                Promise.resolve(JSON.stringify({
                    status: ResponseStatus.ERROR,
                    error: ErrorCode.FORBIDDEN_REQUEST,
                    detailed_error: 'some random error',
                })) :
                Promise.resolve(JSON.stringify({}));
        });
        const tradeInDraftValue = {
            trade_in_type: TradeInType.FOR_MONEY,
            trade_in_price_range: {
                currency: 'RUR',
                from: '517000',
                to: '569000',
            },
        };
        const state = {
            ...defaultState,
            offerDraft: offerDraftMock.withOfferMock(
                cloneOfferWithHelpers(offerMock).withTradeInInfo(tradeInDraftValue),
            ).value(),
        };

        await renderComponent({ state, props: defaultProps });

        await act(async() => {
            defaultProps?.apiRef?.current?.submitForm();

            await flushPromises();
        });

        const tradeInValue = defaultProps?.apiRef?.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

        expect(tradeInValue).toEqual(tradeInDraftValue);

        expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.setFormError).toHaveBeenNthCalledWith(1, 'Произошла ошибка. Попробуйте еще раз.');
    });
});

it('правильно обработает UNKNOWN_ERROR при публикации драфта', async() => {
    fetchMock.mockResponse((req) => {
        return req.url === '/-/ajax/jest/publishDraftPublicApi/' ?
            Promise.resolve(JSON.stringify({ status: ResponseStatus.ERROR, error: ErrorCode.UNKNOWN_ERROR })) :
            Promise.resolve(JSON.stringify({}));
    });

    await renderComponent({ state: defaultState, props: defaultProps });
    (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();

    await act(async() => {
        defaultProps?.apiRef?.current?.submitForm();

        await flushPromises();
    });

    expect(offerFormPageContextMock.setFormError).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.setFormError).toHaveBeenNthCalledWith(1, 'Произошла ошибка. Попробуйте еще раз.');

    expect(scroller.scrollTo).toHaveBeenCalledTimes(0);
    expect(offerFormPageContextMock.rollbackForm).toHaveBeenCalledTimes(1);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { level_3: 'SAVE_ERROR' });
});

async function renderComponent(
    { children, pageContext, props, state }:
    { state: State; props: Props; children?: React.ReactNode; pageContext?: typeof offerFormPageContextMock },
) {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const store = mockStore(state || {});
    const ContextProvider = createContextProvider(contextMock);

    mockUseSelector(state);
    mockUseDispatch(store);

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <OfferFormPageContextProvider value={ pageContext || offerFormPageContextMock }>
                    <AccordionContext.Provider value={ AccordionContextMock }>
                        <ActionQueueProvider value={ actionQueueContextMock }>
                            <OfferForm { ...props }>{ children }</OfferForm>
                        </ActionQueueProvider>
                    </AccordionContext.Provider>
                </OfferFormPageContextProvider>
            </Provider>
        </ContextProvider>,
    );
}
