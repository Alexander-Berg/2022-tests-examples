const transmissionInfo = require('./transmissionInfo');
const transmissionTypes = require('auto-core/data/catalog/transmission_types.json');

Object.entries(transmissionTypes).map(
    ([ transmissionType, transmissionData ]) => {
        it(`должен вернуть корректную информацию о трансмиссии "${ transmissionData.fullName }"`, () => {
            const complectation = {
                tech_info: {
                    tech_param: {
                        transmission: transmissionType,
                    },
                },
            };

            expect(transmissionInfo(complectation)).toStrictEqual({
                title: transmissionData.fullName,
                value: transmissionType,
            });
        });
    },
);
