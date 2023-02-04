package handler

import (
	"encoding/json"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/logs/cmd/collector/domain"
	"github.com/YandexClassifieds/logs/cmd/collector/pusher"
	"github.com/YandexClassifieds/logs/cmd/collector/storage"
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/YandexClassifieds/logs/pkg/auth"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/valyala/fasthttp"
	"testing"
	"time"
)

func TestWrapBrokenMessage(t *testing.T) {
	testutil.Init(t)

	handler := NewHttpHandler(nil, vlogrus.New())
	args := &fasthttp.Args{}
	originalMessage := "not a json"
	args.Set("Message", originalMessage)
	args.Set("Service", "ser")
	args.Set("ContainerId", "cid")
	args.Set("ContainerName", "cname")
	args.Set("AllocationId", "aid")
	args.Set("ImageId", "iid")
	args.Set("ImageName", "iname")
	args.Set("Version", "ver")
	args.Set("Branch", "")
	args.Set("Canary", "true")
	msg := handler.extractMessage(args, "sn", "aid")

	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(msg, &m))
	expected := "cannot parse JSON: unexpected value found: \"not a json\"; unparsed tail: \"not a json\""
	assert.Equal(t, expected, m["validation_error"])
	assert.Equal(t, originalMessage, m["_message"])
	assert.Equal(t, "ERROR", m["_level"])
	assert.Equal(t, domain.ValidationFailedContext, m["_context"])

	_, err := time.Parse(time.RFC3339Nano, m["_time"].(string))
	assert.NoError(t, err)
	assert.Equal(t, "ser", m["_service"])
	assert.Equal(t, "cid", m["_container_id"])
	assert.Equal(t, "aid", m["_allocation_id"])
	assert.Equal(t, "-", m["_dc"])
	assert.Equal(t, "local_test", m["_layer"])
	assert.Equal(t, 36, len(m["_uuid"].(string)))
	assert.NotEmpty(t, m["_host"])
	assert.Equal(t, "ver", m["_version"])
	assert.Equal(t, "", m["_branch"])
	assert.Equal(t, true, m["_canary"])
	assert.Equal(t, "", m["_thread"])
	assert.Equal(t, "", m["_request_id"])
	assert.NotEmpty(t, m["_time_nano"])
	assert.NotEmpty(t, m["_timezone"])
	assert.NotEmpty(t, m["_timestamp"])
}

func TestBulkError(t *testing.T) {
	t.Skip("require LB auth")
	testutil.Init(t)

	tokenProvider := auth.TokenProvider(vlogrus.New())
	pool := pusher.NewPool(tokenProvider, vlogrus.New())
	store, err := storage.NewFileStorage(vlogrus.New())
	require.NoError(t, err)
	newHandler := NewHandler(pool, store, vlogrus.New())
	handler := NewHttpHandler(newHandler, vlogrus.New())
	fasthttpCtx := &fasthttp.RequestCtx{}

	jsonMsg := domain.BulkApiPayload{domain.BulkApiRow{
		"_time": "2018--28T13:12:39.776+03:00",
	}}
	bytes, err := json.Marshal(jsonMsg)
	require.NoError(t, err)
	fasthttpCtx.Request.SetBody(bytes)
	handler.handleBulk(fasthttpCtx, false)
	require.Equal(t, 400, fasthttpCtx.Response.StatusCode())
}
