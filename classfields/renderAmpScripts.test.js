const renderAmpScripts = require('./renderAmpScripts');

const state = {};
const pageTypesList = [
    'listing-amp',
    'card-amp',
    'catalog-amp-index',
    'catalog-amp-card',
];

describe(`renderAmpScripts правильно формирует AMP-скрипты`, () => {
    pageTypesList.forEach((pageType) => {
        it(`для pageType=${ pageType }`, () => {
            expect(renderAmpScripts(pageType, state)).toMatchSnapshot();
        });
    });

    it(`для неизвестного pageType`, () => {
        expect(renderAmpScripts('hehehehe', state)).toMatchSnapshot();
    });

    it(`если есть видео в relatedVideos`, () => {
        const stateMock = {
            relatedVideos: {
                data: [ {}, {} ],
            },
        };

        expect(renderAmpScripts('card-amp', stateMock)).toMatchSnapshot();
    });

    it(`если есть видео в video`, () => {
        const stateMock = {
            video: {
                items: [ {}, {} ],
            },
        };

        expect(renderAmpScripts('catalog-amp-card', stateMock)).toMatchSnapshot();
    });
});
