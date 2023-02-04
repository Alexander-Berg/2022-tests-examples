const preparer = require('./carSharing');

let isCardPage;

describe('карточка:', () => {
    beforeEach(() => {
        isCardPage = true;
    });

    it('status === ERROR: вернет правильный результат', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('если есть подозрение, поменяет статус на UNKNOWN', () => {
        const result = { report: { car_sharing: { car_sharing_records: 0, could_be_used_in_car_sharing: true } } };
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });
});

describe('отчет:', () => {
    beforeEach(() => {
        isCardPage = false;
    });

    it('status === ERROR: вернет правильный результат', () => {
        const item = { status: 'ERROR' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === UNKNOWN: вернет правильный результат', () => {
        const item = { status: 'UNKNOWN' };
        const data = preparer({ item, isCardPage });

        expect(data).toMatchSnapshot();
    });

    it('status === OK: вернет правильный результат', () => {
        const item = { status: 'OK' };
        const result = { report: { offer_id: '123' } };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });

    it('если есть подозрение, поменяет статус на UNKNOWN', () => {
        const result = { report: { car_sharing: { car_sharing_records: 0, could_be_used_in_car_sharing: true } } };
        const item = { status: 'OK' };
        const data = preparer({ item, isCardPage, result });

        expect(data).toMatchSnapshot();
    });
});
