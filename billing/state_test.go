package impl

import (
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type StateActionTestSuite struct {
	ActionTestSuite
}

type stateTechData struct {
	ID    int64
	State []byte
}

type eventTechData struct {
	State   string
	BatchID int64
}

func (s *StateActionTestSuite) SetupTest() {
	cfg := `
manifests:
- namespace: state_tests
  states:
    state_type:
      attributes:
        - a1
        - a2
        - a3
      shard:
        prefix: p
        attributes:
          - a1
      rollup_period:
        - "0 * * *"
`
	s.ActionTestSuite.SetupTest()
	s.ActionTestSuite.loadCfg(cfg)
	s.ActionTestSuite.initActions()
}

func (s *StateActionTestSuite) AfterTest(suiteName, testName string) {
	s.ActionTestSuite.AfterTest(suiteName, testName)
}

func (s *StateActionTestSuite) genWriteRequest(states []entities.StateAttributes) entities.BatchWriteRequest {

	return entities.BatchWriteRequest{
		EventType:  "test",
		ExternalID: btesting.RandS(100),
		Info:       []byte(`{}`),
		States:     states,
	}
}

func (s *StateActionTestSuite) map2attrs(attrsMap map[string]*string) entities.LocationAttributes {
	return entities.LocationAttributes{
		Namespace:  "state_tests",
		Type:       "state_type",
		Attributes: attrsMap,
	}
}

func (s *StateActionTestSuite) attrs2loc(attributes []string) entities.Location {
	attrsMap := map[string]*string{"a1": &attributes[0], "a2": &attributes[1], "a3": &attributes[2]}
	locAttrs := s.map2attrs(attrsMap)
	loc, err := s.ctx.EntitySettings.State().Location(&locAttrs)
	if err != nil {
		s.T().Fatal(err)
	}
	return loc
}

func (s *StateActionTestSuite) createState(attributes []string, state []byte) (*stateTechData, error) {
	queryTpl := `
INSERT INTO acc.t_state_account (id, namespace, type, shard_key, state, dt, %s)
VALUES (nextval('acc.s_state_account_id'), $1, $2, $3, $4, clock_timestamp(), %s)
RETURNING id`

	loc := s.attrs2loc(attributes)
	arguments := []any{loc.Namespace, loc.Type, loc.ShardKey, string(state)}
	columns := make([]string, entities.AttributesCount)
	placeholders := make([]string, entities.AttributesCount)
	for i := 0; i < entities.AttributesCount; i++ {
		var attrVal string
		if i >= len(attributes) {
			attrVal = ""
		} else {
			attrVal = attributes[i]
		}
		arguments = append(arguments, attrVal)
		columns[i] = fmt.Sprintf("attribute_%d", i+1)
		placeholders[i] = fmt.Sprintf("$%d", i+5)
	}

	query := fmt.Sprintf(queryTpl, strings.Join(columns, ", "), strings.Join(placeholders, ", "))
	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		return nil, err
	}

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, arguments...)
	if err != nil {
		return nil, err
	}
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	res := stateTechData{State: state}
	if rows.Next() {
		err := rows.Scan(&res.ID)
		if err != nil {
			return nil, err
		}
	}
	return &res, nil
}

func (s *StateActionTestSuite) TestGetStateOk() {
	var err error

	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{"a1": &attrs[0], "a2": &attrs[1], "a3": &attrs[2]}
	state := []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10)))

	_, err = s.createState(attrs, state)
	if err != nil {
		s.T().Fatal(err)
	}

	getRes, err := s.ctx.Actions.GetState(s.ctx, s.map2attrs(attrsMap))
	if err != nil {
		s.T().Fatal(err)
	}

	assert.Equal(s.T(), getRes.State, state)
}

func (s *StateActionTestSuite) TestGetStateNoData() {
	var err error

	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	brokenAttr := attrs[1] + "_"
	attrsMap := map[string]*string{"a1": &attrs[0], "a2": &brokenAttr, "a3": &attrs[2]}

	_, err = s.createState(attrs, []byte(`["A", "B"]`))
	if err != nil {
		s.T().Fatal(err)
	}

	_, err = s.ctx.Actions.GetState(s.ctx, s.map2attrs(attrsMap))
	if assert.Error(s.T(), err) {
		assert.Equal(s.T(), err.Error(), "no state found")
	}
}

func (s *StateActionTestSuite) TestGetStateInvalidSettings() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
	}

	_, err := s.ctx.Actions.GetState(s.ctx, s.map2attrs(attrsMap))
	assert.EqualError(s.T(), err, "failed to validate attrs: no attribute a3")
}

func (s *StateActionTestSuite) TestUpdateStateNew() {
	var err error

	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{"a1": &attrs[0], "a2": &attrs[1], "a3": &attrs[2]}

	state := []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10)))

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.StateAttributes{{
			Loc:   s.map2attrs(attrsMap),
			State: state,
		}}))
	if err != nil {
		s.T().Fatal(err)
	}

	tech := s.getState("state_tests", "state_type", attrs)
	assert.Equal(s.T(), tech.State, state)

	events := s.getStateEvents(tech.ID)
	assert.Equal(s.T(), events, []eventTechData{{string(state), batchRes.BatchID}})
}

func (s *StateActionTestSuite) TestUpdateStateExisting() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{"a1": &attrs[0], "a2": &attrs[1], "a3": &attrs[2]}

	states := []string{
		fmt.Sprintf(`["%s", "1"]`, btesting.RandS(10)),
		fmt.Sprintf(`["%s", "2"]`, btesting.RandS(10)),
		fmt.Sprintf(`["%s", "3"]`, btesting.RandS(10)),
	}
	batchIDs := make([]int64, 3)

	for i, state := range states {
		batchRes, err := s.ctx.Actions.WriteBatch(
			s.ctx,
			s.genWriteRequest([]entities.StateAttributes{{
				Loc:   s.map2attrs(attrsMap),
				State: []byte(state),
			}}))
		if err != nil {
			s.T().Fatal(err)
		}
		batchIDs[i] = batchRes.BatchID
	}

	tech := s.getState("state_tests", "state_type", attrs)
	assert.Equal(s.T(), tech.State, []byte(states[2]))

	events := s.getStateEvents(tech.ID)
	reqEvents := make([]eventTechData, 3)
	for i, state := range states {
		reqEvents[i].BatchID = batchIDs[i]
		reqEvents[i].State = state
	}
	assert.Equal(s.T(), events, reqEvents)
}

func (s *StateActionTestSuite) TestUpdateStateInvalidSettings() {
	var err error

	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
	}

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.StateAttributes{{
			Loc:   s.map2attrs(attrsMap),
			State: []byte("[]"),
		}}))
	assert.EqualError(s.T(), err, "failed to get shard: failed to validate attributes: no attribute a3")
}

func (s *StateActionTestSuite) TestUpdateMultipleStateMixed() {
	var err error

	mainAttr := btesting.RandS(100)
	attrsLists := make([][]string, 2)
	for i := range attrsLists {
		attrsLists[i] = []string{
			mainAttr,
			btesting.RandS(100),
			btesting.RandS(100),
		}
	}
	states := []string{
		fmt.Sprintf(`["%s", "1"]`, btesting.RandS(10)),
		fmt.Sprintf(`["%s", "2"]`, btesting.RandS(10)),
	}
	stateValues := make([]entities.StateAttributes, 2)
	for i := range attrsLists {
		stateValues[i] = entities.StateAttributes{
			Loc: s.map2attrs(map[string]*string{
				"a1": &attrsLists[i][0],
				"a2": &attrsLists[i][1],
				"a3": &attrsLists[i][2],
			}),
			State: []byte(states[i]),
		}
	}

	_, err = s.createState(attrsLists[0], []byte(states[1]))
	if err != nil {
		s.T().Fatal(err)
	}

	batchRes, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(stateValues))
	if err != nil {
		s.T().Fatal(err)
	}

	for i, attrs := range attrsLists {
		tech := s.getState("state_tests", "state_type", attrs)
		assert.Equal(s.T(), string(tech.State), states[i])
		events := s.getStateEvents(tech.ID)
		assert.Equal(s.T(), events, []eventTechData{{states[i], batchRes.BatchID}})
	}
}

func TestStateActionTestSuite(t *testing.T) {
	suite.Run(t, new(StateActionTestSuite))
}
