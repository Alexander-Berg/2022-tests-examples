<html>
<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
</head>
<style>
    .borderlist {
        list-style-position: inside;
        border: 1px solid black;
    }

    li.red {
        background: #ff3300;
        list-style-type: none;
        list-style-position: inside;
        border: 1px solid black;
    }

    li.green {
        background: #99ff33;
        list-style-type: none;
        list-style-position: inside;
        border: 1px solid black;
    }

    ul {
        margin-left: 0;
        padding: 0;
        display: inline;
    }
</style>
<body>
<div style="float: left; width: 50%;">
    Ожидаем значения:
    <br>
    Совпадает:
    <ul>
        <#list data.common as item>
            <li class="green">${item}</li>
        </#list>
    </ul>
    <br>
    Отсутствует:
    <ul>
        <#list data.expected as item>
            <li class="red">${item}</li>
        </#list>
    </ul>
</div>
<div style="float: left; width: 50%;">
    Проверяемые значения:
    <br>
    Совпадает:
    <ul>
        <#list data.common as item>
            <li class="green">${item}</li>
        </#list>
    </ul>
    <br>
    Лишнее:
    <ul>
        <#list data.actual as item>
            <li class="red">${item}</li>
        </#list>
    </ul>
</div>
</body>
</html>