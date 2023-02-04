/**
 * @jest-environment jsdom
 */

import type { AdsData } from '../../../types/types';
import type { Props } from './Ad';
import { AdSourceType } from '../../../types/types';

import Ad from './Ad';

import React from 'react';
import { render } from 'enzyme';

import { getAd2Mock } from '../../../mocks/AD2';
import { directRender } from '../../../mocks/directRender';

let ads: AdsData;
afterEach(() => {
    delete window.AD2;
});

describe('выдача для роботов', () => {
    it('не должен ничего рендерить и падать на componentDidMount', () => {
        const tree = renderHtml({
            ads: { code: '', data: {} },
            place: 'r1',
        });
        expect(tree).toMatchSnapshot();
    });
});

describe('тестовый баннер', () => {
    beforeEach(() => {
        window.AD2 = getAd2Mock();
    });

    it('должен рендерить HTML для баннера (RTB)', () => {
        ads = {
            code: '',
            data: {},
            settings: {
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
            },
        };
        const tree = renderHtml({ ads, place: 'r1' });
        expect(tree).toMatchSnapshot();
    });

    it('должен рендерить HTML для баннера с несколькими источниками (ADFOX + RTB + ADFOX)', () => {
        ads = {
            code: '',
            data: {},
            settings: {
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
            },
        };
        const tree = renderHtml({
            ads,
            place: 'r1',
        });
        expect(tree).toMatchSnapshot();
    });

    it('должен рендерить HTML для баннера с несколькими источниками (ADFOX + DIRECT)', () => {
        ads = {
            code: 'listing-cars',
            data: {
                direct: {
                    'top:1': {
                        direct_premium: [
                            {
                                domain: 'kamazleasing.ru',
                                link_tail: 'link_tail_0',
                                title: 'КАМАЗ-ЛИЗИНГ – Акция "ГОРЯЧИЙ ЛИЗИНГ"',
                                url: 'https://url_0',
                                body: 'Аванс от 0%. Сниженная ставка лизинга. КАСКО - 1,07%. Срок - до 5 лет. Оставьте заявку!',
                            },
                        ],
                        stat: [
                            {
                                link_head: 'link_head_0',
                            },
                        ],
                    },
                },
                rtb: {},
            },
            settings: {
                top: {
                    sources: [
                        {
                            type: AdSourceType.ADFOX,
                            placeId: '243420',
                            params: {
                                pp: 'g',
                                ps: 'cefr',
                                p2: 'gwom',
                            },
                        },
                        {
                            type: AdSourceType.DIRECT,
                            sections: [
                                'direct_premium',
                                'premium',
                                'direct',
                            ],
                            params: {
                                pageId: '699956',
                            },
                            count: 1,
                        },
                    ],
                    id: 's4ayr0x2xb',
                },
            },
        };

        const tree = renderHtml({
            ads,
            directRender,
            place: 'top',
        });
        expect(tree).toMatchSnapshot();
    });

    describe('модификатор hidden_yes', () => {
        it('должен отрисовать блок rtb открытым', () => {
            ads = {
                code: '',
                data: {},
                settings: {
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
                },
            };
            const tree = renderHtml({ ads, place: 'r1' });
            expect(tree.find('.VertisAds__rtb')).toHaveLength(1);
            expect(tree.find('.VertisAds__rtb_hidden_yes')).toHaveLength(0);
        });

        it('должен отрисовать блок rtb скрытым, если есть настройка initiallyHidden=true', () => {
            ads = {
                code: '',
                data: {},
                settings: {
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
                },
            };
            const tree = renderHtml({ ads, place: 'r1' });
            expect(tree.find('.VertisAds__rtb.VertisAds__rtb_hidden_yes')).toHaveLength(1);
        });

        it('должен отрисовать блок adfox открытым', () => {
            ads = {
                code: '',
                data: {},
                settings: {
                    r1: {
                        id: 'r1',
                        sources: [
                            {
                                type: AdSourceType.ADFOX,
                                params: { pp: 'bbhz' },
                                placeId: '243420',
                            },
                        ],
                    },
                },
            };
            const tree = renderHtml({ ads, place: 'r1' });
            expect(tree.find('.VertisAds__adfox')).toHaveLength(1);
            expect(tree.find('.VertisAds__rtb_hidden_yes')).toHaveLength(0);
        });

        it('должен отрисовать блок adfox скрытым, если есть настройка initiallyHidden=true', () => {
            ads = {
                code: '',
                data: {},
                settings: {
                    r1: {
                        id: 'r1',
                        initiallyHidden: true,
                        sources: [
                            {
                                type: AdSourceType.ADFOX,
                                params: { pp: 'bbhz' },
                                placeId: '243420',
                            },
                        ],
                    },
                },
            };
            const tree = renderHtml({ ads, place: 'r1' });
            expect(tree.find('.VertisAds__adfox.VertisAds__adfox_hidden_yes')).toHaveLength(1);
        });
    });

    describe('top s2s баннер', () => {
        it('должен отрисовать место под баннер с высотой из .meta.settings.*.height', () => {
            ads = {
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
            };
            const tree = renderHtml({ ads, place: 'top' });
            expect(tree.html()).toContain(
                // eslint-disable-next-line max-len
                '<vertisads-rtb style="height:200px;display:block !important" class="VertisAds__rtb VertisAds__index_0 VertisAds__rtb_with-server-data">',
            );
        });

        it('должен отрисовать место под top-баннер с фиксированной высотой для native s2s-баннера', () => {
            ads = {
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
            };

            const tree = renderHtml({ ads, place: 'top' });

            expect(tree.html()).toContain(
                // eslint-disable-next-line max-len
                '<vertisads-rtb style="min-height:67px;display:block !important" class="VertisAds__rtb VertisAds__index_0 VertisAds__rtb_with-server-data">',
            );
        });

        it('должен отрисовать место под баннер без серверных данных', () => {
            ads = {
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
            };

            const tree = renderHtml({ ads, place: 'top' });
            expect(tree.html()).toContain(
                '<vertisads-rtb class="VertisAds__rtb VertisAds__index_0">',
            );
        });

        it('не должен отрисовать top-баннер, если нет данных про высоту', () => {
            ads = {
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
            };

            const tree = renderHtml({ ads, place: 'top' });

            expect(tree.html()).toBe('');
        });

        it('должен рендерить HTML для баннера с несколькими источниками (ADFOX + RTB S2S)', () => {
            ads = {
                code: 'listing-cars',
                data: {
                    direct: {},
                    rtb: {
                        'top:1': {
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
                settings: {
                    top: {
                        sources: [
                            {
                                type: AdSourceType.ADFOX,
                                placeId: '243420',
                                params: {
                                    pp: 'g',
                                    ps: 'cefr',
                                    p2: 'gwom',
                                },
                            },
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
                        id: 's4ayr0x2xb',
                    },
                },
            };

            const tree = renderHtml({ ads, place: 'top' });
            expect(tree).toMatchSnapshot();
        });
    });

    describe('серверный direct', () => {
        it('должен отрисовать два баннера из одного источника с учетом приоритетов (premium+direct)', () => {
            ads = {
                code: 'catalog-card',
                settings: {
                    top: {
                        id: 'top',
                        sources: [
                            {
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                                params: { pageId: '151547' },
                            },
                        ],
                    },
                    c2: {
                        id: 'c2',
                        sources: [
                            {
                                type: AdSourceType.DIRECT,
                                sections: [ 'premium', 'direct' ],
                                groupKey: 'top',
                                num: 1,
                                params: { pageId: '151547' },
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
            };
            const tree = render(
                <div>
                    { renderComponent({ ads, directRender, place: 'top' }) }
                    { renderComponent({ ads, direct: { layout: 'white' }, directRender, place: 'c2' }) }
                </div>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('должен отрисовать два баннера из одного источника (source.groupKey) с учетом приоритетов (direct+direct), если нет первого источника', () => {
            ads = {
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
            };
            const tree = render(
                <div>
                    { renderComponent({ ads, directRender, place: 'top' }) }
                    { renderComponent({ ads, direct: { layout: 'white' }, directRender, place: 'c2' }) }
                </div>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('должен отрисовать баннер для нового формата direct_premium', () => {
            ads = {
                code: 'catalog-card',
                settings: {
                    c3: {
                        id: 'c3',
                        sources: [
                            {
                                params: { pageId: 'c3' },
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
            };
            const tree = render(
                <div>
                    { renderComponent({ ads, direct: { layout: 'white' }, directRender, place: 'c3' }) }
                </div>,
            );

            expect(tree).toMatchSnapshot();
        });

        it('не должен отрисовать баннер без данных', () => {
            ads = {
                code: 'catalog-card',
                settings: {
                    c1: {
                        id: 'c1',
                        sources: [
                            {
                                params: { pageId: 'c1' },
                                type: AdSourceType.DIRECT,
                                sections: [ 'direct_premium', 'premium', 'direct' ],
                            },
                        ],
                    },
                },
                data: {
                    direct: {},
                },
                statId: '100',
            };
            const tree = renderHtml({ ads, directRender, place: 'c1' });

            expect(tree.html()).toBe('');
        });
    });
});

function renderHtml(props: Partial<Props>) {
    return render(renderComponent(props));
}

function renderComponent(props: Partial<Props>) {
    return (
        <Ad
            ads={{ code: 'index', data: {} }}
            getClassForLayoutPlace={ jest.fn((place: string) => `VertisAds_place_${ place }`) }
            getClassForPlace={ jest.fn((place: string) => `VertisAds_place_${ place }`) }
            nonce=""
            place="r1"
            { ...props }
        />
    );
}
