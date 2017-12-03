
dm4c.add_plugin('org.deepamehta.sign-up', function() {

    var aclPlugin

    dm4c.add_listener("init_3", function() {
        aclPlugin = dm4c.get_plugin("de.deepamehta.accesscontrol")
        if (!is_logged_in()) show_sign_up_button()
        $(document).on("dialogopen", function(event, ui) {
            var $login_message = $("#login-message", event.target)
            if ($login_message.length > 0) {
                $('<span class="password-reset">'
                    + '<a href="/sign-up/request-password">Forgot password?</a></span>')
                    .insertAfter($('.ui-dialog #login-message'))
            }
        })
    })

    dm4c.add_listener("authority_increased", function() {
        hide_sign_up_button()
    })

    dm4c.add_listener("authority_decreased", function() {
        if (!is_logged_in()) show_sign_up_button()
    })

    function is_logged_in() {
        // assert the precondition of our implementation is met
        if (!aclPlugin) {
            throw new Error ("Assertion failed: The webclients AccessControlPlugin is "
                + "unavailable for the sign-up plugin relying on init_3-hook.")
        }
        // do login check through the acl plugin
        return aclPlugin.get_username()
    }

    function show_sign_up_button() {
        var $sign_up = jQuery('<a href="/sign-up/" id="sign-up-button">Sign up</a>')
        jQuery('#login-widget').prepend($sign_up)
    }

    function hide_sign_up_button() {
        jQuery('#sign-up-button').remove()
    }

})
