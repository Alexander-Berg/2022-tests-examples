package impl

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/heetch/confita"
	"github.com/jmoiron/sqlx"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/actions"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
	settingsImpl "a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings/impl"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage/db"
	"a.yandex-team.ru/billing/hot/accounts/pkg/templates"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/template"
)

func settingsFromString(cfg string) (entitysettings.Settings, error) {
	tmpFile, err := ioutil.TempFile("", "settings*.yaml")
	if err != nil {
		return nil, err
	}
	//goland:noinspection GoUnhandledErrorResult
	defer os.Remove(tmpFile.Name())

	if _, err := tmpFile.WriteString(cfg); err != nil {
		return nil, err
	}
	if err = tmpFile.Close(); err != nil {
		return nil, err
	}
	return settingsImpl.NewSettings(context.Background(), tmpFile.Name())
}

type LockInfo struct {
	ID       int64
	UID      string
	DT       time.Time
	ShardKey string
}

type testContext struct {
	context.Context
	Shards          storage.ShardStorage
	Templates       template.Renderer
	EntitySettings  entitysettings.Settings
	Actions         actions.Actions
	Sequencer       actions.Sequencer
	OldRowsDeleter  actions.OldRowsDeleter
	SequencerConfig core.SequencerConfig
}

func setupContext() (testContext, func(), error) {
	var loader *confita.Loader

	configPath, ok := os.LookupEnv("LOCAL_TEST_CONFIG_PATH")
	if !ok {
		loader, _ = bconfig.PrepareLoader()
	} else {
		loader, _ = bconfig.PrepareLoader(configPath)
	}

	config := core.Config{}

	ctx := testContext{
		Context: context.Background(),
	}

	err := loader.Load(ctx, &config)
	if err != nil {
		return ctx, nil, err
	}
	for _, shardConfig := range config.Shards.Shards {
		shardConfig.Storage.ReconnectRetries = 10
	}

	// setup storage
	ctx.Shards = db.NewShardStorage(db.CreateShards(config.Shards))
	shard, err := ctx.Shards.GetLastShard()
	if err != nil {
		return ctx, nil, err
	}
	err = shard.Connect(ctx)
	if err != nil {
		return ctx, nil, err
	}

	ctx.Templates = templates.NewCache()

	return ctx, func() {
		_ = shard.Disconnect(ctx)
	}, nil
}

type ActionTestSuite struct {
	suite.Suite
	ctx     testContext
	cleanup func()
}

func (s *ActionTestSuite) SetupTest() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	s.Require().NoError(err)
}

func (s *ActionTestSuite) AfterTest(_, _ string) {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func (s *ActionTestSuite) loadCfg(cfg string) {
	settings, err := settingsFromString(cfg)
	if err != nil {
		s.T().Fatal(err)
	}
	s.ctx.EntitySettings = settings
}

func (s *ActionTestSuite) initActions() {

	s.ctx.Actions = NewActions(s.ctx.EntitySettings, s.ctx.Shards, s.ctx.Templates)
}

func (s *ActionTestSuite) initSequencer() {
	s.ctx.SequencerConfig = core.SequencerConfig{
		UpdateLimit: 30000,
		LockID:      13666,
		MaxProcess:  5,
		IdleTimeout: 5 * time.Second,
	}
	s.ctx.Sequencer = NewSequencerActions(s.ctx.Shards, s.ctx.SequencerConfig, s.ctx.Templates)

	// Предыдущие тесты сбили количество событий - чтобы убедиться на каждом шаге, что мы обновляем в журнале
	// только те события, которые ожидаем, стоит изначально сделать вот это
	_, locked, err := s.ctx.Sequencer.UpdateEventSeqID(s.ctx,
		s.ctx.Shards.GetLastShardID())
	s.Require().NoError(err)
	s.Assert().Equal(false, locked, "the shard should not have been locked")
}

func (s *ActionTestSuite) ctxWithCfg(cfg string) *testContext {
	settings, err := settingsFromString(cfg)
	s.Require().NoError(err)
	return &testContext{
		Context:        s.ctx.Context,
		Shards:         s.ctx.Shards,
		Templates:      s.ctx.Templates,
		EntitySettings: settings,
		Actions:        NewActions(settings, s.ctx.Shards, s.ctx.Templates),
	}
}

func (s *ActionTestSuite) selectAccount(aNamespace, aType string, attributes []string) *sqlx.Rows {
	arguments := []any{aNamespace, aType}
	conditions := []string{"namespace=$1 AND type=$2"}
	for i := 0; i < entities.AttributesCount; i++ {
		var attrVal string
		if i >= len(attributes) {
			attrVal = ""
		} else {
			attrVal = attributes[i]
		}
		arguments = append(arguments, attrVal)
		conditions = append(conditions, fmt.Sprintf("attribute_%d=$%d", i+1, i+3))
	}
	// nolint: gosec
	query := fmt.Sprintf("SELECT id FROM acc.t_account WHERE %s", strings.Join(conditions, " AND "))
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, arguments...)
	s.Require().NoError(err)
	return rows
}

func (s *ActionTestSuite) getAccount(aNamespace, aType string, attributes []string) int64 {
	rows := s.selectAccount(aNamespace, aType, attributes)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	var res int64
	if rows.Next() {
		err := rows.Scan(&res)
		s.Require().NoError(err)
	}
	err := rows.Err()
	s.Require().NoError(err)
	return res
}

func (s *ActionTestSuite) checkAccountExists(loc entities.Location) bool {
	rows := s.selectAccount(loc.Namespace, loc.Type, loc.Attributes[:])
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()
	if rows.Next() {
		return true
	}
	err := rows.Err()
	s.Require().NoError(err)
	return false
}

func (s *ActionTestSuite) getSubaccounts(accountID int64) []subaccountInfo {
	var tmp subaccountInfo
	query := `
select attribute_1, attribute_2, attribute_3, attribute_4, attribute_5
from acc.t_subaccount where account_id = $1`

	refs := [entities.AttributesCount]any{}
	for i := range tmp {
		refs[i] = &tmp[i]
	}

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, accountID)
	s.Require().NoError(err)
	defer rows.Close()

	res := make([]subaccountInfo, 0)
	for rows.Next() {
		err = rows.Scan(refs[:]...)
		s.Require().NoError(err)
		res = append(res, tmp)
	}
	err = rows.Err()
	s.Require().NoError(err)
	return res
}

func (s *ActionTestSuite) getEvents(accountID int64) []eventInfo {
	var tmpEvent eventInfo
	query := "select event_batch_id, dt, type, amount, seq_id, attribute_1, attribute_2, attribute_3, attribute_4, attribute_5 from acc.t_event where account_id = $1"

	refs := []any{&tmpEvent.EventBatchID, &tmpEvent.Dt, &tmpEvent.Type, &tmpEvent.Amount, &tmpEvent.SeqID}
	for i := range tmpEvent.AddAttributes {
		refs = append(refs, &tmpEvent.AddAttributes[i])
	}

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, accountID)
	s.Require().NoError(err)
	defer rows.Close()
	res := make([]eventInfo, 0)
	for rows.Next() {
		err = rows.Scan(refs...)
		s.Require().NoError(err)
		res = append(res, tmpEvent)
	}
	err = rows.Err()
	s.Require().NoError(err)
	return res
}

func (s *ActionTestSuite) getLockInfo(loc entities.Location) LockInfo {
	queryTmpl := `
select id, uid, dt, shard_key from acc.t_lock where namespace = $1 and type = $2 and %s
`
	args := []any{loc.Namespace, loc.Type}
	attrs := make([]string, 0, 5)
	for i, val := range loc.Attributes {
		args = append(args, val)
		attrs = append(attrs, fmt.Sprintf("attribute_%d=$%d", i+1, i+3))
	}
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	query := fmt.Sprintf(queryTmpl, strings.Join(attrs, " and "))
	row := shard.GetDatabase(s.ctx, pg.Master).QueryRowxContext(s.ctx, query, args...)
	var res LockInfo
	err = row.Scan(&res.ID, &res.UID, &res.DT, &res.ShardKey)
	s.Require().NoError(err)
	return res
}

func (s *ActionTestSuite) selectState(sNamespace, sType string, attributes []string) *sqlx.Rows {
	arguments := []any{sNamespace, sType}
	conditions := []string{"namespace=$1 AND type=$2"}
	for i := 0; i < entities.AttributesCount; i++ {
		var attrVal string
		if i >= len(attributes) {
			attrVal = ""
		} else {
			attrVal = attributes[i]
		}
		arguments = append(arguments, attrVal)
		conditions = append(conditions, fmt.Sprintf("attribute_%d=$%d", i+1, i+3))
	}
	// nolint: gosec
	query := fmt.Sprintf("SELECT id, state FROM acc.t_state_account WHERE %s", strings.Join(conditions, " AND "))
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, arguments...)
	s.Require().NoError(err)
	return rows
}

func (s *ActionTestSuite) getState(sNamespace, sType string, attributes []string) *stateTechData {
	rows := s.selectState(sNamespace, sType, attributes)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	res := stateTechData{}
	if rows.Next() {
		err := rows.Scan(&res.ID, &res.State)
		s.Require().NoError(err)
	}
	err := rows.Err()
	s.Require().NoError(err)
	return &res
}

func (s *ActionTestSuite) checkStateExists(loc entities.Location) bool {
	rows := s.selectState(loc.Namespace, loc.Type, loc.Attributes[:])
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()
	if rows.Next() {
		return true
	}
	err := rows.Err()
	s.Require().NoError(err)
	return false
}

func (s *ActionTestSuite) getStateEvents(accountID int64) []eventTechData {
	res := make([]eventTechData, 0)
	query := "SELECT state, event_batch_id FROM acc.t_state_event WHERE state_account_id = $1"
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, accountID)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	for rows.Next() {
		res = append(res, eventTechData{})
		d := &res[len(res)-1]
		err = rows.Scan(&d.State, &d.BatchID)
		s.Require().NoError(err)
	}
	err = rows.Err()
	s.Require().NoError(err)
	return res
}

func (s *ActionTestSuite) checkEntityID(id int64, shard storage.Shard) {
	s.Assert().True(id >= shard.GetMinSeqID())
	s.Assert().True(id <= shard.GetMaxValidSeqID())
}

func (s *ActionTestSuite) checkEntityIDs(ids []int64, shard storage.Shard) {
	for _, id := range ids {
		s.checkEntityID(id, shard)
	}
}
