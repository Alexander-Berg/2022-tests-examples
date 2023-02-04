import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';
import { ISiteCard } from 'realty-core/types/siteCard';

import { SiteCardSecondPackageHeader } from '../';

import {
    card,
    cardWithDifferentImages,
    menu,
    defaultState,
    stateWithSalesDepartment,
    stateWithFavorites,
} from './mocks';

interface IRenderComponentProps extends IAppProviderProps {
    currentCard?: ISiteCard;
    viewport?: { width: number; height: number };
}

const renderComponent = ({
    viewport = { width: 1400, height: 800 },
    currentCard = card,
    ...props
}: IRenderComponentProps) =>
    render(
        <AppProvider initialState={defaultState} {...props}>
            <SiteCardSecondPackageHeader
                // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                // @ts-ignore
                card={currentCard}
                menu={menu}
                placement=""
                redirectParams={{ objectId: currentCard.id, objectType: 'newbuilding' }}
            />
        </AppProvider>,
        { viewport }
    );

describe('SiteCardSecondPackageHeader', () => {
    it('рендерится корректно', async () => {
        await renderComponent({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('телефон показан ', async () => {
        await renderComponent({ initialState: stateWithSalesDepartment });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('в избранном', async () => {
        await renderComponent({ initialState: stateWithFavorites });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Показан плавающий блок', async () => {
        await renderComponent({ viewport: { height: 800, width: 1000 } });
        await page.addStyleTag({ content: 'body{height: 2300px}' });
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        await page.mouse.wheel({ deltaY: 1700 });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // realty-core/app/lib/sites/getSiteGalleryPhotos.js - VIEW_TYPES
    it('в счётчике учитываются только нужные изображения(viewType)', async () => {
        await renderComponent({ currentCard: cardWithDifferentImages });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
