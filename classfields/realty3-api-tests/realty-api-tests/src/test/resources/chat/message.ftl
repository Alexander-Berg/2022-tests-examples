{
  "roomId": "${roomId}",
  "payload": {
<#if skipContentType??>
<#else>
    "contentType": "<#if contentType??>${contentType}<#else>TEXT_PLAIN</#if>",
</#if>
    "value": "${message}"
  },
  "properties": {}
}
