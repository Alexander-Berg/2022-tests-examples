import { t } from 'testcafe';
import { DEFAULT_MCRS_STAGE_PARAMS, HOST, ROUTES, SERVICE, STAGE_STATUSES, TIMEOUTS } from '../../helpers/constants';
// import { ROUTES } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
// import { getYC } from '../../helpers/variableGetters';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;
const { formNew: form, status } = page.stage;
const { filters, results } = page.stage.logs;

const testDate = new Date();
let testID = testDate.getTime();

const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;
const deployUnitStringLogs = 'DeployUnitStringLogs';
const workloadStringLogs = 'WorkloadStringLogs';
const deployUnitJsonLogs = 'DeployUnitJsonLogs';
const workloadJsonLogs = 'WorkloadJsonLogs';
const box = 'box';

// const messages = {
//    host: 'host = host_1, host_2, host_3;',
//    pod: 'pod = pod_1, pod_2, pod_3;',
//    box: 'box = box_1, box_2, box_3;',
//    workload: 'workload = workload_1, workload_2, workload_3;',
//    container: 'container_id = container_id_1, container_id_2, container_id_3;',
//    logger: 'logger_name = logger_name_1, logger_name_2, logger_name_3; ',
// };

const jsonTest = [
   '{ ',
   '\\"levelStr\\": \\"JSON\\", ',
   '\\"custom\\": \\"JSON\\", ',
   '\\"loggerName\\": \\"json\\", ',
   '\\"msg\\": \\"{ ',
   '\\\\\\"json\\\\\\": true, ',
   '\\\\\\"object\\\\\\": {}, ',
   '\\\\\\"array\\\\\\": [], ',
   '\\\\\\"number\\\\\\": 1234567890, ',
   '\\\\\\"string\\\\\\": \\\\\\"string\\\\\\", ',
   '\\\\\\"true\\\\\\": true, ',
   '\\\\\\"false\\\\\\": false, ',
   '\\\\\\"null\\\\\\": null, ',
   '\\\\\\"time\\\\\\": \\\\\\"$TIME\\\\\\" ',
   '}\\" ',
   '}',
].join('');

// stderr
// bash -c 'for((i=0;1;++i)); do echo $i >&2; sleep 1; done'
// bash -c 'for((i=0;1;++i)); do echo stdout $i; echo stderr $i >&2; sleep 1; done'

const commandStartString = `/bin/bash -c 'for n in {0..300}; do echo "String test #$n ($TIME)"; done; /simple_http_server $PORT "OK!"'`;
// const commandStartJson = `/bin/bash -c 'echo "${jsonTest}"; echo "$jsonInfo"; echo "$jsonWarning"; echo "$jsonError"; echo "$jsonDebug"; /simple_http_server $PORT "OK!"'`;
const commandStartJson = `/bin/bash -c 'for n in {0..3}; do echo "${jsonTest}"; echo "$jsonInfo"; echo "$jsonWarning"; echo "$jsonError"; echo "$jsonDebug"; done; /simple_http_server $PORT "OK!"'`;

const getJsonLog = level => {
   return JSON.stringify({
      'msg': `${level} test message (${testID})`,
      'stackTrace': `${level} test stack trace (${testID})`,
      'levelStr': level.toUpperCase(),
      'loggerName': level,
      'request_id': `${level} request`,
      '@fields': {
         'custom': level.toUpperCase(),
         'status': {
            'code': '200',
            'message': 'OK',
         },
         'context': {
            'context': `${level} context`,
            'space space': `${level} space`,
            'minus-minus': `${level} minus`,
            'dot.dot': `${level} dot`,
         },
      },
   });
};

const getDateTimePicker = date => {
   const d = date.getDate();
   const m = date.getMonth() + 1;
   const yyyy = date.getFullYear();

   return {
      date: `${d > 9 ? d : `0${d}`}.${m > 9 ? m : `0${m}`}.${yyyy}`,
      time: '00:01:00',
   };
};

const today = new Date();
const tomorrow = new Date();

tomorrow.setDate(tomorrow.getDate() + 1);

const dateFrom = getDateTimePicker(today);
const dateTo = getDateTimePicker(tomorrow);

const getTextDate = date => {
   const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

   return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
};

const testLevelFilter = async level => {
   await filters.query.typeText(t, `log_level = ${level}; message = ${testID} `);
   await filters.buttons.search.click(t);
   await readLogLevel(1, level);

   await filters.query.typeText(t, `log_level != ${level}; message = ${testID} `);
   await filters.buttons.search.click(t);
   await t.expect(results.rows.row(1).level(level).exists).eql(false, `row 1: log level "${level}" exists`);

   await filterByTestId();
   await readLogLevel(1, 'DEBUG');
   await filters.level.check(t, level);
   await readLogLevel(1, level);

   await filters.level.uncheck(t, level);
};

const readLogDate = async (i, value) => {
   await t.expect(results.rows.row(i).date(value).exists).eql(true, `row ${i}: log date "${value}" exists`);
};

const readLogLevel = async (i, value) => {
   await t.expect(results.rows.row(i).level(value).exists).eql(true, `row ${i}: log level "${value}" exists`);
};

const readLogMessage = async (i, key) => {
   if (await !results.rows.row(i).message(key).exists) {
      await filters.buttons.search.click(t);
   }
   await t
      .expect(results.rows.row(i).message(`${key}`).exists)
      .eql(true, `row ${i}: log message "${key}" exists`, { timeout: 60000 });
   await t
      .expect(results.rows.row(i).message(`${testID}`).exists)
      .eql(true, `row ${i}: log message "${testID}" exists`);
};

const readLogLogger = async (i, value) => {
   await t.expect(results.rows.row(i).logger(value).exists).eql(true, `row ${i}: log logger "${value}" exists`);
};

const readStringLogRowParameters = async (i, key) => {
   await readLogMessage(i, key);
   await readLogLevel(i, '—');
   await readLogLogger(i, 'stdout');
};

const readJsonLogRowParameters = async (i, key) => {
   await readLogDate(i, getTextDate(today));
   await readLogMessage(i, key);
   await readLogLevel(i, key.toUpperCase());
   await readLogLogger(i, key);
};

const logsNotFound = async value => {
   await t.expect(results.empty.exists).eql(value, 'Empty exists', { timeout: 60000 }); // waiting for fetching results
};

const logsNotFoundByQuery = async query => {
   await filters.query.typeText(t, query);
   await filters.buttons.search.click(t);
   await logsNotFound(true);
};

const filterByQuery = async query => {
   await filters.query.typeText(t, query);
   await filters.buttons.search.click(t);
   await readLogMessage(1, `${testID}`);
};

const filterByTestId = async () => {
   await filterByQuery(`message = ${testID}`);
   await filters.buttons.search.click(t);
   await logsNotFound(false);
   await readLogMessage(1, `${testID}`);
};

const resetFilters = async () => {
   await filters.buttons.reset.click(t);
   await filterByTestId();
};

const readQueryError = async (query, error) => {
   await filters.query.typeText(t, query);
   await t.expect(filters.error(error).exists).eql(true);
};

const readQueryErrorNoQuote = async (query, error) => {
   await filters.query.typeText(t, query);
   await filters.query.pressKey(t, 'down backspace');
   await t.expect(filters.error(error).exists).eql(true);
};

const waitUntilLogRowExists = async (row, message) => {
   let i = 0;
   while ((await !results.rows.row(row).message(message).exists) && i < 20) {
      await t.wait(15000);
      await filters.buttons.search.click(t);
   }
};

const expandedJsonContainsKeyValue = async (row, key, value, eql = true) => {
   await results.rows.row(row).expanded.openedMessageJson.containsKeyValue(t, key, value, eql);
};

const createStage = async () => {
   await actions.launchTestStage(t);

   // косяк с клонированием переименованных форм #DEPLOY-3590

   await actions.editDeployUnitSettings(t, deployUnitId);
   await form.deployUnit.id.typeText(t, deployUnitStringLogs);

   // await form.deployUnit.disruptionBudget.typeText(t, '1');

   await actions.editWorkloadSettings(t, deployUnitStringLogs, boxId, workloadId);
   await form.workload.id.typeText(t, workloadStringLogs);
   await form.workload.logs.change(t, 'Disabled', 'Enabled');

   await form.workload.variables.actions.addLiteralVariable(t, 'TIME', `${testID}`);
   await form.workload.variables.actions.addLiteralVariable(t, 'PORT', '80');
   // await form.workload.variables.actions.addSecretVariable(t, 'PORT', 'PORT');

   await form.workload.commands.start.tab.click(t);
   await form.workload.commands.start.exec.value.typeText(t, commandStartString);

   await actions.editDeployUnitSettings(t, deployUnitStringLogs);
   await form.deployUnit.buttons.clone.click(t);
   await actions.editDeployUnitSettings(t, `${deployUnitStringLogs}_clone`);
   await form.deployUnit.id.typeText(t, deployUnitJsonLogs);

   await actions.editWorkloadSettings(t, deployUnitJsonLogs, boxId, workloadStringLogs);
   await form.workload.id.typeText(t, workloadJsonLogs);

   await form.workload.commands.start.tab.click(t);
   await form.workload.commands.start.exec.value.typeText(t, commandStartJson);

   await form.workload.variables.actions.addLiteralVariable(t, 'jsonInfo', getJsonLog('info'));
   await form.workload.variables.actions.addLiteralVariable(t, 'jsonWarning', getJsonLog('warning'));
   await form.workload.variables.actions.addLiteralVariable(t, 'jsonError', getJsonLog('error'));
   await form.workload.variables.actions.addLiteralVariable(t, 'jsonDebug', getJsonLog('debug'));

   await actions.createTestStage(t);

   await t.expect(status.stage.revision('1').ready.exists).ok('', { timeout: TIMEOUTS.stageReadyAfterSave });
   // логи подъезжают с задержкой (несколько минут), нужна проверка с timeout
   // await t.wait(180000);
};

fixture`Logs tests (new)`
   // .only
   // .skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
   })
   .page(HOST);

test('Start Command, Status Ready, Logs test (new)', async t => {
   await createStage();
   // testID = 1631613013545; // для повторных прогонов теста на уже созданном стейдже

   // await page.stage.tabs.logs.click(t);
   await t.navigateTo(ROUTES.STAGE_LOGS);

   // });
   // test.only('Logs filter', async t => {
   //    await t.navigateTo(ROUTES.STAGE_LOGS);
   //    testID = 1635761919652;

   await filters.deployUnit.select(t, deployUnitJsonLogs);

   await logsNotFoundByQuery(`message = " test  "`);
   await logsNotFoundByQuery(`message = "  test "`);

   await readQueryError('message = """"', 'Values error ("). Commas or double quotes are required.');
   await readQueryError('message = "" "" ;', 'Values error ("). Commas or double quotes are required.');
   await readQueryError('message = "" "" , "" ', 'Values error ("). Commas or double quotes are required.');
   await readQueryError('message = test message', 'Values error (test message). Commas or double quotes are required.');
   await readQueryError(
      'message = test message ;',
      'Values error (test message ). Commas or double quotes are required.',
   );
   await readQueryError(
      'message = test message , message',
      'Values error (test message ). Commas or double quotes are required.',
   );
   await readQueryError('message !== test', 'Syntax error: "!==".');
   await readQueryError('message !== test ;', 'Syntax error: "!==".');
   await readQueryError('message !== test , message', 'Syntax error: "!==".');
   await readQueryError('message  test', 'Key error (message test).');
   await readQueryError('message  test ;', 'Key error (message test).');
   await readQueryError('message  test , test', 'Key error (message test).');
   await readQueryError('message = "test" message', 'Values error ("test" m). Commas or double quotes are required.');
   await readQueryError('message = "test" message ;', 'Values error ("test" m). Commas or double quotes are required.');
   await readQueryError(
      'message = "test" message , test',
      'Values error ("test" m). Commas or double quotes are required.',
   );
   await readQueryError('message = test "message"', 'Values error (test "). Commas or double quotes are required.');
   await readQueryError('message = test "message" ;', 'Values error (test "). Commas or double quotes are required.');
   await readQueryError(
      'message = test "message" , test',
      'Values error (test "). Commas or double quotes are required.',
   );
   await readQueryError('messages = test', "'messages' is unknown parameter.");
   await readQueryError('messages = test ;', "'messages' is unknown parameter.");
   await readQueryError('messages = test , "test"', "'messages' is unknown parameter.");
   await readQueryError('message = ,', 'Value error.');
   await readQueryError('message = , ;', 'Value error.');
   await readQueryErrorNoQuote('message = "test', 'Values error (test). Commas or double quotes are required.');
   await readQueryErrorNoQuote('message = "test ;', 'Values error (test ;). Commas or double quotes are required.');
   await readQueryErrorNoQuote(
      'message = "test , test',
      'Values error (test , test). Commas or double quotes are required.',
   );

   await t.click(filters.help.host);
   // await filters.query.readValue(t, messages.host);

   await t.click(filters.help.pod);
   // await filters.query.readValue(t, messages.pod);

   await t.click(filters.help.box);
   // await filters.query.readValue(t, messages.box);

   await t.click(filters.help.workload);
   // await filters.query.readValue(t, messages.workload);

   await t.click(filters.help.container);
   // await filters.query.readValue(t, messages.container);

   await t.click(filters.help.logger);
   // await filters.query.readValue(t, messages.logger);

   await t.click(filters.help.message);
   // await filters.query.readValue(t, 'message = \"Search string with \\\"quotes\\\"\", \"Search string with other punctuation characters (=,;!)\"');

   await filterByTestId();

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await t.click(results.rows.row(1).buttons.expand);
   await expandedJsonContainsKeyValue(1, 'message', `"debug test message (${testID})"`);
   await results.rows.row(1).expanded.openedMessageJson.clickOnValue(t, 'minus-minus', '"debug minus"');
   await filters.buttons.search.click(t);

   await waitUntilLogRowExists(1, 'debug');
   await waitUntilLogRowExists(2, 'debug');
   await waitUntilLogRowExists(3, 'debug');
   // await isLogRowExist(countClusters - 1, 'debug');

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'debug');
   await readJsonLogRowParameters(3, 'debug');

   await filterByTestId();

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await testLevelFilter('WARNING');
   await testLevelFilter('ERROR');
   await testLevelFilter('DEBUG');
   await testLevelFilter('INFO');

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await logsNotFoundByQuery(`box != ${box}; message = ${testID}`);

   await filterByTestId();

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await logsNotFoundByQuery(`workload != ${workloadJsonLogs}; message = ${testID}`);

   await filterByQuery(`box = ${box}; workload = ${workloadJsonLogs}; message = ${testID}`);

   await logsNotFound(false);

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await t.click(results.rows.buttons.changeOrder);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await t.click(results.rows.buttons.changeOrder);

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await t.click(results.rows.buttons.changeOrder);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   // await filters.dateFrom.readDateTime(t, '', '');
   await filters.dateFrom.readDate(t, dateFrom.date);
   await filters.dateFrom.setDateTime(t, dateTo.date, dateTo.time);
   await filters.dateFrom.readDateTime(t, dateTo.date, dateTo.time);

   await logsNotFound(true);

   await filters.dateFrom.clearValue(t);

   await logsNotFound(false);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await filters.dateTo.readDateTime(t, '', '');
   await filters.dateTo.setDateTime(t, dateFrom.date, dateFrom.time);
   await filters.dateTo.readDateTime(t, dateFrom.date, dateFrom.time);

   await logsNotFound(true);

   await filters.dateTo.clearValue(t);

   await logsNotFound(false);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await filters.dateFrom.setDateTime(t, dateFrom.date, dateFrom.time);
   await filters.dateFrom.readDateTime(t, dateFrom.date, dateFrom.time);

   await filters.dateTo.setDateTime(t, dateTo.date, dateTo.time);
   await filters.dateTo.readDateTime(t, dateTo.date, dateTo.time);

   await logsNotFound(false);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await filters.level.check(t, 'INFO');

   await readJsonLogRowParameters(1, 'info');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'info');

   await t.click(results.rows.row(2).buttons.expand);

   await t.click(results.rows.row(2).expanded.buttons.copyLogJson);

   await expandedJsonContainsKeyValue(2, 'message', `"info test message (${testID})"`);
   await results.rows.row(2).link.copy(t);

   const logLinkJson = await results.rows.row(2).link.getValue();

   await resetFilters(t);

   // await filters.query.readValue(t, '');

   await filters.dateFrom.readDateTime(t, '', '');
   await filters.dateTo.readDateTime(t, '', '');

   await filterByTestId();

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await filterByQuery(`context.custom != JSON; message = ${testID}`);
   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'debug');
   await readJsonLogRowParameters(6, 'error');
   await readJsonLogRowParameters(7, 'warning');
   await readJsonLogRowParameters(8, 'info');

   await filterByQuery(`context.custom = JSON; message = ${testID}`);
   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'json');
   await readJsonLogRowParameters(3, 'json');

   await filterByQuery(`context.custom = DEBUG; message = ${testID}`);
   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'debug');
   await readJsonLogRowParameters(3, 'debug');

   await filterByQuery(`context.custom = DEBUG, INFO; message = ${testID}`);
   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'debug');
   await readJsonLogRowParameters(4, 'info');

   await filterByQuery(`context.custom != DEBUG, ERROR; message = ${testID}`);
   await readJsonLogRowParameters(1, 'warning');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'json');

   // go to log link

   await t.navigateTo(logLinkJson);

   await filters.dateFrom.readDateTime(t, dateFrom.date, dateFrom.time);
   await filters.dateTo.readDateTime(t, dateTo.date, dateTo.time);

   await filters.level.readValue(t, 'INFO');

   await readJsonLogRowParameters(1, 'info');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'info');

   await expandedJsonContainsKeyValue(2, 'logger_name', '"info"');
   await expandedJsonContainsKeyValue(2, 'log_level', '"INFO"');
   await expandedJsonContainsKeyValue(2, 'custom', '"INFO"');
   await expandedJsonContainsKeyValue(2, 'message', `"info test message (${testID})"`);
   await expandedJsonContainsKeyValue(2, 'stack_trace', `"info test stack trace (${testID})"`);
   // await results.rows.row(2).expanded.buttons.showLogJson.uncheck(t);
   // await expandedJsonContainsKeyValue(2, 'log_message', `info test message (${testID})`);

   await filterByQuery(`log_level = ERROR; message = \"test message (${testID})\"`);

   await readJsonLogRowParameters(1, 'error');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'error');

   await filterByQuery(`log_level = JSON; message = ${testID}`);

   await t.click(results.rows.row(1).buttons.expand);
   await t.click(results.rows.row(1).expanded.buttons.copyLogJson);

   await expandedJsonContainsKeyValue(1, 'logger_name', '"json"');
   await expandedJsonContainsKeyValue(1, 'log_level', '"JSON"');
   await expandedJsonContainsKeyValue(1, 'custom', '"JSON"');
   await expandedJsonContainsKeyValue(1, 'time', `"${testID}"`);
   // await expandedJsonContainsKeyValue(1, 'message', `${testID}`);

   // тестируем поля джейсона, которые уже два раза ломали #DEPLOY-4929 #DEPLOY-3530
   await expandedJsonContainsKeyValue(1, 'string', '"string"'); // строки должны быть с кавычками
   await expandedJsonContainsKeyValue(1, 'number', '1234567890');
   await expandedJsonContainsKeyValue(1, 'true', 'true');
   await expandedJsonContainsKeyValue(1, 'false', 'false');
   await expandedJsonContainsKeyValue(1, 'null', 'null');
   // await results.rows.row(1).expanded.buttons.showLogJson.uncheck(t);
   // await expandedJsonContainsKeyValue(1, 'log_message', `${testID}`);

   await filterByTestId();

   await readJsonLogRowParameters(1, 'info');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'info');

   await filters.level.uncheck(t, 'INFO');

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await t.click(results.rows.buttons.changeOrder);

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   // message !=

   await filterByQuery(`message != \"debug test message (${testID})\";`);
   await readJsonLogRowParameters(1, 'error');

   await filterByQuery(`message != \"debug test message (${testID})\", \"error test message (${testID})\";`);
   await readJsonLogRowParameters(1, 'warning');

   await filterByQuery(
      `message != \"debug test message (${testID})\", \"error test message (${testID})\", \"warning test message (${testID})\";`,
   );
   await readJsonLogRowParameters(1, 'info');

   await filterByQuery(
      `message != \"debug test message (${testID})\", \"error test message (${testID})\", \"warning test message (${testID})\", \"info test message (${testID})\";`,
   );
   await readJsonLogRowParameters(1, 'json');

   // message =

   await filterByQuery(`message = \"info test message (${testID})\";`);
   await readJsonLogRowParameters(1, 'info');
   await readJsonLogRowParameters(2, 'info');

   await filterByQuery(`message = \"info test message (${testID})\", \"warning test message (${testID})\";`);
   await readJsonLogRowParameters(1, 'warning');
   await readJsonLogRowParameters(2, 'info');

   await filterByQuery(
      `message = \"info test message (${testID})\", \"warning test message (${testID})\", \"error test message (${testID})\";`,
   );
   await readJsonLogRowParameters(1, 'error');
   await readJsonLogRowParameters(2, 'warning');
   await readJsonLogRowParameters(3, 'info');

   await filterByTestId();

   await readJsonLogRowParameters(1, 'debug');
   await readJsonLogRowParameters(2, 'error');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'info');
   await readJsonLogRowParameters(5, 'json');

   await t.click(results.rows.buttons.changeOrder);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'info');
   await readJsonLogRowParameters(3, 'warning');
   await readJsonLogRowParameters(4, 'error');
   await readJsonLogRowParameters(5, 'debug');

   await filterByQuery(`message = ${testID}; stack_trace = \" test stack trace \";`);

   // await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(1, 'info');
   await readJsonLogRowParameters(2, 'warning');
   await readJsonLogRowParameters(3, 'error');
   await readJsonLogRowParameters(4, 'debug');

   await filterByQuery(`message = ${testID}; stack_trace != \" test stack trace \";`);

   await readJsonLogRowParameters(1, 'json');
   await readJsonLogRowParameters(2, 'json');
   await readJsonLogRowParameters(3, 'json');
   await readJsonLogRowParameters(4, 'json');

   // String logs

   await filters.deployUnit.select(t, deployUnitStringLogs);

   await filters.dateFrom.readDateTime(t, dateFrom.date, dateFrom.time);
   await filters.dateTo.readDateTime(t, dateTo.date, dateTo.time);

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await logsNotFoundByQuery(`box != ${box}; message = ${testID}`);

   await filterByTestId();

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await logsNotFoundByQuery(`workload != ${workloadStringLogs}; message = ${testID}`);

   await filterByTestId();

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await logsNotFoundByQuery(`logger_name != stdout; message = ${testID}`);

   await filterByTestId();

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await logsNotFoundByQuery(`message != \"String test #\"`);

   await filterByQuery(`box = ${box}; workload = ${workloadStringLogs}; logger_name = stdout; message = ${testID}`);

   await logsNotFound(false);

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await t.click(results.rows.row(4).buttons.expand);
   await t.click(results.rows.row(4).expanded.buttons.copyLogJson);

   await expandedJsonContainsKeyValue(2, 'message', `"String test #3 (${testID})"`);
   await results.rows.row(4).link.copy(t);

   const logLinkString = await results.rows.row(4).link.getValue();

   // load more logs by scroll

   await t.expect(results.rows.row(50).message(`(${testID})`).exists).eql(true);
   await t.expect(results.rows.row(100).message(`(${testID})`).exists).eql(false);

   await expandedJsonContainsKeyValue(50, 'message', `"String test #49 (${testID})"`, false);

   await t.click(results.rows.row(50).buttons.expand);
   await t.click(results.rows.row(50).expanded.buttons.copyLogJson);

   await expandedJsonContainsKeyValue(50, 'message', `"String test #49 (${testID})"`);

   await t.click(results.rows.row(50).buttons.expand);

   await expandedJsonContainsKeyValue(50, 'message', `"String test #49 (${testID})"`, false);

   await t.expect(results.rows.row(100).message(`(${testID})`).exists).eql(true);
   await t.expect(results.rows.row(150).message(`(${testID})`).exists).eql(false);

   await t.click(results.rows.row(100).buttons.expand);
   await t.click(results.rows.row(100).expanded.buttons.copyLogJson);
   await t.click(results.rows.row(100).buttons.expand);

   await t.expect(results.rows.row(150).message(`(${testID})`).exists).eql(true);
   await t.expect(results.rows.row(200).message(`(${testID})`).exists).eql(false);

   await t.click(results.rows.row(150).buttons.expand);
   await t.click(results.rows.row(150).expanded.buttons.copyLogJson);
   await t.click(results.rows.row(150).buttons.expand);

   await t.expect(results.rows.row(200).message(`(${testID})`).exists).eql(true);
   await t.expect(results.rows.row(250).message(`(${testID})`).exists).eql(false);

   await t.click(results.rows.row(200).buttons.expand);
   await t.click(results.rows.row(200).expanded.buttons.copyLogJson);
   await t.click(results.rows.row(200).buttons.expand);

   await t.expect(results.rows.row(250).message(`(${testID})`).exists).eql(true);
   await t.expect(results.rows.row(300).message(`(${testID})`).exists).eql(false);

   await t.click(results.rows.row(250).buttons.expand);

   await t.expect(results.rows.row(300).message(`(${testID})`).exists).eql(true);

   await readStringLogRowParameters(1, `String test #0 (${testID})`);
   await readStringLogRowParameters(2, `String test #1 (${testID})`);
   await readStringLogRowParameters(3, `String test #2 (${testID})`);
   await readStringLogRowParameters(4, `String test #3 (${testID})`);
   await readStringLogRowParameters(5, `String test #4 (${testID})`);

   await resetFilters(t);

   // await filters.query.readValue(t, '');

   await filters.dateFrom.readDateTime(t, '', '');
   await filters.dateTo.readDateTime(t, '', '');

   await filterByTestId();

   await t.expect(results.rows.row(1).message(`String test #300 (${testID})`).exists).eql(true, '', { timeout: 60000 });
   await t.expect(results.rows.row(2).message(`String test #299 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(3).message(`String test #298 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(4).message(`String test #297 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(5).message(`String test #296 (${testID})`).exists).eql(true);

   // go to log link

   await t.navigateTo(logLinkString);

   await t.expect(results.rows.row(1).message(`String test #0 (${testID})`).exists).eql(true, '', { timeout: 60000 });
   await t.expect(results.rows.row(2).message(`String test #1 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(3).message(`String test #2 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(4).message(`String test #3 (${testID})`).exists).eql(true);
   await t.expect(results.rows.row(5).message(`String test #4 (${testID})`).exists).eql(true);

   await expandedJsonContainsKeyValue(2, 'message', `"String test #3 (${testID})"`);

   await filters.dateFrom.readDateTime(t, dateFrom.date, dateFrom.time);
   await filters.dateTo.readDateTime(t, dateTo.date, dateTo.time);

   // });
   // test.only('Logs backend', async t => {
   //    await t.navigateTo(ROUTES.STAGE_LOGS);
   //    testID = 1607954407061;

   // backend тест со сменой
   if (SERVICE === 'backend') {
      await actions.editTestStage(t);
      await form.stage.project.select(t, 'deploy-e2e-logs');

      await actions.editDeployUnitSettings(t, deployUnitStringLogs);
      await form.deployUnit.formTabs.selectDisks(t);
      await form.deployUnit.disk.bandwidth.guarantee.typeText(t, '29');
      await actions.editDeployUnitSettings(t, deployUnitJsonLogs);
      await form.deployUnit.formTabs.selectDisks(t);
      await form.deployUnit.disk.bandwidth.guarantee.typeText(t, '29');

      await actions.updateTestStage(t);

      await actions.editTestStage(t);
      await form.stage.project.readValue(t, 'deploy-e2e-logs');
      await actions.editDeployUnitSettings(t, deployUnitStringLogs);
      await form.deployUnit.formTabs.selectDisks(t);
      await form.deployUnit.disk.bandwidth.guarantee.readValue(t, '29');
      await actions.editDeployUnitSettings(t, deployUnitJsonLogs);
      await form.deployUnit.formTabs.selectDisks(t);
      await form.deployUnit.disk.bandwidth.guarantee.readValue(t, '29');

      // await page.stage.tabs.logs.click(t);

      await page.stage.tabs.status.click(t);

      await actions.waitForStageValidation(t);
      await actions.waitForReadyStage(t, '2');

      await page.stage.tabs.logs.click(t);

      await filters.deployUnit.select(t, deployUnitJsonLogs);

      await filterByTestId();

      await filters.level.check(t, 'INFO');
      // логи подъезжают с задержкой (несколько минут), нужна проверка с timeout
      await waitUntilLogRowExists(1, 'info');
      // let i = 0;
      // while ((await !results.rows.row(countClusters - 1).message('info').exists) && i < 10) {
      //    await t.wait(30000);
      //    await filters.buttons.search.click(t);
      // }

      await readJsonLogRowParameters(1, 'info');
      // await readJsonLogRowParameters(2, 'info');
      // await readJsonLogRowParameters(countClusters - 1, 'info');

      await filters.level.uncheck(t, 'INFO');

      await readJsonLogRowParameters(1, 'debug');
      await readJsonLogRowParameters(2, 'error');
      await readJsonLogRowParameters(3, 'warning');
      await readJsonLogRowParameters(4, 'info');
      await readJsonLogRowParameters(5, 'json');

      await t.click(results.rows.buttons.changeOrder);

      await readJsonLogRowParameters(1, 'json');
      await readJsonLogRowParameters(2, 'info');
      await readJsonLogRowParameters(3, 'warning');
      await readJsonLogRowParameters(4, 'error');
      await readJsonLogRowParameters(5, 'debug');

      await filters.deployUnit.select(t, deployUnitStringLogs);

      await readStringLogRowParameters(1, `String test #0 (${testID})`);
      await readStringLogRowParameters(2, `String test #1 (${testID})`);
      await readStringLogRowParameters(3, `String test #2 (${testID})`);
      await readStringLogRowParameters(4, `String test #3 (${testID})`);
      await readStringLogRowParameters(5, `String test #4 (${testID})`);
   }

   // await actions.deleteTestStage(t);
});

/*

{
   \\"msg\\": \\"make_sentence\\",
   \\"stackTrace\\": \\"test\\",
   \\"levelStr\\": \\"DEBUG\\",
   \\"level\\": 2000,
   \\"loggerName\\": \\"test\\",
   \\"threadName\\": \\"main\\",
   \\"@fields\\": {
      \\"request_id\\": \\"test\\",
      \\"uri\\": \\"test\\",
      \\"status\\": {
            \\"code\\": \\"222\\",
            \\"message\\": \\"OK\\",
      }
   }
}

*/
