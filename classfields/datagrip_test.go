package email

import (
	"github.com/stretchr/testify/require"
	"html/template"
	"testing"
)

func TestDatagrip_GenerateMySQL(t *testing.T) {
	datagrip := &Datagrip{
		uuid: "abcde12345",
	}
	html, att, err := datagrip.Generate(mysqlParams)

	expected := `#DataSourceSettings#
#LocalDataSource: catalog7_yandex(mdb000000)
#BEGIN#
<data-source source="LOCAL" name="catalog7_yandex(mdb000000)" uuid="abcde12345">
    <database-info product="MySQL" version="8.0.17-8 compatible h2p mysql-proxy" jdbc-version="4.0"
                   driver-name="MySQL Connector Java"
                   driver-version="mysql-connector-java-5.1.47 ( Revision: fe1903b1ecb4a96a917f7ed3190d80c049b1de29 )"
                   dbms="MYSQL" exact-version="5.7.25" exact-driver-version="5.1">
        <extra-name-characters>#@</extra-name-characters>
        <identifier-quote-string>` + "`" + `</identifier-quote-string>
    </database-info>
    <case-sensitivity plain-identifiers="exact" quoted-identifiers="exact"/>
    <driver-ref>mysql.8</driver-ref>
    <synchronize>true</synchronize>
    <jdbc-driver>com.mysql.jdbc.Driver</jdbc-driver>
    <jdbc-url>jdbc:mysql://catalog7_yandex.mdb-rw-mdb000000.query.consul:3306/catalog7_yandex</jdbc-url>
    <secret-storage>master_key</secret-storage>
    <user-name>analyst</user-name>
    <introspection-schemas>*:@</introspection-schemas>
    <driver-properties>
        <property name="autoReconnect" value="true"/>
        <property name="zeroDateTimeBehavior" value="convertToNull"/>
        <property name="tinyInt1isBit" value="false"/>
        <property name="characterEncoding" value="utf8"/>
        <property name="characterSetResults" value="utf8"/>
        <property name="yearIsDateType" value="false"/>
    </driver-properties>
    <ssh-properties>
        <enabled>true</enabled>
        <proxy-host>h2p.vertis.yandex.net</proxy-host>
        <proxy-port>2222</proxy-port>
        <user>userName</user>
        <use-password>false</use-password>
        <auth-type>OPEN_SSH</auth-type>
    </ssh-properties>
</data-source>
#END#`

	require.NoError(t, err)
	require.Equal(t, template.HTML(`DataGrip &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">datagrip.txt</span>`), html)
	require.Len(t, att, 1)
	require.Equal(t, "datagrip.txt", att[0].Filename)
	require.Equal(t, expected, string(att[0].Content))
}

func TestDatagrip_GeneratePostgreSQL(t *testing.T) {
	datagrip := &Datagrip{
		uuid: "abcde12345",
	}
	html, att, err := datagrip.Generate(postgresqlParams)

	expected := `#DataSourceSettings#
#LocalDataSource: catalog7_yandex(mdb000000)
#BEGIN#
<data-source source="LOCAL" name="catalog7_yandex(mdb000000)" uuid="abcde12345">
    <database-info product="PostgreSQL" version="13.4 (Ubuntu 13.4-201-yandex.50204.1072273362)" jdbc-version="4.2"
                   driver-name="PostgreSQL JDBC Driver"
                   driver-version="42.2.22"
                   dbms="POSTGRES" exact-version="13.4" exact-driver-version="42.2">
        <extra-name-characters>#@</extra-name-characters>
        <identifier-quote-string>` + "`" + `</identifier-quote-string>
    </database-info>
    <case-sensitivity plain-identifiers="exact" quoted-identifiers="exact"/>
    <driver-ref>postgresql</driver-ref>
    <synchronize>true</synchronize>
    <jdbc-driver>org.postgresql.Driver</jdbc-driver>
    <jdbc-url>jdbc:postgresql://catalog7_yandex.pg-rw-mdb000000.query.consul:3306/catalog7_yandex</jdbc-url>
    <secret-storage>master_key</secret-storage>
    <user-name>analyst</user-name>
    <introspection-schemas>*:@</introspection-schemas>
    <driver-properties>
        <property name="autoReconnect" value="true"/>
        <property name="zeroDateTimeBehavior" value="convertToNull"/>
        <property name="tinyInt1isBit" value="false"/>
        <property name="characterEncoding" value="utf8"/>
        <property name="characterSetResults" value="utf8"/>
        <property name="yearIsDateType" value="false"/>
    </driver-properties>
    <ssh-properties>
        <enabled>true</enabled>
        <proxy-host>h2p.vertis.yandex.net</proxy-host>
        <proxy-port>2222</proxy-port>
        <user>userName</user>
        <use-password>false</use-password>
        <auth-type>OPEN_SSH</auth-type>
    </ssh-properties>
</data-source>
#END#`

	require.NoError(t, err)
	require.Equal(t, template.HTML(`DataGrip &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">datagrip.txt</span>`), html)
	require.Len(t, att, 1)
	require.Equal(t, "datagrip.txt", att[0].Filename)
	require.Equal(t, expected, string(att[0].Content))
}
