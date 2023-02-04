import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider } from 'view/react/libs/test-helpers';

import { ProfileCardContainer } from '../container';

import mocks from './mocks';

advanceTo(new Date('2020-07-23 12:00'));

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <ProfileCardContainer profile={store.cards.profile} />
    </AppProvider>
);

const options = { viewport: { width: 1100, height: 1300 } };
const optionsWide = { viewport: { width: 1400, height: 1300 } };

describe.skip('ProfileCard', () => {
    describe('Лого', () => {
        describe('Агент', () => {
            it('correct draw agent profile, logo 200x50', async() => {
                const store = merge({}, mocks.defaultAgent, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 200, height: 50 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('correct draw agent profile, logo 50x200', async() => {
                const store = merge({}, mocks.defaultAgent, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 50, height: 200 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('correct draw agent profile, logo 200x200', async() => {
                const store = merge({}, mocks.defaultAgent, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 200, height: 200 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });

        describe('Агенство', () => {
            it('correct draw agency profile, logo 200x50', async() => {
                const store = merge({}, mocks.defaultAgency, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 200, height: 50 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('correct draw agency profile, logo 50x200', async() => {
                const store = merge({}, mocks.defaultAgency, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 50, height: 200 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('correct draw agency profile, logo 200x200', async() => {
                const store = merge({}, mocks.defaultAgency, {
                    cards: {
                        profile: {
                            logo: generateImageAliases({ width: 200, height: 200 })
                        }
                    }
                });

                await render(<Component store={store} />,
                    options
                );

                await customPage.waitForYmapsPins();

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    it('correct draw agent profile 1400', async() => {
        await render(<Component store={mocks.defaultAgent} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw agent profile 1100', async() => {
        await render(<Component store={mocks.defaultAgent} />,
            options
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw agency profile 1400', async() => {
        await render(<Component store={mocks.defaultAgency} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw agency profile 1100', async() => {
        await render(<Component store={mocks.defaultAgency} />,
            options
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw features without offers in user geo', async() => {
        await render(<Component store={mocks.withoutOffersInUserRegion} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw features without offers', async() => {
        await render(<Component store={mocks.withoutOffers} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw features when total offers count equal user geo offers count', async() => {
        await render(<Component store={mocks.userHasSameGeoAsAProfile} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw profile with fetched redirect phones', async() => {
        await render(<Component store={mocks.withRedirectPhones} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw profile with 2 redirect phones', async() => {
        await render(<Component store={mocks.withTwoRedirectPhones} />,
            optionsWide
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
