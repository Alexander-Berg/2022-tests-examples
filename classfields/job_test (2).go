package job

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"testing"

	"aptly/pkg/aptly"
	"aptly/pkg/packages"
	"aptly/pkg/s3"
	"aptly/pkg/st"
	mAptly "aptly/test/mocks/aptly"
	mArc "aptly/test/mocks/arc"
	mArcanum "aptly/test/mocks/arcanum"
	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"golang.org/x/exp/maps"
)

var (
	testBranch         = "test"
	testFileName       = "test"
	testNewCurlVersion = "2.0"
	testAnsibleVersion = "2.9.6+dfsg-1"
	testFile           = []byte(`
---
- name: Install updates for Focal
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 3600
    pkg:
      - ansible=2.9.6+dfsg-1
      - curl=1.0
`)
	testDescription = `Сервис **aptly** обнаружил новые версии пакетов для обновления.
Ниже представлен список файлов и изменений в них:


{% cut "test" %}

|Пакет|Текущая версия|Новая версия|
|:---|:---|:---|
|curl|{yellow}(**1.0**)|{green}(**2.0**)|

{% endcut %}


---

Пулл-реквест подготовлен и ждет одобрения:
<https://a.yandex-team.ru/review/1/details>
`

	testFileName1 = "file1"
	testFileName2 = "file2"
	testFile1     = []byte(`
---
- name: Install common updates
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 3600
    pkg:
      - ansible=2.9.6+dfsg-1
      - nginx
`)
	testFile2 = []byte(`
---
- name: Install updates for Focal
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 3600
    pkg:
      - ansible=2.10.6+dfsg-1
      - curl=1.0
`)
)

func TestJob_Run(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	job := New(
		c,
		mockAptly(t),
		mockArc(t),
		mockArcanum(t, c),
		st.New(c),
		s3.New(c),
		logger,
	)

	repos := []aptly.Repo{{Name: "focal"}}
	files := map[string][]string{"test": {"focal"}}
	job.Run(repos, files)
}

func TestJob_getPackages(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	arc := &mArc.IClient{}
	arc.On("GetFile", testFileName1).Return(testFile1, nil).Once()
	arc.On("GetFile", testFileName2).Return(testFile2, nil).Once()

	t.Cleanup(func() {
		arc.AssertExpectations(t)
	})

	job := New(
		c,
		&mAptly.IService{},
		arc,
		&mArcanum.IClient{},
		st.New(c),
		s3.New(c),
		logger,
	)

	pkgs, err := job.getPackages(aptly.Repo{Name: "test"}, map[string][]string{testFileName1: {"test"}, testFileName2: {"test"}})
	require.NoError(t, err)
	require.ElementsMatch(t, []string{"ansible", "curl", "nginx"}, maps.Keys(pkgs))
}

func mockArc(t *testing.T) *mArc.IClient {
	t.Helper()

	m := mArc.NewIClient(t)
	m.On("GetFile", testFileName).Return(testFile, nil).Once()
	m.On("CreateBranch").Return(testBranch, nil).Once()
	m.On("CommitFile", testBranch, testFileName, mock.Anything).Run(func(args mock.Arguments) {
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
	m.On("CreatePullRequest", testBranch, mock.Anything).Return(int64(1), nil).Once()
	m.On("CommentPullRequest", int64(1), mock.Anything).Run(func(args mock.Arguments) {
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

		require.Equal(t, "[aptly] Update packages in TEST", info.Summary)
		require.Equal(t, testDescription, info.Description)
	}).Return(nil).Once()

	return m
}

func mockAptly(t *testing.T) *mAptly.IService {
	t.Helper()

	repo := mAptly.NewIRepo(t)
	repo.On("Update").Return(packages.Map{"curl": "2.0", "not-new-package": "14.9.1"}, nil).Once()

	m := mAptly.NewIService(t)
	m.On("NewRepoFrom", mock.AnythingOfType("aptly.Repo"), mock.Anything).Return(repo).Once()

	return m
}
