package test

import (
	"crypto/md5"
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/goLB"
	"github.com/YandexClassifieds/logs/cmd/collector/domain"
	"github.com/stretchr/testify/assert"
	"testing"
)

const (
	Message1   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #1", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message2   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #2", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message3   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #3", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message4   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #4", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message5   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #5", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message6   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #6", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message7   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #7", "_time":"2018-10-25T16:35:00.000+05:00"}`
	Message8   = `{"_message":"[base_v1] [2018-10-01 10:50:22.163] [INFO] [178195504:wizard] [tmp] Message #8", "_time":"2018-10-25T16:35:00.000+05:00"}`
	StackTrace = `
		OMG! Error in Earth (World:2345)
		Error in Russia (World:34)
		...
	`
	Service1 = "local_test1"
	Service2 = "local_test2"
)

func CreateWrappers(t *testing.T) []*domain.Wrapper {

	return []*domain.Wrapper{
		CreateWrapper(t, Message1),
		CreateWrapper(t, Message2),
		CreateWrapper(t, Message3),
	}
}

func CreateWrapper(t *testing.T, msg string) *domain.Wrapper {

	w := &domain.Wrapper{
		Message:      []byte(msg),
		ServiceName:  "wizard",
		AllocationId: fmt.Sprintf("%x", md5.Sum([]byte(msg))),
	}
	bytes, err := json.Marshal(w)
	assert.NoError(t, err)
	w.Data = &goLB.Data{Body: bytes}
	return w
}
