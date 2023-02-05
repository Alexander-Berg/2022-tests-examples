<?xml version="1.0"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


    <xsl:key name="attrs" match="/Wikimaps/Attributes/Attribute" use="@id"/>


    <xsl:template match="/Wikimaps">
        <response>
            <xsl:variable name="errors">
                <xsl:apply-templates select="CategoryGroups/CategoryGroup" mode="lookup-errors"/>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="string($errors)">
                    <xsl:copy-of select="$errors"/>
                </xsl:when>
                <xsl:otherwise>
                    <ok/>
                </xsl:otherwise>
            </xsl:choose>
        </response>
    </xsl:template>


    <xsl:template match="CategoryGroup" mode="lookup-errors">
        <xsl:variable name="category-group" select="."/>
        <xsl:for-each select="LookupGroups/LookupGroup">
            <xsl:variable
                name="interface-type-in-category-group"
                select="$category-group/View/attribute[@id = current()/@keyAttribute]/interface/@type"/>
            <xsl:if test="
                ($interface-type-in-category-group and $interface-type-in-category-group != 'combo') or
                (not($interface-type-in-category-group) and not(key('attrs', @keyAttribute)/View/interface/@type = 'combo'))">
                <error>
                    <xsl:text>attribute '</xsl:text>
                    <xsl:value-of select="@keyAttribute"/>
                    <xsl:text>' must have interface/@type = 'combo' (CategoryGroup/@id = '</xsl:text>
                    <xsl:value-of select="$category-group/@id"/>
                    <xsl:text>')</xsl:text>
                </error>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>