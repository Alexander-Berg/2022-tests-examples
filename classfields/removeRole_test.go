package handlers

import (
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"net/http"
	"net/url"
	"testing"
)

func TestHandler_RemoveRole(t *testing.T) {
	tests := map[string]struct {
		Values url.Values
	}{
		"service": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/test-service/provides/test-provides/"},
				"fields": {`{"sox":false}`},
			},
		},
		"service-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/test-service/provides/test-provides/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
		},
		"mysql": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/mysql/instance/mdb00000000/database/h2p_idm/mode/ro/"},
				"fields": {`{"sox":false}`},
			},
		},
		"mysql-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/mysql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
		},
		"postgresql": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/postgresql/instance/mdb00000000/database/h2p_idm/mode/ro/"},
				"fields": {`{"sox":false}`},
			},
		},
		"postgresql-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/postgresql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
		},
	}

	test.InitConfig(t)
	logger := logrus.New("info")

	tvm := &mocks.ITVM{}
	tvm.On("Check", mock.Anything, mock.Anything).Return(nil)

	db := &mocks.IMngr{}
	db.On("DeleteRole", mock.Anything).Return(nil)

	idmService := &mocks.IIDMService{}
	idmService.On("GetSoxTicket", `{"sox":false}`).Return(false, "", nil)
	idmService.On("GetSoxTicket", `{"sox":true, "ticket":"VOID-1"}`).Return(true, "VOID-1", nil)
	idmService.On("ErrorResponse", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).
		Run(func(args mock.Arguments) {
			result := make(map[string]int)
			result["code"] = args.Get(1).(int)

			data, err := json.Marshal(result)
			assert.NoError(t, err)

			_, err = fmt.Fprintln(args.Get(0).(http.ResponseWriter), string(data))
			assert.NoError(t, err)
		})

	notifier := &mocks.INotifier{}

	h := New(Conf{
		Tvm:      tvm,
		DbMngr:   db,
		Idm:      idmService,
		Notifier: notifier,
		Log:      logger,
		Sd:       &mocks.IServiceDiscovery{},
	})

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			test.RequireHTTPPostBodyContains(t, h.RemoveRole, "/remove-role/", tc.Values, `{"code":0}`)

			db.AssertNumberOfCalls(t, "DeleteRole", 1)
			db.Calls = []mock.Call{}
		})
	}
}
