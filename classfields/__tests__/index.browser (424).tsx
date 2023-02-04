import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { BuildingHeatingType, BuildingType, IBuildingData } from 'realty-core/view/react/common/types/egrnPaidReport';
import { getBuildingFeatures } from 'realty-core/view/react/modules/egrn-paid-report/lib/get-building-features';

import { EGRNPaidReportBuildingFeaturesBlock } from '../';

const buildingData: Partial<IBuildingData> = {
    buildingSeries: 'X16-23',
    buildYear: 1999,
    reconstructionYear: 2009,
    buildingType: BuildingType.BUILDING_TYPE_BLOCK,
    flatsCount: 69,
    porchesCount: 38,
    ceilingHeight: 256,
    hasElevator: true,
    hasRubbishChute: true,
    hasSecurity: true,
    expectDemolition: true,
    numberOfFloors: 9,
    heatingType: BuildingHeatingType.CENTRAL,
};

describe('EGRNPaidReportBuildingFeaturesBlock', () => {
    it('рендерится', async () => {
        await render(<EGRNPaidReportBuildingFeaturesBlock features={getBuildingFeatures(buildingData)} />, {
            viewport: { width: 900, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
