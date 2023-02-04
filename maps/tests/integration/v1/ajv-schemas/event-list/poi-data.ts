const optionalPoiData = {
    type: 'object',
    additionalProperties: false,
    properties: {
        coordinate: {
            type: 'object',
            additionalProperties: false,
            properties: {
                lon: {
                    type: 'number',
                    minimum: -180,
                    maximum: 180
                },
                lat: {
                    type: 'number',
                    minimum: -90,
                    maximum: 90
                }
            },
            required: ['lon', 'lat']
        },
        label: {
            type: 'object',
            additionalProperties: false,
            properties: {
                title: {
                    type: 'string'
                },
                subtitle: {
                    type: 'string',
                    nullable: true
                }
            },
            required: ['title']
        },
        subtitle: {
            type: 'string'
        },
        iconTags: {
            type: 'array',
            items: {
                type: 'string'
            },
            nullable: true
        }
    },
    required: ['coordinate', 'label', 'subtitle']
};

const poiData = {
    ...optionalPoiData,
    properties: {
        ...optionalPoiData.properties,
        iconTags: {
            ...optionalPoiData.properties.iconTags,
            minItems: 1
        }
    },
    required: [...optionalPoiData.required, 'iconTags']
};

export {optionalPoiData, poiData};
