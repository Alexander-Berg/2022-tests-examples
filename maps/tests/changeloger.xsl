<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wiki="https://lego.yandex-team.ru/wiki"
    extension-element-prefixes = " wiki "
    version="1.0">

<xsl:import href="wiki-encode.xsl"/>

<xsl:output method="text"/>

<xsl:template match="/">
    <xsl:apply-templates select="changelog"/>
</xsl:template>

<xsl:template match="changelog">== ChangeLog для <xsl:value-of select="full-version"/> (<xsl:value-of select="short-version"/>.<xsl:value-of select="rev-current"/>)

Изменения в Лего <xsl:value-of select="full-version"/> за период с <xsl:value-of select="date-from"/> по <xsl:value-of select="date-to"/>.
<xsl:apply-templates select=".//filter"/>

%%html
&lt;a id="update"&gt;&lt;/a&gt;
%%
=== Миграция с <xsl:value-of select="short-version"/>.<xsl:value-of select="rev-prev"/> на <xsl:value-of select="short-version"/>.<xsl:value-of select="rev-current"/>
==== Обновление версии для пакета «Беззаботный»
  1. ##svn up##

==== Обновление версии для пакета «Осторожный»
  1. Обновляем svn:externals, меняем ревизию с <xsl:value-of select="rev-prev"/> на <xsl:value-of select="rev-current"/>\\
     ##svn ps svn:externals "`svn pg svn:externals . | sed "s#<xsl:value-of select="rev-prev"/>#<xsl:value-of select="rev-current"/>#g"`" .##
  1. Подтягиваем новые svn:externals\\
     ##svn up##
</xsl:template>

<xsl:template match="filter">
    <xsl:if test=".//item">
=== <xsl:value-of select="@title"/>
<xsl:apply-templates select=".//item"/>

Тот же список в ((<xsl:value-of select="@query-printable"/> Jira)).
    </xsl:if>
</xsl:template>

<xsl:template match="item">
    * <xsl:value-of select="wiki:encode(normalize-space(summary))"/>, ((<xsl:value-of select="link"/><xsl:text> </xsl:text><xsl:value-of select="key"/><xsl:text>))</xsl:text>
</xsl:template>

</xsl:stylesheet>
