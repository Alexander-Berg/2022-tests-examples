Feature: KKT withdraw

    Scenario: Stolen KKT withdraw
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate withdraw application for KKT 'new' with reason 'KKT_STOLEN'
        Then Application generation for KKT 'new' completes with success

        When We send obtained withdraw application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' withdraw state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' withdraw state become rejected

    Scenario: Missing KKT withdraw
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate withdraw application for KKT 'new' with reason 'KKT_MISSING'
        Then Application generation for KKT 'new' completes with success

        When We send obtained withdraw application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' withdraw state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' withdraw state become rejected

    Scenario: KKT with broken fn withdraw
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate withdraw application for KKT 'new' with reason 'FN_BROKEN'
        Then Application generation for KKT 'new' completes with success

        When We send obtained withdraw application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' withdraw state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' withdraw state become rejected

    Scenario: Regular KKT withdraw
        Given New KKT 'new'

        When We generate registration application for KKT 'new'
        Then Application generation for KKT 'new' completes with success

        When We send obtained registration application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' registration state within 10 minutes
        Then KKT 'new' registration state become registered

        When We generate withdraw application for KKT 'new' with reason 'FISCAL_CLOSE'
        Then Application generation for KKT 'new' completes with success

        When We send obtained withdraw application for KKT 'new' to fns
        Then Application sending for KKT 'new' completes with success

        When We poll KKT 'new' withdraw state within 10 minutes
        # test stand doesn't store info about previous registrations so we await rejection
        Then KKT 'new' withdraw state become rejected
