package scheduler

import (
	"context"
	"embed"
	"encoding/json"
	"io/fs"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actionworker"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xjson"
	"a.yandex-team.ru/library/go/core/xerrors"
)

//go:embed config/enqueuer/tasks/*
var enqueuerConfigFS embed.FS

func TestActionsForConfig(t *testing.T) {
	taskProvider, err := actionworker.NewTaskProvider(context.Background(), nil, nil, nil, nil)
	require.NoError(t, err)

	assert.NoError(t, fs.WalkDir(enqueuerConfigFS, "config/enqueuer/tasks", walkFunc(taskProvider)))
}

func walkFunc(taskProvider *task.Provider) fs.WalkDirFunc {
	return func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}

		if d.IsDir() || !strings.HasSuffix(d.Name(), ".yaml") {
			return nil
		}

		file, err := enqueuerConfigFS.ReadFile(path)
		if err != nil {
			return err
		}

		var trigger entities.TaskTrigger
		if err := yaml.Unmarshal(file, &trigger); err != nil {
			return err
		}

		t, err := taskProvider.GetTask(trigger.QueueName)
		if err != nil {
			return xerrors.Errorf("getTask for queue with name %s from config %s failed, check its existence: %w",
				trigger.QueueName, path, err)
		}

		actionTask, ok := t.(actionworker.ActionTask)
		if !ok {
			return xerrors.New("Task must implement ActionTask interface")
		}

		actionBytes, err := json.Marshal(trigger.Action)
		if err != nil {
			return xerrors.Errorf("cannot marshal action for config %s: %w", path, err)
		}

		var action entities.Action
		if err := xjson.Unmarshal(actionBytes, &action); err != nil {
			return xerrors.Errorf("cannot unmarshal action for config %s: %w", path, err)
		}

		method, err := actionTask.GetFactoryMethod(action.Name)
		if err != nil {
			return xerrors.Errorf("cannot get factory method for action name %s from config %s: %w",
				action.Name, path, err)
		}

		if err := method.ValidateParams(action.Params); err != nil {
			return xerrors.Errorf("invalid config %s: %w", path, err)
		}
		return nil
	}
}
