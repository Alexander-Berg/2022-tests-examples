class ContestAnswers:
    problem_1 = """
{
    "problems": [
        {
            "compilers": [
                "shagraev_go",
                "shagraev_java",
                "shagraev",
                "shagraev_csc",
                "shagraev_cpp"
            ],
            "alias": "A",
            "id": "1/1/1",
            "name": "Камни и украшения.",
            "statements": [
                {
                    "type": "TEX",
                    "path": "tex-statement.html"
                }
            ],
            "limits": [
                {
                    "compilerName": "Others",
                    "timeLimit": 1000,
                    "idlenessLimit": 10000,
                    "memoryLimit": 67108864,
                    "outputLimit": 67108864
                }
            ],
            "testCount": 56,
            "problemType": "TEXT_ANSWER_PROBLEM"
        },
        {
            "compilers": [
                "shagraev_go",
                "shagraev_java",
                "shagraev",
                "shagraev_csc",
                "shagraev_cpp"
            ],
            "alias": "B",
            "id": "2/2/2",
            "name": "Разбиение на интервалы дат",
            "statements": [
                {
                    "type": "MARKDOWN",
                    "path": "md-statement.html"
                }
            ],
            "limits": [
                {
                    "compilerName": "shagraev",
                    "timeLimit": 4000,
                    "idlenessLimit": 10000,
                    "memoryLimit": 256000000,
                    "outputLimit": 256000000
                },
                {
                    "compilerName": "Others",
                    "timeLimit": 2000,
                    "idlenessLimit": 5000,
                    "memoryLimit": 268435456,
                    "outputLimit": 256000000
                }
            ],
            "testCount": 19,
            "problemType": "PROBLEM_WITH_CHECKER"
        }
    ]
}
    """

    md_statement_html = """
    <head>
        <link rel="stylesheet" href="https://yastatic.net/s3/contest/katex/v0.13.0/katex.min.css"/>
    </head>
    <div class="problem-statement problem-statement_type_markdown">
        <div class="header">
            <h1 class="title">B</h1>
            <table class="limits">
                <tbody>
                <tr class="time-limit">
                    <td class="property-title">Ограничение времени</td>
                    <td>1 секунда</td>
                </tr>
                <tr class="memory-limit">
                    <td class="property-title">Ограничение памяти</td>
                    <td>64.0 Мб</td>
                </tr>
                <tr class="input-file">
                    <td class="property-title">Ввод</td>
                    <td colSpan="1">стандартный ввод или input.txt</td>
                </tr>
                <tr class="output-file">
                    <td class="property-title">Вывод</td>
                    <td colSpan="1">стандартный вывод или output.txt</td>
                </tr>
                </tbody>
            </table>
        </div>
        <h2></h2>
        <div class="legend">
            <div class="Markdown">
                <p class="paragraph"><em>Markdown test</em></p>
                <p class="paragraph"><code> code </code></p>
            </div>
        </div>
    </div>
    """

    tex_statement_html = """
<head>
    <link rel="stylesheet" href="https://yastatic.net/s3/contest/katex/v0.13.0/katex.min.css"/>
</head>
<div class="problem-statement problem-statement_type_markdown">
    <div class="header">
        <h1 class="title">A</h1>
        <table class="limits">
            <tbody>
            <tr class="time-limit">
                <td class="property-title">Ограничение времени</td>
                <td>1 секунда</td>
            </tr>
            <tr class="memory-limit">
                <td class="property-title">Ограничение памяти</td>
                <td>64.0 Мб</td>
            </tr>
            <tr class="input-file">
                <td class="property-title">Ввод</td>
                <td colSpan="1">стандартный ввод или input.txt</td>
            </tr>
            <tr class="output-file">
                <td class="property-title">Вывод</td>
                <td colSpan="1">стандартный вывод или output.txt</td>
            </tr>
            </tbody>
        </table>
    </div>
    <h2></h2>
    <div class="legend">
        <div class="Markdown">
            <p class="paragraph"><em>Markdown test</em></p>
            <p class="paragraph"><code> code </code></p>
        </div>
    </div>
</div>
"""
    contest_info_1 = """
    {
    "result": {
        "contest": {
            "id": 1,
            "name": "Contest for testing femida",
            "startTime": "2022-01-01T12:00:00.000Z",
            "duration": 180
        },
        "problems": [
            {
                "id": "1/1/1",
                "name": "Камни и украшения.",
                "alias": "A",
                "maxScore": 0,
                "pathToHtmlText": "tex",
                "type": "PROBLEM_WITH_CHECKER"
            },
            {
                "id": "2/2/2",
                "name": "Опять JSON’ы перекладывать...",
                "alias": "B",
                "maxScore": 50,
                "pathToHtmlText": "md",
                "type": "PROBLEM_WITH_CHECKER"
            }
        ]
    },
    "error": null
}
    """

    contest_info_2 = """
    {
    "result": {
        "contest": {
            "id": 2,
            "name": "Contest for testing femida",
            "startTime": "2022-01-01T12:00:00.000Z",
            "duration": 0
        },
        "problems": [
        ]
    },
    "error": null
}
    """
