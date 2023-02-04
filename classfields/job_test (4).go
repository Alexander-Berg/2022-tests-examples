package nomad

import (
	"context"
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/pkg/secrets"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestJobBuilder_makeGroups(t *testing.T) {
	test.InitTestEnv()
	type TestCase struct {
		name   string
		count  int
		want   int
		branch string
	}
	cs := []TestCase{
		{name: "simple", count: 10, want: 10, branch: ""},
		{name: "branch", count: 10, want: 1, branch: "branch"},
		{name: "canary_4", count: 5, want: 1, branch: "canary"},
		{name: "canary_10", count: 10, want: 1, branch: "canary"},
		{name: "canary_15", count: 15, want: 1, branch: "canary"},
		{name: "canary_50", count: 50, want: 5, branch: "canary"},
		{name: "canary_101", count: 100, want: 10, branch: "canary"},
	}
	for _, c := range cs {
		t.Run(c.name, func(t *testing.T) {
			m := &manifest.Manifest{
				Name: "service",
				DC: map[string]int{
					"sas": c.count,
					"myt": c.count,
				},
				Config: manifest.NewConfig(),
			}
			sMap := &spb.ServiceMap{}
			ctx := scheduler.MakeContext("1", c.branch, m, sMap, 0, common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
			cfg := NewConf(common.Test)
			b := jobBuilder{
				jobCtx:  ctx,
				conf:    &cfg,
				taskEnv: map[string]string{},
			}
			result := b.makeGroups()
			for _, task := range result {
				assert.Equal(t, c.want, *task.Count)
			}
		})
	}
}

func TestJobBuilder_Build_basic(t *testing.T) {
	test.InitTestEnv()

	m := &manifest.Manifest{
		Name: "test-svc",
		DC: map[string]int{
			"sas": 10,
			"vla": 20,
		},
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{Name: "test-svc"}
	sc := &scheduler.Context{Layer: common.Test, Version: "1", ServiceMap: sMap, Manifest: m}
	cfg := NewConf(common.Test)
	b := jobBuilder{
		jobCtx:  sc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}

	job, err := b.Build()
	require.NoError(t, err)
	assert.Equal(t, "test-svc", *job.Name)
	assert.Equal(t, "service", *job.Type)
}

func TestJobBuilder_Build_batch(t *testing.T) {
	test.InitTestEnv()

	bc := &scheduler.BatchContext{
		BatchId: 42,
		Version: "v1",
		ServiceMap: &spb.ServiceMap{
			Name: "some-sched",
		},
		Manifest: &manifest.Manifest{
			Config: manifest.NewConfig(),
		},
		Context: context.Background(),
	}
	cfg := NewConf(common.Test)
	job, err := (&jobBuilder{
		jobCtx:  bc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}).Build()
	require.NoError(t, err)

	wj := (*jobWrapper)(job)
	require.Equal(t, "42", job.Meta["batch_id"])
	assert.Equal(t, "some-sched", wj.GetName())
	assert.Equal(t, "batch", wj.GetType())
	assert.Nil(t, job.Update)

	grp := job.LookupTaskGroup("batch")
	require.NotNil(t, grp)
	require.Len(t, grp.Tasks, 1)

	task := grp.Tasks[0]
	assert.Equal(t, "some-sched-task", task.Name)
}

func TestJobBuilder_Build_batch_branch(t *testing.T) {
	test.InitTestEnv()

	bc := &scheduler.BatchContext{
		BatchId: 42,
		Branch:  "br1",
		Version: "v1",
		ServiceMap: &spb.ServiceMap{
			Name: "some-sched",
		},
		Manifest: &manifest.Manifest{
			Config: manifest.NewConfig(),
		},
		Context: context.Background(),
	}
	cfg := NewConf(common.Test)
	job, err := (&jobBuilder{
		jobCtx:  bc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}).Build()
	require.NoError(t, err)
	wj := (*jobWrapper)(job)
	assert.Equal(t, "some-sched-br1", wj.GetName())
}

func TestJobBuilder_Build_sidecar(t *testing.T) {
	test.InitTestEnv()

	cfg := NewConf(common.Test)
	job, err := (&jobBuilder{
		UseSidecar: true,
		jobCtx:     testServiceCtx(),
		conf:       &cfg,
		taskEnv:    map[string]string{},
	}).Build()
	require.NoError(t, err)
	require.Len(t, job.TaskGroups[0].Tasks, 2)

	tasksByName := make(map[string]*api.Task)
	for _, v := range job.TaskGroups[0].Tasks {
		tasksByName[v.Name] = v
	}
	mainTask := tasksByName["test-svc-task"]
	sidecarTask := tasksByName["test-svc-sidecar-task"]

	require.NotNil(t, sidecarTask)
	require.Contains(t, sidecarTask.Name, "sidecar")
	require.Equal(t, &api.TaskLifecycle{Hook: "prestart", Sidecar: true}, sidecarTask.Lifecycle)
	require.Equal(t, userBridgeName, sidecarTask.Config["network_mode"])
	require.NotEmpty(t, sidecarTask.Config["dns_options"])
	require.NotEmpty(t, sidecarTask.Config["dns_servers"])
	require.NotEmpty(t, sidecarTask.Config["advertise_ipv6_address"])
	require.NotEmpty(t, sidecarTask.Config["sysctl"])
	require.Len(t, sidecarTask.Services, 2)

	require.NotNil(t, mainTask)
	require.NotContains(t, mainTask.Name, "sidecar")
	require.Empty(t, mainTask.Lifecycle)
	require.Equal(t, "container:test-svc-sidecar-task-${NOMAD_ALLOC_ID}", mainTask.Config["network_mode"])
	require.Empty(t, mainTask.Config["dns_options"])
	require.Empty(t, mainTask.Config["dns_servers"])
	require.Empty(t, mainTask.Config["advertise_ipv6_address"])
	require.Empty(t, mainTask.Config["sysctl"])
	require.Empty(t, mainTask.Services)
}

func TestJobBuilder_Build_nosidecar(t *testing.T) {
	test.InitTestEnv()
	cfg := NewConf(common.Test)
	job, err := (&jobBuilder{
		UseSidecar: false,
		jobCtx:     testServiceCtx(),
		conf:       &cfg,
		taskEnv:    map[string]string{},
	}).Build()

	require.NoError(t, err)
	require.Len(t, job.TaskGroups, 2)
	require.Len(t, job.TaskGroups[0].Tasks, 1)

	task := job.TaskGroups[0].Tasks[0]

	require.Equal(t, userBridgeName, task.Config["network_mode"])
	require.NotEmpty(t, task.Config["dns_options"])
	require.NotEmpty(t, task.Config["dns_servers"])
	require.NotEmpty(t, task.Config["advertise_ipv6_address"])
	require.NotEmpty(t, task.Config["sysctl"])
	require.Len(t, task.Services, 2)
}

func TestGroupName(t *testing.T) {
	DCs := []string{"myt", "sas", "vla", "iva", "man"}
	for _, dc := range DCs {
		t.Run(dc, func(t *testing.T) {
			name := groupName(dc)
			dc2 := extractDcByGroupName(name)
			assert.Equal(t, dc, dc2)
		})
	}
}

func testServiceCtx() *scheduler.Context {
	sctx := &scheduler.Context{
		ServiceMap: &spb.ServiceMap{
			Name: "test-svc",
			Provides: []*spb.ServiceProvides{
				{
					Name:        "api",
					Protocol:    spb.ServiceProvides_http,
					Port:        80,
					Description: "Test api",
				},
			},
		},
		Manifest: &manifest.Manifest{
			Name:   "test-svc",
			Config: manifest.NewConfig(),
			DC:     map[string]int{"sas": 1, "vla": 1},
		},
	}
	return sctx
}

func TestOverrideCPU(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	testCases := []struct {
		name     string
		override int
		expect   int
	}{
		{"no override", 0, 200},
		{"override", 300, 300},
	}

	m := &manifest.Manifest{
		Name: "service",
		Resources: manifest.Resources{
			CPU:     200,
			AutoCpu: true,
		},
		Config: manifest.NewConfig(),
		DC: map[string]int{
			"sas": 5,
			"vla": 5,
		},
	}

	s := NewService(NewConf(common.Test), db, log, nil, feature_flags.NewService(db, nil, log), nil)
	sMap := &spb.ServiceMap{}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			ctx := scheduler.MakeContext("1", "", m, sMap, 0, common.Test, nil, tt.override, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
			job, err := s.buildJob(ctx, "")
			require.NoError(t, err)
			require.NotEmpty(t, job.TaskGroups)

			for _, group := range job.TaskGroups {
				for _, task := range group.Tasks {
					require.Equal(t, tt.expect, *task.Resources.CPU)
				}
			}
		})
	}
}

func TestOverrideDC(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	testCases := []struct {
		name     string
		override map[string]int
		expect   map[string]int
	}{
		{"no override", nil, map[string]int{
			"sas": 5,
			"myt": 6,
		}},
		{"override", map[string]int{
			"sas": 3,
			"myt": 2,
		}, map[string]int{
			"sas": 3,
			"myt": 2,
		}},
	}

	m := &manifest.Manifest{
		Name: "service",
		DC: map[string]int{
			"sas": 5,
			"myt": 6,
		},
		Config: manifest.NewConfig(),
	}

	s := NewService(NewConf(common.Test), db, log, nil, feature_flags.NewService(db, nil, log), nil)
	sMap := &spb.ServiceMap{}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			ctx := scheduler.MakeContext("1", "", m, sMap, 0, common.Test, tt.override, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
			job, err := s.buildJob(ctx, "")
			require.NoError(t, err)
			for _, group := range job.TaskGroups {
				dc := extractDcByGroupName(*group.Name)
				require.Equal(t, tt.expect[dc], *group.Count)
			}
		})
	}
}

func TestJobValidateNoError(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	//prepare
	s := NewService(NewConf(common.Test), db, log, nil, feature_flags.NewService(db, nil, log), nil)
	m := &manifest.Manifest{
		Name: "service",
		DC: map[string]int{
			"sas": 5,
			"myt": 6,
		},
		Resources: manifest.Resources{
			CPU:    200,
			Memory: 200,
		},
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{
		Name: "service",
	}
	ctx := scheduler.MakeContext("1", "", m, sMap, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	job, err := s.buildJob(ctx, "")
	require.NoError(t, err)
	//test
	err = s.jobValidate(context.Background(), job)
	//assert
	require.NoError(t, err)
}

func TestJobValidateError(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	//prepare
	s := NewService(NewConf(common.Test), db, log, nil, feature_flags.NewService(db, nil, log), nil)
	m := &manifest.Manifest{
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{
		Name: "test-svc",
	}
	ctx := scheduler.MakeContext("1", "", m, sMap, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	job, err := s.buildJob(ctx, "")
	require.NoError(t, err)
	//test
	err = s.jobValidate(context.Background(), job)
	//assert
	userError := &user_error.UserError{}
	require.True(t, errors.As(err, &userError))
}

func TestAnyDCJob(t *testing.T) {
	test.InitTestEnv()

	m := &manifest.Manifest{
		Name: "test-svc",
		DC: map[string]int{
			"any": 1,
		},
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{Name: "test-svc"}
	sc := &scheduler.Context{Layer: common.Test, Version: "1", ServiceMap: sMap, Manifest: m}
	cfg := NewConf(common.Test)
	b := jobBuilder{
		jobCtx:  sc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}

	job, err := b.Build()
	require.NoError(t, err)
	assert.Equal(t, "test-svc", *job.Name)
	assert.Equal(t, "service", *job.Type)
	assert.Equal(t, []string{"sas", "vla"}, job.Datacenters)

	for _, group := range job.TaskGroups {
		assert.Empty(t, group.Constraints)
	}
}

func TestTemplates(t *testing.T) {
	const (
		svcName = "test-svc"
	)

	test.InitTestEnv()

	m := &manifest.Manifest{
		Name: svcName,
		DC: map[string]int{
			"vla": 1,
			"sas": 1,
		},
		Config: manifest.NewConfig(),
	}

	sMap := &spb.ServiceMap{Name: svcName}
	sc := &scheduler.Context{Layer: common.Test, Version: "1", ServiceMap: sMap, Manifest: m}
	cfg := NewConf(common.Test)
	b := jobBuilder{
		jobCtx: sc,
		conf:   &cfg,
		taskEnv: map[string]string{
			"SHIVA_URL": "shiva-api.vrts-slb.prod.vertis.yandex.net:80",
			"SHIVA_TVM": "123",
		},
	}

	job, err := b.Build()
	require.NoError(t, err)
	assert.Equal(t, "shiva-api.vrts-slb.prod.vertis.yandex.net:80", job.TaskGroups[0].Tasks[0].Env["SHIVA_URL"])
	assert.Equal(t, "123", job.TaskGroups[0].Tasks[0].Env["SHIVA_TVM"])
}

func TestPriority(t *testing.T) {
	test.InitTestEnv()

	m := &manifest.Manifest{
		Name: "test-svc",
		DC: map[string]int{
			"vla": 1,
			"sas": 1,
		},
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{Name: "test-svc"}
	sc := &scheduler.Context{Layer: common.Test, Version: "1", ServiceMap: sMap, Manifest: m}
	cfg := NewConf(common.Test)
	b := jobBuilder{
		jobCtx:  sc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}

	job, err := b.Build()
	require.NoError(t, err)

	assert.Equal(t, b.conf.Priority, job.Priority)
}

func TestAnyOnePriority(t *testing.T) {
	test.InitTestEnv()

	m := &manifest.Manifest{
		Name: "test-svc",
		DC: map[string]int{
			"any": 1,
		},
		Config: manifest.NewConfig(),
	}
	sMap := &spb.ServiceMap{Name: "test-svc"}
	sc := &scheduler.Context{Layer: common.Test, Version: "1", ServiceMap: sMap, Manifest: m}
	cfg := NewConf(common.Test)
	b := jobBuilder{
		jobCtx:  sc,
		conf:    &cfg,
		taskEnv: map[string]string{},
	}

	job, err := b.Build()
	require.NoError(t, err)

	assert.Equal(t, b.conf.AnyOnePriority, job.Priority)
}
