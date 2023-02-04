import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { rootReducer } from 'view/common/reducers';

import { YaArendaFormBannerContainer } from '../container';

const getState = (date = '2020-12-30 01:00') =>
    merge({
        user: {},
        geo: {
            id: 1,
            type: 'SUBJECT_FEDERATION',
            rgid: 741965,
            name: 'Санкт-Петербург и ЛО',
            locative: 'в Санкт-Петербурге и ЛО'
        },
        yandexArenda: {
            savedPhone: '',
            isSuccess: false
        },
        config: {
            serverTimeStamp: new Date(date).getTime(),
            yaArendaUrl: 'https://arenda.realty.yandex.ru'
        }
    });

const Component = ({ initialState, Gate, ...props }) => (
    <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
        <YaArendaFormBannerContainer {...props} />
    </AppProvider>
);

describe('YaArendaFormBanner', () => {
    it('рисует баннер на форме', async() => {
        const state = getState();

        await render(
            <AppProvider initialState={state}>
                <YaArendaFormBannerContainer />
            </AppProvider>,
            { viewport: { width: 1600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Номер успешно сохранен', async() => {
        const state = getState();
        const Gate = {
            create: () => Promise.resolve({ phone: '+79216663211' })
        };

        await render(<Component initialState={state} Gate={Gate} />,
            { viewport: { width: 1600, height: 700 } });

        await page.type('input', '9216663211');
        await page.click('.Button');

        expect(
            await takeScreenshot()
        ).toMatchImageSnapshot();
    });
});
