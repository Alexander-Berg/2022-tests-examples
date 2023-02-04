const { listingBreadcrumbs, cardBreadcrumbs } = require('realty-core/app/providers/breadcrumbs/');
const desktopListingData = require('./mocks.desktop.listing');
const desktopOfferData = require('./mocks.desktop.offer');

const touchListingData = require('./mocks.touch.listing');
const touchOfferData = require('./mocks.touch.offer');

const routerDesktop = require('realty-router/desktop');
const routerTouch = require('realty-router/touch-phone');

const getData = (controller, platform = 'desktop') => ({
    type: controller,
    req: {
        urlHelper: {
            link: (type, params) => {
                const route = platform === 'desktop' ? routerDesktop.getRouteByName(type) :
                    routerTouch.getRouteByName(type);

                return route.build(params);
            },
            viewType: () => platform
        }
    }
});

const getBreadcrumbsData = result => {
    return result?.breadcrumbs.reduce((acc, { link, title }) => {
        return {
            ...acc,
            links: [ ...acc.links, link ],
            titles: [ ...acc.titles, title ]
        };
    }, { links: [], titles: [] });
};

describe('Breadcrumbs listing desktop', () => {
    desktopListingData.map(({ name, controller, titles, links, data }) => {
        it(name, async() => {
            const context = getData(controller, 'desktop');

            let breadcrumbs = {
                links: [],
                titles: []
            };

            try {
                const result = await listingBreadcrumbs.call(context, data);

                breadcrumbs = getBreadcrumbsData(result);
            } catch (e) {}

            expect(breadcrumbs.titles).toStrictEqual(titles);
            expect(breadcrumbs.links).toStrictEqual(links);
        });
    });
});

describe('Breadcrumbs offer desktop', () => {
    desktopOfferData.map(({ name, controller, titles, links, data }) => {
        it(name, async() => {
            const context = getData(controller, 'desktop');

            let breadcrumbs = {
                links: [],
                titles: []
            };

            try {
                const result = await cardBreadcrumbs.call(context, data);

                breadcrumbs = getBreadcrumbsData(result);
            } catch (e) {}

            expect(breadcrumbs.titles).toStrictEqual(titles);
            expect(breadcrumbs.links).toStrictEqual(links);
        });
    });
});

describe('Breadcrumbs listing touch', () => {
    touchListingData.map(({ name, controller, titles, links, data }) => {
        it(name, async() => {
            const context = getData(controller, 'touch-phone');

            let breadcrumbs = {
                links: [],
                titles: []
            };

            try {
                const result = await listingBreadcrumbs.call(context, data);

                breadcrumbs = getBreadcrumbsData(result);
            } catch (e) {}

            expect(breadcrumbs.titles).toStrictEqual(titles);
            expect(breadcrumbs.links).toStrictEqual(links);
        });
    });
});

describe('Breadcrumbs offer touch', () => {
    touchOfferData.map(({ name, controller, titles, links, data }) => {
        it(name, async() => {
            const context = getData(controller, 'touch-phone');

            let breadcrumbs = {
                links: [],
                titles: []
            };

            try {
                const result = await cardBreadcrumbs.call(context, data);

                breadcrumbs = getBreadcrumbsData(result);
            } catch (e) {}

            expect(breadcrumbs.titles).toStrictEqual(titles);
            expect(breadcrumbs.links).toStrictEqual(links);
        });
    });
});
