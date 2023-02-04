import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingCardFirstScreen, INewbuildingCardFirstScreenProps } from '../index';

import { siteCard } from '../../__tests__/mocks';

describe('NewbuildingCardV2FirstScreen', () => {
    it('Базовая отрисовка', async () => {
        const props = ({
            card: siteCard,
            link: () => 'link',
        } as unknown) as INewbuildingCardFirstScreenProps;

        await render(
            <AppProvider context={{ link: () => 'link' }}>
                <NewbuildingCardFirstScreen {...props} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 700 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
