package email

import (
	"github.com/stretchr/testify/require"
	"testing"
)

var (
	mysqlParams = Params{
		Title: "catalog7_yandex(mdb000000)",
		Db: DbParams{
			Name:     "catalog7_yandex",
			Instance: "mdb000000",
			Mode:     "rw",
			Host:     "catalog7_yandex.mdb-rw-mdb000000.query.consul",
			Port:     3306,
			User:     "analyst",
			Type:     MySQL,
		},
		Ssh: SshParams{
			Host: "h2p.vertis.yandex.net",
			Port: 2222,
			User: "userName",
		},
	}
	postgresqlParams = Params{
		Title: "catalog7_yandex(mdb000000)",
		Db: DbParams{
			Name:     "catalog7_yandex",
			Instance: "mdb000000",
			Mode:     "rw",
			Host:     "catalog7_yandex.pg-rw-mdb000000.query.consul",
			Port:     3306,
			User:     "analyst",
			Type:     PostgreSQL,
		},
		Ssh: SshParams{
			Host: "h2p.vertis.yandex.net",
			Port: 2222,
			User: "userName",
		},
	}
)

func TestNotifier_BuildMySQL(t *testing.T) {
	n := &Notifier{
		From: "no-reply@yandex-team.ru",
	}
	e, err := n.Build(mysqlParams)

	expectedHtml := `<p>
    Привет,
</p>
<p>
    Для того, чтобы подключиться к бд, нужно импортировать настройки в соответствующую IDE. Как это сделать,
    <a href="https://wiki.yandex-team.ru/vertis-admin/h2p/#importnastroekdljaide">описано в wiki</a>.
</p>
<div>Конфигурации для подключения к бд:</div>
<ul>
    <li>Sequel Pro &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">sequel-pro.plist</span></li>
    <li>Mysql Workbench &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">mysql-workbench.zip</span></li>
    <li>DataGrip &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">datagrip.txt</span></li>
    <li>TablePlus &mdash; Используйте следующий uri: <pre>mysql+ssh<b style="font-weight: normal">://</b>userName:<b style="font-weight: normal">@</b>h2p.vertis.yandex.net:2222/analyst<b style="font-weight: normal">@</b>catalog7_yandex.mdb-rw-mdb000000.query.consul:3306/catalog7_yandex?name=catalog7_yandex%28mdb000000%29&amp;tLSMode=1&amp;usePrivateKey=true</pre></li>
    <li>H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p -w mysql-mdb000000<b style="font-weight: normal">@</b>catalog7_yandex</pre></li>
</ul>
<p>
---<br/>
Ваш h2p
</p>`
	expectedText := `Привет, 

 Для того, чтобы подключиться к бд, нужно импортировать настройки в соответствующую IDE. Как это сделать, https://wiki.yandex-team.ru/vertis-admin/h2p/#importnastroekdljaide. 

 Конфигурации для подключения к бд: 
Sequel Pro — Настройки можно импортировать из прикрепленного файла sequel-pro.plist 
Mysql Workbench — Настройки можно импортировать из прикрепленного файла mysql-workbench.zip 
DataGrip — Настройки можно импортировать из прикрепленного файла datagrip.txt 
TablePlus — Используйте следующий uri: mysql+ssh://userName:@h2p.vertis.yandex.net:2222/analyst@catalog7_yandex.mdb-rw-mdb000000.query.consul:3306/catalog7_yandex?name=catalog7_yandex%28mdb000000%29&tLSMode=1&usePrivateKey=true 
H2P cli — Для подключения используйте следующую команду в консоли: h2p -w mysql-mdb000000@catalog7_yandex 
 

 ---
 Ваш h2p `

	require.NoError(t, err)
	require.Len(t, e.Attachments, 3)
	require.Equal(t, expectedHtml, string(e.HTML))
	require.Equal(t, expectedText, string(e.Text))
	require.Equal(t, "Конфигурации для подключения к БД catalog7_yandex(mdb000000)", e.Subject)
	require.Equal(t, n.From, e.From)
}

func TestNotifier_BuildPostgreSQL(t *testing.T) {
	n := &Notifier{
		From: "no-reply@yandex-team.ru",
	}
	e, err := n.Build(postgresqlParams)

	expectedHtml := `<p>
    Привет,
</p>
<p>
    Для того, чтобы подключиться к бд, нужно импортировать настройки в соответствующую IDE. Как это сделать,
    <a href="https://wiki.yandex-team.ru/vertis-admin/h2p/#importnastroekdljaide">описано в wiki</a>.
</p>
<div>Конфигурации для подключения к бд:</div>
<ul>
    <li>DataGrip &mdash; Настройки можно импортировать из прикрепленного файла <span style="white-space:nowrap">datagrip.txt</span></li>
    <li>H2P cli &mdash; Для подключения используйте следующую команду в консоли: <pre>h2p -w pg-mdb000000<b style="font-weight: normal">@</b>catalog7_yandex</pre></li>
</ul>
<p>
---<br/>
Ваш h2p
</p>`
	expectedText := `Привет, 

 Для того, чтобы подключиться к бд, нужно импортировать настройки в соответствующую IDE. Как это сделать, https://wiki.yandex-team.ru/vertis-admin/h2p/#importnastroekdljaide. 

 Конфигурации для подключения к бд: 
DataGrip — Настройки можно импортировать из прикрепленного файла datagrip.txt 
H2P cli — Для подключения используйте следующую команду в консоли: h2p -w pg-mdb000000@catalog7_yandex 
 

 ---
 Ваш h2p `

	require.NoError(t, err)
	require.Len(t, e.Attachments, 1)
	require.Equal(t, expectedHtml, string(e.HTML))
	require.Equal(t, expectedText, string(e.Text))
	require.Equal(t, "Конфигурации для подключения к БД catalog7_yandex(mdb000000)", e.Subject)
	require.Equal(t, n.From, e.From)
}

func TestNotifier_Send(t *testing.T) {
	t.Skip("Этот тест позволяет отправить письмо для отладки")

	n := &Notifier{
		Smtp: "yabacks-test.mail.yandex.net:25",
		From: "noreply-vertis-ops@yandex-team.ru",
	}
	require.NoError(t, n.Send("...@yandex-team.ru", mysqlParams))
	require.NoError(t, n.Send("...@yandex-team.ru", postgresqlParams))
}
