SEVERAL_ALLOWED_TRANSITIONS = '''INITIAL:
  owner: test
  edges:
    HAVE_CHANGES:
      conditions:
        - check(True)
      actions:
        - do_something('HAVE_CHANGES')

    CLOSED:
      conditions:
        - check(True)
      actions:
        - change_status('closed', 'incorrect')'''

NO_ALLOWED_TRANSITIONS = '''INITIAL:
  owner: test
  edges:
    HAVE_CHANGES:
      conditions:
        - check(False)
      actions:
        - do_something('HAVE_CHANGES')

    CLOSED:
      conditions:
        - check(False)
      actions:
        - change_status('closed', 'incorrect')'''

TRIVIAL_GRAPH = '''INITIAL:
  owner: test'''

ALARM_EXCEPTION_IN_CONDITION = '''INITIAL:
  owner: test
  edges:
    HAVE_CHANGES:
      conditions:
        - raise_exception(AlarmException)
      actions:
        - do_something('HAVE_CHANGES')'''

ALARM_EXCEPTION_IN_ACTION = '''INITIAL:
  owner: test
  edges:
    HAVE_CHANGES:
      conditions:
        - check(True)
      actions:
        - raise_exception(AlarmException)'''

TESTS_LAUNCHED = "Test was started:\n\tBalanceFull: {BalanceFull}\n\tBalanceSmoke: {BalanceSmoke}\n\t" \
                 "TrustGreenline: {TrustGreenline}\n\tAqua: {FullJava}\nAsk {TEST} about results"

SUCCESSFULL_SMOKE = '''INITIAL:
  owner: test
  edges:
    HAVE_CHANGES:
      conditions:
        - check(True)
      actions:
        - do_something('HAVE_CHANGES')

HAVE_CHANGES:
  owner: dev
  edges:
    DEPLOYED:
      actions:
        - build_and_deploy()

DEPLOYED:
  owner: test
  edges:
    TEST_IN_PROGRESS:
      actions:
        - start_teamcity(['BalanceFull', 'BalanceSmoke'])
        - start_teamcity(['TrustGreenline'])
        - start_aqua(['FullJava'])
        - comment(TESTS_LAUNCHED)

TEST_IN_PROGRESS:
  owner: test
  edges:
    TEST_COMPLETE:
      conditions:
        - is_task_finish(Python_smoke)
        - is_task_finish(TRUST_smoke)
        - is_task_finish(Java_smoke)
      actions:
        - comment_values(TESTS_LAUNCHED)

TEST_COMPLETE:
  owner: test'''
