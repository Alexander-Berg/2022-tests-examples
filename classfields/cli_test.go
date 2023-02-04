package email

import (
	"github.com/stretchr/testify/require"
	"html/template"
	"testing"
)

func TestCli_GenerateROMySQL(t *testing.T) {
	cli := &Cli{}
	paramsFixture := mysqlParams
	paramsFixture.Db.Instance = "mdbdu7vpgic2t4lie4gh"
	paramsFixture.Db.Mode = "ro"
	html, att, err := cli.Generate(paramsFixture)

	expected := "H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p mysql-mdbdu7vpgic2t4lie4gh<b style=\"font-weight: normal\">@</b>catalog7_yandex</pre>"

	require.NoError(t, err)
	require.Nil(t, att)
	require.Equal(t, template.HTML(expected), html)
}

func TestCli_GenerateRWMySQL(t *testing.T) {
	cli := &Cli{}
	paramsFixture := mysqlParams
	paramsFixture.Db.Instance = "mdbdu7vpgic2t4lie4gh"
	html, att, err := cli.Generate(paramsFixture)

	expected := "H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p -w mysql-mdbdu7vpgic2t4lie4gh<b style=\"font-weight: normal\">@</b>catalog7_yandex</pre>"

	require.NoError(t, err)
	require.Nil(t, att)
	require.Equal(t, template.HTML(expected), html)
}

func TestCli_GenerateROPostgreSQL(t *testing.T) {
	cli := &Cli{}
	paramsFixture := postgresqlParams
	paramsFixture.Db.Instance = "mdbdu7vpgic2t4lie4gh"
	paramsFixture.Db.Mode = "ro"
	html, att, err := cli.Generate(paramsFixture)

	expected := "H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p pg-mdbdu7vpgic2t4lie4gh<b style=\"font-weight: normal\">@</b>catalog7_yandex</pre>"

	require.NoError(t, err)
	require.Nil(t, att)
	require.Equal(t, template.HTML(expected), html)
}

func TestCli_GenerateRWPostgreSQL(t *testing.T) {
	cli := &Cli{}
	paramsFixture := postgresqlParams
	paramsFixture.Db.Instance = "mdbdu7vpgic2t4lie4gh"
	html, att, err := cli.Generate(paramsFixture)

	expected := "H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p -w pg-mdbdu7vpgic2t4lie4gh<b style=\"font-weight: normal\">@</b>catalog7_yandex</pre>"

	require.NoError(t, err)
	require.Nil(t, att)
	require.Equal(t, template.HTML(expected), html)
}
