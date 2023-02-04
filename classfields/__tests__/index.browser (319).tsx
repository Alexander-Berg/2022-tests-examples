import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { LinkProp } from 'realty-core/view/react/common/enhancers/withLink';

import { SiteSnippetPhoneButton, ISiteSnippetPhoneProps } from '../';

const WrappedComponent = (props: ISiteSnippetPhoneProps) => (
    <AppProvider>
        <div style={{ display: 'flex', flexDirection: 'column' }}>
            <SiteSnippetPhoneButton {...props} />
        </div>
    </AppProvider>
);

describe('SiteSnippetPhoneButton', () => {
    it('Рендерится', async () => {
        await render(
            <WrappedComponent
                view="yellow"
                size="xl"
                link={noop as LinkProp}
                item={({ phone: { phoneHash: '123' } } as unknown) as ISiteSnippetType}
                getSalesDepartment={noop as ISiteSnippetPhoneProps['getSalesDepartment']}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится когда телефона нет', async () => {
        await render(
            <WrappedComponent
                view="yellow"
                size="xl"
                link={noop as LinkProp}
                item={({ location: {} } as unknown) as ISiteSnippetType}
                getSalesDepartment={noop as ISiteSnippetPhoneProps['getSalesDepartment']}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с иконкой рекламы', async () => {
        await render(
            <WrappedComponent
                view="yellow"
                size="xl"
                link={noop as LinkProp}
                item={({ phone: { phoneHash: '123' } } as unknown) as ISiteSnippetType}
                salesDepartment={{ id: 1, name: '1' }}
                getSalesDepartment={noop as ISiteSnippetPhoneProps['getSalesDepartment']}
            />,
            {
                viewport: { width: 360, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
