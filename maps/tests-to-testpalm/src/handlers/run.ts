import {promises as fs} from 'fs';
import * as path from 'path';
import {DiffPatcher} from 'jsondiffpatch';
import TestpalmApi, {
    TestCase,
    TestCaseStatus,
    TestCaseStep,
    TestCaseProperty,
    TestCaseAttributes,
    Definition
} from '@yandex-int/testpalm-api';
import PuppeteerTestsParser from '../helpers/parsers/puppeteer-tests-parser';
import HermioneTestsParser from '../helpers/parsers/hermione-tests-parser';
import HermioneTestsRunner from '../helpers/runners/hermione-tests-runner';
import PuppeteerTestsRunner from '../helpers/runners/puppeteer-tests-runner';
import {updateSelectorsDescription} from '../helpers/update-selectors-description';
import {updateScreenshotPaths} from '../helpers/update-screenshot-paths';
import {generateHtmlReport} from '../helpers/generate-html-report';
import * as colors from '../utils/colors';
import {LocalTestCase, Config, Mode, RunParams, TestsGlobal, Engine} from '../types';

require('ts-node').register({
    transpileOnly: true,
    project: path.join(process.cwd(), process.env.TS_NODE_PROJECT || 'tsconfig.json')
});

interface HelpersParams extends Config {
    mode: Mode;
    attributesMapping: Record<string, string>;
    unskipTickets: string[];
    definitions: Definition[];
}

interface TestsUpdateResult {
    added: LocalTestCase[];
    updated: LocalTestCase[];
    deleted: LocalTestCase[];
    beforeUpdate: LocalTestCase[];
    selectorsWithoutDescription: Set<string>;
}

const ATTRIBUTES_TO_REWRITE = ['skip', 'selectorsDecrypted', 'needScreenshot'];

async function run(params: RunParams): Promise<void> {
    const config: Config = (await import(params.configPath)).default;

    if (!config.testPalm.token) {
        throw new Error(
            'No `testPalm.token` specified.\n' +
                'You can get token via next link: https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6d967b191847496a8a7077e2e636142f'
        );
    }
    const testGlobal = (global as unknown) as TestsGlobal;
    const TestsParser = config.engine === Engine.PUPPETEER ? PuppeteerTestsParser : HermioneTestsParser;
    const parser = new TestsParser(testGlobal, config);
    await parser.parse();

    const TestsRunner = config.engine === Engine.PUPPETEER ? PuppeteerTestsRunner : HermioneTestsRunner;
    const runner = new TestsRunner(testGlobal, config);

    const isValidation = params.mode === 'validation';
    const {localTestCases, parsingErrors} = await runner.run(parser.getTestCases(), isValidation);
    if (isValidation) {
        if (parsingErrors.length > 0) {
            printValidationResult(parsingErrors);
            throw new Error('Validation failed.');
        }

        return;
    }

    const testPalmClient = new TestpalmApi(config.testPalm.token);
    const definitions = await testPalmClient.getDefinitions(config.testPalm.projectId);
    const attributesMapping = definitions.reduce((attrs, {title, id}) => ({...attrs, [title]: id}), {});
    const unskipTickets = parser.getUnskipTickets();

    const {added, updated, deleted, beforeUpdate, selectorsWithoutDescription} = await updateTestPalmProject(
        testPalmClient,
        {
            ...config,
            mode: params.mode,
            attributesMapping,
            unskipTickets,
            definitions
        },
        localTestCases
    );
    const commandsWithoutDescription = runner.getCommandsWithoutDescription();
    const commandsWithoutEstimate = runner.getCommandsWithoutEstimate();

    console.log(colors.green(`${added.length} tests cases were added.`));
    console.log(colors.yellow(`${updated.length} tests cases were updated.`));
    console.log(colors.red(`${deleted.length} tests cases were marked as archived.`));
    const jsonReport = {added, updated, deleted, beforeUpdate};

    await fs.mkdir(config.report.folder, {recursive: true});
    await fs.writeFile(
        path.join(config.report.folder, `${config.report.name}.json`),
        JSON.stringify(jsonReport, null, 4)
    );

    await fs.writeFile(
        path.join(config.report.folder, `${config.report.name}.html`),
        generateHtmlReport(jsonReport, {
            projectId: config.testPalm.projectId,
            reportName: config.report.name,
            attributesMapping
        })
    );

    printWarningList(commandsWithoutDescription, 'Commands without description');
    printWarningList(commandsWithoutEstimate, 'Commands without estimate');
    printWarningList(selectorsWithoutDescription, 'Selectors without description');
}

async function updateTestPalmProject(
    testPalmClient: TestpalmApi,
    params: HelpersParams,
    localTestCases: LocalTestCase[]
): Promise<TestsUpdateResult> {
    const updated: TestCase[] = [];
    const added: LocalTestCase[] = [];
    // В этот массив попадут кейсы, которые уже есть в пальме и не были обновлены.
    // Это необходимо для того чтобы зафорсить обновление разметки кейса в пальме,
    // если она была сломана через интерфейс.
    // https://st.yandex-team.ru/TESTPALM-2329
    const untouched: TestCase[] = [];

    // В этот массив попадут копии кейсов из пальмы, обновляемых в ходе текущей таски
    // Это необходимо для того, чтобы обновить у кейсов только ключ,
    // когда происходит выгрузка из pull-request
    // https://st.yandex-team.ru/MAPSUI-20391
    const beforeUpdate: TestCase[] = [];

    // Данные параметры задают критерии сравнения массивов, содержащих объекты.
    // objectHash - определяет название поля, по значениям которого сравниваются объекты.
    // propertyFilter - определяет какие поля будут проигнорированы.

    // Экземпляр для сравнения основных свойств тесткейса: name, stepsExpects.
    const stepsDiffPatcher = new DiffPatcher({
        objectHash: (obj: TestCaseStep) => obj.step,
        propertyFilter: (name: string) => name !== 'stepFormatted' && name !== '_id'
    });

    // Экземпляр для сравнения меты тесткейса: attributes, properties.
    const metaDiffPatcher = new DiffPatcher({
        objectHash: (obj: TestCaseProperty) => obj.value,
        propertyFilter: (name: string) => name !== '_id'
    });

    const {testCasesWithDescription, selectorsWithoutDescription} = await updateSelectorsDescription({
        testCases: localTestCases,
        selectorsData: params.getSelectorsDescriptions()
    });

    const testCasesWithDescriptionAndScreenshots = updateScreenshotPaths(testCasesWithDescription, params.browserId);
    const fetchedTestCases = await testPalmClient.getTestCases(params.testPalm.projectId, {
        // Учитываем только кейсы, которые мы выгрузили этим скриптом.
        expression: JSON.stringify({
            type: 'EQ',
            key: `attributes.${params.attributesMapping.generated}`,
            value: 'true'
        })
    });

    const ticket = (await params.getTicket()) || '';

    testCasesWithDescriptionAndScreenshots.forEach((rawLocalTestCase) => {
        const localTestCase = explainAttributes(rawLocalTestCase, params.attributesMapping);
        const matchingTestCaseIndex = fetchedTestCases.findIndex(
            (fetchedTestCase) => localTestCase.name === fetchedTestCase.name
        );

        if (matchingTestCaseIndex !== -1) {
            const matchingTestCase = fetchedTestCases[matchingTestCaseIndex];
            // Мерджим локальные и полученные из пальмы атрибуты, чтобы не перезатереть те,
            // которые добавлены тестировщиками вручную.
            const mergedAttributes = mergeAttributes(
                params.attributesMapping,
                matchingTestCase.attributes,
                localTestCase.attributes
            );

            const stepsDelta = stepsDiffPatcher.diff(
                {
                    name: localTestCase.name,
                    stepsExpects: localTestCase.stepsExpects,
                    estimatedTime: localTestCase.estimatedTime,
                    isAutotest: localTestCase.isAutotest
                },
                {
                    name: matchingTestCase.name,
                    stepsExpects: matchingTestCase.stepsExpects,
                    estimatedTime: matchingTestCase.estimatedTime,
                    isAutotest: matchingTestCase.isAutotest
                }
            );

            const metaDelta = metaDiffPatcher.diff(
                {
                    attributes: mergedAttributes,
                    properties: localTestCase.properties || [],
                    preconditions: localTestCase.preconditions || ''
                },
                {
                    attributes: matchingTestCase.attributes,
                    properties: matchingTestCase.properties,
                    preconditions: matchingTestCase.preconditions || ''
                }
            );

            if (stepsDelta || metaDelta) {
                const updatedCase = {
                    ...matchingTestCase,
                    ...localTestCase,
                    attributes: mergedAttributes,
                    properties: localTestCase.properties || []
                };

                beforeUpdate.push(matchingTestCase);
                updated.push(updatedCase);
            } else if (matchingTestCase.status === TestCaseStatus.ARCHIVED) {
                // Если нашли старый кейс, то актуализируем его.
                updated.push({
                    ...matchingTestCase,
                    status: TestCaseStatus.DRAFT
                });
            } else {
                untouched.push(matchingTestCase);
            }

            fetchedTestCases.splice(matchingTestCaseIndex, 1);
        } else {
            added.push({
                ...localTestCase,
                status: TestCaseStatus.DRAFT
            });
        }
    });

    const deleted: TestCase[] = [];

    // Перебираем тесткейсы которые есть в пальме, но отсутствуют в проекте.
    // Если кейс уже помечен как удаленный, то просто обновляем ему разметку.
    // Для остальных кейсов обновляем статус на ARCHIVED.
    fetchedTestCases.forEach((testCase) => {
        if (testCase.status === TestCaseStatus.ARCHIVED) {
            untouched.push(testCase);
        } else {
            beforeUpdate.push(testCase);
            deleted.push({
                ...testCase,
                status: TestCaseStatus.ARCHIVED
            });
        }
    });

    const casesToUpdate = [...updated, ...deleted, ...untouched];
    const changingInTasksDefinition = params.definitions.find((definition) => definition.title === 'changingInTasks');

    if (params.mode === 'full') {
        // в первую очередь обновляем значения ключей
        // чтобы при добавлении кейсов не терялись новые значения
        const unskipDefinition = params.definitions.find((definition) => definition.title === 'unskipTicket');
        if (unskipDefinition) {
            const unskipDefinitionModified = {...unskipDefinition, values: params.unskipTickets};
            await testPalmClient.addDefinitions(params.testPalm.projectId, [unskipDefinitionModified]);
        }

        if (changingInTasksDefinition && ticket) {
            const changingInTasksDefinitionModified = {
                ...changingInTasksDefinition,
                values: (changingInTasksDefinition.values || []).filter((value) => value !== ticket)
            };
            await testPalmClient.addDefinitions(params.testPalm.projectId, [changingInTasksDefinitionModified]);
        }

        if (added.length !== 0) {
            await testPalmClient.addTestCases(params.testPalm.projectId, added);
        }
        if (casesToUpdate.length !== 0) {
            await testPalmClient.updateTestCases(params.testPalm.projectId, casesToUpdate);
        }
    } else if (params.mode === 'pr' && ticket) {
        const beforeUpdateWithAttributes = beforeUpdate.map((testCase) => {
            const attributes = testCase.attributes;
            const changingInTasksKey = params.attributesMapping.changingInTasks;
            const changingInTasksValues = attributes[changingInTasksKey] || [];
            if (changingInTasksValues && changingInTasksValues.includes(ticket)) {
                // если ключ уже добавлен
                return testCase;
            }

            return {
                ...testCase,
                attributes: {
                    ...attributes,
                    [changingInTasksKey]: [...changingInTasksValues, ticket]
                }
            };
        });

        if (beforeUpdateWithAttributes.length !== 0) {
            // в первую очередь обновляем значения ключа тасок, в которых модифицировался кейс
            // чтобы при добавлении кейсов не терялись новые значения
            if (changingInTasksDefinition) {
                const changingInTasksDefinitionValues = changingInTasksDefinition.values || [];
                const changingInTasksDefinitionModified = {
                    ...changingInTasksDefinition,
                    values: changingInTasksDefinitionValues.includes(ticket) ?
                        changingInTasksDefinitionValues :
                        [...changingInTasksDefinitionValues, ticket]
                };
                await testPalmClient.addDefinitions(params.testPalm.projectId, [changingInTasksDefinitionModified]);
            }

            await testPalmClient.updateTestCases(params.testPalm.projectId, beforeUpdateWithAttributes);
        }
    }

    return {
        added,
        updated,
        deleted,
        beforeUpdate,
        selectorsWithoutDescription
    };
}

function explainAttributes(testCase: LocalTestCase, mapping: Record<string, string>): LocalTestCase {
    return {
        ...testCase,
        attributes: Object.entries(testCase.attributes || {}).reduce((attrs, [key, value]) => {
            if (!mapping[key]) {
                throw new Error(`Cannot find component "${key}" in definitions.`);
            }

            return {
                ...attrs,
                [mapping[key]]: value
            };
        }, {})
    };
}

function mergeAttributes(
    mapping: Record<string, string>,
    fetchedAttributes: TestCaseAttributes = {},
    localAttributes: TestCaseAttributes = {}
): TestCaseAttributes {
    const idsToRewrite = ATTRIBUTES_TO_REWRITE.map((key) => mapping[key]);
    const attributesKeys = Array.from(
        new Set<string>([...Object.keys(fetchedAttributes), ...Object.keys(localAttributes)])
    );

    return attributesKeys.reduce((attributes: TestCaseAttributes, attributeKey: string) => {
        const attributeValue = idsToRewrite.includes(attributeKey) ?
            localAttributes[attributeKey] ?? [] :
            Array.from(
                new Set([...(fetchedAttributes[attributeKey] ?? []), ...(localAttributes[attributeKey] ?? [])])
            );

        return {
            ...attributes,
            [attributeKey]: attributeValue
        };
    }, {});
}

function printValidationResult(parsingErrors: Error[]): void {
    const errorsMsgs = parsingErrors.map((error) => {
        const mapsDir = process.cwd();
        const errorPlace = error.stack
            ?.split('\n')
            .find((str) => str.includes('.autotest.') || str.includes('tests/sets'));
        const filePath = errorPlace?.slice(errorPlace.indexOf(mapsDir), -1).replace(process.cwd(), '.');
        const errMsgDescription = getErrorMsgDescription(error) ?? error.message;

        return filePath ? `${colors.yellow(filePath)}: ${errMsgDescription}` : errMsgDescription;
    });

    console.log(errorsMsgs.join('\n\n'));
}

function getErrorMsgDescription(err: Error): string | undefined {
    if (
        err.message.includes('Maximum call stack size exceeded') ||
        err.message.includes('Cannot convert object to primitive value')
    ) {
        return 'Использование функции \'expect\' возможно только внутри команды \'perform\'.';
    }

    if (err.message.includes('Cannot read property')) {
        return (
            'Значения, полученные в результате выполнения команд, ' +
            'необходимо обрабатывать внутри команды \'perform\'.'
        );
    }
}

function printWarningList(list: Set<string>, outputHeader: string): void {
    if (list.size !== 0) {
        console.log(colors.red(`\n${outputHeader}:`));
        list.forEach((key) => {
            console.log('\t-', key);
        });
    }
}

export default run;
export {RunParams};
