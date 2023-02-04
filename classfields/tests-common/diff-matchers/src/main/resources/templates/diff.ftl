<!DOCTYPE html>
<html lang="en" class="js">
<head>
    <meta http-equiv="content-type" content="text/html; charset = UTF-8">
    <script src="https://yastatic.net/jquery/2.2.3/jquery.min.js" crossorigin="anonymous"></script>

    <link href="https://yastatic.net/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet" crossorigin="anonymous">
    <script src="https://yastatic.net/bootstrap/3.3.6/js/bootstrap.min.js" crossorigin="anonymous"></script>

    <link type="text/css" href="https://yandex.st/highlightjs/8.0/styles/github.min.css" rel="stylesheet"/>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/highlight.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/bash.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/json.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/xml.min.js"></script>
    <script type="text/javascript">hljs.initHighlightingOnLoad();</script>

    <style>
        .nav {
            margin-bottom: 15px;
        }

        pre {
            white-space: pre-wrap;
        }
    </style>

    <!-- CSS -->
    <link rel="stylesheet" type="text/css" href="https://rawgit.com/rtfpessoa/diff2html/master/dist/diff2html.css">

    <!-- Javascripts -->
    <script type="text/javascript" src="https://rawgit.com/rtfpessoa/diff2html/master/dist/diff2html.js"></script>
    <script type="text/javascript" src="https://rawgit.com/rtfpessoa/diff2html/master/dist/diff2html-ui.js"></script>


    <script type="text/javascript">
        var diff2htmlUi = new Diff2HtmlUI({diff: diffString});

        function drawDiff() {
            var diffExample = '${resultsDiff}';
            var diff = Diff2Html.getPrettyHtml(diffExample);

            var targetElementDiff = document.getElementById('html-diff-element');
            targetElementDiff.innerHTML = diff;
            diff2htmlUi.draw('html-diff-element', {
                inputFormat: 'json',
                outputFormat: 'side-by-side',
                showFiles: true,
                matching: 'words'
            });
        }

        function drawOriginalDiff() {
            var diffExample = '${originalDiff}';
            var original = Diff2Html.getPrettyHtml(diffExample);

            var targetElementOriginal = document.getElementById('html-diff-original');
            targetElementOriginal.innerHTML = original;
            diff2htmlUi.draw('html-diff-original', {
                inputFormat: 'json',
                outputFormat: 'side-by-side',
                showFiles: true,
                matching: 'words'
            });
        }

        function drawModifiedDiff() {
            var diffExample = '${modifiedDiff}';
            var modified = Diff2Html.getPrettyHtml(diffExample);

            var targetElementModified = document.getElementById('html-diff-modified');
            targetElementModified.innerHTML = modified;
            diff2htmlUi.draw('html-diff-modified', {
                inputFormat: 'json',
                outputFormat: 'side-by-side',
                showFiles: true,
                matching: 'words'
            });
        }
    </script>
</head>
<body onload="drawDiff()">

<div class="container">
    <div class="row">
        <div class="col-md-12">
            <ul id="tabs" class="nav nav-pills" role="tablist">
                <li role="presentation" class="active">
                    <a aria-expanded="true" aria-controls="diff" data-toggle="tab" role="tab" href="#diff"
                       onclick="drawDiff()">
                        Full diff
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="original" data-toggle="tab" role="tab" href="#original"
                       onclick="drawOriginalDiff()">
                        Original
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="modified" data-toggle="tab" role="tab" href="#modified"
                       onclick="drawModifiedDiff()">
                        Modified
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="ignored" data-toggle="tab" role="tab" href="#ignored">
                        Ignored fields
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="excluded" data-toggle="tab" role="tab" href="#excluded">
                        Excluded fields
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="regexp" data-toggle="tab" role="tab" href="#regexp">
                        Excluded regexp fields
                    </a>
                </li>
            </ul>
            <div class="tab-content">
                <div id="diff" class="tab-pane fade active in">
                <#if resultsDiff?length == 0>
                    <p>No difference</p>
                <#else>
                    <div id="html-diff-element"></div>
                </#if>
                </div>
                <div id="original" class="tab-pane fade">
                <#if originalDiff?length == 0>
                    <p>No difference</p>
                <#else>
                    <div id="html-diff-original"></div>
                </#if>
                </div>
                <div id="modified" class="tab-pane fade">
                <#if modifiedDiff?length == 0>
                    <p>No difference</p>
                <#else>
                    <div id="html-diff-modified"></div>
                </#if>
                </div>
                <div id="ignored" class="tab-pane fade">
                <#list ignoredFields as field>
                    <div>
                        <pre>${field}</pre>
                    </div>
                <#else>
                    <p>No ignored fields</p>
                </#list>
                </div>
                <div id="excluded" class="tab-pane fade">
                <#list excludedFields as field>
                    <div>
                        <pre>${field}</pre>
                    </div>
                <#else>
                    <p>No excluded fields</p>
                </#list>
                </div>
                <div id="regexp" class="tab-pane fade">
                <#list regexpExcludedFields as field>
                    <div>
                        <pre>${field}</pre>
                    </div>
                <#else>
                    <p>No regexp excluded fields</p>
                </#list>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>