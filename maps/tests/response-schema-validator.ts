import * as fs from 'fs';
import Ajv from 'ajv';
import * as jspath from 'jspath';
import * as got from 'got';

const openApiSpec = JSON.parse(fs.readFileSync('out/generated/spec.json', 'utf8'));

const validator = new Ajv();

interface Options {
    path: string;
    method?: 'get' | 'post' | 'put' | 'patch' | 'delete' | 'options';
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type ResponseSchemaValidator = (response: got.Response<any>) => void;

function createResponseSchemaValidator(options: Options): ResponseSchemaValidator {
    const {path, method = 'get'} = options;

    const operationPath = `.paths."${path}".${method}`;
    const operationSchema = jspath.apply(operationPath, openApiSpec)[0];
    if (!operationSchema) {
        throw new Error(`Operation for path ${operationPath} not found in specification`);
    }

    return (response) => {
        // Find response schema by response status code.
        const responsePath = `.responses."${response.statusCode}".content."application/json".schema`;
        const responseSchema = jspath.apply(responsePath, operationSchema)[0];
        if (!responseSchema) {
            throw new Error(`Response schema for path ${operationPath}${responsePath} not found in specification`);
        }

        const isSchemaValid = validator.validateSchema(responseSchema);
        if (!isSchemaValid) {
            const errorDetails = JSON.stringify(validator.errors, null, 4);
            throw new Error(`Response schema ${operationPath}${responsePath} is invalid: \n${errorDetails}`);
        }

        const isResponseValid = validator.validate(responseSchema, response.body);
        if (!isResponseValid) {
            const errorDetails = JSON.stringify(validator.errors, null, 4);
            throw new Error(`Response body does not match JSON schema: \n${errorDetails}`);
        }
    };
}

export {createResponseSchemaValidator};
