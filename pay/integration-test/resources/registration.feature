Feature: KKT registration

    Scenario: New KKT registration
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate registration report application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration report application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration report state within 10 minutes
        Then KKT 'new' registration report state become rejected due to fpd invalid value
