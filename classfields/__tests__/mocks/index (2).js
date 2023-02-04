const router = require('realty-router');
const Url = require('realty-core/app/lib/url');

const regionsData = require('../data/regions');
const treeData = require('../data/tree');
const streetsData = require('../data/streets');
const sitesData = require('../data/sites');
const developerData = require('../data/developer');
const railwayData = require('../data/railway');
const checkLocationData = require('../data/checkLocation');
const geoSuggestData = require('../data/geoSuggest');
const offerData = require('../data/offer');

import { PROFILE_DATA } from '../data/profiles';
import { GET_PROFILE } from '../data/getProfile';

const metroAPIMock = () => ({
    getStationById: jest.fn(metroGeoId => require('../data/metro')[metroGeoId]),
    getStationsIdsByCityId: jest.fn(geoId => {
        if (! [ 213, 65, 54, 51, 47, 43, 2 ].includes(geoId)) {
            return [];
        }

        return [ 20394 ];
    })
});

const callRealty3Resource = jest.fn((path, params) => {
    let data;

    switch (path) {
        case 'street-geo': {
            data = streetsData[params.streetId];
            break;
        }
        case 'sites.get_site': {
            data = sitesData[params.siteId];
            break;
        }
        case 'developers.get_developer': {
            data = developerData[params.developerId]?.[params.rgid];
            break;
        }
        case 'profiles.search-profile': {
            data = PROFILE_DATA[params.rgid];
            break;
        }
        case 'profiles.get_profile': {
            data = GET_PROFILE[params.uid]?.[params.rgid];
            break;
        }
        case 'seo-links.get_railway_geo': {
            data = railwayData[params.railwayStation];
            break;
        }
        case 'geo.geosuggest': {
            data = geoSuggestData[params.text];
            break;
        }
        case 'offer-card.getUnprocessedCard': {
            if (offerData[params.id].status === 'expired') {
                throw {
                    code: 410,
                    data: {
                        body: [ Buffer.from(JSON.stringify(offerData[params.id].redirectData)) ],
                        statusCode: 410
                    }
                };
            }
            data = offerData[params.id];
            break;
        }
        default: break;
    }

    if (! data) {
        // eslint-disable-next-line no-console
        console.error(path, params);
        throw {
            code: 'HTTP_NOT_FOUND',
            message: `Нет мока данных для ${path}, ${params}`
        };
    }

    return data;
});

const callResource = jest.fn((path, params) => {
    let data;

    switch (path) {
        case 'geobase.region_by_id':
        case 'region_info': {
            const geoId = params.geoid || params.id;

            if (geoId) {
                data = Object.values(regionsData)
                    .find(geo => Number(geo.id) === Number(geoId)) || {};
                break;
            }

            data = regionsData[params.rgid] || {};
            break;
        }
        case 'region_tree': {
            data = treeData[params.rgid];
            break;
        }
        case 'check_location.get': {
            data = checkLocationData[params.address || params.buildingId];
            break;
        }
        default: break;
    }

    if (! data) {
        // eslint-disable-next-line no-console
        console.error(path, params);
        throw {
            code: 'HTTP_NOT_FOUND',
            message: `Нет мока данных для ${path}, ${params}`
        };
    }

    return data;
});

class Response {}

class Request {
    constructor(originalUrl) {
        this.originalUrl = originalUrl;

        this.callRealty3Resource = callRealty3Resource.bind(this);
        this.callResource = callResource.bind(this);
        this.query = {};

        this.urlHelper = new Url({
            url: 'https://realty.yandex.ru' + originalUrl,
            routers: router,
            viewType: 'desktop'
        });
    }
}

const nextFunction = jest.fn();

module.exports = {
    Response,
    Request,
    nextFunction,
    metroAPIMock
};
