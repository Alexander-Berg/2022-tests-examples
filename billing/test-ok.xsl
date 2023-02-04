<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE xsl:stylesheet SYSTEM "symbols.ent">
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>

    <xsl:template match="/">
        <root>
            <xsl:text>&quot;&nbsp;abc - Привет&quot;</xsl:text>
            <xsl:apply-templates select="test/a"/>
        </root>
    </xsl:template>

    <xsl:template match="a"><a-without-b/></xsl:template>
    <xsl:template match="a[b]"><a-with-b/></xsl:template>

</xsl:stylesheet>

