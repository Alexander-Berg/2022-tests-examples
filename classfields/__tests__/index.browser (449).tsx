import React from 'react';
import merge from 'lodash/merge';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';
import { ISiteCard } from 'realty-core/types/siteCard';

import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';

import Link from 'vertis-react/components/Link';

import { NewbuildingSeoPageLayout } from '../';

import {
    card as defaultCard,
    user,
    defaultState,
    stateWithSalesDepartment,
    stateWithFavorites,
    backCallSuccessState,
} from './mocks';

const cardWithoutChat = merge({}, defaultCard, { developer: { hasChat: false } });
const cardWithSpecialDeveloper = merge({}, defaultCard, { hideSimilarSites: true });

const render = async ({
    state = defaultState,
    width,
    card = defaultCard,
}: {
    state?: AnyObject;
    width: number;
    card?: ISiteCard;
}) => {
    await _render(
        <AppProvider initialState={state}>
            <NewbuildingSeoPageLayout
                title="Ипотека в новых Ватутинках мкр. «Центральный»"
                cardTitle={
                    <>
                        Ипотека в&nbsp;
                        <Link size="xxl" url="/">
                            новых Ватутинках мкр. «Центральный»
                        </Link>
                    </>
                }
                user={user}
                queryId=""
                pageName="newbuilding_mortgage"
                card={card}
                geo={{} as IGeoStore}
            >
                контент
            </NewbuildingSeoPageLayout>
        </AppProvider>,
        { viewport: { width, height: 2500 } }
    );
    await page.addStyleTag({ content: 'body{padding: 0}' });
};

const screens = [[1100], [1400]];

describe.each(screens)('NewbuildingSeoPageAsideInfoBlock %s', (width) => {
    it('рендерится корректно', async () => {
        await render({ width });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без блока похожих для спецпроектного застройщика', async () => {
        await render({ width, card: cardWithSpecialDeveloper });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('телефон показан ', async () => {
        await render({ state: stateWithSalesDepartment, width });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('в избранном', async () => {
        await render({ state: stateWithFavorites, width });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без кнопки чатов', async () => {
        await render({ width, card: cardWithoutChat });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('заказан обратный звонок', async () => {
        await render({ width, state: backCallSuccessState });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('заказан обратный звонок без кнопки чатов', async () => {
        await render({ width, state: backCallSuccessState, card: cardWithoutChat });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
