const React = require('react');
const { shallow } = require('enzyme');

const OfferAmpVehicleBodyDamages = require('./OfferAmpVehicleBodyDamages');

it('не должен ничего рисовать, если нет повреждений', () => {
    const wrapper = shallow(
        <OfferAmpVehicleBodyDamages/>,
    );

    expect(wrapper.type()).toBeNull();
});

it('не должен ничего рисовать, если есть повреждения, но нет известного кузова', () => {
    const wrapper = shallow(
        <OfferAmpVehicleBodyDamages
            bodyType="SEDAN1"
            damages={ [
                { car_part: 'REAR_BUMPER', type: [ 'DYED' ] },
                { car_part: 'FRONT_BUMPER', type: [ 'DYED' ] },
            ] }
        />,
    );

    expect(wrapper.type()).toBeNull();
});

it('должен отрисовать обычный кузов без мапинга', () => {
    const wrapper = shallow(
        <OfferAmpVehicleBodyDamages
            bodyType="SEDAN"
            damages={ [
                { car_part: 'REAR_BUMPER', type: [ 'DYED' ] },
                { car_part: 'FRONT_BUMPER', type: [ 'DYED' ] },
            ] }
        />,
    );

    expect(wrapper.find('.OfferAmpVehicleBodyDamages').hasClass('OfferAmpVehicleBodyDamages_body_sedan')).toEqual(true);
});

it('должен отрисовать хитрый кузов с мапингом', () => {
    const wrapper = shallow(
        <OfferAmpVehicleBodyDamages
            bodyType="PHAETON_WAGON"
            damages={ [
                { car_part: 'REAR_BUMPER', type: [ 'DYED' ] },
                { car_part: 'FRONT_BUMPER', type: [ 'DYED' ] },
            ] }
        />,
    );

    expect(wrapper.find('.OfferAmpVehicleBodyDamages').hasClass('OfferAmpVehicleBodyDamages_body_cabrio')).toEqual(true);
});
