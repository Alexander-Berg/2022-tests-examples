import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import noop from 'lodash/noop';

import {
    FlatQuestionnaire_Furniture_Internet_InternetTypeNamespace_InternetType,
    FlatQuestionnaire_Furniture_Oven_OvenTypeNamespace_OvenType,
} from '@vertis/schema-registry/ts-types/realty/rent/api/flat_check_list';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import {
    FlatQuestionnaireBuildingHouseType,
    FlatQuestionnaireBuildingParkingNamespaceParking,
    FlatQuestionnaireFlatFlatTypeNamespaceFlatType,
    FlatQuestionnaireFlatRenovationTypeNamespaceRenovationType,
    FlatQuestionnaireFlatRentHistoryWhoRentedTypeNamespaceWhoRentedType,
    FlatQuestionnaireFlatRoomsNamespaceRooms,
    FlatQuestionnaireFlatWindowSideType,
    FlatQuestionnaireFlatWorldSideType,
    IFlatQuestionnaire,
    IFLatQuestionnaireFlatCleannessType,
    IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation,
    IFlatQuestionnaireGuaranteedPaymentStatus,
} from 'types/flatQuestionnaire';

import { PaymentsCommissionValue } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/manager/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import { Fields } from 'view/modules/managerFlatQuestionnaireForm/types';

import managerFlatQuestionnaireFormStyles from '../styles.module.css';

import { ManagerFlatQuestionnaireFormContainer } from '../container';

import managerFlatQuestionnaireFormOwnerInfoStyles from '../ManagerFlatQuestionnaireFormOwnerInfo/styles.module.css';

import { filledStore, mobileStore, offerCopyright, skeletonStore, store } from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    sendSubmit: `.${managerFlatQuestionnaireFormStyles.submit}.Button`,
    inputs: Object.values(Fields).reduce((acc, field) => {
        acc[field] = `#${field}`;

        return acc;
    }, {} as Record<Fields, string>),
    checkboxes: {
        windowsSide: (n: number) => `#${Fields.FLAT_WINDOW_SIDE} label:nth-of-type(${n}) input`,
        windowsWorld: (n: number) => `#${Fields.FLAT_WORLD_SIDE} label:nth-of-type(${n}) input`,
    },
    stepByStep: {
        left: `.Portal:last-of-type .${stepByStepModalStyles.leftButton}`,
        right: `.Portal:last-of-type .${stepByStepModalStyles.rightButton}`,
        close: `.Portal:last-of-type .${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: Object.values(Fields).reduce((acc, field) => {
            acc[field] = `.Portal:last-of-type .${stepByStepModalStyles.modal} #${field}`;

            return acc;
        }, {} as Record<Fields, string>),
    },
    addOwnerInfo: `.${managerFlatQuestionnaireFormOwnerInfoStyles.button}.Button`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatQuestionnaireFormContainer />
    </AppProvider>
);

describe('ManagerFlatQuestionnaireForm', () => {
    describe(`Базовое состояние`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Заполненное состояние`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение', () => {
        it('Все поля', async () => {
            const Gate = {
                create: () => Promise.resolve(),
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.FLAT_ENTRANCE, '4');
            await page.type(selectors.inputs.FLAT_FLOOR, '18');
            await page.type(selectors.inputs.BUILDING_FLOORS, '21');
            await page.type(selectors.inputs.FLAT_INTERCOM_CODE, '300');
            await page.type(selectors.inputs.FLAT_ENTRANCE_INSTRUCTION, 'Открыть дверь и войти');

            await page.type(selectors.inputs.FLAT_AREA, '184');
            await page.type(selectors.inputs.FLAT_KITCHEN_SPACE, '100');
            await page.focus(selectors.inputs.FLAT_ROOMS);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.focus(selectors.inputs.FLAT_RENOVATION_TYPE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            const onlyOnlineShowings = await page.$(selectors.inputs.ONLY_ONLINE_SHOWINGS);
            await onlyOnlineShowings?.focus();
            await onlyOnlineShowings?.press('Space');

            await page.type(selectors.inputs.PAYMENTS_RENTAL_VALUE, '500000');
            await page.focus(selectors.inputs.PAYMENTS_COMMISSION_VALUE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            await page.type(selectors.inputs.FLAT_BALCONY_AMOUNT, '2');
            await page.type(selectors.inputs.FLAT_LOGGIA_AMOUNT, '2');
            await page.type(selectors.inputs.FLAT_BATHROOM_COMBINED_AMOUNT, '2');
            await page.type(selectors.inputs.FLAT_BATHROOM_SEPARATED_AMOUNT, '2');
            await page.focus(selectors.inputs.FLAT_RENT_HISTORY_WHO_RENTED_BEFORE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            const checkboxOwnerKey = await page.$(selectors.inputs.FLAT_DOES_OWNER_GIVE_KEY);
            await checkboxOwnerKey?.focus();
            await checkboxOwnerKey?.press('Space');
            await page.focus(selectors.inputs.FLAT_KEY_LOCATION);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            const checkboxWindowSide = await page.$(selectors.checkboxes.windowsSide(1));
            await checkboxWindowSide?.focus();
            await checkboxWindowSide?.press('Space');

            const checkboxWindowWorld = await page.$(selectors.checkboxes.windowsWorld(8));
            await checkboxWindowWorld?.focus();
            await checkboxWindowWorld?.press('Space');

            const checkboxTv = await page.$(selectors.inputs.FURNITURE_TV_IS_PRESENT);
            await checkboxTv?.focus();
            await checkboxTv?.press('Space');
            const checkboxOven = await page.$(selectors.inputs.FURNITURE_OVEN_IS_PRESENT);
            await checkboxOven?.focus();
            await checkboxOven?.press('Space');
            const checkboxWashingMachine = await page.$(selectors.inputs.FURNITURE_WASHING_MACHINE_IS_PRESENT);
            await checkboxWashingMachine?.focus();
            await checkboxWashingMachine?.press('Space');
            const checkboxDryingMachine = await page.$(selectors.inputs.FURNITURE_DRYING_MACHINE_IS_PRESENT);
            await checkboxDryingMachine?.focus();
            await checkboxDryingMachine?.press('Space');
            const checkboxFridge = await page.$(selectors.inputs.FURNITURE_FRIDGE_IS_PRESENT);
            await checkboxFridge?.focus();
            await checkboxFridge?.press('Space');
            const checkboxDishWasher = await page.$(selectors.inputs.FURNITURE_DISH_WASHER_IS_PRESENT);
            await checkboxDishWasher?.focus();
            await checkboxDishWasher?.press('Space');
            const checkboxConditioner = await page.$(selectors.inputs.FURNITURE_CONDITIONER_IS_PRESENT);
            await checkboxConditioner?.focus();
            await checkboxConditioner?.press('Space');
            const checkboxBoiler = await page.$(selectors.inputs.FURNITURE_BOILER_IS_PRESENT);
            await checkboxBoiler?.focus();
            await checkboxBoiler?.press('Space');
            const checkboxWarmFloor = await page.$(selectors.inputs.FURNITURE_WARM_FLOOR_IS_PRESENT);
            await checkboxWarmFloor?.focus();
            await checkboxWarmFloor?.press('Space');
            const checkboxBedclothes = await page.$(selectors.inputs.FURNITURE_BEDCLOTHES_IS_PRESENT);
            await checkboxBedclothes?.focus();
            await checkboxBedclothes?.press('Space');
            const checkboxMicrowave = await page.$(selectors.inputs.FURNITURE_MICROWAVE_IS_PRESENT);
            await checkboxMicrowave?.focus();
            await checkboxMicrowave?.press('Space');
            const checkboxVacuumCleaner = await page.$(selectors.inputs.FURNITURE_VACUUM_CLEANER_IS_PRESENT);
            await checkboxVacuumCleaner?.focus();
            await checkboxVacuumCleaner?.press('Space');
            const checkboxDishes = await page.$(selectors.inputs.FURNITURE_DISHES_IS_PRESENT);
            await checkboxDishes?.focus();
            await checkboxDishes?.press('Space');
            await page.type(selectors.inputs.FURNITURE_DISHES_DESCRIPTION, 'вилка тарелка');
            await page.focus(selectors.inputs.FURNITURE_OVEN_TYPE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            await page.focus(selectors.inputs.FURNITURE_INTERNET_TYPE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            const checkboxProvider = await page.$(selectors.inputs.FURNITURE_INTERNET_CAN_RENEW_PROVIDER_CONTRACT);
            await checkboxProvider?.focus();
            await checkboxProvider?.press('Space');
            await page.type(selectors.inputs.FURNITURE_INTERNET_PROVIDER, 'Ростелеком');
            await page.type(selectors.inputs.FURNITURE_INTERNET_PRICE, '700');

            const checkboxPayments = await page.$(selectors.inputs.PAYMENTS_NEED_ALL_RECEIPT_PAYMENTS);
            await checkboxPayments?.focus();
            await checkboxPayments?.press('Space');

            const checkboxPets = await page.$(selectors.inputs.TENANT_REQUIREMENTS_HAS_WITH_PETS);
            await checkboxPets?.focus();
            await checkboxPets?.press('Space');
            await page.type(selectors.inputs.TENANT_REQUIREMENTS_PREFERENCES_FOR_TENANTS, 'Просто молодца');

            const checkboxGarbage = await page.$(selectors.inputs.BUILDING_HAS_GARBAGE_CHUTE);
            await checkboxGarbage?.focus();
            await checkboxGarbage?.press('Space');
            const checkboxBarrier = await page.$(selectors.inputs.BUILDING_HAS_BARRIER);
            await checkboxBarrier?.focus();
            await checkboxBarrier?.press('Space');
            await page.type(selectors.inputs.BUILDING_ELEVATORS_PASSENGER_AMOUNT, '5');
            await page.type(selectors.inputs.BUILDING_ELEVATORS_CARGO_AMOUNT, '10');

            await page.focus(selectors.inputs.BUILDING_HOUSE_TYPE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            const checkboxModernEntrance = await page.$(selectors.inputs.BUILDING_HAS_MODERN_ENTRANCE);
            await checkboxModernEntrance?.focus();
            await checkboxModernEntrance?.press('Space');

            const checkboxParkingUnderground = await page.$(
                selectors.inputs.BUILDING_PARKING_TYPE_BACKYARD_OR_PUBLIC_FREE
            );
            await checkboxParkingUnderground?.focus();
            await checkboxParkingUnderground?.press('Space');
            const checkboxParkingHood = await page.$(selectors.inputs.BUILDING_PARKING_TYPE_PUBLIC_PAID);
            await checkboxParkingHood?.focus();
            await checkboxParkingHood?.press('Space');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        describe('Все поля через пошаговую форму', () => {
            it('Доступ в квартиру', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.FLAT_ENTRANCE);
                await page.type(selectors.stepByStep.inputs.FLAT_ENTRANCE, '4');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_FLOOR, '18');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.BUILDING_FLOORS, '21');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_INTERCOM_CODE, '300');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_ENTRANCE_INSTRUCTION, 'Открыть дверь и войти');
                await page.click(selectors.stepByStep.right);
                const checkboxOwnerKey = await page.$(selectors.stepByStep.inputs.FLAT_DOES_OWNER_GIVE_KEY);
                await checkboxOwnerKey?.focus();
                await checkboxOwnerKey?.press('Space');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_KEY_LOCATION);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Общая информация о квартире', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.FLAT_AREA);
                await page.type(selectors.stepByStep.inputs.FLAT_AREA, '184');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_KITCHEN_SPACE, '100');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_ROOMS);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_RENOVATION_TYPE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Арендная плата', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.PAYMENTS_RENTAL_VALUE);
                await page.type(selectors.stepByStep.inputs.PAYMENTS_RENTAL_VALUE, '500000');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.PAYMENTS_COMMISSION_VALUE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Детали о квартире', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.FLAT_BALCONY_AMOUNT);
                await page.type(selectors.stepByStep.inputs.FLAT_BALCONY_AMOUNT, '2');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_LOGGIA_AMOUNT, '2');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_BATHROOM_COMBINED_AMOUNT, '2');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_BATHROOM_SEPARATED_AMOUNT, '2');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_RENT_HISTORY_WHO_RENTED_BEFORE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Удобства в квартире', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const checkboxTv = await page.$(selectors.inputs.FURNITURE_TV_IS_PRESENT);
                await checkboxTv?.focus();
                await checkboxTv?.press('Space');
                const checkboxOven = await page.$(selectors.inputs.FURNITURE_OVEN_IS_PRESENT);
                await checkboxOven?.focus();
                await checkboxOven?.press('Space');
                const checkboxWashingMachine = await page.$(selectors.inputs.FURNITURE_WASHING_MACHINE_IS_PRESENT);
                await checkboxWashingMachine?.focus();
                await checkboxWashingMachine?.press('Space');
                const checkboxDryingMachine = await page.$(selectors.inputs.FURNITURE_DRYING_MACHINE_IS_PRESENT);
                await checkboxDryingMachine?.focus();
                await checkboxDryingMachine?.press('Space');
                const checkboxFridge = await page.$(selectors.inputs.FURNITURE_FRIDGE_IS_PRESENT);
                await checkboxFridge?.focus();
                await checkboxFridge?.press('Space');
                const checkboxDishWasher = await page.$(selectors.inputs.FURNITURE_DISH_WASHER_IS_PRESENT);
                await checkboxDishWasher?.focus();
                await checkboxDishWasher?.press('Space');
                const checkboxConditioner = await page.$(selectors.inputs.FURNITURE_CONDITIONER_IS_PRESENT);
                await checkboxConditioner?.focus();
                await checkboxConditioner?.press('Space');
                const checkboxBoiler = await page.$(selectors.inputs.FURNITURE_BOILER_IS_PRESENT);
                await checkboxBoiler?.focus();
                await checkboxBoiler?.press('Space');
                const checkboxWarmFloor = await page.$(selectors.inputs.FURNITURE_WARM_FLOOR_IS_PRESENT);
                await checkboxWarmFloor?.focus();
                await checkboxWarmFloor?.press('Space');
                const checkboxBedclothes = await page.$(selectors.inputs.FURNITURE_BEDCLOTHES_IS_PRESENT);
                await checkboxBedclothes?.focus();
                await checkboxBedclothes?.press('Space');
                const checkboxMicrowave = await page.$(selectors.inputs.FURNITURE_MICROWAVE_IS_PRESENT);
                await checkboxMicrowave?.focus();
                await checkboxMicrowave?.press('Space');
                const checkboxVacuumCleaner = await page.$(selectors.inputs.FURNITURE_VACUUM_CLEANER_IS_PRESENT);
                await checkboxVacuumCleaner?.focus();
                await checkboxVacuumCleaner?.press('Space');
                const checkboxDishes = await page.$(selectors.inputs.FURNITURE_DISHES_IS_PRESENT);
                await checkboxDishes?.focus();
                await checkboxDishes?.press('Space');
                await page.click(selectors.inputs.FURNITURE_DISHES_DESCRIPTION);
                await page.type(selectors.stepByStep.inputs.FURNITURE_DISHES_DESCRIPTION, 'вилка тарелка');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FURNITURE_OVEN_TYPE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Интернет', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.focus(selectors.inputs.FURNITURE_INTERNET_TYPE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                const checkboxProvider = await page.$(selectors.inputs.FURNITURE_INTERNET_CAN_RENEW_PROVIDER_CONTRACT);
                await checkboxProvider?.focus();
                await checkboxProvider?.press('Space');
                await page.click(selectors.inputs.FURNITURE_INTERNET_PROVIDER);
                await page.type(selectors.stepByStep.inputs.FURNITURE_INTERNET_PROVIDER, 'Ростелеком');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FURNITURE_INTERNET_PRICE, '700');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Пожелания по жильцам', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const checkboxPets = await page.$(selectors.inputs.TENANT_REQUIREMENTS_HAS_WITH_PETS);
                await checkboxPets?.focus();
                await checkboxPets?.press('Space');
                await page.click(selectors.inputs.TENANT_REQUIREMENTS_PREFERENCES_FOR_TENANTS);
                await page.type(selectors.stepByStep.inputs.TENANT_REQUIREMENTS_PREFERENCES_FOR_TENANTS, 'Тихие');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Информация о доме', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const checkboxGarbage = await page.$(selectors.inputs.BUILDING_HAS_GARBAGE_CHUTE);
                await checkboxGarbage?.focus();
                await checkboxGarbage?.press('Space');
                const checkboxBarrier = await page.$(selectors.inputs.BUILDING_HAS_BARRIER);
                await checkboxBarrier?.focus();
                await checkboxBarrier?.press('Space');
                const checkboxModernEntrance = await page.$(selectors.inputs.BUILDING_HAS_MODERN_ENTRANCE);
                await checkboxModernEntrance?.focus();
                await checkboxModernEntrance?.press('Space');
                await page.click(selectors.inputs.BUILDING_ELEVATORS_CARGO_AMOUNT);
                await page.type(selectors.stepByStep.inputs.BUILDING_ELEVATORS_CARGO_AMOUNT, '10');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.BUILDING_ELEVATORS_PASSENGER_AMOUNT, '5');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.BUILDING_HOUSE_TYPE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Отправка формы', () => {
        renderOptions.forEach((renderOption) => {
            it(`Успешно сохранено ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'manager.update_flat_questionnaire': {
                                return Promise.resolve<{ questionnaire: IFlatQuestionnaire }>({
                                    questionnaire: {
                                        media: {
                                            photoRawUrl: '1',
                                            photoRetouchedUrl: '2',
                                            tour3dUrl: '3',
                                        },
                                        offerCopyright,
                                        building: {
                                            floors: 22,
                                            elevators: {
                                                passengerAmount: 5,
                                                cargoAmount: 12,
                                            },
                                            hasOption: 'Детский сад в доме',
                                            hasConcierge: true,
                                            hasGarbageChute: true,
                                            hasWheelchairStorage: true,
                                            hasBarrier: true,
                                            hasModernEntrance: true,
                                            transportAccessibility: '',
                                            houseType: FlatQuestionnaireBuildingHouseType.BUILDING_TYPE_BRICK,
                                            parking: [
                                                FlatQuestionnaireBuildingParkingNamespaceParking.UNDERGROUND,
                                                FlatQuestionnaireBuildingParkingNamespaceParking.BEHIND_BARRIER,
                                                // eslint-disable-next-line max-len
                                                FlatQuestionnaireBuildingParkingNamespaceParking.BACKYARD_OR_PUBLIC_FREE,
                                                FlatQuestionnaireBuildingParkingNamespaceParking.PUBLIC_PAID,
                                            ],
                                        },
                                        flat: {
                                            entrance: 4,
                                            floor: 18,
                                            intercom: {
                                                code: '300',
                                            },
                                            rooms: FlatQuestionnaireFlatRoomsNamespaceRooms.SIX,
                                            area: 184,
                                            kitchenSpace: 100,
                                            balcony: {
                                                balconyAmount: 3,
                                                loggiaAmount: 2,
                                            },
                                            bathroom: {
                                                combinedAmount: 2,
                                                separatedAmount: 2,
                                            },
                                            renovation:
                                                FlatQuestionnaireFlatRenovationTypeNamespaceRenovationType.BY_DESIGN,
                                            windowSide: [
                                                FlatQuestionnaireFlatWindowSideType.YARD_SIDE,
                                                FlatQuestionnaireFlatWindowSideType.STREET_SIDE,
                                            ],
                                            worldSide: [
                                                FlatQuestionnaireFlatWorldSideType.NORTH,
                                                FlatQuestionnaireFlatWorldSideType.NORTH_EAST,
                                                FlatQuestionnaireFlatWorldSideType.SOUTH,
                                                FlatQuestionnaireFlatWorldSideType.EAST,
                                                FlatQuestionnaireFlatWorldSideType.WEST,
                                                FlatQuestionnaireFlatWorldSideType.NORTH_WEST,
                                                FlatQuestionnaireFlatWorldSideType.SOUTH_EAST,
                                                FlatQuestionnaireFlatWorldSideType.SOUTH_WEST,
                                            ],
                                            doesOwnerGiveKey: true,
                                            keyLocation:
                                                IFlatQuestionnaireFlatKeyLocationNamespaceKeyLocation.IN_OFFICE,
                                            rentHistory: {
                                                whoRented:
                                                    // eslint-disable-next-line max-len
                                                    FlatQuestionnaireFlatRentHistoryWhoRentedTypeNamespaceWhoRentedType.TENANT,
                                            },
                                            entranceInstruction: 'Открой дверь ключом и входи',
                                            ownerDescription: 'Квартира огонь',
                                            flatType: FlatQuestionnaireFlatFlatTypeNamespaceFlatType.FLAT,
                                            cleanness: IFLatQuestionnaireFlatCleannessType.CLEAN,
                                        },
                                        furniture: {
                                            tv: {
                                                isPresent: true,
                                            },
                                            internet: {
                                                isPresent: true,
                                                internetType:
                                                    // eslint-disable-next-line max-len
                                                    FlatQuestionnaire_Furniture_Internet_InternetTypeNamespace_InternetType.ONLY_CABLE,
                                                internetProvider: 'Ростелеком',
                                                canRenewProviderContract: true,
                                                price: 300,
                                            },
                                            oven: {
                                                isPresent: true,
                                                ovenType:
                                                    // eslint-disable-next-line max-len
                                                    FlatQuestionnaire_Furniture_Oven_OvenTypeNamespace_OvenType.ELECTRIC,
                                            },
                                            washingMachine: {
                                                isPresent: true,
                                            },
                                            dishWasher: {
                                                isPresent: true,
                                            },
                                            dryingMachine: {
                                                isPresent: true,
                                            },
                                            fridge: {
                                                isPresent: true,
                                            },
                                            conditioner: {
                                                isPresent: true,
                                            },
                                            boiler: {
                                                isPresent: true,
                                            },
                                            warmFloor: {
                                                isPresent: true,
                                            },
                                            other: {
                                                description: 'Рисоварка',
                                            },
                                        },
                                        counters: {
                                            avgCounters: '100500',
                                            hasWaterCounter: true,
                                            hasElectricCounter: true,
                                            hasGasCounter: true,
                                            hasHeatingCounter: true,
                                        },
                                        payments: {
                                            rentalValue: '50000000',
                                            temporaryRentalValue: '40000000',
                                            temporaryPeriodMonths: 5,
                                            adValue: '30000000',
                                            temporaryAdValue: '20000000',
                                            commissionValue: PaymentsCommissionValue.FIVE,
                                            needElectricPayment: true,
                                            needSanitationPayment: true,
                                            needGasPayment: true,
                                            needHeatingPayment: true,
                                            needInternetPayment: true,
                                            needAllReceiptPayments: true,
                                            needBarrierPayment: true,
                                            needParkingPayment: true,
                                            needConciergePayment: true,
                                        },
                                        tenantRequirements: {
                                            maxTenantCount: 90,
                                            hasWithChildrenRequirement: true,
                                            hasWithPetsRequirement: true,
                                            preferencesForTenants: 'Просто молодца',
                                        },
                                        yandexRentConditions: {
                                            guaranteedPaymentStatus: IFlatQuestionnaireGuaranteedPaymentStatus.YES,
                                        },
                                    },
                                });
                            }
                        }
                    },
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Сохранение в процессе ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => new Promise(noop),
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`Не удалось сохранить ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => Promise.reject(),
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Добавление данных от собственника', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.addOwnerInfo);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
