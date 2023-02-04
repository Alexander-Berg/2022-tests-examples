const gearInfo = require('./gearInfo');
const gearTypes = require('auto-core/data/gear_type.json');

Object.entries(gearTypes).map(
    ([ gearType, humanGearType ]) => {
        it(`должен вернуть корректную информацию о приводе "${ humanGearType }"`, () => {
            const complectation = {
                tech_info: {
                    tech_param: {
                        gear_type: gearType,
                    },
                },
            };

            expect(gearInfo(complectation)).toStrictEqual({
                title: humanGearType,
                value: gearType,
            });
        });
    },
);
