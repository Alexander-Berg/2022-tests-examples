package proxy

import (
	"context"
	"encoding/xml"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/sender-proxy/mocks"
	"github.com/YandexClassifieds/sender-proxy/pb/sender_proxy/event"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	responseOk   = `<?xml version="1.0" encoding="utf-8"?><doc><message-sent id="42" /></doc>`
	responseFail = `<?xml version="1.0" encoding="utf-8"?><doc><error>bad phone</error><errorcode>BADPHONE</errorcode></doc>`
)

func TestSmsProxy_SendSMS_Ok(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		assert.Equal(t, "req_id", request.Header.Get("x-request-id"))
		writer.WriteHeader(200)
		writer.Write([]byte(responseOk))
	}))
	cfg := SmsConfig{
		BaseUrl:           proxy.URL,
		AllowExternalSend: false,
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewSmsProxy(cfg, ms, mb, vlogrus.New())
	ms.On("ExistsByNumber", "+79121112233").Return(true)

	var evt *event.SmsEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*event.SmsEvent)
	})

	req := createSmsRequest(url.Values{
		"phone": []string{"+79121112233"},
		"text":  []string{"testmsg"},
	})
	req.Header.Set("X-Request-Id", "req_id")

	rec := httptest.NewRecorder()
	svc.sendSms(rec, req)
	body, err := ioutil.ReadAll(rec.Result().Body)
	require.NoError(t, err, "body read failed")
	sms := SmsAnswer{}
	err = xml.Unmarshal(body, &sms)
	require.NoError(t, err, "response xml failed")

	assert.Equal(t, 200, rec.Code)
	assert.Equal(t, int64(42), sms.MessageId())

	require.NotNil(t, evt)
	assert.Equal(t, "req_id", evt.RequestId)
	assert.Equal(t, "testmsg", evt.GetRequestInfo().GetText())
	assert.Equal(t, "+79121112233", evt.GetRequestInfo().GetPhone())
	assert.Equal(t, "42", evt.GetResponseInfo().GetMessageId().GetValue())
}

func TestSmsProxy_SendSMS_Fail(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.WriteHeader(400)
		writer.Write([]byte(responseFail))
	}))
	cfg := SmsConfig{
		BaseUrl:           proxy.URL,
		AllowExternalSend: false,
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewSmsProxy(cfg, ms, mb, vlogrus.New())
	ms.On("ExistsByNumber", "+123").Return(true)

	var evt *event.SmsEvent
	mb.On("Add", mock.Anything).Run(func(args mock.Arguments) {
		evt = args.Get(0).(*event.SmsEvent)
	})

	req := createSmsRequest(url.Values{
		"phone": []string{"+123"},
		"text":  []string{"testmsg"},
	})
	req.Header.Set("X-Request-Id", "req_id")

	rec := httptest.NewRecorder()
	svc.sendSms(rec, req)
	body, err := ioutil.ReadAll(rec.Result().Body)
	require.NoError(t, err, "body read failed")
	sms := SmsAnswer{}
	err = xml.Unmarshal(body, &sms)
	require.NoError(t, err, "response xml failed")

	assert.Equal(t, 400, rec.Code)
	assert.Equal(t, "BADPHONE", sms.ErrorCode)

	require.NotNil(t, evt)
	assert.Equal(t, "req_id", evt.RequestId)
	assert.Equal(t, "testmsg", evt.GetRequestInfo().GetText())
	assert.Equal(t, "+123", evt.GetRequestInfo().GetPhone())
	assert.Equal(t, "BADPHONE", evt.GetResponseInfo().GetErrCode().GetValue())
}

func TestSmsProxy_SendSMS_Timeout(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		<-time.After(time.Second)
		writer.WriteHeader(200)
		writer.Write([]byte(responseOk))
	}))
	cfg := SmsConfig{
		BaseUrl:           proxy.URL,
		AllowExternalSend: false,
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	log := &mocks.Logger{}
	svc := NewSmsProxy(cfg, ms, mb, log)
	ms.On("ExistsByNumber", "+123").Return(true)
	log.On("WithField", mock.Anything, mock.Anything).Return(log)
	log.On("WithError", mock.Anything, mock.Anything).Return(log)
	log.On("Warn", mock.Anything).Once()

	ctx, cancel := context.WithTimeout(context.Background(), 1)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, "GET", "http://sms-test.local/sendsms", nil)
	require.NoError(t, err)
	req.URL.RawQuery = url.Values{
		"phone": []string{"+123"},
		"text":  []string{"testmsg"},
	}.Encode()

	rec := httptest.NewRecorder()
	svc.sendSms(rec, req)
	body, err := ioutil.ReadAll(rec.Result().Body)
	require.NoError(t, err, "body read failed")
	sms := SmsAnswer{}
	err = xml.Unmarshal(body, &sms)
	require.NoError(t, err, "response xml failed")

	assert.Equal(t, 500, rec.Code)
	assert.Equal(t, "PROXY_INTERNAL", sms.ErrorCode)
	log.AssertExpectations(t)
}

func TestSmsProxy_SendSMS_Validate(t *testing.T) {
	proxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.WriteHeader(200)
		writer.Write([]byte(responseOk))
	}))
	cfg := SmsConfig{
		BaseUrl:           proxy.URL,
		AllowExternalSend: false,
	}
	ms := new(mockStaff)
	mb := new(mockBroker)
	svc := NewSmsProxy(cfg, ms, mb, vlogrus.New())
	ms.On("ExistsByNumber", "+1234").Return(false)

	req := createSmsRequest(url.Values{
		"phone": []string{"+1234"},
		"text":  []string{"testmsg"},
	})
	req.Header.Set("X-Request-Id", "req_id")

	rec := httptest.NewRecorder()
	svc.sendSms(rec, req)
	body, err := ioutil.ReadAll(rec.Result().Body)
	require.NoError(t, err, "body read failed")
	sms := SmsAnswer{}
	err = xml.Unmarshal(body, &sms)
	require.NoError(t, err, "response xml failed")

	// our proxy handler validates number and returns its own error
	assert.Equal(t, 500, rec.Code)
	assert.Equal(t, "EXTERNAL_NUMBER_DELIVERY_DISABLED", sms.ErrorCode)
}

func TestSmsAnswer_Encode(t *testing.T) {
	a := SmsAnswer{
		MessageSent: &MessageSent{ID: 42},
		Error:       "some_err",
		ErrorCode:   "some_code",
	}
	data, err := xml.Marshal(&a)
	require.NoError(t, err)
	assert.Equal(t, `<doc><message-sent id="42"></message-sent><error>some_err</error><errorcode>some_code</errorcode></doc>`, string(data))
}

func createSmsRequest(params url.Values) *http.Request {
	req, _ := http.NewRequest("GET", "http://sms-test.local/sendsms", nil)
	req.URL.RawQuery = params.Encode()
	return req
}
