import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ISiteSpecialProjectSecondPackageData } from 'realty-core/types/siteSpecialProject';
import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';

import { FiltersBannerSpecialProject } from '../';

const commonProps = {
    geo: {} as IGeoStore,
    type: 'type',
    imageUrl: '/',
};

const banners = [
    {
        title: 'Самолет',
        props: {
            ...commonProps,
            specialProject: {
                developerId: 102320,
            } as ISiteSpecialProjectSecondPackageData,
            bannerText: 'Скидка 10% на квартиры и апартаменты',
        },
    },
    {
        title: 'Унистрой',
        props: {
            ...commonProps,
            specialProject: {
                developerId: 463788,
            } as ISiteSpecialProjectSecondPackageData,
            bannerText: 'Качественное жилье для любимых клиентов',
        },
    },
];

describe('FiltersBannerSpecialProject', function () {
    banners.map(({ title, props }) => {
        it(`рисует баннер (${title})`, async () => {
            await render(
                <AppProvider>
                    <FiltersBannerSpecialProject {...props} />
                </AppProvider>,
                { viewport: { width: 1500, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
