/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { mount, render } from 'enzyme';
import { AdSourceType } from '@vertis/ads/build/types/types';
import { getAd2Mock } from '@vertis/ads/build/mocks/AD2';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { AdPlace, DirectLayout } from 'auto-core/lib/ads/AD2.types';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import Ad from './Ad';
import type { ReduxState } from './Ad';

const Context = createContextProvider(contextMock);

let state: Partial<ReduxState>;
beforeEach(() => {
    state = {
        ads: {
            data: {
                code: 'index',
                data: {},
                settings: {},
                statId: '100',
            },
            isFetching: false,
        },
        config: configStateMock.value(),
    };
});

afterEach(() => {
    delete window.AD2;
});

describe('выдача для роботов', () => {
    it('не должен ничего рендерить и падать на componentDidMount', () => {
        const tree = render(
            <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
        );
        expect(tree).toMatchSnapshot();
    });
});

describe('тестовый баннер', () => {
    beforeEach(() => {
        window.AD2 = getAd2Mock();
    });

    it('должен рендерить HTML для баннера', () => {
        state.ads!.data.settings = {
            r1: {
                id: 'r1',
                reload: 40,
                resize: true,
                scrollReload: 2000,
                sources: [
                    {
                        type: AdSourceType.RTB,
                        extParams: { awaps_section: '29337' },
                        code: [ 'R-A-148437-7', 'R-A-148437-4' ],
                    },
                ],
            },
        };
        const tree = render(
            <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
        );
        expect(tree).toMatchSnapshot();
    });

    it('должен рендерить HTML для баннера с несколькими источниками', () => {
        state.ads!.data.settings = {
            r1: {
                id: 'r1',
                initiallyHidden: false,
                sources: [
                    {
                        type: AdSourceType.ADFOX,
                        params: { pp: 'abcd' },
                        placeId: '243420',
                    },
                    {
                        type: AdSourceType.RTB,
                        extParams: { awaps_section: '29337' },
                        code: 'R-A-148437-7',
                    },
                    {
                        type: AdSourceType.ADFOX,
                        params: { pp: 'efgh' },
                        placeId: '243420',
                    },
                ],
            },
        };
        const tree = render(
            <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
        );
        expect(tree).toMatchSnapshot();
    });

    describe('модификатор hidden_yes', () => {
        it('должен отрисовать блок rtb открытым', () => {
            state.ads!.data.settings = {
                r1: {
                    id: 'r1',
                    reload: 40,
                    resize: true,
                    scrollReload: 2000,
                    sources: [
                        {
                            type: AdSourceType.RTB,
                            extParams: { awaps_section: '29337' },
                            code: [ 'R-A-148437-7', 'R-A-148437-4' ],
                        },
                    ],
                },
            };

            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );
            expect(tree.find('.VertisAds__rtb')).toHaveLength(1);
            expect(tree.find('.VertisAds__rtb_hidden_yes')).toHaveLength(0);
        });

        it('должен отрисовать блок rtb скрытым, если есть настройка initiallyHidden=true', () => {
            state.ads!.data.settings = {
                r1: {
                    id: 'r1',
                    initiallyHidden: true,
                    reload: 40,
                    resize: true,
                    scrollReload: 2000,
                    sources: [
                        {
                            type: AdSourceType.RTB,
                            extParams: { awaps_section: '29337' },
                            code: [ 'R-A-148437-7', 'R-A-148437-4' ],
                        },
                    ],
                },
            };
            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );

            expect(tree.find('.VertisAds__rtb.VertisAds__rtb_hidden_yes')).toHaveLength(1);
        });

        it('должен отрисовать блок adfox открытым', () => {
            state.ads!.data.settings = {
                r1: {
                    id: 'r1',
                    sources: [
                        {
                            type: AdSourceType.ADFOX,
                            params: { pp: 'bbhz' },
                            placeId: '12345',
                        },
                    ],
                },
            };
            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );
            expect(tree.find('.VertisAds__adfox')).toHaveLength(1);
            expect(tree.find('.VertisAds__rtb_hidden_yes')).toHaveLength(0);
        });

        it('должен отрисовать блок adfox скрытым, если есть настройка initiallyHidden=true', () => {
            state.ads!.data.settings = {
                r1: {
                    id: 'r1',
                    initiallyHidden: true,
                    sources: [
                        {
                            type: AdSourceType.ADFOX,
                            params: { pp: 'bbhz' },
                            placeId: '12345',
                        },
                    ],
                },
            };
            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );
            expect(tree.find('.VertisAds__adfox.VertisAds__adfox_hidden_yes')).toHaveLength(1);
        });
    });

    describe('top s2s баннер', () => {
        it('должен отрисовать место под баннер с высотой из .meta.settings.*.height', () => {
            state.ads!.data = {
                code: 'mag-index',
                settings: {
                    top: {
                        id: 'top',
                        reload: 40,
                        resize: true,
                        scrollReload: 2000,
                        sources: [
                            {
                                type: AdSourceType.RTB,
                                extParams: { awaps_section: '29337' },
                                code: 'R-A-464111-1',
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '464111',
                                            'imp-id': '12',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
                data: {
                    rtb: {
                        'top:0': {
                            meta: {
                                settings: {
                                    '12': {
                                        height: '200',
                                    },
                                },
                            },
                        },
                    },
                },
                statId: '100',
            };
            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.TOP }/></Context></Provider>,
            );
            expect(tree.html()).toContain(
                '<vertisads-rtb style="height:200px;display:block !important" class="VertisAds__rtb VertisAds__index_0 VertisAds__rtb_with-server-data">',
            );
        });

        it('должен отрисовать место под top-баннер с фиксированной высотой для native s2s-баннера', () => {
            state.ads!.data = {
                code: 'mag-index',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                type: AdSourceType.RTB,
                                extParams: { awaps_section: '29337' },
                                code: 'R-A-464111-1',
                                'native': true,
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '71',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
                data: {
                    rtb: {
                        'top:0': {
                            meta: {},
                        },
                    },
                },
                statId: '100',
            };

            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.TOP }/></Context></Provider>,
            );

            expect(tree.html()).toContain(
                '<vertisads-rtb style="min-height:67px;display:block !important" class="VertisAds__rtb VertisAds__index_0 VertisAds__rtb_with-server-data">',
            );
        });

        it('должен отрисовать место под баннер без серверных данных', () => {
            state.ads!.data = {
                code: 'mag-index',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                type: AdSourceType.RTB,
                                extParams: { awaps_section: '29337' },
                                code: 'R-A-464111-1',
                                'native': true,
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '71',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
                data: {},
                statId: '100',
            };

            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.TOP }/></Context></Provider>,
            );
            expect(tree.html()).toContain(
                '<vertisads-rtb class="VertisAds__rtb VertisAds__index_0">',
            );
        });

        it('не должен отрисовать top-баннер, если нет данных про высоту', () => {
            state.ads!.data = {
                code: 'index',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                type: AdSourceType.RTB,
                                extParams: { awaps_section: '29337' },
                                code: 'R-A-464111-1',
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '71',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
                data: {
                    rtb: {
                        'top:0': {
                            meta: {
                                settings: {},
                            },
                        },
                    },
                },
                statId: '100',
            };

            const tree = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.TOP }/></Context></Provider>,
            );

            expect(tree.html()).toEqual('');
        });
    });

    describe('серверный direct', () => {
        it('должен отрисовать два баннера из одного источника с учетом приоритетов (premium+direct)', () => {
            state.ads!.data = {
                code: 'catalog-card',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                            },
                        ],
                    },
                    c2: {
                        id: 'c2',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                                num: 1,
                            },
                        ],
                    },
                },
                data: {
                    direct: {
                        'top:0': {
                            common: {
                                linkHead: 'common linkHead',
                            },
                            premium: {
                                ads: [
                                    {
                                        url: 'url-for-premium',
                                        title: 'premium title',
                                        body: 'premium body',
                                        domain: 'premium domain',
                                        linkTail: 'premium linkTail',
                                    },
                                ],
                            },
                            direct: {
                                ads: [
                                    {
                                        url: 'url-for-direct0',
                                        title: 'direct0 title',
                                        body: 'direct0 body',
                                        domain: 'direct0 domain',
                                        linkTail: 'direct0 linkTal',
                                    },
                                    {
                                        url: 'url-for-direct1',
                                        title: 'direct1 title',
                                        body: 'direct1 body',
                                        domain: 'direct1 domain',
                                        linkTail: 'direct1 linkTail',
                                    },
                                ],
                            },
                        },
                    },
                },
                statId: '100',
            };
            const tree = render(
                <Provider store={ mockStore(state) }>
                    <Context>
                        <div>
                            <Ad place={ AdPlace.TOP }/>
                            <Ad place={ AdPlace.C2 } direct={{ layout: DirectLayout.WHITE }}/>
                        </div>
                    </Context>
                </Provider>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('должен отрисовать два баннера из одного источника (source.groupKey) с учетом приоритетов (direct+direct), если нет первого источника', () => {
            state.ads!.data = {
                code: 'catalog-card',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                            },
                        ],
                    },
                    c2: {
                        id: 'c2',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                                num: 1,
                            },
                        ],
                    },
                },
                data: {
                    direct: {
                        'top:0': {
                            common: {
                                linkHead: 'common linkHead',
                            },
                            direct: {
                                ads: [
                                    {
                                        url: 'url-for-direct0',
                                        title: 'direct0 title',
                                        body: 'direct0 body',
                                        domain: 'direct0 domain',
                                        linkTail: 'direct0 linkTal',
                                    },
                                    {
                                        url: 'url-for-direct1',
                                        title: 'direct1 title',
                                        body: 'direct1 body',
                                        domain: 'direct1 domain',
                                        linkTail: 'direct1 linkTail',
                                    },
                                ],
                            },
                        },
                    },
                },
                statId: '100',
            };
            const tree = render(
                <Provider store={ mockStore(state) }>
                    <Context>
                        <div>
                            <Ad place={ AdPlace.TOP }/>
                            <Ad place={ AdPlace.C2 } direct={{ layout: DirectLayout.WHITE }}/>
                        </div>
                    </Context>
                </Provider>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('должен отрисовать баннер для нового формата direct_premium', () => {
            state.ads!.data = {
                code: 'catalog-card',
                settings: {
                    c3: {
                        id: 'c3',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'direct_premium', 'premium', 'direct' ],
                                groupKey: 'AUTORUFRONT-17374',
                            },
                        ],
                    },
                },
                data: {
                    direct: {
                        'AUTORUFRONT-17374:0': {
                            direct_premium: [
                                {
                                    domain: 'direct_premium domain',
                                    url: 'direct_premium url',
                                    link_tail: 'direct_premium link_tail',
                                    title: 'direct_premium title',
                                    body: 'direct_premium body',
                                },
                            ],
                            stat: [
                                { link_head: 'direct_premium link_head' },
                            ],
                        },
                    },
                },
                statId: '100',
            };
            const tree = render(
                <Provider store={ mockStore(state) }>
                    <Context>
                        <div>
                            <Ad place={ AdPlace.C3 } direct={{ layout: DirectLayout.WHITE }}/>
                        </div>
                    </Context>
                </Provider>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('не должен отрисовать баннер без данных', () => {
            state.ads!.data = {
                code: 'catalog-card',
                settings: {
                    c1: {
                        id: 'c1',
                        sources: [
                            {
                                params: { pageId: '151547' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'direct_premium', 'premium', 'direct' ],
                            },
                        ],
                    },
                },
                data: {},
                statId: '100',
            };
            const tree = render(
                <Provider store={ mockStore(state) }>
                    <Context>
                        <Ad place={ AdPlace.C1 }/>
                    </Context>
                </Provider>,
            );

            expect(tree.html()).toEqual('');
        });
    });

    describe('подписка на событие "metrika"', () => {
        it('должен подписаться на событие и отправить его параметром визита в Метрику', () => {
            jest.useFakeTimers();

            state.ads!.data.settings = {
                r1: {
                    id: 'r1',
                    reload: 40,
                    resize: true,
                    scrollReload: 2000,
                    sources: [
                        {
                            type: AdSourceType.RTB,
                            extParams: { awaps_section: '29337' },
                            code: [ 'R-A-148437-7', 'R-A-148437-4' ],
                        },
                    ],
                },
            };
            const tree = mount(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );

            jest.runAllTimers();

            // эмулируем событие, которое бросит inline-код рекламы
            // https://a.yandex-team.ru/arcadia/classifieds/autoru-frontend/auto-core/lib/ads/AD2.ts#L790
            tree.find('vertisads-r1').getDOMNode()
                .dispatchEvent(new CustomEvent('metrika', { detail: { 'rtb-ascii': 'foo|bar' } }));

            expect(contextMock.metrika.params).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.params).toHaveBeenCalledWith({ 'rtb-ascii': 'foo|bar' });
        });
    });
});
