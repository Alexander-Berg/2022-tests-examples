import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { EGRNAddressPurchaseSuggest } from '../';

const Component = ({ Gate, ...props }: { Gate?: Record<string, unknown>; disabled?: boolean }) => (
    <AppProvider Gate={Gate}>
        <EGRNAddressPurchaseSuggest {...props} onSubmit={noop} />
    </AppProvider>
);

describe('EGRNAddressPurchaseSuggest', () => {
    it('рендерится', async () => {
        await render(<Component />, {
            viewport: { width: 600, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в задизейбленном состоянии', async () => {
        await render(<Component disabled />, {
            viewport: { width: 600, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с подсказкой о вводе номера квартиры', async () => {
        const Gate = {
            create: () => Promise.resolve([{ address: 'Москва, ул. Ленина 32' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.keyboard.press('Enter');
        await page.waitFor(500);
        await page.click('html');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится со статичными подсказками (не зависящими от текста в инпуте)', async () => {
        const Gate = {
            create: () => Promise.resolve([{ cadastralNumber: '123 456 789' }, { address: 'Москва, ул. Ленина 32' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с ошибкой', async () => {
        const Gate = {
            create: () => Promise.reject(),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 800, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('подставляет текст в текстовое поле при преселекте подсказки из списка с клавиатуры', async () => {
        const Gate = {
            create: () => Promise.resolve([{ cadastralNumber: '123 456 789' }, { address: 'Москва, ул. Ленина 32' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('не подставляет текст в текстовое поле при преселекте подсказки из списка мышкой', async () => {
        const Gate = {
            create: () => Promise.resolve([{ cadastralNumber: '123 456 789' }, { address: 'Москва, ул. Ленина 32' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);
        await page.hover('li');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // eslint-disable-next-line max-len
    it('с активной кнопкой, первая подсказка - валидная (с номером квартиры или кадастровым номером)', async () => {
        allure.descriptionHtml(
            'Этот кейс проверяет случай, когда пользователь весь адрес ввёл ' +
                'собственноручно, не выбирая ничего из подсказок. В таком случае, ' +
                'если адрес введён корректно, то саджест её расшифрует и правильная подсказка ' +
                'окажется на первом месте в списке подсказок. В текстовом поле в это время ' +
                'будет нерасшифрованный текст, а нужные для ручек данные (кадастровый номер или номер квартиры) ' +
                'будут браться из той первой подсказки, хоть она и не была выбрана'
        );

        const Gate = {
            create: () =>
                Promise.resolve([
                    { address: 'Москва, ул. Ленина 32, кв. 33', flatNumber: '33' },
                    { cadastralNumber: '123 456 789' },
                ]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с активной кнопкой, когда выбрана валидная подсказка', async () => {
        const Gate = {
            create: () =>
                Promise.resolve([
                    { cadastralNumber: '123 456 789' },
                    { address: 'Москва, ул. Ленина 32, кв. 33', flatNumber: '33' },
                ]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 200 },
        });

        await page.type('input', 'blablabla');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.keyboard.press('ArrowDown');
        await page.keyboard.press('Enter');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
