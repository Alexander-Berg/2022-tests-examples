package filter_test

import (
	"github.com/YandexClassifieds/vtail/api/backend"
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/backend/parser"
	"github.com/YandexClassifieds/vtail/cmd/streamer/filter"
	"github.com/YandexClassifieds/vtail/internal/task"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"google.golang.org/protobuf/types/known/timestamppb"

	"sync"
	"testing"
	"time"
)

const waitTimeout = 500 * time.Millisecond

func TestPipe(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	defer close(source)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	sink := pipe.AddFlow(&task.Flow{
		Id:          "flowid-1",
		Filters:     &core.Parenthesis{},
		Include:     includeAllFields(),
		Destination: "localhost:22345",
	})
	// wait until flow added
	time.Sleep(time.Millisecond)

	source <- getLogMessage(t)

	message := <-sink
	require.Equal(t, getLogMessage(t), message)
	requireChanEmpty(t, sink)
}

func TestSelectiveFields(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	defer close(source)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	sink := pipe.AddFlow(&task.Flow{
		Id:          "flowid-1",
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "localhost:22345",
	})
	// wait until flow added
	time.Sleep(time.Millisecond)

	sink2 := pipe.AddFlow(&task.Flow{
		Id:          "flowid-2",
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true, Level: true},
		Destination: "localhost:22345",
	})
	// wait until flow added
	time.Sleep(time.Millisecond)

	logMessage := getLogMessage(t)
	source <- logMessage

	// ensure that multiple flows do not modify same message
	var msg1, msg2 *core.LogMessage
	wg := new(sync.WaitGroup)
	wg.Add(2)
	go func() {
		msg1 = <-sink
		requireChanEmpty(t, sink)
		wg.Done()
	}()

	go func() {
		msg2 = <-sink2
		requireChanEmpty(t, sink2)
		wg.Done()
	}()
	wg.Wait()

	require.Equal(t, &core.LogMessage{
		Timestamp: logMessage.Timestamp,
		Message:   logMessage.Message,
	}, msg1)
	require.Equal(t, &core.LogMessage{
		Timestamp: logMessage.Timestamp,
		Message:   logMessage.Message,
		Level:     "INFO",
	}, msg2)
}

func TestSimilarConsumers(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	defer close(source)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	sink1 := pipe.AddFlow(&task.Flow{
		Id:          "flowid-1",
		Filters:     &core.Parenthesis{},
		Include:     includeAllFields(),
		Destination: "localhost:22345",
	})
	sink2 := pipe.AddFlow(&task.Flow{
		Id:          "flowid-2",
		Filters:     &core.Parenthesis{},
		Include:     includeAllFields(),
		Destination: "localhost:22345",
	})
	// wait until both flows are added
	time.Sleep(time.Millisecond)

	source <- getLogMessage(t)
	select {
	case message := <-sink1:
		require.Equal(t, getLogMessage(t), message)
	case message2 := <-sink2:
		require.Equal(t, getLogMessage(t), message2)
	case <-time.After(waitTimeout):
		t.Errorf("fail waiting for message 1")
	}
	select {
	case message := <-sink1:
		require.Equal(t, getLogMessage(t), message)
	case message2 := <-sink2:
		require.Equal(t, getLogMessage(t), message2)
	case <-time.After(waitTimeout):
		t.Errorf("fail waiting for message 2")
	}

	requireChansEmpty(t, sink1, sink2)
}

func TestMultipleConsumers(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	defer close(source)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	queryParser := parser.NewQueryParser()
	parenthesis, err := queryParser.Parse("service=srv layer=test")
	require.NoError(t, err)

	sink1 := pipe.AddFlow(&task.Flow{
		Id:          "flowid-1",
		Filters:     parenthesis,
		Include:     includeAllFields(),
		Destination: "localhost:22345",
	})
	parenthesis, err = queryParser.Parse("service=srv layer=prod")
	require.NoError(t, err)
	sink2 := pipe.AddFlow(&task.Flow{
		Id:          "flowid-2",
		Filters:     parenthesis,
		Include:     includeAllFields(),
		Destination: "localhost:22345",
	})
	// wait until both flows are added
	time.Sleep(time.Millisecond)

	source <- getLogMessage(t)
	message := <-sink1
	require.Equal(t, getLogMessage(t), message)

	requireChansEmpty(t, sink1, sink2)
}

func TestNoSinks(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	defer close(source)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	source <- getLogMessage(t)
	// source chan was read
	requireChanEmpty(t, source)
}

func TestCloseSource(t *testing.T) {
	defer goleak.VerifyNone(t)

	source := make(chan *core.LogMessage)
	pipe := filter.NewPipe(source, test.NewTestLogger())
	defer pipe.Close()

	sink := pipe.AddFlow(&task.Flow{
		Id:          "flowid-1",
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "localhost:22345",
	})
	// wait until flow added
	time.Sleep(time.Millisecond)

	close(source)

	time.Sleep(time.Millisecond)

	_, ok := <-sink
	require.False(t, ok, "sink must be closed")
}

func getLogMessage(t *testing.T) *core.LogMessage {
	timestamp := timestamppb.New(time.Unix(1580458894, 324))

	return &core.LogMessage{
		Timestamp: timestamp,
		Service:   "srv",
		Version:   "1.2",
		Layer:     "test",
		Level:     "INFO",
		Message:   "random message",
		Rest:      `{"myField": "my field value"}`,
	}
}

func includeAllFields() *backend.IncludeFields {
	return &backend.IncludeFields{
		Message:   true,
		Timestamp: true,
		Service:   true,
		Version:   true,
		Layer:     true,
		Level:     true,
		Rest:      true,
	}
}

func requireChanEmpty(t *testing.T, ch <-chan *core.LogMessage) {
	t.Helper()
	select {
	case value := <-ch:
		t.Errorf("unexpected value: %+v", value)
	case <-time.After(waitTimeout):
	}
}

func requireChansEmpty(t *testing.T, ch1 <-chan *core.LogMessage, ch2 <-chan *core.LogMessage) {
	t.Helper()
	select {
	case value := <-ch1:
		t.Errorf("unexpected value: %+v", value)
	case value := <-ch2:
		t.Errorf("unexpected value: %+v", value)
	case <-time.After(waitTimeout):
	}
}
