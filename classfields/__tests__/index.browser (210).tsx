import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import {
    IOfferData,
    OfferBalconyType,
    OfferBathroomType,
    OfferWindowViewType,
} from 'realty-core/view/react/common/types/egrnPaidReport';
import spoilerStyles from 'realty-core/view/react/common/components/Spoiler/styles.module.css';

import { EGRNPaidReportOfferDataBlock } from '../';

const OPTIONS = {
    viewport: {
        width: 350,
        height: 800,
    },
};

const spoilerExpanderSelector = `.${spoilerStyles.button}`;

const offerData: Partial<IOfferData> = {
    area: 1,
    roomsNumber: 1,
    numberOfFloors: '1',
    ceilingHeight: 1,
    balconyType: OfferBalconyType.BALCONY_TYPE_BALCONY,
    kitchenArea: 5,
    bathroomType: OfferBathroomType.BATHROOM_TYPE_MATCHED,
    windowView: OfferWindowViewType.WINDOW_VIEW_STREET,
    floor: 'чердак',
};

describe('EGRNPaidReportOfferDataBlock', () => {
    it('рендерится с 8 особенностями, 2 из которых скрыты', async () => {
        await render(
            <EGRNPaidReportOfferDataBlock offerData={offerData as IOfferData} offerLink="https://realty.yandex.ru" />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с 8 особенностями, 3 из которых скрыты, раскрываются при клике на спойлер', async () => {
        await render(
            <EGRNPaidReportOfferDataBlock offerData={offerData as IOfferData} offerLink="https://realty.yandex.ru" />,
            OPTIONS
        );

        await page.click(spoilerExpanderSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с 7 особенностями без спойлера', async () => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { kitchenArea, ...trimmedOfferData } = offerData;

        await render(
            <EGRNPaidReportOfferDataBlock
                offerData={trimmedOfferData as IOfferData}
                offerLink="https://realty.yandex.ru"
            />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
