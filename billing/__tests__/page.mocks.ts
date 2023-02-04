import { HOST } from 'common/utils/test-utils/common';

import {
    lastClosedPeriodsResponse,
    personsFirmsResponse,
    requestResponse,
    requestsResponse
} from './page.data';

export const mocks = {
    client: {
        request: {
            url: `${HOST}/client`,
            data: {}
        },
        response: {
            id: 123
        }
    },
    anotherClient: {
        request: {
            url: `${HOST}/client`,
            data: {
                client_id: 234
            }
        },
        response: {
            id: 234
        }
    },
    availability: {
        request: {
            url: `${HOST}/reconciliation/availability`,
            data: {}
        },
        response: {
            isReconciliationReportsAvailable: false,
            isSettlementsAvailable: true
        }
    },
    lastClosedPeriods: {
        request: {
            url: `${HOST}/yadoc/last-closed-periods`,
            data: {}
        },
        response: lastClosedPeriodsResponse
    },
    lastClosedPeriodsForAnotherClient: {
        request: {
            url: `${HOST}/yadoc/last-closed-periods`,
            data: {
                client_id: 234
            }
        },
        response: lastClosedPeriodsResponse
    },
    availabilityForAnotherClient: {
        request: {
            url: `${HOST}/reconciliation/availability`,
            data: {
                client_id: 234
            }
        },
        response: {
            isReconciliationReportsAvailable: false,
            isSettlementsAvailable: true
        }
    },
    personsFirms: {
        request: {
            url: `${HOST}/reconciliation/person-firms`,
            data: {}
        },
        response: personsFirmsResponse
    },
    personsFirmsForAnotherClient: {
        request: {
            url: `${HOST}/reconciliation/person-firms`,
            data: {
                client_id: 234
            }
        },
        response: personsFirmsResponse
    },
    emptyRequests: {
        request: {
            url: `${HOST}/reconciliation/requests`,
            data: {
                client_id: 123,
                pagination_pn: 1,
                pagination_ps: 10,
                hidden: false,
                sort_key: 'DT',
                sort_order: 'ASC'
            }
        },
        response: {
            items: [],
            totalCount: 0
        }
    },
    requests: {
        request: {
            url: `${HOST}/reconciliation/requests`,
            data: {
                client_id: 123,
                pagination_pn: 1,
                pagination_ps: 10,
                hidden: false,
                sort_key: 'DT',
                sort_order: 'ASC'
            }
        },
        response: requestsResponse
    },
    requestsForAnotherClient: {
        request: {
            url: `${HOST}/reconciliation/requests`,
            data: {
                client_id: 234,
                pagination_pn: 1,
                pagination_ps: 10,
                hidden: false,
                sort_key: 'DT',
                sort_order: 'ASC'
            }
        },
        response: requestsResponse
    },
    createRequest: {
        request: {
            url: `${HOST}/reconciliation/create-request`,
            data: {
                client_id: 123,
                email: 'asd@asd.asd',
                dt_from: '2022-05-14T00:00:00',
                dt_to: '2022-05-15T00:00:00',
                firm_id: 123,
                person_id: 345,
                contract_id: 234
            }
        },
        response: requestResponse
    },
    createRequestWithoutContract: {
        request: {
            url: `${HOST}/reconciliation/create-request`,
            data: {
                client_id: 123,
                email: 'asd@asd.asd',
                dt_from: '2022-05-14T00:00:00',
                dt_to: '2022-05-15T00:00:00',
                firm_id: 234,
                person_id: 345
            }
        },
        response: requestResponse
    },
    createRequestWithError: {
        request: {
            url: `${HOST}/reconciliation/create-request`,
            data: {
                client_id: 123,
                email: 'asd@asd.asd',
                dt_from: '2022-05-14T00:00:00',
                dt_to: '2022-05-15T00:00:00',
                firm_id: 123,
                person_id: 345,
                contract_id: 234
            }
        },
        error: {
            data: {
                error: 'SOME_ERROR',
                description: 'some error occured'
            }
        }
    },
    hideRequest: {
        request: {
            url: `${HOST}/reconciliation/hide-request`,
            data: {
                reconciliation_request_id: 1
            }
        },
        response: requestResponse
    },
    hideRequestWithError: {
        request: {
            url: `${HOST}/reconciliation/hide-request`,
            data: {
                reconciliation_request_id: 1
            }
        },
        error: {
            data: {
                error: 'SOME_ERROR',
                description: 'some error occured'
            }
        }
    },
    cloneRequest: {
        request: {
            url: `${HOST}/reconciliation/clone-request`,
            data: {
                reconciliation_request_id: 1
            }
        },
        response: requestResponse
    },
    cloneRequestWithError: {
        request: {
            url: `${HOST}/reconciliation/clone-request`,
            data: {
                reconciliation_request_id: 1
            }
        },
        error: {
            data: {
                error: 'SOME_ERROR',
                description: 'some error occured'
            }
        }
    }
};
