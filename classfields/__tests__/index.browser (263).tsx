/* eslint-disable jest/expect-expect */
import React from 'react';
import { render as _render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { SerpListItem } from 'view/components/Serp/SerpList';

import { OfferSerpSnippetContainer } from '../container';
import { IOfferSerpSnippetBaseProps } from '../types';

import {
    baseOffer,
    commercialOfferLand,
    commercialOfferManufacturing,
    commercialOfferOffice,
    commercialOfferRentOffice,
    garageOffer,
    houseOffer,
    lotOffer,
    lotOfferInKP,
    offerWithDistanceToMRR,
    offerWithLongAddressName,
    offerWithoutMetro,
    offerWithPerMeterPrice,
    offerWithPriceDown,
    offerWithPriceUp,
    offerWithSeveralBages,
    offerWithStepToMetro,
    offerFarFromMetro,
    parkingPlaceOffer,
    boxOffer,
    perDayRentOffer,
    rentOffer,
    roomOffer,
    townhouseOffer,
    parthouseOffer,
    duplexOffer,
    premiumOffer,
    profitOffer,
    offerWithChat,
    offerUserNote,
    getNonCapitalOfferProps,
    offerWith3dTour,
} from './stubs/offers';
import { getStore, IGetStoreArg } from './stubs/store';
import { createStatsSelector } from './stubs/selectors';

const mobileViewports = [
    { width: 320, height: 700 },
    { width: 375, height: 700 },
] as const;

const rootReducer = createRootReducer({});

const render = async (component: React.ReactElement, fn = noop) => {
    for (const viewport of mobileViewports) {
        await _render(component, { viewport });

        await fn();

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const baseProps: IOfferSerpSnippetBaseProps = {
    Component: SerpListItem,
    item: baseOffer,
    onStatsAction: noop,
    onOfferShow: noop,
    onOfferClick: noop,
    createStatsSelector: () => () => ({}),
    isFavorite: false,
    refSetter: noop,
    publishedOffersCount: 0,
    linkProvider: noop,
    serpType: 'search',
    usePreview: true,
    onLinkClick: noop,
    pageType: 'search',
    onPhoneClick: noop,
    addToFavorites: noop,
    removeFromFavorites: noop,
    onToggleFavorites: noop,
    onVisibilityChange: noop,
    onOpenOfferChat: () => Promise.resolve(),
    onStrikeout: noop,
    eventPlace: 'other',
};

interface IComponentProps extends AnyObject {
    store?: IGetStoreArg;
}

const Component: React.FunctionComponent<IComponentProps> = ({ store, ...props }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={getStore(store)}>
            <OfferSerpSnippetContainer {...props} />
        </AppProvider>
    );
};

describe('OfferSerpSnippet', () => {
    it('продажа квартиры', async () => {
        await render(<Component {...baseProps} />);
    });

    it('продажа квартиры в регионе', async () => {
        await render(<Component {...getNonCapitalOfferProps(baseProps)} />);
    });

    it('с изменением цены вверх', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithPriceUp,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('с изменением цены вниз', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithPriceDown,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('расстояние до МКАДа', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithDistanceToMRR,
        };
        const store = {
            geo: {
                isInMO: true,
            },
        };

        await render(<Component store={store} {...serpItemProps} />);
    });

    it('несколько бейджей', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithSeveralBages,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('с 3д туром', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWith3dTour,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('с длинным адресом', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithLongAddressName,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('аренда', async () => {
        const serpItemProps = {
            ...baseProps,
            item: rentOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('с ценной за квадратный метр', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithPerMeterPrice,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('добавлено в избранное', async () => {
        const serpItemProps = {
            ...baseProps,
            isFavorite: true,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('добавление в избранное', async () => {
        for (const viewport of mobileViewports) {
            const serpItemProps = {
                ...baseProps,
                createStatsSelector,
            };

            await _render(<Component {...serpItemProps} />, { viewport });

            await noop();

            expect(
                await takeScreenshot({
                    fullPage: true,
                })
            ).toMatchImageSnapshot();

            await page.click('.ItemAddToFavorite');

            await noop();

            expect(
                await takeScreenshot({
                    fullPage: true,
                })
            ).toMatchImageSnapshot();
        }
    });

    it('посуточная аренда', async () => {
        const serpItemProps = {
            ...baseProps,
            item: perDayRentOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('без метро и расстояния до МКАД', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithoutMetro,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('время до метро пешком', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithStepToMetro,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('СПб-Мск далеко от метро', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerFarFromMetro,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('коммерческая недвижимость: офис', async () => {
        const serpItemProps = {
            ...baseProps,
            item: commercialOfferOffice,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('коммерческая недвижимость: офис в регионе', async () => {
        await render(
            <Component
                {...getNonCapitalOfferProps({
                    ...baseProps,
                    item: commercialOfferOffice,
                })}
            />
        );
    });

    it('коммерческая недвижимость: участок', async () => {
        const serpItemProps = {
            ...baseProps,
            item: commercialOfferLand,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('коммерческая недвижимость: производственное помещение', async () => {
        const serpItemProps = {
            ...baseProps,
            item: commercialOfferManufacturing,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа комнаты', async () => {
        const serpItemProps = {
            ...baseProps,
            item: roomOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа комнаты в регионе', async () => {
        await render(
            <Component
                {...getNonCapitalOfferProps({
                    ...baseProps,
                    item: roomOffer,
                })}
            />
        );
    });

    it('продажа дома', async () => {
        const serpItemProps = {
            ...baseProps,
            item: houseOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа дома в регионе', async () => {
        await render(
            <Component
                {...getNonCapitalOfferProps({
                    ...baseProps,
                    item: houseOffer,
                })}
            />
        );
    });

    it('продажа таунхауса', async () => {
        const serpItemProps = {
            ...baseProps,
            item: townhouseOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа части дома', async () => {
        const serpItemProps = {
            ...baseProps,
            item: parthouseOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа дуплекса', async () => {
        const serpItemProps = {
            ...baseProps,
            item: duplexOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа участка', async () => {
        const serpItemProps = {
            ...baseProps,
            item: lotOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа участка в регионе', async () => {
        await render(
            <Component
                {...getNonCapitalOfferProps({
                    ...baseProps,
                    item: lotOffer,
                })}
            />
        );
    });

    it('продажа участка в КП', async () => {
        const serpItemProps = {
            ...baseProps,
            item: lotOfferInKP,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа гаража', async () => {
        const serpItemProps = {
            ...baseProps,
            item: garageOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа гаража в регионе', async () => {
        await render(
            <Component
                {...getNonCapitalOfferProps({
                    ...baseProps,
                    item: garageOffer,
                })}
            />
        );
    });

    it('продажа машиноместа', async () => {
        const serpItemProps = {
            ...baseProps,
            item: parkingPlaceOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('продажа бокса', async () => {
        const serpItemProps = {
            ...baseProps,
            item: boxOffer,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('аренда офиса', async () => {
        const serpItemProps = {
            ...baseProps,
            item: commercialOfferRentOffice,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('отображение премиум сниппета', async () => {
        const serpItemProps = {
            ...baseProps,
            item: premiumOffer,
            isPremium: true,
        };

        await render(<Component {...serpItemProps} />);
    });

    it('новое положение "Хорошая цена"', async () => {
        const serpItemProps = {
            ...baseProps,
            item: profitOffer,
        };

        const ComponentWithFavButtonExp: React.FunctionComponent<IComponentProps> = ({ store, ...props }) => {
            return (
                <AppProvider rootReducer={rootReducer} initialState={getStore(store)}>
                    <OfferSerpSnippetContainer {...props} />
                </AppProvider>
            );
        };

        await render(<ComponentWithFavButtonExp {...serpItemProps} />);
    });

    it('с кнопкой чата', async () => {
        const serpItemProps = {
            ...baseProps,
            item: offerWithChat,
        };

        const ComponentWithChatButtonExp: React.FunctionComponent<IComponentProps> = ({ store, ...props }) => {
            return (
                <AppProvider rootReducer={rootReducer} initialState={getStore(store)}>
                    <OfferSerpSnippetContainer {...props} />
                </AppProvider>
            );
        };

        await render(<ComponentWithChatButtonExp {...serpItemProps} />);
    });

    it('с заметкой', async () => {
        await render(<Component {...baseProps} item={offerUserNote} />);
    });

    it('открывается панель с действиями', async () => {
        await render(<Component {...baseProps} />, async () => {
            await page.click('[data-test="actions-button"]');
        });
    });

    it('открывает окно поделиться"', async () => {
        await render(<Component {...baseProps} />, async () => {
            await page.click('[data-test="actions-button"]');
            await page.click('[data-test="action_SHARE"]');
        });
    });

    it('открывает окно пожаловаться"', async () => {
        await render(<Component {...baseProps} />, async () => {
            await page.click('[data-test="actions-button"]');
            await page.click('[data-test="action_COMPLAIN"]');
        });
    });

    it('открывает окно "Скрыть"', async () => {
        await render(<Component {...baseProps} />, async () => {
            await page.click('[data-test="actions-button"]');
            await page.click('[data-test="action_HIDE"]');
        });
    });
});
