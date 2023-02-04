import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import spoilerStyles from 'realty-core/view/react/common/components/Spoiler/styles.module.css';

import { OwnerType } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportRightsBlock } from '../';

const OPTIONS = { viewport: { width: 350, height: 600 } };
const OPTIONS_COLLAPSED = { viewport: { width: 350, height: 300 } };

const spoilerButtonSelector = `.${spoilerStyles.button}`;

const currentRights = [
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванов А.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
        },
    },
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванова А.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
        },
    },
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванов К.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
        },
    },
];

const previousRights = [
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванов А.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
            endDate: '2015-10-10',
        },
    },
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванова А.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
            endDate: '2015-10-10',
        },
    },
    {
        owners: [
            {
                type: OwnerType.NATURAL_PERSON,
                name: 'Иванов К.',
            },
        ],
        registration: {
            idRecord: '12345',
            regNumber: '54321',
            type: '',
            name: '',
            regDate: '2010-10-10',
            shareText: '1/3',
            endDate: '2015-10-10',
        },
    },
];

describe('EGRNPaidReportRightsBlock', () => {
    it('рендерит пустой список текущих собственников', async () => {
        await render(<EGRNPaidReportRightsBlock isCurrent ownershipRights={[]} />, OPTIONS_COLLAPSED);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит свёрнутый список текущих собственников', async () => {
        await render(<EGRNPaidReportRightsBlock isCurrent ownershipRights={currentRights} />, OPTIONS_COLLAPSED);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит развёрнутый список текущих собственников', async () => {
        await render(<EGRNPaidReportRightsBlock isCurrent ownershipRights={currentRights} />, OPTIONS);

        await page.click(spoilerButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит пустой список предыдущих собственников', async () => {
        await render(<EGRNPaidReportRightsBlock isCurrent={false} ownershipRights={[]} />, OPTIONS_COLLAPSED);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит свёрнутый список предыдущих собственников', async () => {
        await render(
            <EGRNPaidReportRightsBlock isCurrent={false} ownershipRights={previousRights} />,
            OPTIONS_COLLAPSED
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит развёрнутый список предыдущих собственников', async () => {
        await render(<EGRNPaidReportRightsBlock isCurrent={false} ownershipRights={previousRights} />, OPTIONS);

        await page.click(spoilerButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
