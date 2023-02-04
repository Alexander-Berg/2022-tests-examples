Feature: KKT reregistration

    Scenario: Simple KKT reregistration
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate reregistration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained reregistration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' reregistration state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' registration state become rejected

    Scenario: KKT upgrade to FFD 1.2
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate reregistration application for marked goods without replacing fn KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained reregistration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' reregistration state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' registration state become rejected
