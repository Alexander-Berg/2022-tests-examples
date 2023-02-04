package main

import (
	"github.com/YandexClassifieds/shiva/cmd/generator/handler"
	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	"github.com/YandexClassifieds/shiva/pkg/grafana"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard"
	service_map2 "github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test"
	mocks "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
)

func skipCI(t *testing.T) {
	if os.Getenv("CI") != "" {
		t.Skip("Only for manual testing")
	}
}

func TestOnUpdate(t *testing.T) {
	skipCI(t)
	const servicesJSON = ""
	test.InitTestEnv()
	log := test.NewLogger(t)
	testDB := test_db.NewSeparatedDb(t)
	taskS := task.NewService(log, testDB)

	cli := prepareApi(t)
	ser := dashboard.NewService(cli, log)

	gormDb, err := storage.OpenGorm(log)
	if err != nil {
		log.WithError(err).Fatal("gorm.Open failed")
	}
	updaterDB := storage.NewDatabase(gormDb, log, nil, nil, nil)
	mapService := service_map2.NewService(updaterDB, log, nil)
	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("GetByPaths", mock2.Anything, mock2.Anything).Return(&service_map.GetByPathsResponse{}, nil)
	h := handler.NewGrafanaHandler(log, taskS, ser, sMapCliMock)

	local := simpleClient{
		token:  "Bearer " + config.Str("GRAFANA_TOKEN"),
		url:    config.Str("GRAFANA_URL"),
		client: createHttpClient(2),
	}
	folder, err := local.GetFolder("Generated")
	if err != nil {
		log.Fatal(err)
	}
	dashboards, err := local.SearchAllInFolder(folder)
	if err != nil {
		log.Fatal(err)
	}

	for i, dashboard := range dashboards {
		log.Infof("start processing %d of %d: %s \n", i, len(dashboards), dashboard.Title)

		taskModel := &task.Task{
			Service:    "shiva",
			ChangeType: events.ChangeType_UPDATE,
			Handler:    h.Name(),
			State:      task.New,
		}
		sMap, err := mapService.GetByName(dashboard.Title)
		if err != nil {
			log.Error(err)
		}

		err = h.OnUpdate(taskModel, sMap, sMap)
		require.NoError(t, err)
	}
}

func prepareApi(t *testing.T) *grafana.Client {
	test.InitTestEnv()
	return grafana.NewClient(grafana.NewApiConf(), test.NewLogger(t))
}
