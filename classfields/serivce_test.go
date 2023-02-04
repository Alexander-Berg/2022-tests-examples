package dashboard

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/board"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/panel"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestSaveDashboard(t *testing.T) {
	apiMock := &mock.DashboardClient{}
	service := NewService(apiMock, test.NewLogger(t))

	folder := &panel.Folder{Title: t.Name(), ID: 5}
	apiMock.On("GetFolderByTitle", t.Name()).Return(folder, nil).Once()
	apiMock.On("SaveDashboard", mock2.Anything, folder.ID, true).Return(nil, nil).Once()

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	err := service.SaveDashboard(board.MakeSimpleDashboard(sCtx), t.Name())
	require.NoError(t, err)
	apiMock.AssertExpectations(t)
}

func TestSaveDashboardNewFolder(t *testing.T) {
	apiMock := &mock.DashboardClient{}
	service := NewService(apiMock, test.NewLogger(t))

	folder := &panel.Folder{Title: t.Name(), ID: 5}

	apiMock.On("GetFolderByTitle", t.Name()).Return(nil, common.ErrNotFound).Once()
	apiMock.On("CreateFolder", &panel.Folder{Title: t.Name()}).Return(folder, nil).Once()
	apiMock.On("SaveDashboard", mock2.Anything, folder.ID, true).Return(nil, nil).Once()

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	err := service.SaveDashboard(board.MakeSimpleDashboard(sCtx), t.Name())
	require.NoError(t, err)
	apiMock.AssertExpectations(t)
}
