import * as fs from 'fs';
import * as Ajv from 'ajv';
import * as got from 'got';
import * as jspath from 'jspath';

const apiSpec = JSON.parse(fs.readFileSync('out/generated/spec.json', 'utf-8'));

const validator = new Ajv();

export type ResponseSchemaValidator = (response: got.Response<any>) => void;

export interface Options {
    path: string;
    method?: string;
}

export function createResponseSchemaValidator({path, method = 'get'}: Readonly<Options>): ResponseSchemaValidator {
    const operationPath = `.paths."${path}".${method}`;
    const operationSchema = jspath.apply(operationPath, apiSpec)[0];
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
