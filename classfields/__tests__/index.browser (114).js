import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardFinance from '../';

import {
    developerWithoutDocuments,
    developerWithOneDocument,
    developerWithTwoDocument,
    developerWithFiveDocument
} from './mocks';

describe('DeveloperCardFinance', () => {
    it('рисует полную отчетность без документов', async() => {
        await render(<DeveloperCardFinance developer={developerWithoutDocuments} />,
            { viewport: { width: 900, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует частичную отчетность и один документ', async() => {
        await render(<DeveloperCardFinance developer={developerWithOneDocument} />,
            { viewport: { width: 900, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ссылку на документ другого цвета при наведении', async() => {
        await render(<DeveloperCardFinance developer={developerWithOneDocument} />,
            { viewport: { width: 900, height: 300 } }
        );

        await page.hover('.DeveloperCardFinance__documentName');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует полную отчетность и два документа', async() => {
        await render(<DeveloperCardFinance developer={developerWithTwoDocument} />,
            { viewport: { width: 900, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует только два документа и кнопку для раскрытия остальных документов', async() => {
        await render(<DeveloperCardFinance developer={developerWithFiveDocument} />,
            { viewport: { width: 900, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку для раскрытия остальных документов другого цвета при наведении', async() => {
        await render(<DeveloperCardFinance developer={developerWithFiveDocument} />,
            { viewport: { width: 900, height: 400 } }
        );

        await page.hover('.DeveloperCardFinance__documentsMore');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует все документы после клика на кнопку \'Показать все документы\'', async() => {
        await render(<DeveloperCardFinance developer={developerWithFiveDocument} />,
            { viewport: { width: 900, height: 600 } }
        );

        await page.click('.DeveloperCardFinance__documentsMore');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
