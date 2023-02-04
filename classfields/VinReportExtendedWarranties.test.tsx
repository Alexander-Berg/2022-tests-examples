import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import type { ProgramsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import { mdash, nbsp } from 'auto-core/react/lib/html-entities';

import VinReportExtendedWarranties from './VinReportExtendedWarranties';

const programs: ProgramsBlock = {
    header: {
        title: 'Программы производителей',
        timestamp_update: '1632317481676',
        is_updating: false,
    },
    program_records: [],
    record_count: 0,
    status: Status.IN_PROGRESS,
    description: 'аргентина-ямайка 5:0',
} as unknown as ProgramsBlock;

it('рендерит VinReportLoading, если данные еще не пришли', async() => {
    const programsWithUpdating = _.cloneDeep(programs);
    programsWithUpdating.header!.is_updating = true;

    const wrapper = shallow(
        <VinReportExtendedWarranties programs={ programsWithUpdating }/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});

describe('правильная дата', () => {
    const record = {
        description: 'Продленная гарантия от Nissan',
        finish_date: '1677099600000',
        program_name: 'Контракт NS3+ 12 месяцев или 125000 км.',
        provider_name: 'Nissan',
        start_date: '1645650000000',
        uri: 'https://www.nissan.ru/ownership/nissan-service3plus.html',
    };

    it('если есть начало и окончание', async() => {
        const programsWithDate = _.cloneDeep(programs);
        programsWithDate.program_records.push(record);

        const wrapper = shallow(
            <VinReportExtendedWarranties programs={ programsWithDate }/>,
        );

        expect(wrapper.find('.VinReportExtendedWarranties__recordDates').text())
            .toEqual(`24 февраля 2022${ nbsp }${ mdash }${ nbsp }23 февраля 2023`);
    });

    it('если есть начало и нет окончания', async() => {
        const programsWithDate = _.cloneDeep(programs);
        programsWithDate.program_records.push({ ...record, finish_date: '' });

        const wrapper = shallow(
            <VinReportExtendedWarranties programs={ programsWithDate }/>,
        );

        expect(wrapper.find('.VinReportExtendedWarranties__recordDates').text())
            .toEqual(`С${ nbsp }24 февраля 2022`);
    });

    it('если нет начала и нет окончание', async() => {
        const programsWithDate = _.cloneDeep(programs);
        programsWithDate.program_records.push({ ...record, start_date: '' });

        const wrapper = shallow(
            <VinReportExtendedWarranties programs={ programsWithDate }/>,
        );

        expect(wrapper.find('.VinReportExtendedWarranties__recordDates').text())
            .toEqual(`До${ nbsp }23 февраля 2023`);
    });

    it('если нет дат', async() => {
        const programsWithDate = _.cloneDeep(programs);
        programsWithDate.program_records.push({ ...record, start_date: '', finish_date: '' });

        const wrapper = shallow(
            <VinReportExtendedWarranties programs={ programsWithDate }/>,
        );

        expect(wrapper.find('.VinReportExtendedWarranties__recordDates')).not.toExist();
    });
});
