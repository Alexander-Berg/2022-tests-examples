import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Select } from '../index';

const renderOptions = { viewport: { width: 400, height: 120 } };

describe('Select', () => {
    describe('Внешний вид', () => {
        it('Пустой', async () => {
            await render(
                <Select<number | undefined>
                    size="l"
                    value={undefined}
                    label={'Статус квартиры'}
                    onChange={noop}
                    variant="bordered"
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Пустой, ошибка', async () => {
            await render(
                <Select<number | undefined>
                    size="l"
                    value={undefined}
                    label={'Статус квартиры'}
                    onChange={noop}
                    variant="bordered"
                    isInvalid
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Выбрано значение', async () => {
            const options = [
                {
                    value: 'FLAT_REJECTED',
                    text: 'Отклонена',
                },
                {
                    value: 'FLAT_CONFIRMED',
                    text: 'Сдана',
                },
            ];

            await render(
                <Select<string>
                    options={options}
                    size="l"
                    value="FLAT_CONFIRMED"
                    label={'Статус квартиры'}
                    onChange={noop}
                    variant="bordered"
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
