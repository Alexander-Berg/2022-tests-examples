import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IconId } from 'realty-core/view/react/common/components/FeatureIcon/types';

import { OfferCardFeature } from '../index';

import { link, offer } from './mocks';

describe('OfferCardFeature', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <OfferCardFeature
                link={link}
                linkParams={{ paramName: '', paramValue: '' }}
                iconId={IconId.INTERNET}
                text="интернет"
                rgid={1}
                offer={offer}
            />,
            {
                viewport: { width: 200, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка со ссылкой', async () => {
        await render(
            <OfferCardFeature
                link={link}
                linkParams={{ paramName: 'renovation', paramValue: 'EURO' }}
                iconId={IconId.DECORATION}
                text="Отделка - евроремонт"
                rgid={741964}
                offer={offer}
            />,
            {
                viewport: { width: 200, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
