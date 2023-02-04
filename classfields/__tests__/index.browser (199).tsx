import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import reducer from 'view/reducers/pages/EGRNAddressPurchasePage';

import { AppProvider } from 'view/libs/test-helpers';

import { EGRNAddressPurchaseSuggest } from '../';
import styles from '../styles.module.css';

const modalInputSelector = `.${styles.modalContent} input`;

const Component = ({ Gate, ...props }: { Gate?: Record<string, unknown>; disabled?: boolean }) => (
    <AppProvider rootReducer={reducer} Gate={Gate}>
        <EGRNAddressPurchaseSuggest {...props} onSubmit={() => undefined} />
    </AppProvider>
);

describe('EGRNAddressPurchaseSuggest', () => {
    it('закрытое состояние', async () => {
        await render(<Component />, {
            viewport: { width: 350, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('задизейбленное закрытое состояние', async () => {
        await render(<Component disabled />, {
            viewport: { width: 350, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('автофокус на инпуте в открытом модале', async () => {
        await render(<Component />, {
            viewport: { width: 350, height: 300 },
        });

        await page.click('input');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('со статичными подсказками (не зависящими от текста в инпуте)', async () => {
        const Gate = {
            create: () => Promise.resolve([{ address: 'Москва, ул. Ленина 32' }, { cadastralNumber: '123 456 789' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 350, height: 400 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('со статичными подсказками (не зависящими от текста в инпуте) в альбомной ориентации', async () => {
        const Gate = {
            create: () =>
                Promise.resolve([
                    { address: 'Москва, ул. Ленина 32' },
                    { cadastralNumber: '123 456 789' },
                    { address: 'Москва, ул. Ленина 32' },
                    { cadastralNumber: '123 456 789' },
                    { address: 'Москва, ул. Ленина 32' },
                ]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 600, height: 320 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с ошибкой', async () => {
        const Gate = {
            create: () => Promise.reject(),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 350, height: 400 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // eslint-disable-next-line max-len
    it('активная кнопка, 1-ая подсказка валидная (с ном. квартиры или кадастр. номером)', async () => {
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
            viewport: { width: 350, height: 400 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, 'blablabla');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка не закрывается после выбора невалидной подсказки', async () => {
        const Gate = {
            create: () => Promise.resolve([{ address: 'Москва, ул. Ленина 32, кв. 199' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 350, height: 400 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, 'blablabla');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.keyboard.press('Enter');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка закрывается после выбора валидной подсказки', async () => {
        const Gate = {
            create: () => Promise.resolve([{ cadastralNumber: '123 456 789' }]),
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 350, height: 200 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, '123 456');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.keyboard.press('Enter');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
