import { renderHook } from '@testing-library/react-hooks';
import fetchMock from 'jest-fetch-mock';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import createLinkMock from 'autoru-frontend/mocks/createLinkMock';
import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import type { AppState } from 'www-poffer/react/store/AppState';

import useMmmDraftDialog from './useMmmDraftDialog';

let defaultState: Partial<AppState>;
const confirmMock = jest.fn();
let offer: TOfferMock;

let originalWindowLocation: Location;

beforeEach(() => {
    defaultState = {
        config: configMock.value(),
        offerDraft: offerDraftMock.value(),
    };
    offer = cloneOfferWithHelpers(offerMock);

    jest.spyOn(global.history, 'pushState').mockImplementation(() => {});
    jest.spyOn(global, 'confirm').mockImplementation(confirmMock);

    originalWindowLocation = global.location;
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        ...originalWindowLocation,
        reload: jest.fn(),
    };
});

afterEach(() => {
    fetchMock.resetMocks();
    jest.restoreAllMocks();
    global.location = originalWindowLocation;
});

describe('диалог', () => {
    it('будет показан, если МММ в драфте и урле не совпадают', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'Audi' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.value());

        expect(confirmMock).toHaveBeenCalledTimes(1);
        expect(confirmMock).toHaveBeenCalledWith('У вас есть сохраненный черновик объявления с Ford EcoSport. Хотите его восстановить?');
    });

    it('если пользователь согласится, то просто поменяет урл', () => {
        confirmMock.mockImplementationOnce(() => true);
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'Audi', category: 'cars', section: 'used', form_type: 'add' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.value());

        expect(global.history.pushState).toHaveBeenCalledTimes(1);
        expect(global.history.pushState).toHaveBeenCalledWith(undefined, global.document.title, 'link/form/?category=cars&section=used&form_type=add');
    });

    it('если пользователь откажется, то удалить текущий драфт и перезагрузит страницу', async() => {
        const deleteDraftPromise = Promise.resolve(JSON.stringify({ status: 'SUCCESS' }));
        confirmMock.mockImplementationOnce(() => false);
        fetchMock.mockResponseOnce(() => deleteDraftPromise);
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'Audi', category: 'cars', section: 'used', form_type: 'add' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.value());

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe('/-/ajax/jest/deleteDraft/');
        await flushPromises();
        expect(global.location.reload).toHaveBeenCalledTimes(1);
    });
});

describe('не покажет диалог', () => {
    it('если драфт пустой', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'Audi' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.withMarkInfo({}).value());

        expect(confirmMock).toHaveBeenCalledTimes(0);
    });

    it('если роут не тот', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageType('form').withPageParams({ mark: 'Audi' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.value());

        expect(confirmMock).toHaveBeenCalledTimes(0);
    });

    it('если марка в урле и драфте совпадает а модели не указаны', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'audi' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.withMarkInfo({ code: 'Audi' }).withModelInfo({}).value());

        expect(confirmMock).toHaveBeenCalledTimes(0);
    });

    it('если марка и модель в урле и драфте совпадают', () => {
        const state = {
            ...defaultState,
            config: configMock.withPageType('form-mmm').withPageParams({ mark: 'audi', model: 'a1' }).value(),
        };
        const openDialog = renderCustomHook({ state });
        openDialog(offer.withMarkInfo({ code: 'Audi' }).withModelInfo({ code: 'A1' }).value());

        expect(confirmMock).toHaveBeenCalledTimes(0);
    });
});

function renderCustomHook({ state }: { state: Partial<AppState> }) {
    const store = mockStore(state);
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);
    mockUseDispatch(store);
    const { result } = renderHook(() => useMmmDraftDialog(createLinkMock('link')));

    return result.current;
}
