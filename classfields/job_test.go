package job

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"testing"

	"aptly/pkg/arcanum"
	"aptly/pkg/packages"
	"aptly/pkg/s3"
	"aptly/pkg/st"
	mArc "aptly/test/mocks/arc"
	mArcanum "aptly/test/mocks/arcanum"
	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	testBranch         = "test"
	testNewCurlVersion = "2.0"
	testAnsibleVersion = "2.9.6+dfsg-1"
	testProdFile       = []byte(`
---
- name: Install updates for Focal
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 3600
    force: yes
    pkg:
      - ansible=2.9.6+dfsg-1
      - curl=1.0
`)
	testDescription = `Сервис **aptly** обнаружил новые версии пакетов для обновления.
Ниже представлен список файлов и изменений в них:


{% cut "prod" %}

|Пакет|Текущая версия|Новая версия|
|:---|:---|:---|
|curl|{yellow}(**1.0**)|{green}(**2.0**)|

{% endcut %}


---

Пулл-реквест подготовлен и ждет одобрения:
<https://a.yandex-team.ru/review/2/details>
`
)

func TestJob_Run(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	job := New(
		mockArc(t),
		mockArcanum(t, c),
		st.New(c),
		s3.New(c),
		logger,
	)

	tpMap := map[string]string{"test": "prod"}
	job.Run(tpMap)
}

func mockArc(t *testing.T) *mArc.IClient {
	t.Helper()

	m := mArc.NewIClient(t)
	m.On("CreateBranch").Return(testBranch, nil).Once()
	m.On("GetFile", "prod").Return(testProdFile, nil).Once()
	m.On("CommitFile", testBranch, "prod", mock.Anything).Run(func(args mock.Arguments) {
		content := args.Get(2)
		pkgs, err := packages.Parse(content.([]byte))

		require.NoError(t, err)
		require.Equal(t, testNewCurlVersion, pkgs["curl"])
		require.Equal(t, testAnsibleVersion, pkgs["ansible"])
	}).Return(nil).Once()

	return m
}

func mockArcanum(t *testing.T, c conf.Conf) *mArcanum.IClient {
	t.Helper()

	m := mArcanum.NewIClient(t)
	m.On("GetPullRequestStatus", mock.AnythingOfType("int64")).Return(arcanum.StatusMerged, nil).Once()
	m.On("CreatePullRequest", testBranch, mock.Anything).Return(int64(2), nil).Once()
	m.On("CommentPullRequest", int64(2), mock.Anything).Run(func(args mock.Arguments) {
		issue := args.Get(1).(string)

		req, err := http.NewRequest(http.MethodGet, c.Str("ST_URL")+"/v2/issues/"+issue, nil)
		require.NoError(t, err)
		req.Header.Add("Authorization", "OAuth "+c.Str("ST_TOKEN"))

		var cli http.Client
		resp, err := cli.Do(req)
		require.NoError(t, err)

		body, err := ioutil.ReadAll(resp.Body)
		require.NoError(t, err)
		require.NoError(t, resp.Body.Close())

		var info struct {
			Summary     string `json:"summary"`
			Description string `json:"description"`
		}
		require.NoError(t, json.Unmarshal(body, &info))

		require.Equal(t, "[aptly] Update packages in PROD", info.Summary)
		require.Equal(t, testDescription, info.Description)
	}).Return(nil).Once()

	return m
}
