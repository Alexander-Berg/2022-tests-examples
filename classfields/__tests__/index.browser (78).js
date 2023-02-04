import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardOffices from '../';
import itemStyles from '../DeveloperCardOfficeItem/styles.module.css';
import styles from '../styles.module.css';

import {
    officeDataWithoutContacts,
    officeDataWithoutLogo,
    officeDataWithoutWorkTime,
    commonDeveloperParams
} from './mocks';

const renderWithTZ = (node, options) => render(node, {
    ...options,
    before: async page => {
        await page.emulateTimezone('Europe/Moscow');
    }
});

describe('DeveloperCardOffices', () => {
    advanceTo(new Date(Date.UTC(2020, 4, 27, 12)));

    it('рисует спипет ОП с картинкой, бесплатной парковкой, именем', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [ officeDataWithoutContacts ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует спипет ОП без картинки, с адресом и контактами', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [ officeDataWithoutLogo ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует спипет ОП без режима работы', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [ officeDataWithoutWorkTime ]
        };

        await render(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку с телефоном после клика на нее', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [ officeDataWithoutLogo ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click(`.${itemStyles.phoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует два снипета ОП без кнопки раскрытия', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [ officeDataWithoutContacts, officeDataWithoutLogo ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 400, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует два снипета ОП c кнопкой раскрытия', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [
                officeDataWithoutContacts, officeDataWithoutLogo,
                officeDataWithoutWorkTime, officeDataWithoutContacts
            ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 400, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует все снипеты ОП после клика на кнопку раскрытия', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [
                officeDataWithoutContacts, officeDataWithoutLogo,
                officeDataWithoutWorkTime, officeDataWithoutContacts
            ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 400, height: 1400 } }
        );

        await page.click(`.${styles.showMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует два снипета ОП в ряд на широком экране', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [
                officeDataWithoutContacts, officeDataWithoutLogo,
                officeDataWithoutWorkTime, officeDataWithoutContacts
            ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 700, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует по два снипета ОП в ряд на широком экране после раскрытия', async() => {
        const developer = {
            ...commonDeveloperParams,
            offices: [
                officeDataWithoutContacts, officeDataWithoutLogo,
                officeDataWithoutWorkTime, officeDataWithoutContacts
            ]
        };

        await renderWithTZ(<DeveloperCardOffices developer={developer} />,
            { viewport: { width: 700, height: 900 } }
        );

        await page.click(`.${styles.showMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
