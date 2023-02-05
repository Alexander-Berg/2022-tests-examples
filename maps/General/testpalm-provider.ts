import TestpalmClient, {VersionStatus} from '@yandex-int/testpalm-api';
import logError from '../utils/log-error';
import createRequest, {RequestOptions} from '../utils/request';

const request = createRequest('testpalm-provider');

type TP = typeof TestpalmClient.prototype;

interface TestCase {
    status: string;
    testCase: {
        id: number
    }
}
interface TestGroup {
    testCases: TestCase[];
    defaultOrder: boolean;
    path: [];
}
interface ImportTestRunBody {
    tags: string[];
    title: string;
    version: string;
    environments: {title: string}[];
    testGroups: TestGroup[];
    properties: [];
}

class TestpalmProvider {
    private _client: TestpalmClient;
    private _token: string;
    private _baseUrl = 'https://testpalm-api.yandex-team.ru/v2';

    constructor(token: string) {
        this._token = token;
        this._client = new TestpalmClient(token, {retryCount: 2});
    }

    private _request<T>(path: string, options: RequestOptions = {}): Promise<T> {
        return request<T>(`${this._baseUrl}${path}`, {
            query: options.query,
            method: options.method,
            body: options.body,
            headers: {
                Authorization: `OAuth ${this._token}`
            },
            json: true,
            allowGetBody: true,
            timeout: 120 * 1000 // 2 min
        })
            .then((res) => res.body);
    }

    changeVersionStatus(
        projectId: string,
        version: string,
        status: VersionStatus,
        options: Record<string, unknown> = {}
    ): Promise<void> {
        return request<void>(`https://testpalm-api.yandex-team.ru/version/${projectId}`, {
            method: 'PATCH',
            headers: {
                Authorization: `OAuth ${this._token}`
            },
            body: {
                id: version,
                status,
                ...options
            },
            json: true
        })
            .then((res) => res.body)
            .catch((error) => logError('Не удалось стартовать версию', error, {version, status}));
    }

    getVersion(...args: Parameters<TP["getVersion"]>): ReturnType<TP["getVersion"]> {
        return this._client.getVersion(...args)
            .catch((error) => logError('Не удалось получить версию в тестпалме', error, {...args}));
    }

    async getTestCases(...args: Parameters<TP["getTestCases"]>): Promise<ReturnType<TP["getTestCases"]>> {
        const [projectId, query] = args;

        if (!query?.limit) {
            return this._client.getTestCases(projectId, query)
                .catch((error) => logError( 'Не удалось получить тесткейсы', error, {projectId, query}));
        }

        let response = await this._client.getTestCases(projectId, {...query, skip: 0});
        let result = response;
        let skip = response.length;

        while (response.length > 0) {
            response = await this._client.getTestCases(projectId, {...query, skip});
            result = result.concat(response);
            skip += response.length;
        }

        return result;
    }

    addVersion(...args: Parameters<TP["addVersion"]>): ReturnType<TP["addVersion"]> {
        return this._client.addVersion(...args)
            .catch((error) => logError('Не удалось создать версию в тестпалме', error, {...args}));
    }

    importTestRun(projectId: string, body: ImportTestRunBody): unknown {
        return this._request(
            `/testrun/${projectId}/import?includeOnlyExisted=1`,
            {body: body as unknown as Record<string, unknown>, method: 'POST'})
            .catch((error) => logError('Не удалось импортировать тестран', error, {body}));
    }

    getDefinitions(...args: Parameters<TP["getDefinitions"]>): ReturnType<TP["getDefinitions"]> {
        return this._client.getDefinitions(...args)
            .catch((error) => logError('Не удалось создать версию в тестпалме', error, {...args}));
    }
}

export default new TestpalmProvider(process.env.TESTPALM_OAUTH_TOKEN);
