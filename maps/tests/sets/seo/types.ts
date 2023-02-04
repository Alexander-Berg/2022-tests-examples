import {Tld} from '../../lib/func/url';

interface SeoBreadcrumb {
    name: string;
    url: string;
}

interface SeoAlternate {
    hreflang: string;
    href: string;
}

interface SeoCase {
    name: string;
    url: string;
    tld?: Tld;
    skip?: string;
    only?: boolean;
    og?: {
        title?: string;
        description?: string;
        image?: string;
    };
    breadcrumbList?: SeoBreadcrumb[];
    h1?: string;
    title?: string;
    description?: string;
    canonical?: string;
    canonicalBrowserPath?: string;
    alternates?: SeoAlternate[];
    noIndex?: boolean;
    jsonLd?: {
        type: string;
        value: object;
    };
    redirectUrl?: string;
    schemaVerifications?: ({
        selector: string;
    } & (
        | {
              value?: string | string[] | RegExp | RegExp[];
          }
        | {
              content?: string | string[] | RegExp | RegExp[];
          }
        | {
              amount: number;
          }
    ))[];
    mockVersion?: string;
    check404?: boolean;
}

interface SeoSet {
    name: string;
    skip?: string;
    only?: boolean;
    specs: SeoCase[];
}

type SeoFile = SeoCase | SeoSet;

export {SeoCase, SeoSet, SeoFile, SeoBreadcrumb};
