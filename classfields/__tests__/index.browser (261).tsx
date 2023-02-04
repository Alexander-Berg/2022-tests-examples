import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferNote } from '../';
import styles from '../styles.module.css';

import { GateSuccess, GatePending, baseProps, offerWithOverflowingNote, baseOffer } from './mocks';

const render = (children: React.ReactElement, height = 100, Gate = GateSuccess) =>
    _render(<AppProvider Gate={Gate}>{children}</AppProvider>, {
        viewport: { height, width: 320 },
    });

describe('OfferNote', () => {
    it('рендерится корректно', async () => {
        await render(<OfferNote {...baseProps} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно с переполнением', async () => {
        await render(<OfferNote {...baseProps} item={offerWithOverflowingNote} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыта форма создания', async () => {
        await render(<OfferNote {...baseProps} item={baseOffer} modalVisible />, 400);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыта форма редактирования', async () => {
        await render(<OfferNote {...baseProps} modalVisible />, 400);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('форма отправляется', async () => {
        await render(<OfferNote {...baseProps} modalVisible />, 400, GatePending);

        await page.click(`.${styles.modalBtn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
