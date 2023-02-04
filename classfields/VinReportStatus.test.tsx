import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import type { SourcesBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import VinReportStatus from './VinReportStatus';

it('должен правильно отрендерить статус неполного отчёта', () => {
    const tree = shallow(
        <VinReportStatus showComplete={ true } sources={{
            sources_count: 15,
            ready_count: 10,
            records_count: 5,
        } as SourcesBlock}
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен правильно отрендерить статус полного отчёта', () => {
    const tree = shallow(
        <VinReportStatus showComplete={ true } sources={{
            sources_count: 15,
            ready_count: 15,
            records_count: 5,
        } as SourcesBlock}
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен правильно отрендерить статус полного отчёта, если showComplete=false', () => {
    const tree = shallow(
        <VinReportStatus showComplete={ false } sources={{
            sources_count: 15,
            ready_count: 15,
            records_count: 5,
        } as SourcesBlock}
        />,
    );

    expect(tree).toBeEmptyRender();
});

it('должен быть правильный текст с plural form, если не все источники ответили', () => {
    const tree = shallow(
        <VinReportStatus showComplete={ true } sources={{
            sources_count: 21,
            ready_count: 1,
            records_count: 1,
        } as SourcesBlock}
        />,
    );
    expect(tree.find('.VinReportStatus').text()).toBe('Отчёт может дополняться. Опрошен 1 из 21 источника. Найдена 1 запись.');
});

it('должен быть правильный текст с plural form, если все источники ответили', () => {
    const tree = shallow(
        <VinReportStatus showComplete={ true } sources={{
            sources_count: 21,
            ready_count: 21,
            records_count: 16,
        } as SourcesBlock}
        />,
    );
    expect(tree.find('.VinReportStatus').text()).toBe('Отчёт полностью готов. Опрошен 21 источник. Найдено 16 записей');
});
