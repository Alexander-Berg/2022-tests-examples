/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/state/actions/sellerPopupOpen', () => {
    return jest.fn(() => ({ type: 'sellerPopupOpen_action' }));
});

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import { ContextBlock, ContextPage, SelfType } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import withChatOnly from 'autoru-frontend/mockData/state/helpers/offer/withChatOnly';

import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import sellerPopupOpen from 'auto-core/react/dataDomain/state/actions/sellerPopupOpen';
import type { TStateSearchID } from 'auto-core/react/dataDomain/searchID/TStateSearchID';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { ReduxState } from './OfferPhone';
import OfferPhone from './OfferPhone';

const searchID = {
    searchID: 'searchID',
} as TStateSearchID;

const CONTEXT_PROPS = {
    contextBlock: ContextBlock.BLOCK_CARD,
    contextPage: ContextPage.PAGE_CARD,
    selfType: SelfType.TYPE_SINGLE,
    source: 'card',
};

let store: ThunkMockStore<ReduxState>;
beforeEach(() => {
    const config = configStateMock.withPageType('card').value();

    store = mockStore({
        config,
        searchID,
    });
});

describe('владелец', () => {
    describe('нет подменника', () => {
        it('должен отрендерить один телефон без времени', () => {
            const offer = {
                additional_info: { is_owner: true },
                seller: {
                    phones: [
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                    ],
                },
                seller_type: 'PRIVATE',
            } as Offer;

            const wrapper = shallow(
                <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
                { context: { ...contextMock, store } },
            );
            expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
        });

        it('должен отрендерить несколько телефонов без времени', () => {
            const offer = {
                additional_info: { is_owner: true },
                seller: {
                    phones: [
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                    ],
                },
                seller_type: 'PRIVATE',
            } as Offer;

            const wrapper = shallow(
                <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
                { context: { ...contextMock, store } },
            );
            expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
        });

        it('не должен отрендерить время для телефона', () => {
            const offer = {
                additional_info: { is_owner: true },
                seller: {
                    phones: [
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                            call_hour_start: 9,
                            call_hour_end: 20,
                        },
                    ],
                },
                seller_type: 'PRIVATE',
            } as Offer;

            const wrapper = shallow(
                <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
                { context: { ...contextMock, store } },
            );
            expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
        });
    });

    describe('есть подменник', () => {
        it('должен отрендерить один телефон + подсказка "почему номер не мой?"', () => {
            const offer = {
                additional_info: { is_owner: true },
                seller: {
                    phones: [
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                    ],
                    redirect_phones: true,
                },
                seller_type: 'PRIVATE',
            } as Offer;

            const wrapper = shallow(
                <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
                { context: { ...contextMock, store } },
            );
            expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
        });

        it('должен отрендерить несколько телефонов + подсказка "почему номер не мой?', () => {
            const offer = {
                additional_info: { is_owner: true },
                seller: {
                    phones: [
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                        {
                            phone: '+7 916 039-63-38',
                            title: 'Михаил',
                        },
                    ],
                    redirect_phones: true,
                },
                seller_type: 'PRIVATE',
            } as Offer;

            const wrapper = shallow(
                <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
                { context: { ...contextMock, store } },
            );
            expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
        });
    });
});

it('должен отрендерить упрощенную кнопку показа, если нет телефона', () => {
    const offer = {
        additional_info: { is_owner: true },
        seller: {
            phones: [],
            redirect_phones: true,
        },
        seller_type: 'PRIVATE',
    } as unknown as Offer;

    const wrapper = shallow(
        <OfferPhone offer={ offer } sendEventsToMarketing={ noop } previewOnly { ...CONTEXT_PROPS }/>,
        { context: { ...contextMock, store } },
    );
    expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
});

it('должен отрендерить упрощенную кнопку показа, если есть телефон', () => {
    const offer = {
        additional_info: { is_owner: true },
        seller: {
            phones: [
                {
                    phone: '+7 916 039-63-38',
                    title: 'Михаил',
                },
            ],
            redirect_phones: true,
        },
        seller_type: 'PRIVATE',
    } as Offer;

    const wrapper = shallow(
        <OfferPhone offer={ offer } sendEventsToMarketing={ noop } previewOnly { ...CONTEXT_PROPS }/>,
        { context: { ...contextMock, store } },
    );
    expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
});

describe('не владелец', () => {
    it('должен нарисовать кнопку', () => {
        const offer = {
            seller: {
                phones: [],
                redirect_phones: true,
            },
            seller_type: 'PRIVATE',
        } as unknown as Offer;

        const wrapper = shallow(
            <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
            { context: { ...contextMock, store } },
        );
        expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
    });

    it('должен нарисовать кнопку с правильным плейсхолдером для Беларуси', () => {
        const offer = {
            seller: {
                phones: [],
                location: {
                    region_info: {
                        parent_ids: [ '157', '29630', '149', '166', '10001', '10000' ],
                    },
                },
            },
            seller_type: 'PRIVATE',
        } as unknown as Offer;

        const wrapper = shallow(
            <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
            { context: { ...contextMock, store } },
        );
        const placeholder = wrapper.dive().find('.OfferPhone__placeholder');
        expect(placeholder.text()).toEqual('+375 ●●● ●●● ●● ●●');
    });

    it('должен нарисовать кнопку с правильным плейсхолдером для Казахстана', () => {
        const offer = {
            seller: {
                phones: [],
                location: {
                    region_info: {
                        parent_ids: [ '159', '29630', '166', '10001', '10000' ],
                    },
                },
            },
            seller_type: 'PRIVATE',
        } as unknown as Offer;

        const wrapper = shallow(
            <OfferPhone offer={ offer } sendEventsToMarketing={ noop } { ...CONTEXT_PROPS }/>,
            { context: { ...contextMock, store } },
        );
        const placeholder = wrapper.dive().find('.OfferPhone__placeholder');
        expect(placeholder.text()).toEqual('+7 ●●● ●●● ●● ●●');
    });

    it('должен вызывать экшен запроса телефона', () => {
        const offer = {
            id: '123',
            hash: 'abc',
            seller: {
                phones: [],
                redirect_phones: true,
            },
            seller_type: 'PRIVATE',
        } as unknown as Offer;

        const wrapper = shallow(
            <OfferPhone
                offer={ offer }
                sendEventsToMarketing={ noop }
                { ...CONTEXT_PROPS }
            />,
            { context: { ...contextMock, pageParams: {}, store } },
        );
        wrapper.dive().simulate('click');

        expect(sellerPopupOpen).toHaveBeenCalledTimes(1);
    });
});

it('не будет рисовать компонент если у оффера стоит галка "только чат"', () => {
    store = mockStore({
        card: withChatOnly(_.cloneDeep(cardStateMock), true),
        config: configStateMock.value(),
        searchID,
    });
    const wrapper = shallow(
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        <OfferPhone
            sendEventsToMarketing={ noop }
            { ...CONTEXT_PROPS }
        />,
        { context: { ...contextMock, pageParams: {}, store } },
    ).dive();

    expect(wrapper.html()).toBeNull();
});

it('отправляет метрику при наличии from=reseller_public', () => {
    store = mockStore({
        config: configStateMock.withPageParams({ from: 'reseller_public' }).value(),
        searchID,
    });

    const offer = {
        id: '123',
        hash: 'abc',
        seller: {
            phones: [],
            redirect_phones: true,
        },
        seller_type: 'PRIVATE',
    } as unknown as Offer;

    const wrapper = shallow(
        <OfferPhone
            offer={ offer }
            sendEventsToMarketing={ noop }
            { ...CONTEXT_PROPS }
        />,
        { context: { ...contextMock, pageParams: {}, store } },
    );
    wrapper.dive().simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-phone', 'from_reseller_public' ]);
});

function noop() {}
