/* eslint-disable max-len */
import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ProfileTypes } from 'realty-core/types/profile/profileTypes';

import { IProfilesPageStore, profilesReducer } from 'view/react/deskpad/reducers/roots/profiles';
import profileFiltersStyles from 'view/react/deskpad/components/filters/ProfileSearchFilters/styles.module.css';
import geoSuggestStyles from 'view/react/deskpad/components/filters/ProfileSearchFilters/GeoSuggest/styles.module.css';

import profileSerpItemStyles from '../ProfileSerpItem/styles.module.css';
import { ProfileSerp } from '../index';

import {
    getStore,
    agencyProfiles,
    agentProfiles,
    redirectPhonesGateStub,
    geoSuggestGateStub,
    secondProfilesPageGateStub,
} from './stubs';

advanceTo(new Date('2020-10-23 04:20'));

const VIEWPORTS = [
    { width: 1000, height: 1500 },
    { width: 1400, height: 1500 },
] as const;

const selectors = {
    showPhoneButton: `.${profileSerpItemStyles.phoneButton}`,
    thirdPageButton: '.Pager2 .Radio:nth-of-type(3)',
    resetFiltersButton: `.${profileFiltersStyles.reset}`,
    showButton: `.${profileFiltersStyles.actions}:last-child`,
    filters: {
        selectType: `.${profileFiltersStyles.type}`,
        selectCategory: `.${profileFiltersStyles.category}`,
        clearSuggest: `.${profileFiltersStyles.geoSuggest} .TextInput__clear_visible`,
        suggestInput: `.${profileFiltersStyles.geoSuggest} .TextInput__control`,
        nameInput: `.${profileFiltersStyles.name} .TextInput__control`,
    },
    getGeoSuggestListItem: (n: number) => `.${geoSuggestStyles.suggestItem}:nth-of-type(${n})`,
    getSelectMenuItem: (n: number) => `.Popup_visible .Menu .Menu__item:nth-of-type(${n})`,
};

const MockGate = {
    get: (path: string) => {
        switch (path) {
            case 'geo.geosuggest': {
                return Promise.resolve(geoSuggestGateStub);
            }
            case 'profiles.get_redirect_phones': {
                return Promise.resolve(redirectPhonesGateStub);
            }
            case 'profiles.search_profile_count': {
                return Promise.resolve({
                    total: 11,
                });
            }
        }
    },
};

const Component: React.FunctionComponent<{
    store?: DeepPartial<IProfilesPageStore>;
    Gate?: Record<string, unknown>;
}> = ({ store, Gate }) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={profilesReducer as any} initialState={store} context={{}} Gate={Gate || MockGate}>
        <ProfileSerp />
    </AppProvider>
);

describe('ProfilesSerp', () => {
    describe('Базовое состояние', () => {
        VIEWPORTS.forEach((viewport) => {
            it(`карточки агенства ${viewport.width}px`, async () => {
                const component = (
                    <Component
                        store={getStore({ profiles: agencyProfiles, searchQuery: { userType: ProfileTypes.AGENCY } })}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.showPhoneButton);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`карточки агентов ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.get_redirect_phones': {
                                return Promise.reject();
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({ profiles: agentProfiles, searchQuery: { userType: ProfileTypes.AGENT } })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.showPhoneButton);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`карточки агентов ожидание получения телефона ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.get_redirect_phones': {
                                return new Promise(noop);
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({ profiles: agentProfiles, searchQuery: { userType: ProfileTypes.AGENT } })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });

                await page.click(selectors.showPhoneButton);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`пустая выдача ${viewport.width}px`, async () => {
                const component = (
                    <Component
                        store={getStore({
                            profiles: { order: [], ids: {} },
                            searchQuery: { userType: ProfileTypes.AGENCY },
                        })}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Фильтры', () => {
        VIEWPORTS.forEach((viewport) => {
            it(`cброс ${viewport.width}px`, async () => {
                allure.descriptionHtml(`"Купить" "Квартиру" "Москва и МО" <br/> -> Сброс`);

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.resetFiltersButton);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`cмена ${viewport.width}px`, async () => {
                allure.descriptionHtml(
                    `"Купить" "Квартиру" "Москва и МО" -><br/>"Снять" "Дом" "Новосибирская область" "Бонд" ->`
                );

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.filters.selectType);
                expect(await takeScreenshot()).toMatchImageSnapshot();
                await page.click(selectors.getSelectMenuItem(3));

                await page.click(selectors.filters.selectCategory);
                expect(await takeScreenshot()).toMatchImageSnapshot();
                await page.click(selectors.getSelectMenuItem(4));

                await page.click(selectors.filters.clearSuggest);
                expect(await takeScreenshot()).toMatchImageSnapshot();

                // await page.focus(selectors.filters.suggestInput); // TODO: разобраться с debounce в GeoSuggest
                // await page.type(selectors.filters.suggestInput, 'Новосибирская область');
                // await page.click(selectors.getGeoSuggestListItem(1));

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.type(selectors.filters.nameInput, 'Бонд');
                // @ts-expect-error blur
                await page.$eval(selectors.filters.nameInput, (e) => e.blur());

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`в зависимости типа недвижимости и выбранной сделки ${viewport.width}px`, async () => {
                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                    />
                );

                await render(component, { viewport });

                await page.click(selectors.filters.selectType);
                await page.click(selectors.getSelectMenuItem(2));
                await page.click(selectors.filters.selectCategory);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.filters.selectType);
                await page.click(selectors.getSelectMenuItem(3));
                await page.click(selectors.filters.selectCategory);
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.filters.selectType);
                await page.click(selectors.getSelectMenuItem(4));
                await page.click(selectors.filters.selectCategory);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`ожидание ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.search_profile_count': {
                                return new Promise(noop);
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });

                await page.type(selectors.filters.nameInput, 'Имя');
                // @ts-expect-error blur
                await page.$eval(selectors.filters.nameInput, (e) => e.blur());

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`ошибка фильтрации ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.search_profile_count': {
                                return Promise.reject();
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });

                await page.type(selectors.filters.nameInput, 'Ошибка');
                // @ts-expect-error blur
                await page.$eval(selectors.filters.nameInput, (e) => e.blur());

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`ошибка при поиске ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.search_profile': {
                                return Promise.reject();
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });

                await page.type(selectors.filters.nameInput, 'Ошибка');
                // @ts-expect-error blur
                await page.$eval(selectors.filters.nameInput, (e) => e.blur());
                await page.click(selectors.showButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it(`применение фильтров ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.search_profile_count': {
                                return Promise.resolve({
                                    total: 2,
                                });
                            }
                            case 'profiles.search_profile': {
                                return Promise.resolve({
                                    page: { current: 1, total: 1 },
                                    profiles: {
                                        ...agentProfiles,
                                        order: agentProfiles.order.filter((id) => {
                                            const profile = agentProfiles.ids[id];
                                            return profile.name && profile.name.includes('Бонд');
                                        }),
                                    },
                                });
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                        })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });

                await page.type(selectors.filters.nameInput, 'Бонд');
                // @ts-expect-error blur
                await page.$eval(selectors.filters.nameInput, (e) => e.blur());

                await page.click(selectors.showButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Паджинация', () => {
        VIEWPORTS.forEach((viewport) => {
            it(`Переход на другую страницу ${viewport.width}px`, async () => {
                const Gate = {
                    get: (path: string) => {
                        switch (path) {
                            case 'profiles.search_profile': {
                                return Promise.resolve(secondProfilesPageGateStub);
                            }
                        }
                    },
                };

                const component = (
                    <Component
                        store={getStore({
                            profiles: agentProfiles,
                            searchQuery: { userType: ProfileTypes.AGENT },
                            page: { current: 1, total: 3 },
                            searchCount: 22,
                        })}
                        Gate={Gate}
                    />
                );

                await render(component, { viewport });
                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.thirdPageButton);
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });
});
