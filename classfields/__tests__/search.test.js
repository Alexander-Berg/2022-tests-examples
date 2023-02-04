const { geoTextBuild, categoryBuild } = require('../pages/search');

const { getDefaultGeoTextOpts, getDefaultCategoryOpts } = require('./mocks');

describe('geoText with sublocality', () => {
    let opts = getDefaultGeoTextOpts();

    beforeEach(() => {
        opts = getDefaultGeoTextOpts();
    });

    it('should return geo text with arrangement -> "sublocality locative"', () => {
        opts.refinements.subLocality.list.push({
            name: 'Академический',
            locative: 'в Академическом районе',
            id: '12446'
        });

        expect(geoTextBuild(opts)).toBe('- район Академический в Москве');
    });

    it('should return geo text with arrangement -> "county locative"', () => {
        opts.refinements.subLocality.list.push({
            id: '17385874',
            name: 'Коломенский округ',
            locative: 'в Коломенском городском округе'
        });

        expect(geoTextBuild(opts)).toBe('- округ Коломенский в Москве');
    });

    it('should return geo text without sublocality', () => {
        opts.refinements.subLocality.list.push({
            name: 'Академический',
            locative: 'в Академическом районе',
            id: '12446'
        }, {
            id: '17385878',
            name: 'Раменский округ',
            locative: 'в Раменском районе'
        });

        expect(geoTextBuild(opts)).toBe('в Москве');
    });
});

describe('categoryBuild', () => {
    let opts = getDefaultCategoryOpts();

    beforeEach(() => {
        opts = getDefaultCategoryOpts();
    });

    it('should return correct default value', () => {
        expect(categoryBuild(opts)).toBe('квартира');
    });

    it('should return correct value for apartments=NO', () => {
        opts.searchParams.apartments = 'NO';

        expect(categoryBuild(opts)).toBe('квартира');
    });

    it('should return correct value for apartments=YES', () => {
        opts.searchParams.apartments = 'YES';

        expect(categoryBuild(opts)).toBe('апартаменты');
    });
});
