package task

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/telegram/message"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	taskPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	dState "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	i2 "github.com/YandexClassifieds/shiva/test/mock/mockery/mocks/i"
	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"google.golang.org/protobuf/types/known/timestamppb"
)

var (
	start *timestamp.Timestamp
	end   *timestamp.Timestamp
)

func init() {
	test.InitTestEnv()
	date := time.Date(2020, 6, 15, 1, 15, 30, 0, time.Local)
	start = timestamppb.New(date)
	end = timestamppb.New(date.Add(10 * time.Minute))
}

func TestHandler_Handle(t *testing.T) {
	tests := []struct {
		name   string
		branch string
		st     taskPb.State
		author string
		end    *timestamppb.Timestamp
		result string
	}{
		{
			name: "process",
			st:   taskPb.State_Process,
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Start: ¬2020-06-15T01:15¬

State: *process*
`,
		},
		{
			name:   "process_branch",
			st:     taskPb.State_Process,
			branch: "br1",
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Branch: ¬br1¬
Start: ¬2020-06-15T01:15¬

State: *process*
`,
		},
		{
			name:   "manual",
			st:     taskPb.State_Process,
			author: "danevge",
			result: `Manual task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Start: ¬2020-06-15T01:15¬

State: *process*
`,
		},
		{
			name: "success",
			st:   taskPb.State_Success,
			end:  end,
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Start: ¬2020-06-15T01:15¬
End: ¬2020-06-15T01:25¬

State: *success*
`,
		},
		{
			name: "failed",
			st:   taskPb.State_Failed,
			end:  end,
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Start: ¬2020-06-15T01:15¬
End: ¬2020-06-15T01:25¬

State: *failed*
`,
		},
		{
			name: "skipped",
			st:   taskPb.State_Skipped,
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬

State: *skipped*
`,
		},
		{
			name: "canceled",
			st:   taskPb.State_Canceled,
			result: `Periodic task
Service: ¬service-name¬
Layer: Test
Version: ¬0.1.0¬
Start: ¬2020-06-15T01:15¬

State: *canceled*
`,
		},
	}
	for _, c := range tests {
		t.Run(c.name, func(t *testing.T) {
			tgMock := mock.NewTelegramMock(t)
			log := test.NewLogger(t)
			msgS := message.NewService(log, mock.NewNDAStub(), message.NewMarkdown())
			kv := &i2.KV{}
			kv.On("Get", mock2.Anything).Return(nil, common.ErrNotFound)
			kv.On("Save", mock2.Anything, mock2.Anything).Return(nil)
			handler := NewHandler(log, msgS, tgMock, kv)
			handler.Handle(makeEvent(c.st, c.branch, c.author, c.end), 1)
			outMsg := tgMock.Get(t)
			assert.Equal(t, msgS.BacktickHack(c.result), outMsg.Message)
			assert.Empty(t, outMsg.Buttons)
		})
	}
}

func makeEvent(st taskPb.State, branch string, author string, end *timestamppb.Timestamp) *batch_task.BatchTaskEvent {
	return &batch_task.BatchTaskEvent{
		Deployment: &deployment.Deployment{
			ServiceName: "service-name",
			Branch:      branch,
			Version:     "0.1.0",
			Layer:       layer.Layer_TEST,
			State:       dState.DeploymentState_SUCCESS,
			Comment:     "",
			Source:      "",
		},
		Batch: &batch.Batch{
			State:    batch.State_Active,
			Periodic: "* 5 * * *",
		},
		Task: &taskPb.BatchTask{
			State:     st,
			Author:    author,
			StartDate: start,
			EndDate:   end,
		},
	}
}
