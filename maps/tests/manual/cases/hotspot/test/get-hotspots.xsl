<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:x="http://www.yandex.ru/xscript"
                extension-element-prefixes="x"
                exclude-result-prefixes="x">

    <xsl:output method="text" encoding="utf-8" indent="no" />

    <xsl:template match="/page">
        <xsl:value-of select="x:urlencode(x:get-query-arg('callback'))"/>(<xsl:value-of select="text"/>)
    </xsl:template>

</xsl:stylesheet>