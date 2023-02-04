package task

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
)

func NewTaskForProviderTest(name, queueName string) Task {
	return &TaskForProviderTest{
		TaskForTest: *NewTaskForTest(name, queueName).(*TaskForTest),
	}
}

type TaskForProviderTest struct {
	TaskForTest
}

func (d *TaskForProviderTest) NewPayload() any {
	return struct{}{}
}

func (d *TaskForProviderTest) Process(ctx context.Context, msg *queue.Message) error {
	return nil
}

func TestProvider_AddGetTask(t *testing.T) {
	t.Parallel()
	ctx := PrepareContext(t)

	p, err := NewProvider(ctx)
	if err != nil {
		t.Fatal(err)
	}

	addedTask := &TaskForProviderTest{}

	if err := p.AddTask(addedTask); err != nil {
		t.Fatal(err)
	}

	gotTask, err := p.GetTask(addedTask.Name())
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, addedTask, gotTask, "Should get same task that was added")
}

func TestProvider_AddTwoTasks(t *testing.T) {
	t.Parallel()

	ctx := PrepareContext(t)

	firstTask := NewTaskForProviderTest("FirstTask", "AnyQueue")

	secondTask := NewTaskForProviderTest("SecondTask", "AnyQueue")

	p, err := NewProvider(ctx, firstTask, secondTask)
	if err != nil {
		t.Fatal(err)
	}

	gotFirstTask, err := p.GetTask(firstTask.Name())
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, firstTask, gotFirstTask, "Should get same task that was added")

	gotSecondTask, err := p.GetTask(secondTask.Name())
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, secondTask, gotSecondTask, "Should get same task that was added")
}

func TestProvider_GetAbsentTask(t *testing.T) {
	t.Parallel()
	ctx := PrepareContext(t)

	p, err := NewProvider(ctx)
	if err != nil {
		t.Fatal(err)
	}

	gotTask, err := p.GetTask("SomeTask")
	assert.Error(t, err, "Should return error when getting not added task")
	assert.Nil(t, gotTask, "Should return empty task")
}
