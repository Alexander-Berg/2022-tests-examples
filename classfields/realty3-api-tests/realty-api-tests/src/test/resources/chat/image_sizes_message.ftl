{
    "roomId": "${roomId}",
    "payload": {
        "contentType": "TEXT_PLAIN"
    },
    "attachments": [{"image":{"sizes": {
    <#list imageSizes?keys as size>
        "${size}":"${imageSizes[size]}"<#if size_has_next>,</#if>
    </#list>
    }}}],
    "properties": {}
}
