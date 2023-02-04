<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:lego="https://lego.yandex-team.ru"
    xmlns:x="http://www.yandex.ru/xscript"
    xmlns:str="http://exslt.org/strings"
    xmlns:exsl="http://exslt.org/common"
    xmlns:func="http://exslt.org/functions"
    exclude-result-prefixes=" lego x str exsl func "
    extension-element-prefixes=" func "
    version="1.0">

<!-- ############################################################################################################## -->

<xsl:template match="/lego:page/global"/>
<xsl:template match="/lego:page/lego:params"/>

<xsl:variable name="lego:lego-static-host" select="'//yandex.st/lego/2.8-0'"/>

<!-- Разные дефолтные параметры, которые можно переопределить, передав их в lego:params. -->
<!-- !!! Эта переменная должна быть определена выше, чем lego:user-params-tmp -->
<xsl:variable name="lego:default-params">
    <lego:js-jquery>//yandex.st/jquery/1.6.2/jquery.min.js</lego:js-jquery>

    <lego:static-host>//<xsl:value-of select="$lego:id"/>.static.yandex.net</lego:static-host>
    <lego:lego-static-host><xsl:value-of select="$lego:lego-static-host"/></lego:lego-static-host>
    <lego:passport-host>http://passport.yandex.ru</lego:passport-host>
    <lego:passport-msg><xsl:value-of select="$lego:id"/></lego:passport-msg>
    <lego:css-protocol>http</lego:css-protocol>
    <lego:lego-version>2.8-0</lego:lego-version>
    <lego:lego-path>/lego</lego:lego-path>
    <xsl:apply-templates select="/" mode="lego:default-params"/>
</xsl:variable>

<xsl:template match="/" mode="lego:default-params"/>

<xsl:template match="state/param">
    <lego:param>
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates/>
    </lego:param>
</xsl:template>

<xsl:variable name="lego:user-params-tmp">
    <xsl:apply-templates select="/" mode="lego:params"/>
</xsl:variable>
<xsl:variable name="lego:user-params" select="exsl:node-set($lego:user-params-tmp)"/>

<!-- Из всех наборов параметров копируем только последние -->
<xsl:variable name="lego:params-tmp-tmp">
    <xsl:copy-of select="exsl:node-set($lego:default-params)/lego:* | $lego:user-params"/>
</xsl:variable>
<xsl:variable name="lego:params-tmp">
    <xsl:apply-templates select="exsl:node-set($lego:params-tmp-tmp)/*" mode="lego:params-normalize"/>
</xsl:variable>
<xsl:variable name="lego:params" select="exsl:node-set($lego:params-tmp)"/>

<xsl:template match="*" mode="lego:params-normalize">
    <xsl:copy-of select="."/>
</xsl:template>
<xsl:template match="lego:*" mode="lego:params-normalize"/>
<xsl:template match="lego:*[count(following::*[name()=name(current())])=0]" mode="lego:params-normalize">
    <xsl:copy-of select="."/>
</xsl:template>
<!-- /Из всех наборов параметров копируем только последние -->

<!-- ############################################################################################################## -->

<xsl:variable name="lego:id" select="$lego:user-params/lego:id"/>

<!-- ############################################################################################################## -->

<xsl:variable name="lego:locale">
    <xsl:apply-templates select="/" mode="lego:locale"/>
</xsl:variable>

<xsl:template match="/" mode="lego:locale">
    <xsl:choose>
        <xsl:when test="lego:page/lego:messages/@xml:lang"><xsl:value-of select="lego:page/lego:messages/@xml:lang"/></xsl:when>
        <xsl:otherwise>ru</xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- ############################################################################################################## -->

<xsl:template match="*" mode="lego:meta-ie">
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7, IE=edge"/>
</xsl:template>

<xsl:template name="lego:css">
<xsl:param name="css"/>
<xsl:param name="meta" select="true()"/>
<xsl:param name="build" select="$lego:params/lego:build"/>
<xsl:param name="ie" select="true()"/>
<xsl:param name="ie-only" select="false()"/>
<xsl:param name="media" select="''"/>
    <xsl:if test="$meta">
        <xsl:apply-templates select="." mode="lego:meta-ie"/>
    </xsl:if>
    <xsl:variable name="build-string">
        <xsl:if test="$build">?build=<xsl:value-of select="$build"/></xsl:if>
    </xsl:variable>
    <xsl:for-each select="str:split($css, ',')">
        <xsl:variable name="href-original"><xsl:value-of select="normalize-space(.)"/></xsl:variable>
        <xsl:variable name="href-with-proto">
            <xsl:if test="starts-with($href-original, '//')"
                    ><xsl:value-of select="$lego:params/lego:css-protocol"
                />:</xsl:if><xsl:value-of select="$href-original"/>
        </xsl:variable>

        <xsl:if test="not($ie-only)">
            <xsl:if test="$ie"><xsl:comment>[if gt IE 7]>&lt;!</xsl:comment></xsl:if>
                <link rel="stylesheet" href="{$href-original}.css{$build-string}">
                    <xsl:if test="$media">
                        <xsl:attribute name="media"><xsl:value-of select="$media"/></xsl:attribute>
                    </xsl:if>
                </link>
            <xsl:if test="$ie"><xsl:comment>&lt;![endif]</xsl:comment></xsl:if>
        </xsl:if>

        <xsl:if test="$ie or $ie-only">
            <xsl:comment>[if lt IE 8]&gt;&lt;link rel=stylesheet href="<xsl:value-of select="$href-with-proto"/>.ie.css<xsl:value-of select="$build-string"/>"&gt;&lt;![endif]</xsl:comment>
        </xsl:if>
    </xsl:for-each>
</xsl:template>

<xsl:template name="lego:css-inline">
<xsl:param name="css"/>
<xsl:param name="ie" select="false()"/>
    <xsl:choose>
        <xsl:when test="$ie">
            <xsl:comment>[if lt IE 8]&gt;&lt;style type="text/css"&gt;<xsl:value-of select="$css"/>&lt;/style&gt;&lt;![endif]</xsl:comment>
        </xsl:when>
        <xsl:otherwise>
            <style type="text/css">
                <xsl:value-of select="$css"/>
            </style>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- ############################################################################################################## -->

<xsl:template name="lego:js">
<xsl:param name="js"/>
<xsl:param name="charset" select="'utf-8'"/>
<xsl:param name="build" select="$lego:params/lego:build"/>
    <xsl:variable name="build-string">
        <xsl:if test="$build">?build=<xsl:value-of select="$build"/></xsl:if>
    </xsl:variable>
    <xsl:for-each select="str:split($js, ',')">
        <script type="text/javascript" charset="{$charset}" src="{normalize-space(.)}{$build-string}"></script>
    </xsl:for-each>
</xsl:template>

<xsl:template name="lego:js-inline">
<xsl:param name="js"/>
    <xsl:text disable-output-escaping="yes">&lt;script type="text/javascript"&gt;</xsl:text>
        <xsl:text>//</xsl:text>
        <xsl:comment>
        <xsl:text>&#10;</xsl:text>
        <xsl:value-of select="$js" disable-output-escaping="yes"/>
        <xsl:text>&#10;//</xsl:text>
        </xsl:comment>
    <xsl:text disable-output-escaping="yes">&lt;/script&gt;</xsl:text>
</xsl:template>

<!-- ############################################################################################################## -->

<xsl:template match="/" mode="lego:init">
    <xsl:apply-templates select="." mode="lego:css-include"/>
    <xsl:apply-templates select="." mode="lego:favicon"/>
    <xsl:apply-templates select="." mode="lego:touchicon"/>
    <xsl:apply-templates select="." mode="lego:js-include"/>
    <xsl:apply-templates select="." mode="lego:js-init"/>
</xsl:template>

<xsl:template match="/" mode="lego:favicon">
    <link rel="shortcut icon">
        <xsl:attribute name="href">
            <xsl:apply-templates select="." mode="lego:favicon-content"/>
        </xsl:attribute>
    </link>
</xsl:template>

<xsl:template match="/" mode="lego:favicon-content">
    <xsl:text>/favicon.ico</xsl:text>
</xsl:template>

<xsl:template match="/" mode="lego:touchicon">
    <xsl:variable name="icon" >
        <xsl:apply-templates select="." mode="lego:touchicon-content"/>
    </xsl:variable>

    <xsl:if test="$icon != ''">
        <link rel="apple-touch-icon" href="$icon"/>
        <!--<meta name="viewport" content="width = device-width"/>-->
    </xsl:if>
</xsl:template>

<xsl:template match="/" mode="lego:touchicon-content"/>

<xsl:variable name="lego:css-static-prefix" select="concat($lego:params/lego:static-host, '/css')"/>

<xsl:template match="/" mode="lego:css-include">
    <xsl:call-template name="lego:css">
        <xsl:with-param name="css" select="concat($lego:css-static-prefix, '/_', $lego:id)"/>
    </xsl:call-template>
</xsl:template>

<xsl:variable name="lego:js-static-prefix" select="concat($lego:params/lego:static-host, '/js')"/>

<xsl:template match="/" mode="lego:js-include">
    <xsl:apply-templates select="." mode="lego:js-include-jquery"/>
    <xsl:apply-templates select="." mode="lego:js-include-prj"/>
    <xsl:if test="$lego:locale != 'ru'">
        <xsl:apply-templates select="." mode="lego:js-include-locale"/>
    </xsl:if>
</xsl:template>

<xsl:template match="/" mode="lego:js-include-reflow-meter">
    <xsl:if test="lego:params()/lego:environment = 'development'">
        <xsl:call-template name="lego:js">
            <xsl:with-param name="js" select="'//yandex.st/reflowmeter/_reflow-meter.js'"/>
        </xsl:call-template>
    </xsl:if>
</xsl:template>

<xsl:template match="/" mode="lego:js-include-columns">
    <xsl:if test="lego:params()/lego:environment = 'development'">
        <xsl:call-template name="lego:js">
            <xsl:with-param name="js" select="concat($lego:lego-static-host, '/blocks/i-common/columns/i-common__columns.js')"/>
        </xsl:call-template>
    </xsl:if>
</xsl:template>

<xsl:template match="/" mode="lego:js-include-jquery">
    <xsl:call-template name="lego:js">
        <xsl:with-param name="js" select="$lego:params/lego:js-jquery"/>
        <xsl:with-param name="build" select="false()"/>
    </xsl:call-template>
</xsl:template>

<xsl:template match="/" mode="lego:js-include-prj">
    <xsl:call-template name="lego:js">
        <xsl:with-param name="js" select="concat($lego:js-static-prefix, '/_', $lego:id, '.js')"/>
    </xsl:call-template>
</xsl:template>

<xsl:template match="/" mode="lego:js-include-locale">
    <xsl:call-template name="lego:js">
        <xsl:with-param name="js" select="concat($lego:lego-static-host, '/blocks/i-messages/_messages.', $lego:locale, '.js')"/>
    </xsl:call-template>
</xsl:template>

<!-- ############################################################################################################## -->

<xsl:template match="/" mode="lego:js-init">
    <xsl:apply-templates select="." mode="lego:js-sharp-and-quirks"/>
    <xsl:apply-templates select="." mode="lego:js-lego-init"/>
    <xsl:apply-templates select="." mode="lego:js-include-reflow-meter"/>
    <xsl:apply-templates select="." mode="lego:js-include-columns"/>
</xsl:template>

<xsl:template match="/" mode="lego:js-init-params">
    <xsl:text>login:</xsl:text><xsl:value-of select="x:js-quote($lego:params/lego:login)"/>
    <xsl:text>,locale:</xsl:text><xsl:value-of select="x:js-quote($lego:locale)"/>
    <xsl:text>,id:</xsl:text><xsl:value-of select="x:js-quote($lego:id)"/>
    <xsl:apply-templates select="lego:params()/*" mode="lego:params-param"/>
</xsl:template>

<xsl:template match="/" mode="lego:js-lego-init">
    <xsl:call-template name="lego:js-inline">
        <xsl:with-param name="js">
            <xsl:text>Lego.init({</xsl:text>
                <xsl:apply-templates select="." mode="lego:js-init-params"/>
            <xsl:text>})</xsl:text>
        </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="/" mode="lego:js-sharp-and-quirks">
    <xsl:call-template name="lego:js-inline">
        <xsl:with-param name="js">
            <xsl:text>var docElem = document.documentElement;</xsl:text>
            <xsl:text>docElem.id = 'js';</xsl:text>
            <xsl:text>document.compatMode != "CSS1Compat" &amp;&amp; (docElem.className += ' quirks');</xsl:text>
        </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="*" mode="lego:params-param"/>
<xsl:template match="
    lego:index |
    lego:yandexuid |
    lego:passport-host |
    lego:pass-host |
    lego:passport-msg |
    lego:lego-static-host |
    lego:social-host |
    lego:lego-path |
    lego:retpath
    " mode="lego:params-param">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="x:js-quote(local-name())"/>
    <xsl:text>:</xsl:text>
    <xsl:value-of select="x:js-quote(.)"/>
</xsl:template>

<xsl:template match="lego:display-name" mode="lego:params-param">
    <xsl:text>,displayName:{</xsl:text>
        <xsl:value-of select="x:js-quote('social')"/>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="boolean(social)"/>
        <xsl:text>,name:</xsl:text>
        <xsl:value-of select="x:js-quote($lego:params/lego:display-name/name)"/>
    <xsl:text>}</xsl:text>
</xsl:template>

<!-- ############################################################################################################## -->

<func:function name="lego:message">
<xsl:param name="id"/>
<xsl:param name="text"/>
    <func:result>
        <xsl:call-template name="lego:message">
            <xsl:with-param name="id" select="$id"/>
            <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
    </func:result>
</func:function>

<xsl:template name="lego:message">
<xsl:param name="id"/>
<xsl:param name="text"/>
    <xsl:value-of select="$text"/>
</xsl:template>

<!-- ############################################################################################################## -->

<func:function name="lego:params">
    <func:result select="$lego:params"/>
</func:function>

<!-- ############################################################################################################## -->

<func:function name="lego:auth">
    <func:result select="$lego:params/lego:login != ''"/>
</func:function>

<!-- ############################################################################################################## -->

<xsl:template name="lego:apply">
<xsl:param name="content"/>
    <xsl:apply-templates select="exsl:node-set($content)/*"/>
</xsl:template>

<!-- ############################################################################################################## -->

<func:function name="lego:child-node">
<xsl:param name="elements"/>
    <xsl:variable name="nodes">
        <xsl:element name="{name()}">
            <xsl:for-each select="str:tokenize($elements, ',| ')">
                <xsl:element name="{normalize-space(.)}"/>
            </xsl:for-each>
        </xsl:element>
    </xsl:variable>
    <func:result select="exsl:node-set($nodes)/*/*"/>
</func:function>

<!-- ############################################################################################################## -->

<xsl:template match="*" mode="lego:copy">
    <xsl:copy>
        <xsl:apply-templates select="@*" mode="lego:copy"/>
        <xsl:apply-templates mode="lego:copy"/>
    </xsl:copy>
</xsl:template>

<xsl:template match="lego:*" mode="lego:copy">
    <xsl:apply-templates select="."/>
</xsl:template>

<xsl:template match="@*" mode="lego:copy">
    <xsl:copy-of select="."/>
</xsl:template>

<!-- ############################################################################################################## -->

<!-- хелпер для копирования атрибутов в js-параметры -->
<xsl:template match="lego:*/@*" mode="lego:js-params-content">
    <xsl:value-of select="concat(',', name(), ':', x:json-quote(.))"/>
</xsl:template>

<!-- ############################################################################################################## -->

<!-- тег для декларации чего-либо в raw-файле, костыль для динамических lego-блоков -->
<xsl:template match="lego:declaration"/>

<!-- ############################################################################################################## -->

<xsl:template match="lego:mix"/>

<func:function name="lego:name">
<xsl:param name="element"/>
    <xsl:choose>
        <xsl:when test="$element/@b">
            <func:result select="concat($element/@b, '__', local-name($element))"/>
        </xsl:when>
        <xsl:when test="lego:is-block($element)">
            <func:result select="local-name($element)"/>
        </xsl:when>
        <xsl:otherwise>
            <func:result select="concat(local-name($element/ancestor::lego:*[lego:is-block(.)][1]), '__', local-name($element))"/>
        </xsl:otherwise>
    </xsl:choose>
</func:function>

<func:function name="lego:is-block">
<xsl:param name="element"/>
    <func:result select="translate(substring(local-name($element), 1, 2), 'blegzi', '') = '-'"/>
</func:function>

<!-- ############################################################################################################## -->

<func:function name="lego:if">
<xsl:param name="condition"/>
<xsl:param name="a"/>
<xsl:param name="b"/>
    <func:result>
        <xsl:choose>
            <xsl:when test="$condition"><xsl:value-of select="$a"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="$b"/></xsl:otherwise>
        </xsl:choose>
    </func:result>
</func:function>

<!-- ############################################################################################################## -->

<func:function name="lego:clever-substring">
    <xsl:param name="string"/> <!-- Строка на вход -->
    <xsl:param name="maxlength"/> 
    <xsl:param name="maxlength-relative"/> 

    <!-- Пустые параметры дотуннеливаются как истинные :( -->
    <xsl:variable name="local-maxlength" select="lego:if(normalize-space($maxlength), $maxlength, 1000)"/>
    <xsl:variable name="local-maxlength-relative" select="lego:if(normalize-space($maxlength-relative), $maxlength-relative, 3)"/>

    <!-- Локальная переменная, true(), если надо отрезать -->
    <xsl:variable name="need-cut" select="string-length($string) > ($local-maxlength + $local-maxlength-relative)"/>

    <xsl:choose>
        <xsl:when test="$need-cut">
            <func:result select="concat(substring(., 1, $local-maxlength - 1), '…')"/>
        </xsl:when>
        <xsl:otherwise>
            <func:result select="."/>
        </xsl:otherwise>
    </xsl:choose>
</func:function>

<!-- ############################################################################################################## -->

<func:function name="lego:max">
<xsl:param name="a"/>
<xsl:param name="b"/>
    <func:result select="lego:if($a &gt; $b, $a, $b)"/>
</func:function>

<func:function name="lego:user-name">
    <func:result>
        <xsl:choose>
            <xsl:when test="lego:params()/lego:display-name/name">
                <xsl:value-of select="lego:params()/lego:display-name/name"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="not(starts-with(lego:params()/lego:login, 'uid-'))">
                    <xsl:value-of select="lego:params()/lego:login"/>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </func:result>
</func:function>

<func:function name="lego:min">
<xsl:param name="a"/>
<xsl:param name="b"/>
    <func:result select="lego:if($a &lt; $b, $a, $b)"/>
</func:function>

<func:function name="lego:ends-with">
    <xsl:param name="string"/>
    <xsl:param name="substring"/>
    <func:result select="$string = concat(substring($string, 1, string-length($string) - string-length($substring)), $substring)"/>
</func:function>


<!-- ############################################################################################################## -->

</xsl:stylesheet>

