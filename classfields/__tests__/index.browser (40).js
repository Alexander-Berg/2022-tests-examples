import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import TopSheetModalContainer from '../container';

const defaultStore = {
    popups: {
        coronavirusSales: { visible: false },
        raisingScheduler: { visible: true }
    },
    page: {
        params: {}
    },
    offersNew: {},
    geo: { isMsk: true },
    user: { isAuth: true, userType: 'AGENT', isJuridical: false },
    vosUserData: {
        extendedUserType: 'AGENT'
    },
    ownOffersStatistics: {
        published: 1
    }
};

const Component = ({ store, ...props }) => (
    <AppProvider initialState={store}>
        <TopSheetModalContainer {...props} />
    </AppProvider>
);

describe('TopSheetModalContainer coronavirusSales variant', () => {
    it.skip('correct draw for naturals 1000', async() => {
        await render(<Component store={defaultStore} variant='coronavirusSales' />, {
            viewport: { width: 1000, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw for naturals 1300', async() => {
        await render(<Component store={defaultStore} variant='coronavirusSales' />, {
            viewport: { width: 1300, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw for juridical 1000', async() => {
        const store = {
            ...defaultStore,
            user: {
                ...defaultStore.user,
                isJuridical: true
            }
        };

        await render(<Component store={store} variant='coronavirusSales' />, {
            viewport: { width: 1000, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw for juridical 1300', async() => {
        const store = {
            ...defaultStore,
            user: {
                ...defaultStore.user,
                isJuridical: true
            }
        };

        await render(<Component store={store} variant='coronavirusSales' />, {
            viewport: { width: 1300, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw for juridical in region 1000', async() => {
        const store = {
            ...defaultStore,
            user: {
                ...defaultStore.user,
                isJuridical: true
            },
            geo: {}
        };

        await render(<Component store={store} variant='coronavirusSales' />, {
            viewport: { width: 1000, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw for juridical in region 1300', async() => {
        const store = {
            ...defaultStore,
            user: {
                ...defaultStore.user,
                isJuridical: true
            },
            geo: {}
        };

        await render(<Component store={store} variant='coronavirusSales' />, {
            viewport: { width: 1300, height: 500 }
        });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

describe('TopSheetModalContainer raisingScheduler variant', () => {
    it('draw correctly on screen 1000', async() => {
        advanceTo(new Date('2020-07-23 12:00'));

        await render(<Component store={defaultStore} variant='raisingScheduler' />, {
            viewport: { width: 1000, height: 500 }
        });
        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('draw correctly on screen 1300', async() => {
        advanceTo(new Date('2020-07-23 12:00'));

        await render(<Component store={defaultStore} variant='raisingScheduler' />, {
            viewport: { width: 1300, height: 500 }
        });
        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
