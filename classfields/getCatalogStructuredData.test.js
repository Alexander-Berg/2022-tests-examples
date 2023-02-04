const getCatalogStructuredData = require('./getCatalogStructuredData');
const stateMock = require('../../../../../mocks/ampCatalogStore.mock');
const no = require('nommon');

it('Должен отдать структурированные данные для амп-каталога', () => {
    const state = {
        seoParams: no.jpath('.seo.seoParams', stateMock) || {},
        ratings: no.jpath('.averageRating.ratings', stateMock) || [],
        reviewCountValue: no.jpath('.seo.seoParams.reviewCount', stateMock) || 0,
        catalogComplectations: no.jpath('.catalogComplectations.list', stateMock) || [],
        image: no.jpath('.seo.image', stateMock) || '',
        catalogOffers: no.jpath('.catalogGeneration.relatedOffers.offers', stateMock) || [],
        state: stateMock || {},
    };
    expect(getCatalogStructuredData(state)).toMatchSnapshot();
});

it('Должен отдать структурированные данные без разметки отзывов для амп-каталога без отзывов', () => {
    const state = {
        seoParams: no.jpath('.seo.seoParams', stateMock) || {},
        ratings: no.jpath('.averageRating.ratings', stateMock) || [],
        reviewCountValue: 0,
        catalogComplectations: no.jpath('.catalogComplectations.list', stateMock) || [],
        image: no.jpath('.seo.image', stateMock) || '',
        catalogOffers: no.jpath('.catalogGeneration.relatedOffers.offers', stateMock) || [],
        state: stateMock || {},
    };
    expect(getCatalogStructuredData(state)).toMatchSnapshot();
});
