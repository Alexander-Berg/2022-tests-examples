jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getColorTags');
jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getEngineTags');
jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getGearTags');
jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getBodyTags');
jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getGenerationTags');
jest.mock('auto-core/react/dataDomain/crossLinks/helpers/getTransmissionTags');

const tagsState = require('auto-core/react/dataDomain/crossLinks/helpers/mock/tagsState.mock');

const getColorTags = require('auto-core/react/dataDomain/crossLinks/helpers/getColorTags');
const getEngineTags = require('auto-core/react/dataDomain/crossLinks/helpers/getEngineTags');
const getGearTags = require('auto-core/react/dataDomain/crossLinks/helpers/getGearTags');
const getBodyTags = require('auto-core/react/dataDomain/crossLinks/helpers/getBodyTags');
const getGenerationTags = require('auto-core/react/dataDomain/crossLinks/helpers/getGenerationTags');
const getTransmissionTags = require('auto-core/react/dataDomain/crossLinks/helpers/getTransmissionTags').default;

const getListingCrossLinksData = require('./getListingCrossLinksData');

// проверяем только, что отдается в searchParameters и передается в хелперы
// так как остальное уже проверено в отдельных тестах хелперов
it('должен вернуть в searchParameters только catalog_filter, category и section, а в хелперы отдавать полные параметры', () => {
    const searchParameters = {
        body_type_groups: [
            'HATCHBACK_5_DOORS',
            'LIFTBACK',
            'HATCHBACK_3_DOORS',
            'HATCHBACK',
        ],
        catalog_filter: [
            { mark: 'AUDI', model: 'A6' },
        ],
        category: 'cars',
        section: 'all',
        price_to: 10000,
    };

    const state = {
        ...tagsState(),
        listing: {
            data: {
                search_parameters: searchParameters,
            },
        },
        geo: { gids: [ 213 ] },
    };

    expect(getListingCrossLinksData(state).searchParameters).toEqual({
        catalog_filter: [
            { mark: 'AUDI', model: 'A6' },
        ],
        category: 'cars',
        section: 'all',
    });
    expect(getEngineTags.mock.calls[0][0]).toEqual(searchParameters);
    expect(getColorTags.mock.calls[0][0]).toEqual(searchParameters);
    expect(getGearTags.mock.calls[0][0]).toEqual(searchParameters);
    expect(getBodyTags.mock.calls[0][0]).toEqual(searchParameters);
    expect(getGenerationTags.mock.calls[0][0]).toEqual(searchParameters);
    expect(getTransmissionTags.mock.calls[0][0]).toEqual(searchParameters);
});
