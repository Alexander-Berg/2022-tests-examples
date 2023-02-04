package pusher

import (
	"context"
	"testing"
	"time"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/logs/cmd/collector/domain"
	"github.com/YandexClassifieds/logs/cmd/collector/domain/test"
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
)

const (
	testSourceId = "logs-collector-test"
)

func TestSuccessPush(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0
	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)

	// main
	pusher.Init()
	<-pusher.Connected

	pusher.Push(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message2)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message3)})

	// assert
	assertPusher(t, pusher, mock, 0, 3)
}

func TestFailPush(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0
	mock, provider := mock(false)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)

	// main
	pusher.Init()
	<-pusher.Connected
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message2)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message3)})

	// assert
	assertPusher(t, pusher, mock, 3*viper.GetInt("ATTEMPTS"), 0)
}

func TestFailPushByOffline(t *testing.T) {

	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)
	pusher.Init()
	<-pusher.Connected
	pusher.TryClose(context.Background())
	assert.False(t, pusher.open)

	// main
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message2)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message3)})

	// assert
	assert.Equal(t, 3, len(pusher.Fail.Get()))
}

func TestEmptyStart(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(false)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)

	// main
	pusher.Init()
	<-pusher.Connected

	// assert
	assertPusher(t, pusher, mock, 0, 0)
}

func TestStartWithBackup(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)

	pusher.Backup = []*domain.Wrapper{
		test.CreateWrapper(t, test.Message1),
		test.CreateWrapper(t, test.Message2),
		test.CreateWrapper(t, test.Message3),
		test.CreateWrapper(t, test.Message4),
		test.CreateWrapper(t, test.Message5),
		test.CreateWrapper(t, test.Message6),
	}
	pusher.Backup[0].Data.SeqNo = 9
	pusher.Backup[1].Data.SeqNo = 10
	pusher.Backup[2].Data.SeqNo = 10
	pusher.Backup[3].Data.SeqNo = 10
	pusher.Backup[4].Data.SeqNo = 11
	pusher.Backup[5].Data.SeqNo = 11

	// main
	pusher.Init()
	<-pusher.Connected

	// assert
	assertPusher(t, pusher, mock, 0, 2)
}

func TestStartWithFailData(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)
	pusher.Fail = domain.NewWrappers(test.CreateWrappers(t))

	// main
	pusher.Init()
	<-pusher.Connected

	// assert
	assertPusher(t, pusher, mock, 0, 3)
}

func TestPusher_TryClose(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)
	pusher.Init()
	<-pusher.Connected
	assertPusher(t, pusher, mock, 0, 0)

	ws := test.CreateWrappers(t)
	ws[0].Data.SeqNo = 11
	ws[1].Data.SeqNo = 12
	ws[2].Data.SeqNo = 13
	pusher.Backup = ws

	pusher.Fail = domain.NewWrappers([]*domain.Wrapper{
		test.CreateWrapper(t, test.Message4),
		test.CreateWrapper(t, test.Message5),
	})

	// mock inflight processing
	pusher.inFlightCtr = 3
	go func() {
		for i := 0; i < 3; i++ {
			time.Sleep(time.Second / 10)
			pusher.decInFlight(ws[i])
		}
	}()

	// main
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*2)
	defer cancel()
	pusher.TryClose(ctx)

	// assert
	assert.Equal(t, 3, len(pusher.Backup))
	assert.Equal(t, 2, len(pusher.Fail.Get()))
	assert.False(t, pusher.open)
	assert.Equal(t, uint64(0), pusher.inFlightCtr)
}

func TestPusher_TryClose_Timeout(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)
	pusher.Init()
	<-pusher.Connected
	assertPusher(t, pusher, mock, 0, 0)

	// mock inflight processing
	pusher.inFlightCtr = 3
	go func() {
		time.Sleep(time.Second / 2)
		pusher.decInFlight(&domain.Wrapper{})
	}()

	// main
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	pusher.TryClose(ctx)

	// assert
	assert.Equal(t, uint64(2), pusher.inFlightCtr)
}

func TestCleanupAttributes(t *testing.T) {

	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 0

	mock, provider := mock(false)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)

	// main
	pusher.Init()
	<-pusher.Connected

	// close
	pusher.TryClose(context.Background())

	pusher.Push(&domain.Wrapper{Message: []byte(test.Message1), SeqNo: 1, Partition: 1})
	assert.Equal(t, 1, len(pusher.Fail.Get()))

	wrapper := pusher.Fail.Get()[0]
	assert.Equal(t, uint64(0), wrapper.SeqNo)
	assert.Equal(t, 0, wrapper.Attempts)
}

func TestPusherLimit(t *testing.T) {
	testutil.Init(t)
	// run up
	var inFlightBytes uint64 = 100

	mock, provider := mock(true)
	cfg := ProducerConfig(testSourceId, 1)
	pusher := newPusher(mock, cfg, provider, vlogrus.New(), &inFlightBytes)
	pusher.inFlightBytesLimit = 200

	// main
	pusher.Init()
	<-pusher.Connected

	pusher.Push(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message2)})
	assertPusher(t, pusher, mock, 0, 2)

	pusher.incInFlight(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message3)})
	assertPusher(t, pusher, mock, 0, 2)

	pusher.decInFlight(&domain.Wrapper{Message: []byte(test.Message1)})
	pusher.Push(&domain.Wrapper{Message: []byte(test.Message4)})
	assertPusher(t, pusher, mock, 0, 3)
}

func assertPusher(t *testing.T, pusher *Pusher, mock *mockProducer, failCount int, successCount int) {
	assert.Equal(t, uint64(10), pusher.MaxSeqNo)
	assert.Equal(t, 0, pusher.Fail.Len())
	assert.Equal(t, 0, len(pusher.Backup))
	assert.True(t, pusher.open)
	assert.Equal(t, failCount, mock.failCount)
	assert.Equal(t, successCount, mock.successCount)
}
