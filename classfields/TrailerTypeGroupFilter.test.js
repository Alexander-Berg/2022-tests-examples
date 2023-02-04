const React = require('react');
const TrailerTypeGroupFiletr = require('./TrailerTypeGroupFilter');

const { shallow } = require('enzyme');

const TESTS = [
    {
        params: [ 'SB_VAN' ],
        result: [ 'SB_VAN' ],
    },
    {
        params: [ 'SWAP_BODY_ALL' ],
        result: [
            'SWAP_BODY_ALL',
            'ISOTHERMAL',
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
        ],
    },
    {
        params: [
            'ISOTHERMAL',
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
        ],
        value: [
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
        ],
        result: [
            'ISOTHERMAL',
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
            'SWAP_BODY_ALL',
        ],
    },
    {
        params: [
            'ISOTHERMAL',
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
        ],
        value: [
            'ISOTHERMAL',
            'BULK_CARGO',
            'SB_TARPAULIN',
            'SB_PLATFORM',
            'SB_REFRIGERATOR',
            'SB_VAN',
            'SPECIAL',
            'CONTAINER_TANK',
            'SWAP_BODY_ALL',
        ],
        result: [ ],
    },
];

TESTS.forEach(test => {
    it(JSON.stringify(test.params) + ' => ' + JSON.stringify(test.result), () => {
        const onChane = jest.fn();
        const wrapper = shallow(
            <TrailerTypeGroupFiletr
                value={ test.value || [] }
                onChange={ onChane }
            />,
        );
        const instance = wrapper.dive().instance();
        instance.onMenuChange(test.params);
        expect(onChane).toHaveBeenCalledWith(test.result, expect.anything());
    });
});
