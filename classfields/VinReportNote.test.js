const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const VinReportNote = require('./VinReportNote');

let context;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

it('должен отправлять метрику показа VinReportNote', () => {
    shallow(<VinReportNote/>, { context });

    const expectedResult = [ 'history_report', 'no_license_plate_photo', 'view', 'not_owner' ];
    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
});

it('должен отправлять метрику клика "добавить фото" VinReportNote', () => {
    const wrapper = shallow(<VinReportNote isOwner={ true } editLink=""/>, { context });

    wrapper.find('Button').first().dive().simulate('click');

    const expectedResult = [ 'history_report', 'no_license_plate_photo', 'add_photo' ];
    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
    expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual(expectedResult);
});

it('должен отправлять метрику клика "написать в поддержку" VinReportNote', () => {
    const wrapper = shallow(<VinReportNote isOwner={ true } editLink=""/>, { context });

    wrapper.find('Button').last().dive().simulate('click');

    const expectedResult = [ 'history_report', 'no_license_plate_photo', 'techsupport' ];
    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
    expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual(expectedResult);
});
