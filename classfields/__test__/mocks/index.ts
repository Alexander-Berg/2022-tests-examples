import { FlatStatus } from 'types/flat';
import { RentContractStatus } from 'types/contract';

import { Fields } from 'app/libs/search-filter/manager-flats-search-filter/types';

import { ISearchFilterBody } from 'app/libs/search-filter/types';

export const filterByFlatStatus: ISearchFilterBody<Fields> = {
    searchFilter: {
        in: {
            field: Fields.OWNER_REQUEST_STATUS,
            value: [{ string: FlatStatus.RENTED }, { string: FlatStatus.DRAFT }],
        },
    },
};

export const filterByStatusWithQuery: ISearchFilterBody<Fields> = {
    searchFilter: {
        and: {
            operands: [
                {
                    in: {
                        field: Fields.OWNER_REQUEST_STATUS,
                        value: [{ string: FlatStatus.RENTED }, { string: FlatStatus.DRAFT }],
                    },
                },
                {
                    equal: {
                        field: Fields.QUERY,
                        value: {
                            string: 'куше',
                        },
                    },
                },
            ],
        },
    },
};

export const filterBySigningWindow: ISearchFilterBody<Fields> = {
    searchFilter: {
        and: {
            operands: [
                {
                    in: {
                        field: Fields.OWNER_REQUEST_STATUS,
                        value: [{ string: FlatStatus.RENTED }],
                    },
                },
                {
                    between: {
                        field: Fields.ACTIVE_CONTRACT_RENT_START_DATE,
                        from: { string: '2022-01-22T00:00:00+03:00' },
                        to: { string: '2022-01-24T00:00:00+03:00' },
                    },
                },
            ],
        },
    },
};

export const filterByContractStatus: ISearchFilterBody<Fields> = {
    searchFilter: {
        in: {
            field: Fields.ACTIVE_CONTRACT_STATUS,
            value: [{ string: RentContractStatus.SIGNED }, { string: RentContractStatus.ACTIVE }],
        },
    },
};

export const commonFilter: ISearchFilterBody<Fields> = {
    searchFilter: {
        and: {
            operands: [
                {
                    in: {
                        field: Fields.OWNER_REQUEST_STATUS,
                        value: [
                            {
                                string: FlatStatus.RENTED,
                            },
                        ],
                    },
                },
                {
                    equal: {
                        field: Fields.OWNER_REQUEST_STATUS,
                        value: {
                            string: FlatStatus.RENTED,
                        },
                    },
                },
                {
                    match: {
                        field: Fields.OWNER_REQUEST_STATUS,
                        value: FlatStatus.RENTED,
                    },
                },
                {
                    lt: {
                        field: Fields.NEED_VIRTUAL_TOUR_LINK,
                        value: {
                            boolean: true,
                        },
                        orEquals: false,
                    },
                },
                {
                    gt: {
                        field: Fields.NEED_COPYRIGHTER_REVIEW,
                        value: {
                            boolean: true,
                        },
                        orEquals: false,
                    },
                },
                {
                    wildcard: {
                        field: Fields.ACTIVE_CONTRACT_STATUS,
                        value: RentContractStatus.SIGNED,
                    },
                },
                {
                    between: {
                        field: Fields.ACTIVE_CONTRACT_RENT_START_DATE,
                        from: {
                            string: 'date_value',
                        },
                        includeFrom: false,
                        to: {
                            string: 'date_value',
                        },
                        includeTo: false,
                    },
                },
                {
                    and: {
                        operands: [
                            {
                                equal: {
                                    field: Fields.QUERY,
                                    value: {
                                        string: 'value',
                                    },
                                },
                            },
                        ],
                    },
                },
                {
                    or: {
                        operands: [
                            {
                                equal: {
                                    field: Fields.QUERY,
                                    value: {
                                        string: 'value',
                                    },
                                },
                            },
                        ],
                    },
                },
                {
                    not: {
                        equal: {
                            field: Fields.QUERY,
                            value: {
                                string: 'not',
                            },
                        },
                    },
                },
                {
                    exist: {
                        field: Fields.FLAT_ID,
                        filter: {
                            and: {
                                operands: [
                                    {
                                        equal: {
                                            field: Fields.FLAT_ID,
                                            value: {
                                                string: 'flat_id',
                                            },
                                        },
                                    },
                                    {
                                        equal: {
                                            field: Fields.CODE,
                                            value: {
                                                string: 'code',
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    },
                },
            ],
        },
    },
};
