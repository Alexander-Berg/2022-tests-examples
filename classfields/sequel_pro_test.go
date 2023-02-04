package email

import (
	"github.com/stretchr/testify/require"
	"html/template"
	"testing"
)

func TestSequelPro(t *testing.T) {
	sequelPro := &SequelPro{}
	html, att, err := sequelPro.Generate(mysqlParams)

	expected := `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>SPConnectionFavorites</key>
	<array>
		<dict>
			<key>colorIndex</key>
			<integer>-1</integer>
			<key>database</key>
			<string>catalog7_yandex</string>
			<key>host</key>
			<string>catalog7_yandex.mdb-rw-mdb000000.query.consul</string>
			<key>name</key>
			<string>catalog7_yandex(mdb000000)</string>
			<key>port</key>
			<string>3306</string>
			<key>socket</key>
			<string></string>
			<key>sshHost</key>
			<string>h2p.vertis.yandex.net</string>
			<key>sshKeyLocation</key>
			<string></string>
			<key>sshKeyLocationEnabled</key>
			<integer>0</integer>
			<key>sshPort</key>
			<string>2222</string>
			<key>sshUser</key>
			<string>userName</string>
			<key>sslCACertFileLocation</key>
			<string></string>
			<key>sslCACertFileLocationEnabled</key>
			<integer>0</integer>
			<key>sslCertificateFileLocation</key>
			<string></string>
			<key>sslCertificateFileLocationEnabled</key>
			<integer>0</integer>
			<key>sslKeyFileLocation</key>
			<string></string>
			<key>sslKeyFileLocationEnabled</key>
			<integer>0</integer>
			<key>type</key>
			<integer>2</integer>
			<key>useSSL</key>
			<integer>0</integer>
			<key>user</key>
			<string>analyst</string>
		</dict>
	</array>
</dict>
</plist>
`

	require.NoError(t, err)
	require.Equal(t, template.HTML(`Sequel Pro &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">sequel-pro.plist</span>`), html)
	require.Len(t, att, 1)
	require.Equal(t, "sequel-pro.plist", att[0].Filename)
	require.Equal(t, expected, string(att[0].Content))
}
