type TsearchMock1 = {
    offers: {
        pager: {
            totalItems: number;
        };
    };
};

type TsearchMock2 = {
    searchResults: {
        pager: {
            totalItems: number;
        };
    };
};

type TsearchMock3 = {
    pager: {
        totalItems: number;
    };
};

type TsearchMock4 = {
    search: {
        pager: {
            totalItems: number;
        };
    };
};

export type Tmock = {
    searchParams: {
        type: string;
    };
    pageParams: {
        type: string;
    };
    search: TsearchMock1 | TsearchMock2 | TsearchMock3 | TsearchMock4;
    seo: {
        ampUrl: string;
        canonicalUrl: string;
    };
    seoParams: Array<string>;
    req: {
        urlHelper: {
            getSeoUrl: (opts: Record<string, unknown>) => string | null;
        };
    };
    getParams: () => Record<string, unknown>;
    isAmpPage: boolean;
};
