import React from 'react';
import { render as _render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ComplainReasonsTreeContainer } from '../container';
import listItemStyles from '../ListItem/styles.module.css';

import REASONS from './reasons.json';

const renderOptions = [
    { viewport: { width: 450, height: 600 } },
    { viewport: { width: 1000, height: 800 } }
];

const state = {
    user: { crc: '' }
};

const render = async(component, opts) => {
    await _render(component, opts);
    await page.addStyleTag({ content: 'body{padding: 0}' });
};

const complainActionLoading = () => new Promise(noop);
const complainActionSuccess = () => Promise.resolve();
const complainActionFail = () => Promise.reject();

const Component = props => (
    <AppProvider initialState={state} disableSetTimeoutDelay>
        <ComplainReasonsTreeContainer
            visible
            data={REASONS}
            offerId='777'
            complainAction={() => Promise.resolve()}
            onFinish={() => {}}
            {...props}
        />
    </AppProvider>
);

describe('ComplainReasonsTreeContainer', () => {
    renderOptions.forEach(renderOption => {
        it(`Показ экрана загрузки ${renderOption.viewport.width} px`, async() => {
            const component = <Component complainAction={complainActionLoading} />;

            await render(component, renderOption);

            await page.click(`.${listItemStyles.container}:nth-child(1)`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Успешная отправка жалобы ${renderOption.viewport.width} px`, async() => {
            const component = <Component complainAction={complainActionSuccess} />;

            await render(component, renderOption);

            await page.click(`.${listItemStyles.container}:nth-child(1)`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Не удалось отправить жалобу ${renderOption.viewport.width} px`, async() => {
            const component = <Component complainAction={complainActionFail} />;

            await render(component, renderOption);

            await page.click(`.${listItemStyles.container}:nth-child(1)`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Клик на другую причину жалобы ${renderOption.viewport.width} px`, async() => {
            const anotherReason = REASONS.find(item => item.reason === 'ANOTHER');
            const index = REASONS.indexOf(anotherReason);

            const component = <Component />;

            await render(component, renderOption);
            await page.click(`.${listItemStyles.container}:nth-child(${index + 1})`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
