/**
 * @jest-environment jsdom
 */

import type { AdsData } from '../types/types';
import type { Props } from '../client/components/Ad/Ad';
import { AdSourceType } from '../types/types';

import { render } from '@testing-library/react';

import Ad from '../client/components/Ad/Ad';
import { initAd } from './index';

import React from 'react';
import { directRender } from '../mocks/directRender';
import Mock = jest.Mock;

let ads: AdsData;

describe('top-баннер (ADFOX + DIRECT)', () => {
    beforeAll(() => {
        Object.defineProperty(document, 'readyState', {
            get() {
                return 'interactive';
            },
        });
    });

    let adfoxCodeCreate: Mock;
    beforeEach(() => {
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

        adfoxCodeCreate = jest.fn();
        window.Ya = {
            adfoxCode: {
                create: adfoxCodeCreate,
                createScroll: jest.fn(),
                destroy: jest.fn(),
                reload: jest.fn(),
            },
        };
    });

    it('должен отрисовать ADFOX и скрыть DIRECT, если есть и то и то (onRender callback)', () => {
        adfoxCodeCreate.mockImplementation((config: YaAdfoxCodeBannerConfig) => {
            config.onRender();
        });

        renderAndInitAd();

        expect(
            document.getElementsByTagName('vertisads-adfox')[0],
        ).toHaveProperty('className', 'VertisAds__adfox VertisAds__index_0');
        expect(
            document.getElementsByTagName('vertisads-direct')[0],
        ).toHaveProperty('className', 'VertisAds__direct VertisAds__index_1 VertisAds__direct_view_default VertisAds__direct_hidden_yes');
    });

    it('должен отрисовать DIRECT и скрыть ADFOX, если нет ADFOX (onError callback)', () => {
        adfoxCodeCreate.mockImplementation((config: YaAdfoxCodeBannerConfig) => {
            config.onError();
        });

        renderAndInitAd();

        expect(
            document.getElementsByTagName('vertisads-adfox')[0],
        ).toHaveProperty('className', 'VertisAds__adfox VertisAds__index_0 VertisAds__adfox_hidden_yes');
        expect(
            document.getElementsByTagName('vertisads-direct')[0],
        ).toHaveProperty('className', 'VertisAds__direct VertisAds__index_1 VertisAds__direct_view_default');
    });

    it('должен отрисовать ADFOX и скрыть DIRECT, если есть и то и то (onLoad callback)', () => {
        adfoxCodeCreate.mockImplementation((config: YaAdfoxCodeBannerConfig) => {
            config.onLoad({});
        });

        renderAndInitAd();

        expect(
            document.getElementsByTagName('vertisads-adfox')[0],
        ).toHaveProperty('className', 'VertisAds__adfox VertisAds__index_0');
        expect(
            document.getElementsByTagName('vertisads-direct')[0],
        ).toHaveProperty('className', 'VertisAds__direct VertisAds__index_1 VertisAds__direct_view_default VertisAds__direct_hidden_yes');
    });

    function renderAndInitAd() {
        initAd({});

        // Рисуем статичную верстку от рекламного компонента
        const container = document.createElement('div');
        document.body.appendChild(container);

        render(
            renderComponent({ ads, place: 'top' }),
            { container },
        );

        // TODO: так и не понял как его заставить нормально исполнить скрипт
        const text = container.getElementsByTagName('script')[0];
        eval(text.innerHTML);
        window.yaContextCb.forEach((item) => item());
    }
});

function renderComponent(props: Partial<Props>) {
    return (
        <Ad
            ads={{ code: 'index', data: {} }}
            directRender={ directRender }
            getClassForLayoutPlace={ jest.fn((place: string) => `VertisAds_place_${ place }`) }
            getClassForPlace={ jest.fn((place: string) => `VertisAds_place_${ place }`) }
            nonce=""
            place="r1"
            { ...props }
        />
    );
}
