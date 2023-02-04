const React = require('react');
const { shallow } = require('enzyme');

const ColorMarker = require('./ColorMarker');

const TEST_CLASSNAME = 'testColorClass';
const TEST_COLORS = [ 'FFFFFF', '000000' ];

it('должен содержать переданный класс', () => {
    const tree = shallow(
        <ColorMarker
            colors={ TEST_COLORS }
            className={ TEST_CLASSNAME }
        />,
    );
    expect(tree.find('.' + TEST_CLASSNAME)).toHaveLength(1);
});
