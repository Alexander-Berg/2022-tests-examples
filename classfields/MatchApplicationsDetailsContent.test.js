const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');
const MockDate = require('mockdate');

const matchApplicationsMock = require('www-cabinet/react/dataDomain/matchApplications/mocks/withApplications.mock');
const application = matchApplicationsMock.applicationsList.match_applications[0];

const MatchApplicationsDetailsContent = require('./MatchApplicationsDetailsContent');

afterEach(() => {
    MockDate.reset();
});

it('не должен рендерить оффер юзера, если пришел пустой объект', () => {
    const applicationClone = _.cloneDeep(application);

    applicationClone.user_car_info = {};

    const tree = shallow(
        <MatchApplicationsDetailsContent
            application={ applicationClone }
            fetchApplicationPhone={ _.noop }
        />,
    );

    const instance = tree.instance();

    expect(instance.renderUserCar()).toBeNull();
});

it('не должен рендерить лишние поля оффера юзера, если их нет', () => {
    const applicationClone = _.cloneDeep(application);

    applicationClone.user_car_info = {
        mark: 'Nissan',
        model: 'Teana',
    };

    const tree = shallow(
        <MatchApplicationsDetailsContent
            application={ applicationClone }
            fetchApplicationPhone={ _.noop }
        />,
    );

    const instance = tree.instance();
    const userCarInfo = instance.renderUserCar();

    expect(userCarInfo).toMatchSnapshot();
});

it('должен фетчить телефон, если его еще нет и заявка не просрочена', () => {
    const fetchApplicationPhone = jest.fn();
    MockDate.set('2019-02-03');

    const tree = shallow(
        <MatchApplicationsDetailsContent
            application={ application }
            fetchApplicationPhone={ fetchApplicationPhone }
        />,
    );

    const instance = tree.instance();
    instance.componentDidMount();

    expect(fetchApplicationPhone).toHaveBeenCalledWith(application.id);
});

it('не должен фетчить телефон, если заявка просрочена', () => {
    const fetchApplicationPhone = jest.fn();
    MockDate.set('2020-09-03');

    const tree = shallow(
        <MatchApplicationsDetailsContent
            application={ application }
            fetchApplicationPhone={ fetchApplicationPhone }
        />,
    );

    const instance = tree.instance();
    instance.componentDidMount();

    expect(fetchApplicationPhone).not.toHaveBeenCalled();
});
