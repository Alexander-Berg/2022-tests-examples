package handlers

import (
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/cmd/h2p-idm/email"
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/YandexClassifieds/h2p/test/mocks"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"net/http"
	"net/url"
	"testing"
	"time"
)

var (
	mysqlParams = email.Params{
		Title: "h2p_idm(mdb00000000)",
		Db: email.DbParams{
			Name:     "h2p_idm",
			Instance: "mdb00000000",
			Mode:     "rw",
			Host:     "h2p_idm.mdb-rw-mdb00000000.query.consul",
			Port:     3306,
			User:     "analyst",
			Type:     email.MySQL,
		},
		Ssh: email.SshParams{
			Host: "h2p.vertis.yandex.net",
			Port: 2222,
			User: "login",
		},
	}

	postgresqlParams = email.Params{
		Title: "h2p_idm(mdb00000000)",
		Db: email.DbParams{
			Name:     "h2p_idm",
			Instance: "mdb00000000",
			Mode:     "rw",
			Host:     "h2p_idm.pg-rw-mdb00000000.query.consul",
			Port:     6432,
			User:     "analyst",
			Type:     email.PostgreSQL,
		},
		Ssh: email.SshParams{
			Host: "h2p.vertis.yandex.net",
			Port: 2222,
			User: "login",
		},
	}
)

func TestHandler_AddRole_Email(t *testing.T) {
	tests := map[string]struct {
		Values   url.Values
		Result   string
		NeedSend bool
	}{
		"service": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/test-service/provides/test-provides/"},
				"fields": {`{"sox":false}`},
			},
			Result: `{"code":0,"data":{}}`,
		},
		"service-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/test-service/provides/test-provides/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
			Result: `{"code":0,"data":{"sox":true,"ticket":"VOID-1"}}`,
		},
		"mysql": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/mysql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":false}`},
			},
			Result:   `{"code":0,"data":{}}`,
			NeedSend: true,
		},
		"mysql-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/mysql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
			Result:   `{"code":0,"data":{"sox":true,"ticket":"VOID-1"}}`,
			NeedSend: true,
		},
		"postgresql": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/postgresql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":false}`},
			},
			Result: `{"code":0,"data":{}}`,
			NeedSend: true,
		},
		"postgresql-sox": {
			Values: url.Values{
				"login":  {"login"},
				"path":   {"/service/postgresql/instance/mdb00000000/database/h2p_idm/mode/rw/"},
				"fields": {`{"sox":true, "ticket":"VOID-1"}`},
			},
			Result: `{"code":0,"data":{"sox":true,"ticket":"VOID-1"}}`,
			NeedSend: true,
		},
	}

	test.InitConfig(t)
	logger := logrus.New("info")

	tvm := &mocks.ITVM{}
	tvm.On("Check", mock.Anything, mock.Anything).Return(nil)

	db := &mocks.IMngr{}
	db.On("AddRole", mock.Anything).Return(nil)

	idmService := &mocks.IIDMService{}
	idmService.On("GetSoxTicket", `{"sox":false}`).Return(false, "", nil)
	idmService.On("GetSoxTicket", `{"sox":true, "ticket":"VOID-1"}`).Return(true, "VOID-1", nil)
	idmService.On("ErrorResponse", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).
		Run(func(args mock.Arguments) {
			result := make(map[string]interface{})
			result["code"] = args.Get(1).(int)

			data, ok := args.Get(4).(*idm.FieldsData)
			assert.True(t, ok)

			if data != nil {
				result["data"] = data
			}

			response, err := json.Marshal(result)
			assert.NoError(t, err)

			_, err = fmt.Fprintln(args.Get(0).(http.ResponseWriter), string(response))
			assert.NoError(t, err)
		})

	notifier := &mocks.INotifier{}
	notifier.On("Send", "login@yandex-team.ru", mysqlParams).Return(nil)
	notifier.On("Send", "login@yandex-team.ru", postgresqlParams).Return(nil)

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
			test.RequireHTTPPostBodyContains(t, h.AddRole, "/add-role/", tc.Values, tc.Result)

			db.AssertNumberOfCalls(t, "AddRole", 1)
			db.Calls = []mock.Call{}

			if tc.NeedSend {
				assert.Eventually(t, func() bool {
					for _, call := range notifier.Calls {
						if call.Method == "Send" {
							return true
						}
					}
					return false
				}, time.Second, time.Second/10)
				notifier.AssertNumberOfCalls(t, "Send", 1)
				notifier.Calls = []mock.Call{}
			}
		})
	}
}
