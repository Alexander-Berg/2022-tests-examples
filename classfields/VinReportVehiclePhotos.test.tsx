import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import type {
    VehiclePhotos,
    VehiclePhotoItem,
} from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

import vinReportMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock';

import VinReportVehiclePhotos from './VinReportVehiclePhotos';

it('рисует компонент, если есть фото', () => {
    const vinReport = _.cloneDeep(vinReportMock);
    const records = vinReport.data.report?.vehicle_photos?.records as Array<VehiclePhotoItem>;
    records[0].gallery = [ records[0].gallery[0] ];

    const tree = shallow(
        <VinReportVehiclePhotos vehiclePhotos={ vinReport.data.report?.vehicle_photos }/>,
    );
    expect(tree).not.toBeEmptyRender();
});

it('не рисует компонент, если нет records в vehicle_photos', () => {
    const tree = shallow(
        <VinReportVehiclePhotos vehiclePhotos={{ } as VehiclePhotos}/>,
    );
    expect(tree).toBeEmptyRender();
});

it('не рисует компонент, если в records пустой массив', () => {
    const vinReport = _.cloneDeep(vinReportMock);
    if (vinReport.data.report?.vehicle_photos) {
        vinReport.data.report.vehicle_photos.records = [];
        if (vinReport.data.report?.vehicle_photos.header) {
            vinReport.data.report.vehicle_photos.header.is_updating = false;
        }
    }

    const tree = shallow(
        <VinReportVehiclePhotos vehiclePhotos={ vinReport.data.report?.vehicle_photos }/>,
    );
    expect(tree).toBeEmptyRender();
});

it('VinReportVehiclePhotos должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const block = {
        header: {
            title: 'Есть фотки',
            timestamp_update: '1571028005586',
            is_updating: true,
        },
        records: [],
        record_count: 0,
        comments_count: 0,
        status: Status.UNKNOWN,
    } as VehiclePhotos;

    const wrapper = shallow(
        <VinReportVehiclePhotos vehiclePhotos={ block }/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
