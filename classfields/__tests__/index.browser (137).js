import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import OfficeSnippetPin from '../';

import { officeDataWithoutContacts, officeDataWithoutLogo, officeDataWithoutWorkTime } from './mocks';

const renderWithTZ = (node, options) => render(node, {
    ...options,
    before: async page => {
        await page.emulateTimezone('Europe/Moscow');
    }
});

describe('OfficeSnippetPin', () => {
    advanceTo(new Date(Date.UTC(2020, 4, 27, 12)));

    it('рисует тултип ОП с картинкой, бесплатной парковкой, именем', async() => {
        await renderWithTZ(<OfficeSnippetPin item={officeDataWithoutContacts} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип ОП без картинки, с адресом и контактами', async() => {
        await renderWithTZ(<OfficeSnippetPin item={officeDataWithoutLogo} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип ОП без режима работы', async() => {
        await render(<OfficeSnippetPin item={officeDataWithoutWorkTime} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип ОП, который сейчас работает', async() => {
        await renderWithTZ(<OfficeSnippetPin item={officeDataWithoutLogo} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку с телефоном другого цвета при наведении', async() => {
        await renderWithTZ(<OfficeSnippetPin item={officeDataWithoutLogo} />,
            { viewport: { width: 300, height: 300 } }
        );

        await page.hover('.OfficeSnippetPinPhoneButton');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует кнопку с телефоном после клика на нее', async() => {
        await renderWithTZ(<OfficeSnippetPin item={officeDataWithoutLogo} />,
            { viewport: { width: 300, height: 300 } }
        );

        await page.click('.OfficeSnippetPinPhoneButton');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
