import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardBreadcrumbs from '../';

const initialState = {
    config: {
        origin: 'yandex.ru'
    }
};

const developer = {
    id: '1',
    name: 'Самолет'
};

const geo = {
    rgid: 741964,
    locative: 'в Москве и МО'
};

describe('DeveloperCardBreadcrumbs', () => {
    it('рисует хлебные крошки с текущим застройщиком и регионом + ссылкой на листинг застройщиков', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardBreadcrumbs developer={developer} geo={geo} />
            </AppProvider>,
            { viewport: { width: 400, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
