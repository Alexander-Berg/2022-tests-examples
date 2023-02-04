package status

import (
	_ "embed"
	"fmt"
	"strconv"
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	sMapPB "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	manifestParser "github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	svcName    = "status-svc"
	svcVersion = "v42"
	//go:embed testdata/map.yml
	yamlMapFile string
	//go:embed testdata/manifest.yml
	yamlManifestFile string
)

func TestHandle(t *testing.T) {
	type Params struct {
		traffic traffic.Traffic
		branch  string
		state   model.State
		dType   dtype.DeploymentType
		layer   layer.Layer
	}
	type TestCase struct {
		Name                    string
		prepare                 *Params
		params                  *Params
		assertBranchDomainNames string
		assertDomainNames       string
		assertTraffic           traffic.Traffic
		noStoredResult          bool
	}

	cases := []*TestCase{
		{
			Name: "Run_CanaryOnePercent",
			params: &Params{
				state: model.CanaryOnePercent,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			assertBranchDomainNames: BranchDomains("canary", common.Prod),
			assertDomainNames:       Domains(common.Prod),
			assertTraffic:           traffic.Traffic_ONE_PERCENT,
		},
		{
			Name: "Run_Canary",
			params: &Params{
				state: model.CanarySuccess,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			assertBranchDomainNames: BranchDomains("canary", common.Prod),
			assertDomainNames:       Domains(common.Prod),
			assertTraffic:           traffic.Traffic_REAL,
		},
		{
			Name: "Run_CanaryStopped",
			params: &Params{
				state: model.CanaryStopped,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			noStoredResult: true,
		},
		{
			Name: "Run_CanaryOnePercent_CanaryStopped",
			prepare: &Params{
				state: model.CanaryOnePercent,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			params: &Params{
				state: model.CanaryStopped,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			noStoredResult: true,
		},
		{
			Name: "Run_Canary_CanaryStopped",
			prepare: &Params{
				state: model.CanarySuccess,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			params: &Params{
				state: model.CanaryStopped,
				dType: dtype.DeploymentType_RUN,
				layer: layer.Layer_PROD,
			},
			noStoredResult: true,
		},
		{
			Name: "Update_CanaryOnePercent",
			params: &Params{
				state: model.CanaryOnePercent,
				dType: dtype.DeploymentType_UPDATE,
				layer: layer.Layer_PROD,
			},
			assertBranchDomainNames: BranchDomains("canary", common.Prod),
			assertDomainNames:       Domains(common.Prod),
			assertTraffic:           traffic.Traffic_ONE_PERCENT,
		},
		{
			Name: "Update_Canary",
			params: &Params{
				state: model.CanarySuccess,
				dType: dtype.DeploymentType_UPDATE,
				layer: layer.Layer_PROD,
			},
			assertBranchDomainNames: BranchDomains("canary", common.Prod),
			assertDomainNames:       Domains(common.Prod),
			assertTraffic:           traffic.Traffic_REAL,
		},
	}

	for _, c := range cases {
		t.Run(c.Name, func(t *testing.T) {
			test.RunUp(t)
			defer test.Down(t)

			db := test_db.NewDb(t)
			log := test.NewLogger(t)
			mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
			svc := NewService(db, log, mapService)

			err := svc.mapSvc.ReadAndSave([]byte(yamlMapFile), 10, sMapPB.ToFullPath(svcName))
			require.NoError(t, err)

			_, mapID, err := svc.mapSvc.GetByFullPath(sMapPB.ToFullPath(svcName))
			require.NoError(t, err)

			staffApi := staffapi.NewApi(staffapi.NewConf(), log)
			staffService := staff.NewService(db, staffApi, log)
			user, err := staffService.GetByLogin("danevge")
			require.NoError(t, err)
			uID := user.ID

			mParser := manifestParser.NewService(log, nil)
			includeService := include.NewService(db, log)
			mService := manifest.NewService(db, log, mParser, includeService)
			require.NoError(t, mService.ReadAndSave([]byte(yamlManifestFile), 10, sMapPB.ToFullPath(svcName)))
			_, mID, err := mService.GetByNameWithId(common.Prod, svcName)
			require.NoError(t, err)

			if c.prepare != nil {
				p := c.prepare
				handleD(t, mapID, mID, uID, svc, p.traffic, p.branch, p.state, p.dType, p.layer)
			}
			p := c.params
			d := handleD(t, mapID, mID, uID, svc, p.traffic, p.branch, p.state, p.dType, p.layer)

			result, err := svc.GetAllRunning(true)
			if c.noStoredResult {
				assert.Empty(t, result)
				return
			}
			fmt.Println(err)
			fmt.Println(result)

			require.Len(t, result, 1)
			r := result[0]
			assert.Equal(t, StateRunning, r.State)
			assert.Equal(t, d.Branch, r.Branch)
			assert.Equal(t, d.Layer, r.Layer)
			assert.Equal(t, c.assertDomainNames, r.MainDomainNames)
			assert.Equal(t, c.assertBranchDomainNames, r.BranchDomainNames)
			if c.assertBranchDomainNames != "" {
				assert.Equal(t, int64(99), r.BranchUIDSuffix)
			}
			assertTraffic(t, c.assertTraffic, r.Traffic)
		})
	}
}

func TestStatuses(t *testing.T) {
	test.RunUp(t)

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&model.Deployment{}))
	log := test.NewLogger(t)
	mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
	mParser := manifestParser.NewService(log, nil)
	includeService := include.NewService(db, log)
	mService := manifest.NewService(db, log, mParser, includeService)
	svc := NewService(db, log, mapService)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)

	err := svc.mapSvc.ReadAndSave([]byte(yamlMapFile), 10, sMapPB.ToFullPath(svcName))
	require.NoError(t, err)
	_, mapID, err := svc.mapSvc.GetByFullPath(sMapPB.ToFullPath(svcName))
	require.NoError(t, err)

	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	uID := user.ID

	require.NoError(t, mService.ReadAndSave([]byte(yamlManifestFile), 10, sMapPB.ToFullPath(svcName)))
	_, mID, err := mService.GetByNameWithId(common.Prod, svcName)
	require.NoError(t, err)

	// without branch in prod
	prod := handleD(t, mapID, mID, uID, svc, traffic.Traffic_UNKNOWN, "", model.Success, dtype.DeploymentType_PROMOTE, layer.Layer_PROD)
	// without branch in prod
	canary := handleD(t, mapID, mID, uID, svc, traffic.Traffic_UNKNOWN, "", model.CanarySuccess, dtype.DeploymentType_RUN, layer.Layer_PROD)
	// branch with trafficShare 1% in prod
	prod1 := handleD(t, mapID, mID, uID, svc, traffic.Traffic_ONE_PERCENT, "b1", model.Success, dtype.DeploymentType_PROMOTE, layer.Layer_PROD)
	// branch with real trafficShare in prod
	prod2 := handleD(t, mapID, mID, uID, svc, traffic.Traffic_REAL, "b2", model.Success, dtype.DeploymentType_PROMOTE, layer.Layer_PROD)
	// branch without trafficShare in prod
	handleD(t, mapID, mID, uID, svc, traffic.Traffic_UNKNOWN, "b3", model.Success, dtype.DeploymentType_PROMOTE, layer.Layer_PROD)
	// run and stop
	handleD(t, mapID, mID, uID, svc, traffic.Traffic_ONE_PERCENT, "b4", model.Success, dtype.DeploymentType_RUN, layer.Layer_TEST)
	handleD(t, mapID, mID, uID, svc, traffic.Traffic_ONE_PERCENT, "b4", model.Success, dtype.DeploymentType_STOP, layer.Layer_TEST)
	// update
	prod32 := newDeploymentModel(mapID, mID, uID, traffic.Traffic_UNKNOWN, model.Success, common.Promote, common.Prod, "B3")
	prod32.Version = "updated_version"
	require.NoError(t, svc.dstore.Save(prod32))
	require.NoError(t, svc.handle(newEvent(prod32, state.DeploymentState_SUCCESS, dtype.DeploymentType_PROMOTE, layer.Layer_PROD)))

	result, err := svc.GetAllRunning(true)
	require.NoError(t, err)

	// 3 branches, prod, canary
	require.Len(t, result, 5)
	type resultKey struct {
		Layer  common.Layer
		Branch string
		Canary bool
	}
	resultMap := make(map[resultKey]*Status)
	for _, v := range result {
		k := resultKey{
			Layer:  v.Layer,
			Branch: v.Branch,
			Canary: v.Canary,
		}
		resultMap[k] = v
	}

	{
		r := resultMap[resultKey{Layer: common.Prod, Canary: true}]
		assert.Equal(t, StateRunning, r.State)
		assert.Equal(t, canary.Layer, r.Layer)
		assert.Equal(t, Domains(r.Layer), r.MainDomainNames)
		assert.Equal(t, BranchDomains(config.Canary, common.Prod), r.BranchDomainNames)
		assertTraffic(t, traffic.Traffic_REAL, r.Traffic)
	}
	{
		r := resultMap[resultKey{Layer: common.Prod}]
		assert.Equal(t, StateRunning, r.State)
		assert.Equal(t, prod.Layer, r.Layer)
	}
	{
		r := resultMap[resultKey{Layer: common.Prod, Branch: "b1"}]
		assert.Equal(t, StateRunning, r.State)
		assert.Equal(t, prod1.Branch, r.Branch)
		assert.Equal(t, prod1.Layer, r.Layer)
		assertTraffic(t, traffic.Traffic_ONE_PERCENT, r.Traffic)
	}
	{
		r := resultMap[resultKey{Layer: common.Prod, Branch: "b2"}]
		assert.Equal(t, StateRunning, r.State)
		assert.Equal(t, prod2.Branch, r.Branch)
		assert.Equal(t, prod2.Layer, r.Layer)
		assertTraffic(t, traffic.Traffic_REAL, r.Traffic)
	}
	{
		r := resultMap[resultKey{Layer: common.Prod, Branch: "B3"}]
		assert.Equal(t, StateRunning, r.State)
		assert.Equal(t, prod32.Branch, r.Branch)
		assert.Equal(t, prod32.Layer, r.Layer)
		assert.Equal(t, "updated_version", r.Version)
		assert.Equal(t, Domains(prod32.Layer), r.MainDomainNames)
		assert.Equal(t, BranchDomains("b3", prod32.Layer), r.BranchDomainNames)
		assertTraffic(t, traffic.Traffic_UNKNOWN, r.Traffic)
	}
}

func BranchDomains(name string, l common.Layer) string {
	lStr := strings.ToLower(l.String())
	return fmt.Sprintf(
		"status-svc-%s-deploy.vrts-slb.%s.vertis.yandex.net "+
			"status-svc-%s-tcp-thing.vrts-slb.%s.vertis.yandex.net "+
			"status-svc-%s-http-api.vrts-slb.%s.vertis.yandex.net "+
			"status-svc-%s-old.vrts-slb.%s.vertis.yandex.net", name, lStr, name, lStr, name, lStr, name, lStr)
}

func Domains(l common.Layer) string {
	lStr := strings.ToLower(l.String())
	return fmt.Sprintf("status-svc-deploy.vrts-slb.%s.vertis.yandex.net "+
		"status-svc-tcp-thing.vrts-slb.%s.vertis.yandex.net "+
		"status-svc-http-api.vrts-slb.%s.vertis.yandex.net "+
		"status-svc-old.vrts-slb.%s.vertis.yandex.net", lStr, lStr, lStr, lStr)
}

func newEvent(d *model.Deployment, dst state.DeploymentState, dt dtype.DeploymentType, l layer.Layer) *event2.Event {
	return &event2.Event{
		Deployment: &deployment.Deployment{
			Id:          strconv.FormatInt(d.ID, 10),
			ServiceName: d.Name,
			Version:     d.Version,
			State:       dst,
			Type:        dt,
			Layer:       l,
			Branch:      d.Branch,
			SmapId:      strconv.FormatInt(d.ServiceMapsID, 10),
			Traffic:     d.Traffic,
			End:         ptypes.TimestampNow(),
		},
	}
}

func assertTraffic(t *testing.T, want, actual traffic.Traffic) {
	assert.Equal(t, want.String(), actual.String())
}

func newDeploymentModel(dSMapID, mID, userID int64, tr traffic.Traffic, st model.State, t common.Type, l common.Layer, b string) *model.Deployment {
	return &model.Deployment{
		State:            st,
		Type:             t,
		Layer:            l,
		Name:             svcName,
		Version:          svcVersion,
		Traffic:          tr,
		Branch:           b,
		ServiceMapsID:    dSMapID,
		AuthorID:         userID,
		DeployManifestID: mID,
	}
}

func handleD(t *testing.T, dSMapID, mID, userID int64, s *Service, tr traffic.Traffic, b string, st model.State, dt dtype.DeploymentType, l layer.Layer) *model.Deployment {
	d := newDeploymentModel(dSMapID, mID, userID, tr, st, dt.ToCommonType(), l.GetCommonLayer(), b)
	require.NoError(t, s.dstore.Save(d))
	require.NoError(t, s.handle(newEvent(d, d.GetStateProto(), dt, l)))
	return d
}
