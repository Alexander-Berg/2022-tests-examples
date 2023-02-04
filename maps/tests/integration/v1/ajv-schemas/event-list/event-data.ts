import {StoryButtonType, TypeLink} from 'app/types/consts';

const image = {
    type: 'object',
    additionalProperties: false,
    properties: {
        urlTemplate: {
            type: 'string'
        },
        caption: {
            type: 'string'
        },
        origUrl: {
            type: 'string'
        },
        width: {
            type: 'number'
        },
        height: {
            type: 'number'
        }
    },
    required: ['urlTemplate']
};

const images = {
    type: 'array',
    items: {
        ...image
    },
    minItems: 1
};

const features = {
    type: 'array',
    items: {
        type: 'object',
        additionalProperties: false,
        properties: {
            key: {
                type: 'string'
            },
            name: {
                type: 'string'
            },
            value: {
                type: 'string'
            },
            id: {
                type: 'string',
                nullable: true
            }
        },
        required: ['key', 'name', 'value']
    }
};

const tags = {
    type: 'string',
    nullable: true
};

const openUrlButton = {
    type: 'object',
    additionalProperties: false,
    properties: {
        type: {
            type: 'string',
            enum: [StoryButtonType.OPEN_URL]
        },
        typeLink: {
            type: 'string',
            enum: [...Object.values(TypeLink)]
        },
        tags,
        title: {
            type: 'string'
        },
        icon: {
            ...image
        },
        backgroundColor: {
            type: 'string'
        },
        titleColor: {
            type: 'string'
        },
        url: {
            type: 'string'
        }
    },
    required: ['type', 'typeLink', 'title', 'backgroundColor', 'titleColor', 'url']
};

const addBookmarkButton = {
    type: 'object',
    additionalProperties: false,
    properties: {
        type: {
            type: 'string',
            enum: [StoryButtonType.ADD_BOOKMARK]
        },
        tags,
        oid: {
            type: 'string'
        }
    },
    required: ['type', 'oid']
};

const addToCalendarButton = {
    type: 'object',
    additionalProperties: false,
    properties: {
        type: {
            type: 'string',
            enum: [StoryButtonType.ADD_TO_CALENDAR]
        },
        tags,
        title: {
            type: 'string'
        },
        startDate: {
            type: 'string'
        },
        endDate: {
            type: 'string',
            nullable: true
        }
    },
    required: ['type', 'title', 'startDate']
};

const defaultButton = {
    type: 'object',
    additionalProperties: false,
    properties: {
        type: {
            type: 'string',
            enum: [StoryButtonType.DEFAULT]
        },
        url: {
            type: 'string'
        },
        name: {
            type: 'string'
        }
    },
    required: ['type', 'url', 'name']
};

const eventData = {
    type: 'object',
    additionalProperties: false,
    properties: {
        title: {
            type: 'string'
        },
        description: {
            type: 'string'
        },
        rubric: {
            type: 'string',
            nullable: true
        },
        address: {
            type: 'string',
            nullable: true
        },
        buttons: {
            type: 'array',
            items: {
                oneOf: [openUrlButton, addBookmarkButton, addToCalendarButton, defaultButton]
            }
        },
        images,
        features
    },
    required: ['title', 'description', 'buttons']
};

export {eventData};
