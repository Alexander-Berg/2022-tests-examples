const React = require('react');
const { shallow } = require('enzyme');

const { VIN_REPORT_PRESET } = require('auto-core/react/dataDomain/vinReport/vinReportPreset');

const VinReportStatusNew = require('./VinReportStatusNew').default;

it('должен быть правильный текст с plural form', () => {
    const tree = shallow(
        <VinReportStatusNew sources={{
            header: { title: 'aaa' },
            sources_count: 21,
            ready_count: 1,
        }}
        />,
    );
    expect(tree.find('.VinReportStatusNew__title').text()).toBe('Проверен 1 из 21 источника');
});

it('должен быть правильный текст с plural form для pdf-версии', () => {
    const tree = shallow(
        <VinReportStatusNew sources={{
            header: { title: 'aaa' },
            sources_count: 21,
            ready_count: 1,
        }}
        preset={ VIN_REPORT_PRESET.PDF }
        />,
    );
    expect(tree.find('.VinReportStatusNew__text').text()).toBe('На момент печати опрошен 1 из 21 источника.');
});

it('Если опрошены все источники, не должен рисовать плашку что отчёт может обновиться', () => {
    const tree = shallow(
        <VinReportStatusNew sources={{
            header: { title: 'aaa' },
            sources_count: 13,
            ready_count: 13,
        }}
        />,
    );
    expect(tree).toBeEmptyRender();
});
