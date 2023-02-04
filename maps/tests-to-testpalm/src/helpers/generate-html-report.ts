import {TestCaseProperty, TestCaseStep} from '@yandex-int/testpalm-api';
import {LocalTestCase} from '../types';
import {encodeHtmlEntities} from '../utils';

interface Report {
    added: LocalTestCase[];
    updated: LocalTestCase[];
    deleted: LocalTestCase[];
    beforeUpdate: LocalTestCase[];
}

interface Meta {
    projectId: string;
    reportName: string;
    attributesMapping: Record<string, string>;
}

function markdown(input: string): string {
    return input
        .replace(/`([^`]*)`/g, (_match, value) => `<code>${encodeHtmlEntities(value)}</code>`)
        .replace(/!\[([^[\]]*)?\]\(([^()]*)?\)/g, '<img src="$2" alt="$1" loading="lazy" />');
}

function formatTime(input?: number): string {
    if (!input) {
        return '';
    }

    const minutes = parseInt(String(input / (60 * 1000)), 10);
    const seconds = Math.ceil((input / 1000) % 60);
    return `${minutes}m ${seconds}s`;
}

function renderDetails(summary: string, body: string): string {
    return `
        <details>
            <summary>${summary}</summary>
            <div>
                ${body}
            </div>
        </details>
    `;
}

function renderProperty(property: TestCaseProperty): string {
    return `<li><b>${encodeHtmlEntities(property.title)}</b>: ${encodeHtmlEntities(property.value)}</li>`;
}

function renderAttribute([id, values]: [string, string[]], meta: Meta): string {
    if (values.length === 0) {
        return '';
    }

    const knownAttributes = Object.entries(meta.attributesMapping);
    const [name] = knownAttributes.find(([, ids]) => ids.includes(id)) || [];
    if (!name) {
        return '';
    }

    return `<li><b>${name}</b>: ${values.map((value) => `<code>${value}</code>`).join(', ')}</li>`;
}

function renderTestCaseStep(step: TestCaseStep): string {
    return `<li>${markdown(step.step)}</li>`;
}

function renderTestCase(testCase: LocalTestCase, meta: Meta): string {
    return renderDetails(
        `<span style="font-size: 16px;">${encodeHtmlEntities(testCase.name!)}</span>`,
        `
            <p><b style="font-size: 16px;">Status:</b> ${testCase.status}</p>
            <p><b style="font-size: 16px;">Automated</b>: ${String(testCase.isAutotest ?? false)}</p>
                ${
    testCase.id ?
        `<p><b style="font-size: 16px;">Case in TestPalm</b>: <a href="https://testpalm.yandex-team.ru/testcase/${meta.projectId}-${testCase.id}" target="_blank">${meta.projectId}-${testCase.id}</a></p>` :
        ''
}
            <p><b style="font-size: 16px;">Steps</b></p>
            <ol>
                ${testCase.stepsExpects.map(renderTestCaseStep).join('\n')}
            </ol>
            <p><b style="font-size: 16px;">Estimated Time:</b> ${formatTime(testCase.estimatedTime)}</p>
            <p><b style="font-size: 16px;">Keys</b></p>
            <ul>
                ${Object.entries(testCase.attributes || {})
        .map((attribute) => renderAttribute(attribute, meta))
        .join('\n')}
            </ul>
            <p><b style="font-size: 16px;">Properties</b></p>
            <ul>
                ${(testCase.properties || []).map(renderProperty).join('\n')}
            </ul>
        `
    );
}

function renderTestCases(name: string, testCases: LocalTestCase[], meta: Meta): string {
    return renderDetails(
        `
            <b style="text-transform: capitalize; font-size: 18px;">${name} cases</b>
            <small>(${testCases.length})</small>
        `,
        testCases.map((testCase) => renderTestCase(testCase, meta)).join('\n')
    );
}

function renderHtml(body: string, meta: Meta): string {
    return `
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="X-UA-Compatible" content="IE=edge">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Synced test cases (${meta.reportName})</title>
                <style>
                    summary { cursor: pointer; outline: none; }
                    details > *:not(summary) { font-size: 14px; white-space: normal; margin: 12px; }
                </style>
            </head>
            <body>
                ${body}
            </body>
        </html>
    `;
}

function renderModifiedInPalm(name: string, testCases: LocalTestCase[], meta: Meta): string {
    return renderDetails(
        `
            <b style="text-transform: capitalize; font-size: 18px;">${name} cases</b>
            <small>(${testCases.length})</small>
        `,
        `
            <ul>${testCases
        .sort((a, b) => a.id! - b.id!)
        .map(
            (testCase) =>
                `<li>${testCase.id}: <a href="https://testpalm.yandex-team.ru/testcase/${meta.projectId}-${testCase.id}" target="_blank">${testCase.name}</a></li>`
        )
        .join('\n')}
            </ul>
        `
    );
}

function generateHtmlReport(report: Report, meta: Meta): string {
    const modifiedCases = renderModifiedInPalm('Modified in Testpalm', report.beforeUpdate, meta);
    const testCases = (['added', 'updated', 'deleted'] as const)
        .map((name) => renderTestCases(name, report[name], meta))
        .join('<hr/>');
    return renderHtml(
        `
            <h1>Synced test cases (${meta.reportName})</h1>
            ${modifiedCases}<hr/>
            ${testCases}
        `,
        meta
    );
}

export {generateHtmlReport};
