import React from 'react';
import noop from 'lodash/noop';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { getPhoneStats } from 'realty-core/view/react/modules/sites/stats';
import { IUserStore } from 'realty-core/view/react/common/reducers/user';
import { ISiteCardBaseType } from 'realty-core/types/siteCard';

import { LinkProp } from 'realty-core/view/react/common/enhancers/withLink';

import { YandexMapsSiteWidget, IYandexMapsSiteWidgetProps } from '../index';

import { card, genPlan, sitePlans, similar } from './mocks';

const props = ({
    card,
    genplan: genPlan,
    sitePlans,
    getSitePhoneShowStats: () =>
        getPhoneStats({
            placement: '',
            eventPlace: '',
            queryId: '',
            card: (card as unknown) as ISiteCardBaseType,
            link: noop as LinkProp,
            user: {} as IUserStore,
        }),
} as unknown) as IYandexMapsSiteWidgetProps;

describe('YandexMapsSiteWidget', () => {
    it('рендерится корректнто', async () => {
        await render(
            <AppProvider initialState={{ genPlan, similar }}>
                <YandexMapsSiteWidget {...props} />
            </AppProvider>,
            {
                viewport: { width: 1100, height: 1000 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
