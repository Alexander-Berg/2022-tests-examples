import React, { ReactElement } from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IProps as OfferWarningProps } from '../index';

import { AgencyOfferWarnings } from '../index';

import { withDiscriminationMock } from './mocks';

const desktopViewports = [
    { width: 1400, height: 200 },
    { width: 1000, height: 200 },
] as const;

const render = async (component: React.ClassicElement<OfferWarningProps>) => {
    for (const viewport of desktopViewports) {
        await _render(component as ReactElement, { viewport });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    }
};

describe('AgencyOfferWarnings', () => {
    it('[warning] Дискриминация', async () => {
        await render(<AgencyOfferWarnings {...withDiscriminationMock} />);
    });
});
