import { advanceTo } from 'jest-date-mock';

import getBuildingLabelParts from '../getBuildingLabelParts';

import { i18n } from './mocks/buildingLabelPartsI18n';
import {
    buildingItem,
    buildingWithVillageItem,
    floorsOfferedItem,
    floorsOfferedWithoutFloorsTotalItem,
    newBuildingItem,
    newBuilding2020Item,
    newBuilding2023Item,
    villageItem,
} from './mocks/buildingLabelPartsData';

const TEST_CASES = [
    {
        name: 'New building',
        data: newBuildingItem,
    },
    {
        name: 'New building with built year (2021)',
        data: newBuilding2020Item,
    },
    {
        name: 'New building with built year (2023)',
        data: newBuilding2023Item,
    },
    {
        name: 'Village',
        data: villageItem,
    },
    {
        name: 'Building',
        data: buildingItem,
    },
    {
        name: 'Floors offered',
        data: floorsOfferedItem,
    },
    {
        name: 'Floors offered without total floors',
        data: floorsOfferedWithoutFloorsTotalItem,
    },
    {
        name: 'Building with village',
        data: buildingWithVillageItem,
    },
];

describe('Построение building label parts', () => {
    beforeAll(() => {
        advanceTo(new Date('2021-01-01T16:08:00Z'));
    });

    TEST_CASES.map((testCase) =>
        it(testCase.name, () => {
            const labelParts = getBuildingLabelParts(i18n, testCase.data);
            expect(labelParts).toMatchSnapshot();
        })
    );
});
