const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const VinReportCarSharing = require('./VinReportCarSharing');
const vinReportMock = require('auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock');

const DATA_MOCK = _.cloneDeep(vinReportMock.data.report.car_sharing);

it('VinReportCarSharing рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportCarSharing carSharing={{
            header: DATA_MOCK.header,
        }}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
