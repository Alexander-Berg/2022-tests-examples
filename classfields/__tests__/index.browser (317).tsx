import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteSnippetContacts } from '../';

const WrappedComponent = (props: { item: ISiteSnippetType }) => (
    <AppProvider>
        <SiteSnippetContacts item={props.item} salesDepartmentParams={{ objectId: 1, objectType: 'newbuilding' }} />
    </AppProvider>
);

describe('SiteSnippetContacts', () => {
    it('Рендерится с кнопками телефона и чата', async () => {
        await render(
            <WrappedComponent
                item={({ developers: [{ hasChat: true }], phone: { phoneHash: '123' } } as unknown) as ISiteSnippetType}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без кнопки чата', async () => {
        await render(
            <WrappedComponent
                item={({ developers: [], phone: { phoneHash: '123' } } as unknown) as ISiteSnippetType}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с кнопкой чата, но без телефона', async () => {
        await render(
            <WrappedComponent
                item={({ developers: [{ hasChat: true }], location: {} } as unknown) as ISiteSnippetType}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
