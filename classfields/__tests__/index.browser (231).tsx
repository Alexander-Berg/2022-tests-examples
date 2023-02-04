import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageFooterLinks } from '../';

const mobileViewports = [
    { width: 320, height: 200 },
    { width: 400, height: 200 },
];

const render = async (component: React.ReactElement) => {
    for (const viewport of mobileViewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const sectionsMock = [
    {
        title: 'По типу жилья',
        items: [
            {
                text: 'Ипотека на новостройку',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Ипотека на вторичном рынке',
                url: 'https://m.realty.yandex.ru/',
            },
        ],
    },
    {
        title: 'Льготная ипотека',
        items: [
            {
                text: 'Ипотека с господдержкой',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Военная ипотека',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Дальневосточная ипотека',
                url: 'https://m.realty.yandex.ru/',
            },
        ],
    },
    {
        title: 'Для семей',
        items: [
            {
                text: 'Семейная ипотека',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Ипотека с материнским капиталом',
                url: 'https://m.realty.yandex.ru/',
            },
        ],
    },
    {
        title: 'Часто ищут',
        items: [
            {
                text: 'Ипотека без подтверждения дохода',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Ипотека без первонаяального взноса',
                url: 'https://m.realty.yandex.ru/',
            },
            {
                text: 'Ипотечный калькулятор',
                url: 'https://m.realty.yandex.ru/',
            },
        ],
    },
];

describe('MortgageFooterLinks', () => {
    it('рендерится корректно', async () => {
        await render(<MortgageFooterLinks sections={sectionsMock} />);
    });
});
