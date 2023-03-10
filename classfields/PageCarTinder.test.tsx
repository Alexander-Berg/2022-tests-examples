jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn().mockImplementation(() => Promise.resolve([ {} ])),
}));

import React from 'react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';
import { screen, render, act } from '@testing-library/react';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import type { StateCookies } from 'auto-core/react/dataDomain/cookies/types';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { StateGeo } from 'auto-core/react/dataDomain/geo/StateGeo';
import gateApi from 'auto-core/react/lib/gateApi';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import configMock from 'auto-core/react/dataDomain/config/mocks/config';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';

import type { StateCarTinder } from 'www-mobile/react/dataDomain/carTinder/types';

import PageCarTinder from './PageCarTinder';
import PageCarTinderDumb from './PageCarTinderDumb';
import type { Props } from './connector';

import '@testing-library/jest-dom';

jest.useFakeTimers();

const getResourceMock = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

type TState = {
    carTinder: StateCarTinder;
    cookies: StateCookies;
    config: StateConfig;
    geo: StateGeo;
};

const INITIAL_STATE: TState = {
    carTinder: {
        offers: [
            cloneOfferWithHelpers(offerMock).withSaleId('111-111').value(),
            cloneOfferWithHelpers(offerMock).withSaleId('222-222').value(),
            cloneOfferWithHelpers(offerMock).withSaleId('333-333').value(),
        ],
        search_parameters: {
            price_from: 3000000,
            price_to: 3500000,
            category: 'cars',
        },
        onboardingState: {
            'return': false,
            modal: false,
            favorite: false,
        },
        currentOffer: offerMock,
        isPending: false,
        isEmpty: false,
        isPendingCount: false,
        hasCountError: false,
        hasError: false,
        filteredOffersCount: 3,
    },
    cookies: {
        'car-tinder-onboarding': JSON.stringify({ modal: true, 'return': true, favorite: true }),
    },
    config: configMock,
    geo: geoMock,
};

describe('PageCarTinder', () => {
    describe('??????????????', () => {
        it('open ???? ??????????', async() => {
            render(getComponent());

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'open' ]);
        });

        it('no_offers,show ???????? ???????????? ???????????? ?????????????? ???? ??????????', async() => {
            const store = mockStore({
                ...INITIAL_STATE,
                carTinder: {
                    ...INITIAL_STATE.carTinder,
                    slideList: [],
                    isEmpty: true,
                },
            });
            render(getComponent(store));

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'no_offers', 'show' ]);
        });

        it('no_offers,show ???????? ???????????? ???????????? ?????????????? ?? didUpdate', async() => {
            const INITIAL_PROPS: Props = {
                fetchMore: jest.fn(),
                setOnboardingState: jest.fn(),
                items: [ offerMock ],
                userOffer: cloneOfferWithHelpers(offerMock).withSaleId('111-111').value(),
                isEmpty: false,
                isLoading: false,
                hasError: false,
                searchParameters: {},
                onboardingCookie: { modal: false, favorite: false, 'return': false },
                onboardingState: { modal: false, favorite: false, 'return': false },
                showAutoclosableMessage: jest.fn(),
                hideMessage: jest.fn(),
            };

            const Context = createContextProvider(contextMock);
            const wrapper = ({ children }: { children: JSX.Element }) => (
                <Context>
                    { children }
                </Context>
            );

            const { rerender } = render(getDumbComponent(INITIAL_PROPS), { wrapper });

            rerender(getDumbComponent({
                ...INITIAL_PROPS,
                isEmpty: true,
            }));

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'no_offers', 'show' ]);
        });

        it('???????????????? ????????????????', async() => {
            render(getComponent());

            const filtersButton = document.getElementsByClassName('CarTinder__control_filters')[0];

            userEvent.click(filtersButton);

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'card', 'filters', 'click' ]);
        });

        it('????????', async() => {
            render(getComponent());

            const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

            userEvent.click(likeButton);

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'card', 'like', 'click', '111-111' ]);
        });

        it('??????????????', async() => {
            render(getComponent());

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];

            userEvent.click(dislikeButton);

            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'card', 'dislike', 'click', '111-111' ]);
        });
    });

    describe('??????????????????', () => {
        const state = {
            ...INITIAL_STATE,
            cookies: {},
        };
        it('??????????', async() => {
            const store = mockStore(state);

            render(getComponent(store));

            expect(store.getActions()).toEqual([
                {
                    payload: {
                        modal: true,
                    },
                    type: 'CAR_TINDER_CHANGE_ONBOARDING_STATE',
                },
                {
                    payload: {},
                    type: 'COOKIES_CHANGE',
                },
            ]);
        });

        it('??????????????????', async() => {
            const store = mockStore(state);
            render(getComponent(store));

            const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

            userEvent.click(likeButton);

            expect(store.getActions()).toEqual(expect.arrayContaining([
                {
                    payload: {
                        favorite: true,
                    },
                    type: 'CAR_TINDER_CHANGE_ONBOARDING_STATE',
                },
                {
                    payload: {},
                    type: 'COOKIES_CHANGE',
                },
            ]));
        });

        it('??????????', async() => {
            const store = mockStore(state);
            render(getComponent(store));

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];

            userEvent.click(dislikeButton);

            expect(store.getActions()).toEqual(expect.arrayContaining([
                {
                    payload: {
                        'return': true,
                    },
                    type: 'CAR_TINDER_CHANGE_ONBOARDING_STATE',
                },
                {
                    payload: {},
                    type: 'COOKIES_CHANGE',
                },
            ]));
        });
    });

    describe('??????????????', () => {
        it('???????????????? ?????? ???????????? ?????????? ???????????????????????? ?????????????????? ???? ??????????', async() => {
            const store = mockStore(INITIAL_STATE);

            render(getComponent(store));

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];

            userEvent.click(dislikeButton);
            userEvent.click(dislikeButton);
            userEvent.click(dislikeButton);

            // 3 ?????????????? ???? ??????????????
            expect(getResourceMock).toHaveBeenCalledTimes(4);
            expect(getResourceMock).toHaveBeenCalledWith('carTinderListing', {
                price_from: 3000000,
                price_to: 3500000,
                category: 'cars',
            });
        });

        it('???????????????? ?????????????? ???? ???????? ?? ??????????????', () => {
            const store = mockStore(INITIAL_STATE);

            render(getComponent(store));

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];
            const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

            userEvent.click(dislikeButton);
            userEvent.click(likeButton);

            expect(getResourceMock).toHaveBeenCalledTimes(2);
            expect(getResourceMock.mock.calls).toEqual([
                [ 'carTinderDislike', {
                    category: 'cars',
                    offer_id: '111-111',
                    self_offer_id: '1085562758-1970f439',
                } ],
                [ 'carTinderLike', {
                    category: 'cars',
                    offer_id: '222-222',
                    self_offer_id: '1085562758-1970f439',
                } ],
            ]);
        });

        it('?????????????? ?????????????? ?? ?????????????? ???????? ???????????? ???? ????????/?????????????? ????????', async() => {
            const store = mockStore(INITIAL_STATE);

            render(getComponent(store));

            const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];
            const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

            getResourceMock.mockImplementationOnce(() => Promise.reject());
            userEvent.click(dislikeButton);

            // ???????? ???????????????????? ?????????????? ???? ??????????????
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(store.getActions()[0].type).toEqual('NOTIFIER_SHOW_MESSAGE');

            getResourceMock.mockImplementationOnce(() => Promise.reject());
            userEvent.click(likeButton);

            // ???????? ???????????????????? ?????????????? ???? ????????
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(store.getActions()[0].type).toEqual('NOTIFIER_SHOW_MESSAGE');
        });
    });

    it('?????????????????? ?????????? ???????????????????? ???????? ???????????????? ?????????? ?? ?????????? cartinder_liked_you', async() => {
        const store = mockStore({
            ...INITIAL_STATE,
            carTinder: {
                ...INITIAL_STATE.carTinder,
                offers: [ cloneOfferWithHelpers(offerMock).withSaleId('111-111').withTags([ 'cartinder_liked_you' ]).value() ],
            },
        });
        render(getComponent(store));

        const likeButton = document.getElementsByClassName('Tinder__mainControl_like')[0];

        expect(screen.queryByText('??????????????????')).not.toBeInTheDocument();
        userEvent.click(likeButton);

        // ???????? ???????????????????? ?????????????? ???? ????????
        await act(async() => {
            await jest.runAllTicks();
        });
        jest.runAllTimers();

        expect(screen.queryByText('??????????????????')).toBeInTheDocument();
    });
});

function getComponent(store: ThunkMockStore<TState> = mockStore(INITIAL_STATE)) {
    const Context = createContextProvider(contextMock);

    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();

    mockUseDispatch(store);
    mockUseSelector(store.getState());

    return (
        <Provider store={ store }>
            <Context>
                <PageCarTinder/>
            </Context>
        </Provider>
    );
}

function getDumbComponent(props: Props) {
    return (
        <PageCarTinderDumb { ...props }/>
    );
}
