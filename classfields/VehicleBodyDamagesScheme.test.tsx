import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import { Damage_CarPart, Damage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { Car_BodyType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import CAR_PARTS from 'auto-core/data/damage/Damage.CarPart';

import VehicleBodyDamagesScheme, { BODY_DAMAGES_MODE } from './VehicleBodyDamagesScheme';

const invalidBodyType = 'SEDAN1' as Car_BodyType;

const damages = [
    { car_part: Damage_CarPart.FRONT_BUMPER, type: [ Damage_DamageType.DENT ], description: 'description' },
    { car_part: Damage_CarPart.FRONT_LEFT_DOOR, type: [ Damage_DamageType.DYED ], description: '' },
];

const DEFAULT_CAR_PARTS_LENGTH = (Object.keys(CAR_PARTS) as Array<Damage_CarPart>).filter((carPart) => CAR_PARTS[carPart]).length;

it(`должен отрисовать пустую схему если нет повреждений в режиме ${ BODY_DAMAGES_MODE.VIEW }`, () => {
    const { queryAllByRole } = render(
        <VehicleBodyDamagesScheme bodyType={ Car_BodyType.SEDAN } mode={ BODY_DAMAGES_MODE.VIEW }/>,
    );

    const damageDots = queryAllByRole('button');

    expect(damageDots).toHaveLength(0);
});

it('не должен ничего рисовать, если есть повреждения, но нет известного кузова', () => {
    const { queryByRole } = render(
        <VehicleBodyDamagesScheme
            bodyType={ invalidBodyType }
            damages={ damages }
        />,
    );

    const component = queryByRole('figure');

    expect(component).toBeNull();
});

it(`должен отрисовать точки c mode=VIEW, если пришли повреждения, в режиме ${ BODY_DAMAGES_MODE.VIEW }`, () => {
    const { getAllByRole, getByRole } = render(
        <VehicleBodyDamagesScheme
            bodyType={ Car_BodyType.SEDAN }
            mode={ BODY_DAMAGES_MODE.VIEW }
            damages={ damages }
        />,
    );

    const component = getByRole('figure');
    const dots = getAllByRole('button');

    expect(dots).toHaveLength(damages.length);

    expect(component).not.toBeNull();
});

it(`должен отрисовать точки c mode=EDIT, если пришли повреждения, в режиме ${ BODY_DAMAGES_MODE.EDIT }`, () => {
    const { getAllByRole, getByRole } = render(
        <VehicleBodyDamagesScheme
            bodyType={ Car_BodyType.SEDAN }
            mode={ BODY_DAMAGES_MODE.EDIT }
            damages={ damages }
        />,
    );

    const component = getByRole('figure');
    const dots = getAllByRole('button');

    const editedDots = dots.filter((dot) => dot.className.includes('DamageDot_mode_edit'));
    const addedDots = dots.filter((dot) => dot.className.includes('DamageDot_mode_add'));

    expect(editedDots).toHaveLength(damages.length);
    expect(addedDots).toHaveLength(DEFAULT_CAR_PARTS_LENGTH - damages.length);

    expect(component).not.toBeNull();
});

it(`должен отрисовать точки c mode=ADD, если нет повреждений, в режиме ${ BODY_DAMAGES_MODE.EDIT }`, () => {
    const { getAllByRole, getByRole } = render(
        <VehicleBodyDamagesScheme
            bodyType={ Car_BodyType.SEDAN }
            mode={ BODY_DAMAGES_MODE.EDIT }
        />,
    );

    const component = getByRole('figure');
    const dots = getAllByRole('button');

    expect(dots).toHaveLength(DEFAULT_CAR_PARTS_LENGTH);
    expect(component).not.toBeNull();
});

it('должен отрисовать обычный кузов без мапинга', () => {
    const { getByRole } = render(
        <VehicleBodyDamagesScheme
            bodyType={ Car_BodyType.SEDAN }
            damages={ damages }
        />,
    );

    const component = getByRole('figure');
    expect(component?.className).toContain('sedan');
});
