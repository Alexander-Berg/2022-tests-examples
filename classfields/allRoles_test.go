package handlers

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/cmd/h2p-idm/models"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/stretchr/testify/mock"
	"net/url"
	"testing"
)

var (
	roles = []models.Role{
		{
			Login:   "login1",
			Service: "/service/test-service/provides/test-provides/",
		},
		{
			Login:   "login1",
			Service: "/service/sox-service/provides/sox-provides/",
			Sox:     true,
			Ticket:  "VOID-1",
		},
		{
			Login:   "login1",
			Service: "/service/mysql/instance/mdb000000/database/test/mode/ro/",
		},
		{
			Login:   "login1",
			Service: "/service/postgresql/instance/mdb000000/database/test/mode/ro/",
		},
		{
			Login:   "login2",
			Service: "/service/test-service/provides/test-provides/",
		},
		{
			Login:   "login-owner",
			Service: "/service/test-service/provides/owner/",
		},
	}
	result = `{"code":0,"users":[{"login":"login1","roles":[[{"service":"test-service","provides":"test-provides"},{}],[{"service":"sox-service","provides":"sox-provides"},{"sox":true,"ticket":"VOID-1"}],[{"service":"mysql","instance":"mdb000000","database":"test","mode":"ro"},{}],[{"service":"postgresql","instance":"mdb000000","database":"test","mode":"ro"},{}]]},{"login":"login2","roles":[[{"service":"test-service","provides":"test-provides"},{}]]},{"login":"login-owner","roles":[[{"service":"test-service","provides":"owner"},{}]]}]}`
)

func TestHandler_AllRoles(t *testing.T) {
	test.InitConfig(t)
	logger := logrus.New("info")

	tvm := &mocks.ITVM{}
	tvm.On("Check", mock.Anything, mock.Anything).Return(nil)

	db := &mocks.IMngr{}
	db.On("GetRoles", mock.Anything).Return(roles, nil)

	idmService := &mocks.IIDMService{}
	notifier := &mocks.INotifier{}

	h := New(Conf{
		Tvm:      tvm,
		DbMngr:   db,
		Idm:      idmService,
		Notifier: notifier,
		Log:      logger,
		Sd:       &mocks.IServiceDiscovery{},
	})

	test.RequireHTTPPostBodyContains(t, h.AllRoles, "/get-all-roles/", url.Values{}, result)
}
