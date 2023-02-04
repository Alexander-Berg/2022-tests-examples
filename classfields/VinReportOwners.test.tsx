import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import type { PtsOwnersBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import DateMock from 'autoru-frontend/mocks/components/DateMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import vinReportMock from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import VinReportOwners from './VinReportOwners';

const storeMock = {
    config: {
        data: {
            pageType: 'proauto-report',
        },
    },
};

it('VinReportOwners показывает VinReportLoading, если данные еще не пришли', async() => {
    const mock = {
        header: {
            ...vinReportMock.report!.pts_owners!.header,
            is_updating: true,
        },
    };
    const wrapper = shallow(
        <DateMock date="2019-12-31">
            <Provider store={ mockStore(storeMock) }>
                <VinReportOwners ptsOwners={ mock as PtsOwnersBlock }/>
            </Provider>
        </DateMock>,
    ).dive().dive();

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});

it('VinReportOwners не отрисовывается, если status NOT_VISIBLE', async() => {
    const mock = _.cloneDeep(vinReportMock.report!.pts_owners);
    mock!.status = Status.NOT_VISIBLE;

    const wrapper = shallow(
        <DateMock date="2019-12-31">
            <Provider store={ mockStore(storeMock) }>
                <VinReportOwners ptsOwners={ mock }/>
            </Provider>
        </DateMock>,
    ).dive().dive();

    expect(wrapper).toBeEmptyRender();
});

it('VinReportOwners не падает, если нет данных', async() => {
    const wrapper = shallow(
        <DateMock date="2019-12-31">
            <Provider store={ mockStore(storeMock) }>
                <VinReportOwners/>
            </Provider>
        </DateMock>,
    ).dive().dive();

    expect(wrapper).toBeEmptyRender();
});
