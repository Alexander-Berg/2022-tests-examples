import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageBankPageFooterLinks } from '../';
import styles from '../styles.module.css';

const geo = {
    rgid: 1,
};

function getBanks(amount = 17) {
    const names = [
        'Альфа-Банк',
        'АТБ',
        'Абсолют',
        'Ак Барс',
        'ВТБ',
        'Газпромбанк',
        'ДОМ.РФ',
        'Открытие',
        'Промсвязьбанк',
        'РНКБ',
        'Росбанк Дом',
        'Россельхозбанк',
        'СМП Банк',
        'Санкт-Петербург',
        'Совкомбанк',
        'Сургутнефтегазбанк',
        'Транскапиталбанк',
    ];

    return Array(amount)
        .fill(undefined)
        .map((_, index) => {
            return {
                id: index,
                name: index < names.length ? names[index] : names[index % names.length],
            };
        });
}

describe('MortgageBankPageFooterLinks', () => {
    it('рисует все ссылки', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageBankPageFooterLinks geo={geo} banks={getBanks()} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ссылку другого цвета при наведении', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageBankPageFooterLinks geo={geo} banks={getBanks()} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 200 } }
        );

        await page.hover(`.${styles.column}:nth-child(1) .${styles.link}:nth-child(1)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует ссылку с кнопкой "Все банки"', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageBankPageFooterLinks geo={geo} banks={getBanks(49)} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует все ссылки после нажатия кнопки "Все банки"', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageBankPageFooterLinks geo={geo} banks={getBanks(49)} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 400 } }
        );

        await page.hover(`.${styles.showMore}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.click(`.${styles.showMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
