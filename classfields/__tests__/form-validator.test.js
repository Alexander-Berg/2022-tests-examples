const { errorCodes } = require('tv4');
const { parseErrors } = require('../index');

describe('Account form error parser', () => {
    it('should return dictionary with field name as key and error message as value', () => {
        const phrases = {
            required: 'required msg',
            pattern: 'pattern msg',
            format: 'format msg',
            minLength: 'minLength msg',
            common: 'default error msg'
        };

        const translater = key => phrases[key];

        const parsedErrors = parseErrors([
            {
                code: errorCodes.INVALID_TYPE,
                dataPath: '/requiredField1'
            },
            {
                code: errorCodes.NOT_PASSED,
                dataPath: '/requiredField2'
            },
            {
                code: errorCodes.OBJECT_REQUIRED,
                dataPath: '/requiredField3'
            },
            {
                code: errorCodes.OBJECT_ADDITIONAL_PROPERTIES,
                dataPath: '/requiredField4'
            },
            {
                code: errorCodes.STRING_PATTERN,
                dataPath: '/patternField'
            },
            {
                code: errorCodes.FORMAT_CUSTOM,
                dataPath: '/formatField1'
            },
            {
                code: errorCodes.ARRAY_LENGTH_SHORT,
                dataPath: '/minimumLengthField',
                params: {
                    minimum: 12
                }
            },
            {
                code: 111,
                dataPath: '/someOtherInvalidField'
            }
        ], translater);

        expect(parsedErrors).toEqual({
            requiredField1: 'required msg',
            requiredField2: 'required msg',
            requiredField3: 'required msg',
            requiredField4: 'required msg',
            patternField: 'pattern msg',
            formatField1: 'format msg',
            minimumLengthField: 'minLength msg',
            someOtherInvalidField: 'default error msg'
        });
    });

    it('set specific field name error message if it specified', () => {
        const phrases = {
            required: 'required msg',
            required_someField: 'specific required msg for the someField'
        };

        const translater = key => phrases[key];

        const parsedErrors = parseErrors([ {
            code: errorCodes.INVALID_TYPE,
            dataPath: '/someField'
        } ], translater);

        expect(parsedErrors).toEqual({
            someField: 'specific required msg for the someField'
        });
    });

    it('call translater for minLenght error with min value params', () => {
        const translater = (key, params) => `${key} error;  min params is ${params.smart_count}`;

        const parsedErrors = parseErrors([ {
            code: errorCodes.ARRAY_LENGTH_SHORT,
            dataPath: '/someField',
            params: {
                minimum: 12
            }
        } ], translater);

        expect(parsedErrors).toEqual({
            someField: 'minLength_someField error;  min params is 12'
        });
    });
});
