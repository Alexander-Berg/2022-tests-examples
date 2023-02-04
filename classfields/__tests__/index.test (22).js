import { shallow } from 'enzyme';
import RadioGroup from '../../../ui/options';
import Option from '../../../ui/options/components/option';
import { FormRoomsTotalComponent } from '../';

const setValues = jest.fn();

const commonProps = {
    name: 'roomsTotal',
    values: [ '2', '3', '4', '5', '6', '7' ],
    setValues
};

describe('FormRoomsTotalComponent', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('resets amount and areas of offered rooms on deselect', () => {
        const wrapper = shallow(
            <FormRoomsTotalComponent
                {...commonProps}
                roomsTotal='5'
                roomsOffered='4'
            />
        );

        wrapper.find(RadioGroup).simulate('change', undefined);

        expect(setValues).toBeCalledWith({
            roomsTotal: undefined,
            roomsOffered: undefined,
            rooms: []
        });
    });

    it('disables values less than rooms offered', () => {
        const wrapper = shallow(
            <FormRoomsTotalComponent
                {...commonProps}
                roomsTotal='5'
                roomsOffered='4'
            />
        );

        const options = wrapper.find(RadioGroup).dive().find(Option);

        options.forEach(option => {
            const isOptionValueLessThanRoomsOffered = Number(option.prop('value')) <= 4;

            expect(option).toHaveProp({
                disabled: isOptionValueLessThanRoomsOffered
            });
        });
    });
});
