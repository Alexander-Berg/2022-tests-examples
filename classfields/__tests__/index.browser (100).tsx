import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Input } from '../index';
import styles from '../styles.module.css';

const renderOptions = { viewport: { width: 400, height: 120 } };

describe('Input', () => {
    describe('Внешний вид', () => {
        it('Базовое состояние', async () => {
            await render(
                <Input<string | undefined> variant="bordered" size="l" label={'Адрес'} onChange={noop} type="text" />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовое состояние заполненное', async () => {
            await render(
                <Input<string>
                    variant="bordered"
                    size="l"
                    label="Адрес"
                    value="Санкт-Петербург, Ленина 45"
                    onChange={noop}
                    type="text"
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовое состояние c ошибкой', async () => {
            await render(
                <Input<string>
                    variant="bordered"
                    size="l"
                    label={'Адрес'}
                    value={'Санкт-Петербург, Ленина 45'}
                    isInvalid
                    onChange={noop}
                    type="text"
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Логика', () => {
        const id = 'input';
        const selectors = {
            input: `#${id}`,
            close: `.${styles.close}`,
        };

        const LogicComponent = () => {
            const [value, setState] = React.useState<string>('');

            const eventHandler = (value: string | undefined) => setState(value || '');

            return (
                <Input<string>
                    id={id}
                    variant="bordered"
                    size="l"
                    label="Адрес"
                    value={value}
                    onChange={eventHandler}
                    type="text"
                />
            );
        };

        it('При фокусе отодвигается лейбл', async () => {
            await render(<LogicComponent />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(selectors.input);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('При фокусе наличии текста отодвигается лейбл', async () => {
            await render(<LogicComponent />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.input, 'Улица Пушкина, дом колотушкина');

            await page.$eval(selectors.input, (e) => (e as HTMLInputElement).blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Удаляется текст при нажатии на крестик', async () => {
            await render(<LogicComponent />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.input, 'Улица Пушкина, дом колотушкина');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
