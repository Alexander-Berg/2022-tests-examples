package handler

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/board"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/panel"
	"github.com/YandexClassifieds/shiva/test"
	mocks "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	testPanelsFunc = []panelFunc{
		panel.MakeResourcePanel,
		panel.MakeProvidesPanel,
		panel.MakeRequestTimingPanel,
	}

	sMap = &proto.ServiceMap{
		Name: "shiva",
		Provides: []*proto.ServiceProvides{
			{
				Name:     "deploy",
				Protocol: proto.ServiceProvides_grpc,
			},
			{
				Name:     "ci",
				Protocol: proto.ServiceProvides_http,
			},
		},
		Type: proto.ServiceType_service,
		DependsOn: []*proto.ServiceDependency{
			{Service: "mysql/mdbsvc1"},
			{Service: "some-srv1"},
			{ServiceName: "some-srv2"}},
		Language: proto.ServiceLanguage_GO,
	}

	sMapNew = &proto.ServiceMap{
		Name:   "shiva",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/"},
		Provides: []*proto.ServiceProvides{
			{
				Name:     "deploy",
				Protocol: proto.ServiceProvides_grpc,
			},
			{
				Name:     "ci",
				Protocol: proto.ServiceProvides_http,
			},
			{
				Name:     "public",
				Protocol: proto.ServiceProvides_grpc,
			},
			{
				Name:     "admin",
				Protocol: proto.ServiceProvides_grpc,
			},
		},
		DependsOn: []*proto.ServiceDependency{{Service: "srv1"}},
	}

	newBoard             = makeNewBoard(sMap)
	userDashboard        = makeUserBoard(sMap)
	regeneratedDashboard = makeRegeneratedDashboard(sMapNew)
)

func TestOnNew(t *testing.T) {
	log, taskS := prepare(t)

	dSvcMock := &DashboardSvcMock{}
	dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(nil, nil)
	dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil)
	dSvcMock.On("GetFolderByTitle", mock2.Anything).Return(nil, common.ErrNotFound)

	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("GetByPaths", mock2.Anything, mock2.Anything).Return(&service_map.GetByPathsResponse{}, nil)

	handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
	handler.(*Grafana).panelsFunc = testPanelsFunc

	taskModel := &task.Task{
		Service:    "shiva",
		ChangeType: events.ChangeType_NEW,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnNew(taskModel, sMap))
	expectedBoard := newBoard
	assertDashboard(t, expectedBoard, dSvcMock.board)
}

func TestOnNewDuplicateFolder(t *testing.T) {
	log, taskS := prepare(t)

	dSvcMock := &DashboardSvcMock{}
	dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(nil, nil)
	dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil)
	dSvcMock.On("GetFolderByTitle", mock2.Anything).Return(nil, nil)

	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("GetByPaths", mock2.Anything, mock2.Anything).Return(&service_map.GetByPathsResponse{}, nil)

	handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
	handler.(*Grafana).panelsFunc = testPanelsFunc

	taskModel := &task.Task{
		Service:    "shiva",
		ChangeType: events.ChangeType_NEW,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnNew(taskModel, sMap))
	expectedBoard := newBoard
	expectedBoard.SetTitle("shiva_")
	assertDashboard(t, expectedBoard, dSvcMock.board)
}

func TestOnUpdate(t *testing.T) {
	log, taskS := prepare(t)

	testCases := []struct {
		name       string
		changeType events.ChangeType
	}{
		{
			"update",
			events.ChangeType_UPDATE,
		},
		{
			"new when dashboard already exists",
			events.ChangeType_NEW,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			dSvcMock := &DashboardSvcMock{}
			dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(userDashboard, nil)
			dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil)
			dSvcMock.On("GetFolderByTitle", mock2.Anything).Return(nil, common.ErrNotFound)

			sMapCliMock := &mocks.ServiceMapsClient{}
			sMapCliMock.On("GetByPaths", mock2.Anything, mock2.Anything).Return(&service_map.GetByPathsResponse{}, nil)

			handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
			handler.(*Grafana).panelsFunc = testPanelsFunc

			taskModel := &task.Task{
				Service:    "shiva",
				ChangeType: tc.changeType,
				Handler:    handler.Name(),
				State:      task.New,
			}

			switch tc.changeType {
			case events.ChangeType_UPDATE:
				require.NoError(t, handler.OnUpdate(taskModel, sMapNew, sMap))
			case events.ChangeType_NEW:
				require.NoError(t, handler.OnNew(taskModel, sMapNew))
			}

			expectedBoard := regeneratedDashboard
			assertDashboard(t, expectedBoard, dSvcMock.board)
		})
	}
}

func TestOnNewWithMdb(t *testing.T) {
	log, taskS := prepare(t)

	dSvcMock := &DashboardSvcMock{}
	dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(nil, nil)
	dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil)
	dSvcMock.On("GetFolderByTitle", mock2.Anything).Return(nil, common.ErrNotFound)

	mdbSMap := &proto.ServiceMap{
		Name:       "mdbsvc1",
		Type:       proto.ServiceType_mdb_mysql,
		MdbCluster: &proto.MDBCluster{ProdId: "ProdId", TestId: "TestId"},
	}

	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("GetByPaths", mock2.Anything, &service_map.GetByPathsRequest{
		Path: []string{"maps/mysql/mdbsvc1.yml", "maps/some-srv1.yml", "maps/some-srv2.yml"}}).
		Return(&service_map.GetByPathsResponse{Services: []*proto.ServiceMap{mdbSMap, {}, {}}}, nil)

	handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
	taskModel := &task.Task{
		Service:    "shiva",
		ChangeType: events.ChangeType_NEW,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnNew(taskModel, sMap))

	dashboard := dSvcMock.board

	gHandler := handler.(*Grafana)
	require.Equal(t, len(gHandler.panelsFunc)-1, len(dashboard.GetPanels()))
	require.Equal(t, 3, len(dashboard.GetTemplates()))
}

func TestOnUpdateMdb(t *testing.T) {
	log, taskS := prepare(t)

	dSvcMock := &DashboardSvcMock{}
	dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(userDashboard, nil).Twice()
	dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil).Twice()

	mdbSMap := &proto.ServiceMap{
		Name:     "mdbsvc",
		Type:     proto.ServiceType_mdb_mysql,
		MdbMysql: &proto.MDBMySQL{ProdId: "ProdId", TestId: "TestId"},
		Path:     "maps/mysql/mdbsvc.yml",
	}

	mdbSMapOld := &proto.ServiceMap{
		Name:     "mdbsvc",
		Type:     proto.ServiceType_mdb_mysql,
		MdbMysql: &proto.MDBMySQL{ProdId: "ProdId1", TestId: "TestId1"},
		Path:     "maps/mysql/mdbsvc.yml",
	}

	sMap1 := &proto.ServiceMap{
		Name: "s1",
		DependsOn: []*proto.ServiceDependency{
			{Service: "some-srv"},
			{Service: "mysql/mdbsvc"},
			{Service: "some-srv1"},
		},
	}
	sMap2 := sMap1

	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("List", mock2.Anything, mock2.Anything).Return(&service_map.ListResponse{Service: []*proto.ServiceMap{sMap, sMap1, sMap2}}, nil)

	sMapCliMock.On("GetByPaths", mock2.Anything, &service_map.GetByPathsRequest{
		Path: []string{"maps/some-srv.yml", "maps/mysql/mdbsvc.yml", "maps/some-srv1.yml"}}).
		Return(&service_map.GetByPathsResponse{Services: []*proto.ServiceMap{mdbSMap, {}, {}}}, nil).Twice()

	handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
	taskModel := &task.Task{
		Service:    "mdbsvc",
		ChangeType: events.ChangeType_UPDATE,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnUpdate(taskModel, mdbSMap, mdbSMapOld))
	dSvcMock.AssertExpectations(t)
}

func TestOnNewMdb(t *testing.T) {
	log, taskS := prepare(t)

	mdbSMap := &proto.ServiceMap{
		Name:     "mdbsvc",
		Type:     proto.ServiceType_mdb_mysql,
		MdbMysql: &proto.MDBMySQL{ProdId: "ProdId", TestId: "TestId"},
	}

	handler := NewGrafanaHandler(log, taskS, nil, nil)
	taskModel := &task.Task{
		Service:    "mdbsvc",
		ChangeType: events.ChangeType_UPDATE,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnNew(taskModel, mdbSMap))
}

func TestOnNewWithBatch(t *testing.T) {
	log, taskS := prepare(t)

	dSvcMock := &DashboardSvcMock{}
	dSvcMock.On("GetDashboard", mock2.Anything, mock2.Anything).Return(nil, nil)
	dSvcMock.On("SaveDashboard", mock2.Anything, mock2.Anything).Return(nil)
	dSvcMock.On("GetFolderByTitle", mock2.Anything).Return(nil, common.ErrNotFound)

	batchSMap := &proto.ServiceMap{
		Name: "batch",
		Type: proto.ServiceType_batch,
	}

	sMapCliMock := &mocks.ServiceMapsClient{}
	sMapCliMock.On("GetByPaths", mock2.Anything, mock2.Anything).Return(&service_map.GetByPathsResponse{}, nil)

	handler := NewGrafanaHandler(log, taskS, dSvcMock, sMapCliMock)
	taskModel := &task.Task{
		Service:    "batch",
		ChangeType: events.ChangeType_NEW,
		Handler:    handler.Name(),
		State:      task.New,
	}

	require.NoError(t, handler.OnNew(taskModel, batchSMap))
	require.Equal(t, 2, len(dSvcMock.board.GetPanels()))
}

func assertDashboard(t *testing.T, expected, actual *board.Dashboard) {
	require.Equal(t, len(expected.GetAnnotations()), len(actual.GetAnnotations()))
	for i := range expected.GetAnnotations() {
		require.Equal(t, expected.GetAnnotations()[i].Name, actual.GetAnnotations()[i].Name)
	}

	require.Equal(t, len(expected.GetTemplates()), len(actual.GetTemplates()))
	for i := range expected.GetTemplates() {
		require.Equal(t, expected.GetTemplates()[i].Name, actual.GetTemplates()[i].Name)
	}

	assertPanels(t, expected, actual)
}

func assertPanels(t *testing.T, expected, actual *board.Dashboard) {
	require.Equal(t, len(expected.GetPanels()), len(actual.GetPanels()))

	for i := range expected.GetPanels() {
		require.Equal(t, expected.GetPanels()[i].GetTitle(), actual.GetPanels()[i].GetTitle())
		require.Equal(t, len(expected.GetPanels()[i].GetPanels()), len(actual.GetPanels()[i].GetPanels()))

		expPanels := expected.GetPanels()[i].GetPanels()
		actPanels := actual.GetPanels()[i].GetPanels()
		require.Equal(t, len(expPanels), len(actPanels))

		for j := range expPanels {
			require.Equal(t, getPanelTitle(expPanels[j]), getPanelTitle(actPanels[j]))
		}
	}
}

func getPanelTitle(p interface{}) string {
	var title string
	switch expPanel := p.(type) {
	case *panel.Preferences:
		title = expPanel.Title
	case panel.Panel:
		title = expPanel.GetTitle()
	}

	return title
}

func prepare(t *testing.T) (logger.Logger, *task.Service) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)

	return log, taskS
}

type DashboardSvcMock struct {
	mock2.Mock
	board *board.Dashboard
}

func (m *DashboardSvcMock) SaveDashboard(dashboard *board.Dashboard, folderTitle string) error {
	m.board = dashboard
	args := m.Called(dashboard, folderTitle)
	return args.Error(0)
}

func (m *DashboardSvcMock) GetDashboard(title, folderTitle string) (*board.Dashboard, error) {
	args := m.Called(title, folderTitle)

	var d *board.Dashboard
	if args.Get(0) != nil {
		d = args.Get(0).(*board.Dashboard)
	}
	return d, args.Error(1)
}

func (m *DashboardSvcMock) GetFolderByTitle(folderTitle string) (*panel.Folder, error) {
	args := m.Called(folderTitle)

	var d *panel.Folder
	if args.Get(0) != nil {
		d = args.Get(0).(*panel.Folder)
	}
	return d, args.Error(1)
}

func (m *DashboardSvcMock) DeleteDashboard(title, folderTitle string) error {
	panic("implement me")
}

func makeUserBoard(sMap *proto.ServiceMap) *board.Dashboard {
	b, _ := board.MakeFromRawResponse(map[string]interface{}{})

	b.SetTitle(sMap.Name)
	b.AddTemplates([]board.Template{
		{Name: panel.DatasourceTmp},
		{Name: panel.DcTmp},
		{Name: "var1"},
		{Name: "var2"},
	})
	b.AddAnnotations([]board.Annotation{
		{Name: sMap.Name},
		{Name: board.Drills},
		{Name: board.Depends},
		{Name: "an1"},
		{Name: "an2"},
	})
	b.AddPanels([]*panel.Panel{
		panel.MakeFromPreferences(panel.Preferences{Title: "p1", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p2", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "_Resources", Type: panel.RowType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "_p3", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "userPanel", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "UserRow", Type: panel.RowType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p4", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "_Provides",
			Type:      panel.RowType,
			Collapsed: true,
			Panels: []*panel.Preferences{
				{
					Title: fmt.Sprintf("_%s-%s codes", sMap.Name, sMap.Provides[0].Name),
					Type:  panel.TimeSeriesType,
				},
				{
					Title: fmt.Sprintf("_%s-%s codes", sMap.Name, sMap.Provides[1].Name),
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p5", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "_SomeGenRow", Type: panel.RowType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p6", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "UserRowCollapsed",
			Type:      panel.RowType,
			Collapsed: true,
			Panels: []*panel.Preferences{
				{Title: "p7", Type: panel.TimeSeriesType},
				{Title: "p8", Type: panel.TimeSeriesType},
			},
		}),
	})

	return b
}

func makeRegeneratedDashboard(sMap *proto.ServiceMap) *board.Dashboard {
	b, _ := board.MakeFromRawResponse(map[string]interface{}{})

	b.SetTitle(sMap.Name)
	b.AddTemplates([]board.Template{
		{Name: panel.DatasourceTmp},
		{Name: panel.DcTmp},
		{Name: "var1"},
		{Name: "var2"},
	})
	b.AddAnnotations([]board.Annotation{
		{Name: sMap.Name},
		{Name: board.Drills},
		{Name: board.Depends},
		{Name: "an1"},
		{Name: "an2"},
	})
	b.AddPanels([]*panel.Panel{
		panel.MakeFromPreferences(panel.Preferences{Title: "p1", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p2", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "_Resources",
			Collapsed: true,
			Type:      panel.RowType,
			Panels: []*panel.Preferences{
				{
					Title: panel.CpuPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.MemPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.NetworkRxPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.NetworkTxPanel,
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "UserRow",
			Type:      panel.RowType,
			Collapsed: true,
			Panels: []*panel.Preferences{
				{
					Title: "p4",
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "_Provides",
			Type:      panel.RowType,
			Collapsed: true,
			Panels: []*panel.Preferences{
				{
					Title: fmt.Sprintf("_%s-%s codes", sMapNew.Name, sMapNew.Provides[0].Name),
					Type:  panel.TimeSeriesType,
				},
				{
					Title: fmt.Sprintf("_%s-%s codes", sMapNew.Name, sMapNew.Provides[1].Name),
					Type:  panel.TimeSeriesType,
				},
				{
					Title: fmt.Sprintf("_%s-%s codes", sMapNew.Name, sMapNew.Provides[2].Name),
					Type:  panel.TimeSeriesType,
				},
				{
					Title: fmt.Sprintf("_%s-%s codes", sMapNew.Name, sMapNew.Provides[3].Name),
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{Title: "p5", Type: panel.TimeSeriesType}),
		panel.MakeFromPreferences(panel.Preferences{
			Title: panel.RequestTimingPanel,
			Panels: []*panel.Preferences{
				{
					Title: panel.RequestRatePanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.RequestQuantilePanel,
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{
			Title:     "UserRowCollapsed",
			Type:      panel.RowType,
			Collapsed: true,
			Panels: []*panel.Preferences{
				{Title: "p7", Type: panel.TimeSeriesType},
				{Title: "p8", Type: panel.TimeSeriesType},
			},
		}),
	})

	return b
}

func makeNewBoard(sMap *proto.ServiceMap) *board.Dashboard {
	b, _ := board.MakeFromRawResponse(map[string]interface{}{})

	b.SetTitle(sMap.Name)
	b.AddTemplates([]board.Template{
		{Name: panel.DatasourceTmp},
		{Name: panel.DcTmp},
	})
	b.AddAnnotations([]board.Annotation{
		{Name: sMap.Name},
		{Name: board.Drills},
		{Name: board.Depends},
	})
	b.AddPanels([]*panel.Panel{
		panel.MakeFromPreferences(panel.Preferences{
			Title: panel.ResourcePanel,
			Panels: []*panel.Preferences{
				{
					Title: panel.CpuPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.MemPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.NetworkRxPanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.NetworkTxPanel,
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{
			Title: panel.ProvidesPanel,
			Panels: []*panel.Preferences{
				{
					Title: fmt.Sprintf("_%s-%s codes", sMap.Name, sMap.Provides[0].Name),
					Type:  panel.TimeSeriesType,
				},
				{
					Title: fmt.Sprintf("_%s-%s codes", sMap.Name, sMap.Provides[1].Name),
					Type:  panel.TimeSeriesType,
				},
			},
		}),
		panel.MakeFromPreferences(panel.Preferences{
			Title: panel.RequestTimingPanel,
			Panels: []*panel.Preferences{
				{
					Title: panel.RequestRatePanel,
					Type:  panel.TimeSeriesType,
				},
				{
					Title: panel.RequestQuantilePanel,
					Type:  panel.TimeSeriesType,
				},
			},
		}),
	})

	return b
}
