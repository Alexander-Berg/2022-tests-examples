package proxy

import (
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	spe "github.com/YandexClassifieds/sender-proxy/pb/sender_proxy/event"
	"github.com/gorilla/mux"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

func TestEmailProxy_Handle_OK(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusOK)
		writer.Write([]byte(emailResponseOk))
	}))
	cfg := EmailConfig{
		AllowExternalSend: false,
		BaseUrl:           proxy.URL,
		Environment:       "dev",
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewEmailProxy(cfg, ms, mb, vlogrus.New())

	var evt *spe.EmailEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*spe.EmailEvent)
	})
	ms.On("ExistsByEmail", "test@yandex-team.ru").Return(true)

	req, err := http.NewRequest("POST", "http://testdomain/endpoint?to_email=test@yandex-team.ru", nil)
	require.NoError(t, err)
	req = mux.SetURLVars(req, map[string]string{"account": "acct", "campaign": "camp"})
	req.Header.Set("X-Request-Id", "req_id")
	req.Header.Set("X-Application", "app")

	rec := httptest.NewRecorder()
	svc.handleEmail(rec, req)

	assert.Equal(t, 200, rec.Code)
	require.NotNil(t, evt)
	assert.Equal(t, []string{"test@yandex-team.ru"}, evt.GetRecipients())
	assert.Equal(t, int32(200), evt.GetResponseInfo().GetHttpCode())
	assert.Equal(t, "some-task-id", evt.GetResponseInfo().GetTaskId().GetValue())
	assert.Equal(t, "some-msg-id", evt.GetResponseInfo().GetMessageId().GetValue())
	assert.Equal(t, "req_id", evt.RequestId)
	assert.Equal(t, "acct", evt.GetAccount())
	assert.Equal(t, "camp", evt.GetCampaign())
	assert.Equal(t, "app", evt.GetApplication())
}

func TestEmailProxy_Handle_Json(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusOK)
		writer.Write([]byte(emailResponseOk))
	}))
	cfg := EmailConfig{
		AllowExternalSend: false,
		BaseUrl:           proxy.URL,
		Environment:       "dev",
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewEmailProxy(cfg, ms, mb, vlogrus.New())

	var evt *spe.EmailEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*spe.EmailEvent)
	})
	ms.On("ExistsByEmail", "test@yandex-team.ru").Return(true)

	jsonReq := strings.NewReader(`{"to_email":"test@yandex-team.ru","args":{"a":"one","b":2}}`)
	req, err := http.NewRequest("POST", "http://testdomain/endpoint", jsonReq)
	require.NoError(t, err)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Request-Id", "req_id")

	rec := httptest.NewRecorder()
	svc.handleEmail(rec, req)

	assert.Equal(t, 200, rec.Code) // 202 => 200 hack
	require.NotNil(t, evt)
	assert.Equal(t, []string{"test@yandex-team.ru"}, evt.GetRecipients())
	assert.Len(t, evt.GetRequestInfo().GetArgs(), 2)
	assert.Equal(t, "some-msg-id", evt.GetResponseInfo().GetMessageId().GetValue())
}

func TestEmailProxy_Handle_Form(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusOK)
		writer.Write([]byte(emailResponseOk))
	}))
	cfg := EmailConfig{
		AllowExternalSend: false,
		BaseUrl:           proxy.URL,
		Environment:       "dev",
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewEmailProxy(cfg, ms, mb, vlogrus.New())

	var evt *spe.EmailEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*spe.EmailEvent)
	})
	ms.On("ExistsByEmail", "test@yandex-team.ru").Return(true)

	formReq := url.Values{
		"to_email": []string{"test@yandex-team.ru"},
		"args":     []string{`{"a":"one","b":2}`},
	}
	req, err := http.NewRequest("POST", "http://testdomain/endpoint", strings.NewReader(formReq.Encode()))
	require.NoError(t, err)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("X-Request-Id", "req_id")

	rec := httptest.NewRecorder()
	svc.handleEmail(rec, req)

	assert.Equal(t, 200, rec.Code) // 202 => 200 hack
	require.NotNil(t, evt)
	assert.Equal(t, []string{"test@yandex-team.ru"}, evt.GetRecipients())
	assert.Len(t, evt.GetRequestInfo().GetArgs(), 2)
	assert.Equal(t, "some-msg-id", evt.GetResponseInfo().GetMessageId().GetValue())
}

func TestEmailProxy_Handle_202Hack(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		writer.WriteHeader(http.StatusAccepted)
		writer.Write([]byte(`{"result":"skip:ignore_empty_email"}`))
	}))
	cfg := EmailConfig{
		AllowExternalSend: false,
		BaseUrl:           proxy.URL,
		Environment:       "dev",
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewEmailProxy(cfg, ms, mb, vlogrus.New())

	var evt *spe.EmailEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*spe.EmailEvent)
	})
	ms.On("ExistsByEmail", "test@yandex-team.ru").Return(true)

	req, err := http.NewRequest("POST", "http://testdomain/endpoint", nil)
	require.NoError(t, err)
	req = mux.SetURLVars(req, map[string]string{"account": "acct", "campaign": "camp"})
	req.Header.Set("X-Request-Id", "req_id")
	req.Header.Set("X-Application", "app")

	rec := httptest.NewRecorder()
	svc.handleEmail(rec, req)

	assert.Equal(t, 200, rec.Code)
	require.NotNil(t, evt)
	assert.Equal(t, int32(202), evt.GetResponseInfo().GetHttpCode())
	assert.Equal(t, "req_id", evt.RequestId)
	assert.Equal(t, "acct", evt.GetAccount())
	assert.Equal(t, "camp", evt.GetCampaign())
	assert.Equal(t, "app", evt.GetApplication())
}

var (
	emailResponseOk = `{
   "params" : {
      "source" : {
         "cc" : [
            {
               "name" : "Mary",
               "email" : "mary@example.com"
            },
            {
               "email" : "john@example.com",
               "name" : "John"
            }
         ],
         "to" : [
            {
               "name" : "Alice",
               "email" : "alice@example.com"
            },
            {
               "name" : "Bob",
               "email" : "bob@example.com"
            }
         ],
         "args" : {
            "var" : "val"
         },
         "attachments" : [
            {
               "filename" : "a.txt",
               "mime_type" : "application/text",
               "data" : "<binary data>"
            }
         ],
         "bcc" : [
            {
               "email" : "tom@example.com",
               "name" : "Tom"
            },
            {
               "name" : "Jan",
               "email" : "jan@example.com"
            }
         ],
         "headers" : {
            "three" : "3",
            "two" : "2",
            "one" : "1"
         }
      },
      "control" : {
         "for_testing" : false,
         "async" : true,
         "expires" : 86400,
         "countdown" : null
      }
   },
   "result" : {
      "message_id" : "some-msg-id",
      "status" : "OK",
      "task_id" : "some-task-id"
   }
}
`
)
