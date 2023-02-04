package domain

import (
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/stretchr/testify/require"
	"github.com/valyala/fasthttp"
	"github.com/valyala/fastjson"
	"sync"
	"testing"
)

func BenchmarkValidate(b *testing.B) {
	testutil.Init(b)

	var f = NewMessageFiller()
	f.layer = "local_test"
	var pool = &sync.Pool{New: f.newMessage}

	messagePayload := `{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN","_message":"this is warn message","_context":"ctx","_thread":"thr","_request_id":"req","customField":"abc","objKey":{"objProp":"5"}}`
	postValues := &fasthttp.Args{}
	postValues.Set("Message", messagePayload)
	postValues.Set("Canary", `true`)
	postValues.Set("ContainerName", `my-cont-name`)
	postValues.Set("AllocationId", `abcdef123456`)
	postValues.Set("ImageId", `123456"abcdef`)
	postValues.Set("ImageName", `YandexVerticals/myimage:123`)
	postValues.Set("Service", `service-name`)
	postValues.Set("Version", `123`)
	postValues.Set("ContainerId", `123-22-33`)

	b.Run("validate", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			innerMessage := postValues.Peek("Message")
			err := fastjson.ValidateBytes(innerMessage)
			require.NoError(b, err)

			m := pool.Get().(*message)
			m.Reset(postValues)

			err = m.UnmarshalFrom(innerMessage)
			require.NoError(b, err)
			err = m.Validate()
			require.NoError(b, err)
			message, err := m.Marshal()
			require.NoError(b, err)
			require.NotNil(b, message)
			pool.Put(m)
		}
	})

	b.Run("no-validate", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			innerMessage := postValues.Peek("Message")

			m := pool.Get().(*message)
			m.Reset(postValues)

			err := m.UnmarshalFrom(innerMessage)
			require.NoError(b, err)
			err = m.Validate()
			require.NoError(b, err)
			message, err := m.Marshal()
			require.NoError(b, err)
			require.NotNil(b, message)
			pool.Put(m)
		}
	})
}
