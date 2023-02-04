package email

import (
	"archive/zip"
	"bytes"
	"github.com/stretchr/testify/require"
	"html/template"
	"io"
	"strings"
	"testing"
)

func TestMysqlWorkbench_Generate(t *testing.T) {
	w := &MysqlWorkbench{
		uuid: "abcde12345",
	}

	expectedConnections := `<?xml version="1.0"?>
<data grt_format="2.0">
  <value _ptr_="0x600003693360" type="list" content-type="object" content-struct-name="db.mgmt.Connection">
    <value type="object" struct-name="db.mgmt.Connection" id="abcde12345" struct-checksum="0x96ba47d8">
      <link type="object" struct-name="db.mgmt.Driver" key="driver">com.mysql.rdbms.mysql.driver.native_sshtun</link>
      <value type="string" key="hostIdentifier">Mysql@catalog7_yandex.mdb-rw-mdb000000.query.consul:3306@h2p.vertis.yandex.net:2222</value>
      <value type="int" key="isDefault">1</value>
      <value _ptr_="0x600003632460" type="dict" key="modules"/>
      <value _ptr_="0x600003632700" type="dict" key="parameterValues">
        <value type="string" key="SQL_MODE"></value>
        <value type="string" key="hostName">catalog7_yandex.mdb-rw-mdb000000.query.consul</value>
        <value type="string" key="password"></value>
        <value type="int" key="port">3306</value>
        <value type="string" key="schema"></value>
        <value type="int" key="sshCompressionLevel">0</value>
        <value type="string" key="sshHost">h2p.vertis.yandex.net:2222</value>
        <value type="string" key="sshKeyFile"></value>
        <value type="string" key="sshPassword"></value>
        <value type="string" key="sshUserName">userName</value>
        <value type="string" key="sslCA"></value>
        <value type="string" key="sslCert"></value>
        <value type="string" key="sslCipher"></value>
        <value type="string" key="sslKey"></value>
        <value type="int" key="useSSL">0</value>
        <value type="string" key="userName">analyst</value>
      </value>
      <value type="string" key="name">catalog7_yandex(mdb000000)</value>
    </value>
  </value>
</data>
`
	expectedServerInstances := `<?xml version="1.0"?>
<data grt_format="2.0">
  <value _ptr_="0x600003693420" type="list" content-type="object" content-struct-name="db.mgmt.ServerInstance">
    <value type="object" struct-name="db.mgmt.ServerInstance" id="95E9D623-8329-45E7-B753-7D98DC2D109E" struct-checksum="0x367436e2">
      <link type="object" struct-name="db.mgmt.Connection" key="connection">abcde12345</link>
      <value _ptr_="0x60000365aac0" type="dict" key="loginInfo"/>
      <value _ptr_="0x60000365b420" type="dict" key="serverInfo">
        <value type="int" key="setupPending">0</value>
        <value type="string" key="sys.sudo"></value>
      </value>
      <value type="string" key="name">catalog7_yandex(mdb000000)</value>
      <link type="object" struct-name="GrtObject" key="owner">C42F556B-BFEE-482B-A54B-14CE46818464</link>
    </value>
  </value>
</data>
`

	html, att, err := w.Generate(mysqlParams)
	require.NoError(t, err)
	require.Equal(t, template.HTML(`Mysql Workbench &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">mysql-workbench.zip</span>`), html)
	require.Len(t, att, 1)
	require.Equal(t, "mysql-workbench.zip", att[0].Filename)

	buf := bytes.NewReader(att[0].Content)
	reader, err := zip.NewReader(buf, int64(len(att[0].Content)))
	require.NoError(t, err)

	require.Len(t, reader.File, 2)
	for _, file := range reader.File {
		readCloser, err := file.Open()
		require.NoError(t, err)
		b := &strings.Builder{}
		_, err = io.Copy(b, readCloser)
		require.NoError(t, err)
		err = readCloser.Close()
		require.NoError(t, err)

		switch file.Name {
		case "connections.xml":
			require.Equal(t, expectedConnections, b.String())
		case "server_instances.xml":
			require.Equal(t, expectedServerInstances, b.String())
		default:
			require.FailNow(t, "unexpected file name: %s", file.Name)
		}
	}
}
