package startrack

import (
	"io/ioutil"
	"math"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	sMapMock "github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	serviceMapYml = "{name: test-service}"
	manifestYml   = `
name: test-service
test:
  datacenters:
    any:
      count: 1
prod:
  datacenters:
    any:
      count: 1
`
	serviceMapPath = "maps/test-service.yml"
	manifestPath   = "deploy/test-service.yml"
)

func TestService_Handler(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	sMapID, mID, uID := makeContext(t, db, log)

	deploymentData := data.NewService(db, log)
	service := NewService(deploymentData, NewConf(), log)

	server := httptest.NewServer(service.Handler())
	defer server.Close()

	type TestCase struct {
		name       string
		url        string
		wantCode   int
		wantAnswer string
		wantJson   string
	}
	testCases := []TestCase{
		{
			name:     "wrong endpoint",
			url:      "/dd",
			wantCode: 404,
		},
		{
			name:       "root",
			url:        "/deployment/",
			wantCode:   404,
			wantAnswer: `{"code":404,"error":"Not Found"}`,
		},
		{
			name:       "non-id url",
			url:        "/deployment/abc",
			wantCode:   404,
			wantAnswer: `{"code":404,"error":"Not Found"}`,
		},
		{
			name:       "big id",
			url:        "/deployment/1" + strconv.Itoa(math.MaxInt64),
			wantCode:   400,
			wantAnswer: `{"code":400,"error":"Bad Request"}`,
		},
		{
			name:       "missing deployment",
			url:        "/deployment/100",
			wantCode:   404,
			wantAnswer: `{"code":404,"error":"Not Found"}`,
		},
		func() TestCase {
			d := &dModel.Deployment{
				Layer:            common.Prod,
				DeployManifestID: mID,
				ServiceMapsID:    sMapID,
				Name:             "test-service",
				Version:          "my-ver-1",
				AuthorID:         uID,
				Type:             common.Update,
				StartDate:        time.Date(2011, 1, 10, 13, 01, 02, 0, time.Local),
			}
			db.GormDb.Create(d)
			return TestCase{
				name:       "existing deployment",
				url:        "/deployment/" + strconv.FormatInt(d.ID, 10),
				wantCode:   200,
				wantAnswer: `{"key":"1","summary":"[Prod] Update test-service:my-ver-1","assignee":{"login":"danevge"},"updated":"2011-01-10T13:01:02.000+0300"}`,
			}
		}(),
		func() TestCase {
			d := &dModel.Deployment{
				Layer:            common.Test,
				DeployManifestID: mID,
				ServiceMapsID:    sMapID,
				Name:             "test-service",
				Version:          "my-ver-1",
				AuthorID:         uID,
				Type:             common.Run,
				Branch:           "VOID-1",
				StartDate:        time.Date(2011, 1, 10, 13, 01, 02, 0, time.Local),
			}
			db.GormDb.Create(d)
			return TestCase{
				name:       "deploy to branch",
				url:        "/deployment/" + strconv.FormatInt(d.ID, 10),
				wantCode:   200,
				wantAnswer: `{"key":"2","summary":"[Test][VOID-1] Run test-service:my-ver-1","assignee":{"login":"danevge"},"updated":"2011-01-10T13:01:02.000+0300"}`,
			}
		}(),
	}

	for _, tt := range testCases {
		t.Run(
			tt.name, func(t *testing.T) {
				resp, err := http.Get(server.URL + tt.url)
				require.NoError(t, err)
				defer resp.Body.Close()

				assert.Equal(t, tt.wantCode, resp.StatusCode)
				bytes, err := ioutil.ReadAll(resp.Body)
				require.NoError(t, err)
				if tt.wantAnswer != "" {
					require.JSONEq(t, tt.wantAnswer, string(bytes))
				}
			},
		)
	}
}

func TestService_Handler_Post(t *testing.T) {
	test.InitTestEnv()

	log := test.NewLogger(t)
	service := NewService(nil, NewConf(), log)

	server := httptest.NewServer(service.Handler())
	defer server.Close()

	resp, err := http.Post(server.URL+"/deployment/1", "application/json", strings.NewReader(""))
	require.NoError(t, err)
	defer resp.Body.Close()

	assert.Equal(t, 405, resp.StatusCode)
	bytes, err := ioutil.ReadAll(resp.Body)
	require.NoError(t, err)
	require.JSONEq(t, `{"code":405,"error":"Method Not Allowed"}`, string(bytes))
}

func makeContext(t *testing.T, db *storage.Database, log logger.Logger) (int64, int64, int64) {
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &dModel.Deployment{}, &approve.Approve{}))

	sMapSrv := service_map.NewService(db, log, sMapMock.NewNotificationMock())
	includeSrv := include.NewService(db, log)
	manifestSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)

	require.NoError(t, sMapSrv.ReadAndSave([]byte(serviceMapYml), 10, serviceMapPath))
	require.NoError(t, manifestSrv.ReadAndSave([]byte(manifestYml), 10, manifestPath))

	_, sMapID, err := sMapSrv.GetByFullPath(serviceMapPath)
	require.NoError(t, err)
	_, mID, err := manifestSrv.GetByNameWithId(common.Prod, "test-service")
	require.NoError(t, err)

	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)

	return sMapID, mID, user.ID
}
