import { shallow } from 'enzyme';
import RadioGroup from '../../../ui/options';
import Option from '../../../ui/options/components/option';
import { FormRoomsOfferedComponent } from '../';

const setValues = jest.fn();

const commonProps = {
    name: 'roomsOffered',
    values: [ '1', '2', '3', '4', '5', '6' ],
    setValues
};

describe('FormRoomsTotalComponent', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('resets room areas on deselect', () => {
        const wrapper = shallow(
            <FormRoomsOfferedComponent
                {...commonProps}
                roomsTotal='5'
                roomsOffered='4'
            />
        );

        wrapper.find(RadioGroup).simulate('change', undefined);

        expect(setValues).toBeCalledWith({
            roomsOffered: undefined,
            rooms: []
        });
    });

    it('removes areas of rooms outside of selected amount', () => {
        const wrapper = shallow(
            <FormRoomsOfferedComponent
                {...commonProps}
                roomsTotal='5'
                roomsOffered='4'
                rooms={[ 11, 22, 33, 44 ]}
            />
        );

        wrapper.find(RadioGroup).simulate('change', '3');

        expect(setValues).toBeCalledWith({
            roomsOffered: '3',
            rooms: [ 11, 22, 33 ]
        });
    });

    it('disables values more than total rooms', () => {
        const wrapper = shallow(
            <FormRoomsOfferedComponent
                {...commonProps}
                roomsTotal='5'
                roomsOffered='4'
            />
        );

        const options = wrapper.find(RadioGroup).dive().find(Option);

        options.forEach(option => {
            const isOptionValueMoreThanRoomsTotal = Number(option.prop('value')) >= 5;

            expect(option).toHaveProp({
                disabled: isOptionValueMoreThanRoomsTotal
            });
        });
    });
});
