const getComplaintsListByOffer = require('./getComplaintsListByOffer');
const COMPLAINTS = require('auto-core/data/complaints.json');

it('Возвращает списк жалоб для объявления частника', () => {
    const result = getComplaintsListByOffer({
        category: 'cars',
        seller_type: 'PRIVATE',
    });

    expect(result).toEqual(COMPLAINTS);
});

it('Возвращает списк жалоб для объявления дилера', () => {
    const result = getComplaintsListByOffer({
        category: 'trucks',
        sub_category: 'municipal',
        seller_type: 'COMMERCIAL',
    });

    expect(result).toEqual(COMPLAINTS.filter((item) => item.forClient));
});
