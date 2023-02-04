import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { salesDepartmentReducer } from 'realty-core/view/react/modules/sites/redux/reducer';

import { CardPhoneNew } from '../';
import styles from '../styles.module.css';

const initialState = {
    salesDepartment: {},
};

const salesDepartment = {
    id: 918322,
    name: 'ПИК',
    phones: ['+74959999999'],
    isRedirectPhones: true,
    weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '00:00', close: '23:59' }] }],
    statParams: 'stats',
    encryptedDump: 'dump',
};
const developers = [
    {
        id: 918322,
        name: 'ПИК',
        encryptedPhones: [
            {
                phoneWithMask: '+7 495 120 ×× ××',
                phoneHash: 'KzcF0OHTUJxMLjANxMPzUR4',
            },
        ],
    },
];
const Gate = {
    get: () => Promise.resolve(salesDepartment),
};

const rootReducer = createRootReducer({ salesDepartment: salesDepartmentReducer });

describe('CardPhoneNew', () => {
    it('Рисует полученный salesDepartment при нажатии на кнопку', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardPhoneNew
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    redirectParams={{
                        objectId: 918322,
                        objectType: 'newbuilding',
                    }}
                />
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует попап при клике на значок под кнопкой', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <div style={{ paddingTop: '200px' }}>
                    <CardPhoneNew
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        redirectParams={{
                            objectId: 918322,
                            objectType: 'newbuilding',
                        }}
                    />
                </div>
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        await page.click('.Button');
        await page.click(`.${styles.hint}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует телефоны застройщиков в отдельном режиме', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardPhoneNew developers={developers} withDevModal />
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
