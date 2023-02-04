<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" encoding="utf-8"/>

    <xsl:template match="/report">
        <xsl:for-each select="merged-edges/edge"><xsl:value-of select="lengthM"/>;<xsl:value-of select="speedKmph"/>;<xsl:value-of select="ids | id"/><xsl:text>&#x0A;</xsl:text></xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
