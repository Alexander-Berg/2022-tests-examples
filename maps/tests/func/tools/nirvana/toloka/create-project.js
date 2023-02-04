const fs = require('fs');
const path = require('path');
const got = require('got');

const ROOT = path.join(__dirname, '..');
const sandboxData = require(path.join(ROOT, 'sandbox.json'));
const htmlTemplate = fs.readFileSync(path.join(ROOT, 'views', 'project.handlebars'), {encoding: 'utf-8'});
const styles = fs.readFileSync(path.join(ROOT, 'views', 'styles.css'), {encoding: 'utf-8'});

function createProject(issue) {
    return got.post('https://sandbox.toloka.yandex.ru/api/v1/projects/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`,
            'Content-Type': 'application/json'
        },
        json: true,
        body: {
            public_name: `[${issue.key}] Релиз ${issue.versionSingle.display}`,
            public_description: "Тест интерактивности карты",
            public_instructions: 'https://wiki.yandex-team.ru/users/farvater/mapsapitesting/new-flow-design-testing',
            private_comment: [
                `Отчет (день): ${sandboxData.download_link}/day/index.html`,
                `Отчет (ночь): ${sandboxData.download_link}/night/index.html`
            ].join('\n'),
            task_spec: {
                input_spec: {
                    url: {
                        type: 'string',
                        required: true
                    },
                    name: {
                        type: 'string',
                        required: true
                    },
                    diffUrl: {
                        type: 'string',
                        hidden: false,
                        required: false
                    }
                },
                output_spec: {
                    comment: {
                        type: 'string',
                        hidden: false,
                        required: false
                    },
                    diffUrl: {
                        type: 'string',
                        hidden: false,
                        required: false
                    },
                    labelBug: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    },
                    displayBug: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    },
                    missingElement: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    },
                    elementOverlap: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    },
                    colorBug: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    },
                    interactivityBug: {
                        type: 'boolean',
                        hidden: false,
                        required: false
                    }
                },
                view_spec: {
                    type: 'classic',
                    assets: {
                        style_urls: [],
                        script_urls: [
                            '$TOLOKA_ASSETS/js/toloka-handlebars-templates.js'
                        ]
                    },
                    markup: htmlTemplate.toString(),
                    script: '',
                    styles: styles.toString(),
                    settings: {
                        showSkip: false,
                        showTimer: false,
                        showTitle: true,
                        showFinish: true,
                        showReward: false,
                        showSubmit: true,
                        showMessage: true,
                        showFullscreen: false,
                        showInstructions: true
                    }
                }
            },
            assignments_issuing_type: 'AUTOMATED',
            assignments_automerge_enabled: false
        }
    }).then(({body}) => body);
}

module.exports = createProject;
