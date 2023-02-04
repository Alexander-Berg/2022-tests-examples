/**
 * @jest-environment jsdom
 */

import type { AdsData } from '../../../types/types';
import type { Props } from './Ad';
import { AdSourceType } from '../../../types/types';

import { renderToString } from 'react-dom/server';
import { render } from '@testing-library/react';

import Ad from './Ad';

import React from 'react';

import { getAd2Mock } from '../../../mocks/AD2';

let ads: AdsData;
beforeEach(() => {
    window.AD2 = getAd2Mock();
});

afterEach(() => {
    delete window.AD2;
});

describe('гидрация', () => {
    it('должен гидрировать компонент и сохранить рекламную верстку внутри', () => {
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

        // Рисуем статичную верстку от рекламного компонента
        const container = document.createElement('div');
        container.innerHTML = renderToString(
            renderComponent({ ads, place: 'r1' }),
        );
        // эмулируем как будто рекламный код отработал и внутри появился iframe
        container.getElementsByTagName('vertisads-rtb')[0].innerHTML = '<iframe src="//localhost"/>';
        document.body.appendChild(container);

        // запоминаем ссылки на ноды
        const adTag = container.getElementsByTagName('vertisads-r1')[0];
        const sourceTag = container.getElementsByTagName('vertisads-rtb')[0];
        const iframeTag = container.getElementsByTagName('iframe')[0];

        Object.defineProperty(document, 'readyState', {
            get() {
                return 'interactive';
            },
        });

        render(
            renderComponent({ ads, place: 'r1' }),
            { container, hydrate: true },
        );

        // проверяем что ссылки сохранились и ноды не поменялись
        expect(document.getElementsByTagName('vertisads-r1')[0] === adTag).toBe(true);
        expect(document.getElementsByTagName('vertisads-rtb')[0] === sourceTag).toBe(true);
        expect(document.getElementsByTagName('iframe')[0] === iframeTag).toBe(true);
    });
});

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
