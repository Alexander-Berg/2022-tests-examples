import cssSelectors from '../../../common/css-selectors';
import counterGenerator, {CaseSpec} from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

interface Urls {
    business: string;
    org: string;
    poi: string;
}

function getUrls(id: string, queryParams: Record<string, string> = {}) {
    const query = Object.entries(queryParams)
        .map(([key, value]) => `${key}=${value}`)
        .join('&');
    return {
        business: `?ol=biz&oid=${id}${query ? '&' + query : ''}`,
        org: `/org/${id}/?${query}`,
        poi: `?poi[uri]=ymapsbm1://org?oid=${id}`
    };
}

interface SpecParams {
    regexpStr: string;
    description: string;
    url: string;
    selector: string;
    isMobile?: boolean;
}

function getClickCaseSpec({regexpStr, description, url, selector, isMobile}: SpecParams): CaseSpec {
    return {
        name: new RegExp(regexpStr),
        description,
        url,
        selector,
        isMobile: Boolean(isMobile),
        events: [{type: 'click'}]
    };
}

interface SpecsParams {
    name: string;
    urls: Urls;
    regexpStr: string;
    selector: string;
    skip1org?: boolean;
}

function getClickSpecs({name, urls, regexpStr, selector, skip1org}: SpecsParams): CaseSpec[] {
    const base = {regexpStr, selector};
    const specs = [
        getClickCaseSpec({...base, description: `Организация. ${name}.`, url: urls.business}),
        getClickCaseSpec({...base, description: `ТАЧ. Организация. ${name}.`, url: urls.org, isMobile: true}),
        getClickCaseSpec({...base, description: `POI. Организация. ${name}.`, url: urls.poi})
    ];
    if (!skip1org) {
        specs.unshift(getClickCaseSpec({...base, description: `1орг. ${name}.`, url: urls.org}));
    }
    return specs;
}

counterGenerator({
    name: 'Сторис.',
    isMainMetric: true,
    specs: [
        // Клик в сторис из карточки
        ...getClickSpecs({
            name: 'Блок сторис',
            urls: getUrls('237523254531'),
            regexpStr: ANALYTIC_NAMES.storyOpen.regexp,
            selector: cssSelectors.businessStories.previewNth.replace('%i', '1')
        }),
        // Клик в последнюю милю без панорамы
        ...getClickSpecs({
            name: 'Блок "Последняя миля" без панорамы',
            urls: getUrls('208998706796'),
            regexpStr: ANALYTIC_NAMES.lastMileStory.regexp,
            selector: cssSelectors.cardEntranceStory.button,
            skip1org: true
        }),
        // Клик в последнюю милю на карте
        ...getClickSpecs({
            name: 'Блок "Последняя миля" на карте',
            urls: getUrls('208998706796', {ll: '37.488485,55.676775', z: '19', 'view-state': 'mini'}),
            regexpStr: ANALYTIC_NAMES.lastMileStory.regexp,
            selector: cssSelectors.businessEntranceStory.button
        })
    ]
});
