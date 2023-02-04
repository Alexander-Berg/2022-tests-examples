package email

import (
	"github.com/stretchr/testify/require"
	"html/template"
	"testing"
)

func TestTablePlus_Generate(t *testing.T) {
	tablePlus := &TablePlus{}
	html, att, err := tablePlus.Generate(mysqlParams)

	expected := "TablePlus &mdash; Используйте следующий uri: <pre>mysql+ssh<b style=\"font-weight: normal\">://</b>userName:<b style=\"font-weight: normal\">@</b>h2p.vertis.yandex.net:2222/analyst<b style=\"font-weight: normal\">@</b>catalog7_yandex.mdb-rw-mdb000000.query.consul:3306/catalog7_yandex?name=catalog7_yandex%28mdb000000%29&amp;tLSMode=1&amp;usePrivateKey=true</pre>"

	require.NoError(t, err)
	require.Nil(t, att)
	require.Equal(t, template.HTML(expected), html)
}
