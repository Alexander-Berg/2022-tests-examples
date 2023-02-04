import {EventType} from 'app/types/db/events';
import eventMeta from './event-meta.json';
import {eventData} from './event-data';
import {optionalPoiData, poiData} from './poi-data';

const EVENT_LIST_SCHEMA = {
    type: 'object',
    additionalProperties: false,
    properties: {
        totalCount: {
            type: 'number'
        },
        events: {
            type: 'array',
            items: {
                type: 'object',
                properties: {
                    externalId: {
                        type: 'string'
                    },
                    geoRegionId: {
                        type: 'number'
                    },
                    startDate: {
                        type: 'string'
                    },
                    endDate: {
                        type: 'string'
                    },
                    type: {
                        type: 'string',
                        enum: [EventType.POI]
                    },
                    oid: {type: 'string'},
                    url: {type: 'string'},
                    zoomRange: {
                        type: 'object',
                        additionalProperties: false,
                        properties: {
                            min: {
                                type: 'number'
                            },
                            max: {
                                type: 'number'
                            }
                        },
                        required: ['min', 'max']
                    },
                    eventMeta,
                    eventData
                },
                required: [
                    'externalId',
                    'geoRegionId',
                    'startDate',
                    'endDate',
                    'type',
                    'zoomRange',
                    'eventMeta',
                    'eventData'
                ],
                if: {
                    properties: {
                        type: {const: EventType.POI}
                    }
                },
                then: {
                    anyOf: [
                        {
                            properties: {
                                oid: {type: 'string'},
                                poiData: optionalPoiData
                            }
                        },
                        {
                            properties: {
                                url: {type: 'string'},
                                poiData
                            }
                        }
                    ],
                    required: ['poiData']
                },
                else: {
                    properties: {
                        poiData: {
                            type: 'null'
                        }
                    }
                }
            }
        }
    },
    required: ['totalCount', 'events']
};

export {EVENT_LIST_SCHEMA};
