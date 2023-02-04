import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardFinance from '../';

import styles from '../styles.module.css';

import {
    developerWithoutDocuments,
    developerWithOneDocument,
    developerWithTwoDocument,
    developerWithFiveDocument
} from './mocks';

describe('DeveloperCardFinance', () => {
    it('рисует полную отчетность без документов', async() => {
        await render(<DeveloperCardFinance developer={developerWithoutDocuments} />,
            { viewport: { width: 400, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует частичную отчетность и один документ', async() => {
        await render(<DeveloperCardFinance developer={developerWithOneDocument} />,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует полную отчетность и два документа', async() => {
        await render(<DeveloperCardFinance developer={developerWithTwoDocument} />,
            { viewport: { width: 640, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует только два документа и кнопку для раскрытия остальных документов', async() => {
        await render(<DeveloperCardFinance developer={developerWithFiveDocument} />,
            { viewport: { width: 450, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует все документы после клика на кнопку \'Показать все документы\'', async() => {
        await render(<DeveloperCardFinance developer={developerWithFiveDocument} />,
            { viewport: { width: 400, height: 500 } }
        );

        await page.click(`.${styles.documentsMore}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
