import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { TextArea } from '../index';

const renderOptions = { viewport: { width: 500, height: 200 } };

describe('TextArea', () => {
    describe('Внешний вид', () => {
        it('Базовое состояние', async () => {
            await render(
                <TextArea<string | undefined> variant="bordered" size="l" label={'Адрес'} onChange={noop} />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовое состояние заполненное', async () => {
            await render(
                <TextArea<string>
                    variant="bordered"
                    size="l"
                    label="Адрес"
                    value="Санкт-Петербург, Ленина 45"
                    onChange={noop}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовое состояние c ошибкой', async () => {
            await render(
                <TextArea<string>
                    variant="bordered"
                    size="l"
                    label="Адрес"
                    value="Санкт-Петербург, Ленина 45"
                    isInvalid
                    onChange={noop}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Логика', () => {
        const id = 'textarea';
        const selectors = {
            textarea: `#${id}`,
        };

        const LogicComponent = () => {
            const [value, setState] = React.useState<string>('');

            const eventHandler = (value: string | undefined) => setState(value || '');

            return (
                <TextArea<string>
                    id={id}
                    variant="bordered"
                    size="l"
                    label="Адрес"
                    value={value}
                    onChange={eventHandler}
                />
            );
        };

        it('При фокусе отодвигается лейбл', async () => {
            await render(<LogicComponent />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.focus(selectors.textarea);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('При фокусе наличии текста отодвигается лейбл', async () => {
            await render(<LogicComponent />, renderOptions);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.textarea, 'Улица Пушкина, дом колотушкина');

            await page.$eval(selectors.textarea, (e) => (e as HTMLInputElement).blur());

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
