import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard, IPredictions, OfferType } from 'realty-core/types/offerCard';
import { IAlfaBankMortgageStore } from 'realty-core/view/react/modules/alfa-bank-mortgage/redux/reducer';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardSummaryTagsContainer } from '../container';

import {
    alfaBankMortgageLoaded,
    newbuildingOffer,
    rentOffer,
    secondaryOffer,
    disabledNewBuildingOffer,
    disabledRentOffer,
    disabledSecondaryOffer,
    goodPriceNewbuildingOffer,
    goodPriceSecondaryOffer,
    severalMainBadgesNewbuildingOffer,
    severalMainBadgesRentOffer,
    severalMainBadgesSecondaryOffer,
    yaRentOffer,
    alfaBankMortgageInitial,
    apartmentsOffer,
} from './mocks';

const Component: React.FC<{
    offer: IOfferCard;
    alfaBankMortgage?: IAlfaBankMortgageStore;
}> = ({ offer, alfaBankMortgage = alfaBankMortgageInitial }) => (
    <AppProvider initialState={{ alfaBankMortgage }}>
        <OfferCardSummaryTagsContainer offer={offer} disableMortgage />
    </AppProvider>
);

describe('OfferCardSummaryTags', function () {
    describe('новостройка', function () {
        it('базовая отрисовка', async () => {
            await render(<Component offer={newbuildingOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('отрисовка c бейджем про стоимость ипотеки', async () => {
            await render(<Component offer={newbuildingOffer} alfaBankMortgage={alfaBankMortgageLoaded} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неактивное объявление', async () => {
            await render(<Component offer={disabledNewBuildingOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('хорошая цена', async () => {
            await render(<Component offer={goodPriceNewbuildingOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('в случае с несколькими акцентными бейджами, цветом выделяется только первый', async () => {
            await render(
                <Component offer={severalMainBadgesNewbuildingOffer} alfaBankMortgage={alfaBankMortgageLoaded} />,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('апартаменты', async () => {
            await render(
                <Component offer={{ ...newbuildingOffer, house: { ...newbuildingOffer.house, apartments: true } }} />,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('вторичная продажа', () => {
        it('базовая отрисовка', async () => {
            await render(<Component offer={secondaryOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('отрисовка c бейджем про стоимость ипотеки', async () => {
            await render(<Component offer={secondaryOffer} alfaBankMortgage={alfaBankMortgageLoaded} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неактивное объявление', async () => {
            await render(<Component offer={disabledSecondaryOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('хорошая цена', async () => {
            await render(<Component offer={goodPriceSecondaryOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('в случае с несколькими акцентными бейджами, цветом выделяется только первый', async () => {
            await render(
                <Component offer={severalMainBadgesSecondaryOffer} alfaBankMortgage={alfaBankMortgageLoaded} />,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('аренда', function () {
        it('базовая отрисовка', async () => {
            await render(<Component offer={rentOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('отрисовка Яндекс.Аренды', async () => {
            await render(<Component offer={yaRentOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неактивное объявление', async () => {
            await render(<Component offer={disabledRentOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('в случае с несколькими акцентными бейджами, цветом выделяется только первый', async () => {
            await render(<Component offer={severalMainBadgesRentOffer} />, {
                viewport: { width: 400, height: 170 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Бейдж от Яндекс.Аренды должен быть перед хорошей ценой', async () => {
            await render(
                <Component
                    offer={{
                        ...yaRentOffer,
                        predictions: { predictedPriceAdvice: { summary: 'LOW' } } as IPredictions,
                    }}
                />,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Апартаменты', () => {
        it('отрисовываются у оффера продажи', async () => {
            await render(<Component offer={apartmentsOffer} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('не отрисовываются у оффера аренды', async () => {
            await render(<Component offer={{ ...apartmentsOffer, offerType: OfferType.RENT }} />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
