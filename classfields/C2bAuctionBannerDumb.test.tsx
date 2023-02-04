/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
import React from 'react';
import { mount, shallow } from 'enzyme';
import type { ShallowWrapper } from 'enzyme';
import { noop } from 'lodash';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import C2bAuctionBannerDumb from './C2bAuctionBannerDumb';
import type { Props } from './C2bAuctionBannerDumb';

const Context = createContextProvider(contextMock);

const APPLICATION_INFO = {
    isAvailable: true,
    priceRange: {
        from: 1000000,
        to: 5000000,
    },
    pricePrediction: 5000000,
};

afterEach(() => {
    jest.clearAllMocks();
});

describe('C2bAuctionBannerDumb метрика', () => {
    describe('аукцион Авто.ру', () => {
        it('должен отправить метрику и фронтлог на маунт компонента, если аукцион доступен (создание)', () => {
            const logShowMock = jest.fn();

            mount(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                    />
                </Context>,
            );

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                {
                    banner_c2b_auction: {
                        form: {
                            show: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logShowMock).toHaveBeenCalledTimes(1);
        });

        it('должен отправить метрику и фронтлог на маунт компонента, если аукцион доступен (редактирование)', () => {
            const logShowMock = jest.fn();

            mount(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                        formType="edit"
                    />
                </Context>,
            );

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                {
                    banner_c2b_auction: {
                        form_edit: {
                            show: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logShowMock).toHaveBeenCalledTimes(1);
        });

        it('не должен отправить метрику и фронтлог на маунт компонента, если аукцион не доступен', () => {
            const logShowMock = jest.fn();

            const applicationInfo = {
                ...APPLICATION_INFO,
                isAvailable: false,
            };

            mount(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                    />
                </Context>,
            );

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(0);
            expect(logShowMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику и фронтлог, когда аукцион стал доступен (создание)', () => {
            const logShowMock = jest.fn();

            const applicationInfo1 = {
                ...APPLICATION_INFO,
                isAvailable: false,
            };

            const applicationInfo2 = {
                ...APPLICATION_INFO,
                isAvailable: true,
            };

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo1 }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                    />
                </Context>,
            );

            const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

            component.setProps({
                applicationInfo: applicationInfo2,
            });

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                {
                    banner_c2b_auction: {
                        form: {
                            show: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logShowMock).toHaveBeenCalledTimes(1);
        });

        it('должен отправить метрику и фронтлог, когда аукцион стал доступен (редактирование)', () => {
            const logShowMock = jest.fn();

            const applicationInfo1 = {
                ...APPLICATION_INFO,
                isAvailable: false,
            };

            const applicationInfo2 = {
                ...APPLICATION_INFO,
                isAvailable: true,
            };

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo1 }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                        formType="edit"
                    />
                </Context>,
            );

            const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

            component.setProps({
                applicationInfo: applicationInfo2,
            });

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                {
                    banner_c2b_auction: {
                        form_edit: {
                            show: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logShowMock).toHaveBeenCalledTimes(1);
        });

        it('не должен отправить метрику и фронтлог второй раз, когда аукцион доступен', () => {
            const logShowMock = jest.fn();

            const applicationInfo1 = {
                ...APPLICATION_INFO,
                isAvailable: true,
            };

            const applicationInfo2 = {
                ...APPLICATION_INFO,
                isAvailable: true,
                isApplicationInfoPending: true,
            };

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo1 }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                    />
                </Context>,
            );

            const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

            component.setProps({
                applicationInfo: applicationInfo2,
            });

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenCalledTimes(1);
        });

        it('должен отправить метрику и фронтлог на клик по кнопке (создание)', () => {
            const logClickMock = jest.fn();

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ logClickMock }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                    />
                </Context>,
            );

            tree.dive().find('.C2bAuctionBanner__button').simulate('click');

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_CLICK',
                {
                    banner_c2b_auction: {
                        form: {
                            click: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logClickMock).toHaveBeenCalledTimes(1);
        });

        it('должен отправить метрику и фронтлог на клик по кнопке (редактирование)', () => {
            const logClickMock = jest.fn();

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ logClickMock }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ false }
                        formType="edit"
                    />
                </Context>,
            );

            tree.dive().find('.C2bAuctionBanner__button').simulate('click');

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_CLICK',
                {
                    banner_c2b_auction: {
                        form_edit: {
                            click: {
                                draft_id: '7389068134669063260-d56e243f',
                                price_from: 1000000,
                                price_to: 5000000,
                            },
                        },
                    },
                },
            );

            expect(logClickMock).toHaveBeenCalledTimes(1);
        });
    });

    describe('аукцион КарПрайс', () => {
        it('должен отправить метрику (но не фронтлог) (но не фронтлог) на маунт компонента, если аукцион доступен (создание)', () => {
            const logShowMock = jest.fn();

            mount(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                    />
                </Context>,
            );

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                { banner_c2b_carprice_auction: { form: { show: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logShowMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику (но не фронтлог) на маунт компонента, если аукцион доступен (редактирование)', () => {
            const logShowMock = jest.fn();

            mount(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                        formType="edit"
                    />
                </Context>,
            );

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                { banner_c2b_carprice_auction: { form_edit: { show: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logShowMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику (но не фронтлог), когда аукцион стал доступен (создание)', () => {
            const logShowMock = jest.fn();

            const applicationInfo1 = {
                ...APPLICATION_INFO,
                isAvailable: false,
            };

            const applicationInfo2 = {
                ...APPLICATION_INFO,
                isAvailable: true,
            };

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo1 }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                    />
                </Context>,
            );

            const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

            component.setProps({
                applicationInfo: applicationInfo2,
            });

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                { banner_c2b_carprice_auction: { form: { show: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logShowMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику (но не фронтлог), когда аукцион стал доступен (редактирование)', () => {
            const logShowMock = jest.fn();

            const applicationInfo1 = {
                ...APPLICATION_INFO,
                isAvailable: false,
            };

            const applicationInfo2 = {
                ...APPLICATION_INFO,
                isAvailable: true,
            };

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ applicationInfo1 }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ logShowMock }
                        logClickEvent={ noop }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                        formType="edit"
                    />
                </Context>,
            );

            const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

            component.setProps({
                applicationInfo: applicationInfo2,
            });

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_SHOW',
                { banner_c2b_carprice_auction: { form_edit: { show: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logShowMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику (но не фронтлог) на клик по кнопке (создание)', () => {
            const logClickMock = jest.fn();

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ logClickMock }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                    />
                </Context>,
            );

            tree.dive().find('CarPriceAuctionBanner').dive().find('.CarPriceAuctionBanner__button').simulate('click');

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_CLICK',
                { banner_c2b_carprice_auction: { form: { click: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logClickMock).toHaveBeenCalledTimes(0);
        });

        it('должен отправить метрику (но не фронтлог) на клик по кнопке (редактирование)', () => {
            const logClickMock = jest.fn();

            const tree = shallow(
                <Context>
                    <C2bAuctionBannerDumb
                        applicationInfo={ APPLICATION_INFO }
                        isValidationPending={ false }
                        isApplicationInfoPending={ false }
                        offerId="7389068134669063260-d56e243f"
                        validateAndSubmit={ noop }
                        logShowEvent={ noop }
                        logClickEvent={ logClickMock }
                        category="cars"
                        isEditingDraft={ false }
                        isCarPrice={ true }
                        formType="edit"
                    />
                </Context>,
            );

            tree.dive().find('CarPriceAuctionBanner').dive().find('.CarPriceAuctionBanner__button').simulate('click');

            expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.reachGoal).toHaveBeenLastCalledWith(
                'BANNER_C2B_AUCTION_CLICK',
                { banner_c2b_carprice_auction: { form_edit: { click: { draft_id: '7389068134669063260-d56e243f' } } } },
            );

            expect(logClickMock).toHaveBeenCalledTimes(0);
        });
    });
});

describe('C2bAuctionBannerDumb фронтлог', () => {
    it('Cоздание черновика и клик по нему', () => {
        const logShowMock = jest.fn();
        const logClickMock = jest.fn();

        const applicationInfo1 = {
            ...APPLICATION_INFO,
            isAvailable: false,
        };

        const applicationInfo2 = {
            ...APPLICATION_INFO,
            isAvailable: true,
        };

        const tree = shallow(
            <Context>
                <C2bAuctionBannerDumb
                    applicationInfo={ applicationInfo1 }
                    isValidationPending={ false }
                    isApplicationInfoPending={ false }
                    offerId="7389068134669063260-d56e243f"
                    validateAndSubmit={ noop }
                    logShowEvent={ logShowMock }
                    logClickEvent={ logClickMock }
                    category="cars"
                    isEditingDraft={ false }
                    isCarPrice={ false }
                />
            </Context>,
        );

        const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

        component.setProps({
            applicationInfo: applicationInfo2,
        });

        expect(logShowMock).toHaveBeenCalledTimes(1);
        expect(logShowMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '7389068134669063260-d56e243f',
            offerId: undefined,
            contextPage: ContextPage.PAGE_DRAFT_CREATE,
            contextBlock: ContextBlock.BLOCK_CARD,
        });

        component.find('.C2bAuctionBanner__button').simulate('click');

        expect(logClickMock).toHaveBeenCalledTimes(1);
        expect(logClickMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '7389068134669063260-d56e243f',
            offerId: undefined,
            contextPage: ContextPage.PAGE_DRAFT_CREATE,
            contextBlock: ContextBlock.BLOCK_CARD,
        }, true);
    });

    it('Редактирование черновика и клик по нему', () => {
        const logShowMock = jest.fn();
        const logClickMock = jest.fn();

        const applicationInfo1 = {
            ...APPLICATION_INFO,
            isAvailable: false,
        };

        const applicationInfo2 = {
            ...APPLICATION_INFO,
            isAvailable: true,
        };

        const tree = shallow(
            <Context>
                <C2bAuctionBannerDumb
                    applicationInfo={ applicationInfo1 }
                    isValidationPending={ false }
                    isApplicationInfoPending={ false }
                    offerId="7389068134669063260-d56e243f"
                    validateAndSubmit={ noop }
                    logShowEvent={ logShowMock }
                    logClickEvent={ logClickMock }
                    category="cars"
                    isEditingDraft={ true }
                    isCarPrice={ false }
                />
            </Context>,
        );

        const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

        component.setProps({
            applicationInfo: applicationInfo2,
        });

        expect(logShowMock).toHaveBeenCalledTimes(1);
        expect(logShowMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '7389068134669063260-d56e243f',
            offerId: undefined,
            contextPage: ContextPage.PAGE_DRAFT_EDIT,
            contextBlock: ContextBlock.BLOCK_CARD,
        });

        component.find('.C2bAuctionBanner__button').simulate('click');

        expect(logClickMock).toHaveBeenCalledTimes(1);
        expect(logClickMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '7389068134669063260-d56e243f',
            offerId: undefined,
            contextPage: ContextPage.PAGE_DRAFT_EDIT,
            contextBlock: ContextBlock.BLOCK_CARD,
        }, true);
    });

    it('Редактирование оффера и клик по нему', () => {
        const logShowMock = jest.fn();
        const logClickMock = jest.fn();

        const applicationInfo1 = {
            ...APPLICATION_INFO,
            isAvailable: false,
        };

        const applicationInfo2 = {
            ...APPLICATION_INFO,
            isAvailable: true,
        };

        const tree = shallow(
            <Context>
                <C2bAuctionBannerDumb
                    applicationInfo={ applicationInfo1 }
                    isValidationPending={ false }
                    isApplicationInfoPending={ false }
                    offerId="738906813-d56e243f"
                    validateAndSubmit={ noop }
                    logShowEvent={ logShowMock }
                    logClickEvent={ logClickMock }
                    category="cars"
                    isEditingDraft={ true }
                    isCarPrice={ false }
                    formType="edit"
                />
            </Context>,
        );

        const component: ShallowWrapper<Props, unknown, C2bAuctionBannerDumb> = tree.dive();

        component.setProps({
            applicationInfo: applicationInfo2,
        });

        expect(logShowMock).toHaveBeenCalledTimes(1);
        expect(logShowMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: undefined,
            offerId: '738906813-d56e243f',
            contextPage: ContextPage.PAGE_OFFER_EDIT,
            contextBlock: ContextBlock.BLOCK_CARD,
        });

        component.find('.C2bAuctionBanner__button').simulate('click');

        expect(logClickMock).toHaveBeenCalledTimes(1);
        expect(logClickMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: undefined,
            offerId: '738906813-d56e243f',
            contextPage: ContextPage.PAGE_OFFER_EDIT,
            contextBlock: ContextBlock.BLOCK_CARD,
        }, true);
    });
});

it('не должен отрендерить компонент, если аукцион не доступен', () => {
    const applicationInfo = {
        ...APPLICATION_INFO,
        isAvailable: false,
    };

    const tree = shallow(
        <Context>
            <C2bAuctionBannerDumb
                applicationInfo={ applicationInfo }
                isValidationPending={ false }
                isApplicationInfoPending={ false }
                validateAndSubmit={ noop }
                logShowEvent={ noop }
                logClickEvent={ noop }
                category="cars"
                isEditingDraft={ false }
                isCarPrice={ false }
            />
        </Context>,
    );

    expect(tree.dive()).toBeEmptyRender();
});

it('не должен отрендерить компонент, если цена from равна нулю', () => {
    const applicationInfo = {
        priceRange: {
            from: 0,
            to: 5000000,
        },
        isAvailable: true,
    };

    const tree = shallow(
        <Context>
            <C2bAuctionBannerDumb
                applicationInfo={ applicationInfo }
                isValidationPending={ false }
                isApplicationInfoPending={ false }
                validateAndSubmit={ noop }
                logShowEvent={ noop }
                logClickEvent={ noop }
                category="cars"
                isEditingDraft={ false }
                isCarPrice={ false }
            />
        </Context>,
    );

    expect(tree.dive()).toBeEmptyRender();
});

it('не должен отрендерить компонент, если цена to равна нулю', () => {
    const applicationInfo = {
        priceRange: {
            from: 1000000,
            to: 0,
        },
        isAvailable: true,
    };

    const tree = shallow(
        <Context>
            <C2bAuctionBannerDumb
                applicationInfo={ applicationInfo }
                isValidationPending={ false }
                isApplicationInfoPending={ false }
                validateAndSubmit={ noop }
                logShowEvent={ noop }
                logClickEvent={ noop }
                category="cars"
                isEditingDraft={ false }
                isCarPrice={ false }
            />
        </Context>,
    );

    expect(tree.dive()).toBeEmptyRender();
});

it('не должен отрендерить компонент карпрайс, если pricePrediction равен нулю', () => {
    const applicationInfo = {
        pricePrediction: 0,
        isAvailable: true,
    };

    const tree = shallow(
        <Context>
            <C2bAuctionBannerDumb
                applicationInfo={ applicationInfo }
                isValidationPending={ false }
                isApplicationInfoPending={ false }
                validateAndSubmit={ noop }
                logShowEvent={ noop }
                logClickEvent={ noop }
                category="cars"
                isEditingDraft={ false }
                isCarPrice={ true }
            />
        </Context>,
    );

    expect(tree.dive()).toBeEmptyRender();
});
