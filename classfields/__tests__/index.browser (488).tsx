import React from 'react';
import { render } from 'jest-puppeteer-react';
import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { salesDepartmentReducer } from 'realty-core/view/react/modules/sites/redux/reducer';

import SnippetContacts, { ISnippetContactsProps, SnippetPlacement } from '../';

const commonProps = {
    phone: {
        phoneWithMask: '+7 999 888 ×× ××',
        phoneHash: '',
    },
    siteId: '123',
    redirectParams: { objectId: 189856, objectType: 'newbuilding' },
    backCallTrafficInfo: {} as TrafficSourceInfo,
    withoutMediaQueries: true,
    fullName: 'qq',
};

const Gate = {
    get: (action: string, { objectId }: { objectId: number }) => {
        switch (action) {
            case 'contacts.get':
                return Promise.resolve({
                    id: 1,
                    name: 'ГК «А101»',
                    logo: objectId === 1 ? undefined : generateImageUrl({ width: 75, height: 75 }),
                    phones: ['+79998887765'],
                });
        }
    },
};

function Component(props: Partial<ISnippetContactsProps>) {
    return (
        <AppProvider Gate={Gate} rootReducer={createRootReducer({ salesDepartment: salesDepartmentReducer })}>
            <SnippetContacts {...commonProps} {...props} />
        </AppProvider>
    );
}

describe('SnippetContacts', () => {
    it('рендерится по дефолту', async () => {
        await render(
            <Component
                {...commonProps}
                withoutMediaQueries={false}
                redirectParams={{ objectId: 1, objectType: 'newbuilding' }}
            />,
            {
                viewport: { width: 350, height: 300 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится для блока спецпроектов', async () => {
        await render(
            <Component
                {...commonProps}
                snippetPlacement={SnippetPlacement.SPECIAL_PROJECT_PINNED_SITES}
                withoutMediaQueries={false}
                redirectParams={{ objectId: 1, objectType: 'newbuilding' }}
            />,
            {
                viewport: { width: 350, height: 300 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('c полным salesDepartment', async () => {
        await render(<Component {...commonProps} withBilling />, {
            viewport: { width: 400, height: 300 },
        });

        await page.click(`[data-test="SnippetContactsPhoneButton"]`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('c минимальным salesDepartment', async () => {
        await render(
            <Component
                {...commonProps}
                withoutMediaQueries={false}
                redirectParams={{ objectId: 1, objectType: 'newbuilding' }}
            />,
            {
                viewport: { width: 350, height: 300 },
            }
        );

        await page.click(`[data-test="SnippetContactsPhoneButton"]`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('без рекламы', async () => {
        await render(<Component {...commonProps} withoutAd />, {
            viewport: { width: 400, height: 300 },
        });

        await page.click(`[data-test="SnippetContactsPhoneButton"]`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
