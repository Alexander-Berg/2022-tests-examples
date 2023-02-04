const carInfo = require('./carInfo');

describe('equipment', () => {
    it('должен заполнить опции из черновика поффера', () => {
        const publicApiFields = {};
        const pofferApiFields = {
            options: { usb: '1', abs: '1' },
        };
        carInfo(publicApiFields, pofferApiFields);

        expect(publicApiFields).toHaveProperty('car_info.equipment', {
            abs: true,
            gbo: false,
            usb: true,
        });
    });
});
