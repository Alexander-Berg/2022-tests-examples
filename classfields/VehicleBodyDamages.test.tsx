import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import { Damage_CarPart, Damage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { Damage } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { Car_BodyType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import VehicleBodyDamages from './VehicleBodyDamages';

const invalidBodyType = 'SEDAN1' as Car_BodyType;

const damages = [
    { car_part: Damage_CarPart.FRONT_BUMPER, type: [ Damage_DamageType.DENT ], description: '' },
    { car_part: Damage_CarPart.TRUNK_DOOR, type: [ Damage_DamageType.DENT ], description: '' },
    { car_part: Damage_CarPart.FRONT_LEFT_DOOR, type: [ Damage_DamageType.DENT ], description: '' },
] as Array<Damage>;

it('не должен ничего рисовать, если нет повреждений', () => {
    render(
        <VehicleBodyDamages bodyType={ Car_BodyType.SEDAN }/>,
    );

    const component = document.querySelector('.VehicleBodyDamages');

    expect(component).toBeNull();
});

it('не должен ничего рисовать, если есть повреждения, но нет известного кузова', () => {
    render(
        <VehicleBodyDamages
            bodyType={ invalidBodyType }
            damages={ damages }
        />,
    );

    const component = document.querySelector('.VehicleBodyDamages');

    expect(component).toBeNull();
});
