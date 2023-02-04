package mock

import (
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
)

type BatchScheduler struct {
	ResultC     map[string][]chan *scheduler.State
	ResultError map[string]error
}

func NewMockBatchScheduler() *BatchScheduler {
	return &BatchScheduler{
		ResultC:     make(map[string][]chan *scheduler.State),
		ResultError: make(map[string]error),
	}
}

func (b *BatchScheduler) AddResultC(name, branch string, c chan *scheduler.State) {
	fullName := b.fullName(name, branch)
	_, ok := b.ResultC[fullName]
	if !ok {
		b.ResultC[fullName] = []chan *scheduler.State{}
	}
	b.ResultC[fullName] = append(b.ResultC[fullName], c)
}

func (b *BatchScheduler) AddResultError(name, branch string, err error) {
	b.ResultError[b.fullName(name, branch)] = err
}

func (b *BatchScheduler) GetResultC(name, branch string) chan *scheduler.State {

	fullName := b.fullName(name, branch)
	arr, ok := b.ResultC[fullName]
	if !ok || len(arr) == 0 {
		result := make(chan *scheduler.State, 2)
		result <- &scheduler.State{
			StateType: scheduler.Process,
		}
		result <- &scheduler.State{
			StateType: scheduler.Success,
		}
		close(result)
		return result
	}
	result := arr[0]
	b.ResultC[fullName] = b.ResultC[fullName][1:]
	return result
}

func (b *BatchScheduler) fullName(name, branch string) string {
	result := name
	if branch != "" {
		result += "-" + branch
	}
	return result
}

func (b *BatchScheduler) Run(ctx *scheduler.BatchContext) (chan *scheduler.State, error) {
	return b.get(ctx)
}

func (b *BatchScheduler) get(ctx *scheduler.BatchContext) (chan *scheduler.State, error) {
	resultErr := b.ResultError[b.fullName(ctx.GetName(), ctx.GetBranchName())]
	if resultErr != nil {
		delete(b.ResultError, b.fullName(ctx.GetName(), ctx.GetBranchName()))
		return nil, resultErr
	}
	return b.GetResultC(ctx.GetName(), ctx.GetBranchName()), nil
}

func (b *BatchScheduler) Stop(ctx *scheduler.BatchContext) error {
	return nil
}

func (b *BatchScheduler) State(ctx *scheduler.BatchContext) (chan *scheduler.State, error) {
	return b.get(ctx)
}
