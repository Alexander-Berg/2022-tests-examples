const React = require('react');
const TimeAgo = require('./TimeAgo');
const MockDate = require('mockdate');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { MINUTE } = require('auto-core/lib/consts');

const currentTime = '2019-02-26T13:00:00.000+0300';

beforeEach(() => {
    MockDate.set(currentTime);
});

afterEach(() => {
    MockDate.reset();
});

describe('правильно рисует компонент:', () => {
    const testCases = [
        {
            name: 'если передан таймстемп в прошлом то рисует разницу во времени',
            props: { timestamp: new Date(currentTime).getTime() - MINUTE },
        },
        {
            name: 'если передан таймстемп в будущем то ничего не рисует',
            props: { timestamp: new Date(currentTime).getTime() + MINUTE },
        },
    ];

    testCases.forEach(({ name, props }) => {
        it(name, () => {
            const tree = shallow(<TimeAgo { ...props }/>);
            expect(shallowToJson(tree)).toMatchSnapshot();
        });
    });
});
