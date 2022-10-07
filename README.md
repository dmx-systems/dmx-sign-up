
# DMX Sign-up

This plugin introduces a configurable user registration process for DMX _User Accounts_. The plugin can be configured so users need to opt-in, utilizing confirmation emails (active for 60mins).

Most (if not all) user facing message are translatable by now. A german language version of this bundle can be build from source after setting the language option to `de` using the `plugin.properties` file.

This plugin adds:
*   A "User Mailbox" association type to associate "Email Address" w. "User Accounts".
*   A `Sign-up`-link next to the `Login`-button in the DMX Webclient
*   A `Sign-up Configuration` topic associated to the `DMX Sign up` Plugin topic
     (part of the "System" workspace and thus editable by all members of it)
*   A `Forgot password?` link to the `Login`-dialog in the DMX Webclient

The special features of the **self-registration ui** is comprised of:
*   Username existence check
*   Email existence check
*   Simple GUI-Notification mechanism
*   Minimal CSS Definition
*   Administration workspace members can create accounts without email confirmation

The special features of the **login ui** is comprised of:
*   Simple GUI-Notification mechanism
*   Automatic redirect
*   Minimal CSS Definition

The **special logic** of this plugin is comprised of:
*   Optionally: Setup an email based confirmation workflow for new accounts<br/>
    Sends confirmation mail with token to the users registering Email address<br/>
    Allows for the password reset functionality to take place also via an Email based confirmation workflow
*   Optionally: Send notifications to system administrator if a new user account was created
*   Optionally: If `dmx.security.new_accounts_are_enabled` (platform configuration option) is set to `true` an account activation notice is sent
*   Optionally: If a `User Mailbox` exists a "Passwort reset"-workflow is available
*   Optionally: A workspace URI can be specified whose members have the right to create new users.
*   Optionally: User accounts can be created through the LDAP plugin.

**Note:** If `Email Confirmation Required` is set to _true_ the confirmation tokens the system sends out are **not persisted** and get lost after a bundle/system restart. Once a token was send out the link containing it is valid for sixty minutes.

Email address topics of new user accounts are all placed in the "Administration" workspace too.

## Requirements

DMX 5.1: DMX is a platform for collaboration and knowledge management.
https://github.com/dmx-systems/dmx-platform

To be able to install this module you first and additionally have to install the following DMX Plugins.

*    `dmx-thymeleaf-0.9.3+`-Bundle - Build from [sources](https://git.dmx.systems/dmx-plugins/dmx-thymeleaf)
*    `dmx-sendmail-2.0.2+`-Bundle - Build from [sources](https://git.dmx.systems/dmx-plugins/dmx-sendmail)
*    `dmx-ldap-0.5.4-SNAPSHOT`+`-Bundle - Build from [sources](https://git.dmx.systems/dmx-plugins/dmx-ldap)

You can find the respective plugin version and its dependencies for download at [https://download.dmx.systems/plugins/](https://download.dmx.systems/plugins/).

**Operations:** For the plugins mailbox validation process to run you must install these plugins with DMX on a web server with a 
`postfix` -> `Internet Site` like mail send functionality.

After downloading all bundle-files, place them in the `bundle-deploy` folder of your DMX installation and restart DMX.

## Plugin Configuration

Since the 2.0.0 release, the following options must be configured in either the dmx-platform's `config.properties` (binary release) or `pom.xml` (if you run the platform from sources) file.

```
dmx.signup.confirm_email_address = true
dmx.signup.admin_mailbox = signup-test@dmx.systems
dmx.signup.system_mailbox = nomail@dmx.systems
dmx.signup.self_registration = false
dmx.signup.account_creation_auth_ws_uri =
dmx.signup.ldap_account_creation = false
```

Legacy wise, the rest of the plugin options are stored in DB. The central topic for configuring the sign-up plugin is of type `Sign-up Configuration`. Editing this topic via the DMX Webclient allows you to interactively configure the appearance of the custom login and self-registration dialogs.

The sign-up configuration is associated with the "Plugin" topic representing this plugin ("DMX Sign up"). It can be revealed by all members of the `Administration` workspace.

Note: If you want to use the "Password reset" functionality without allowing users to self-register you must make sure "User Account" topics are equipped with a "User Mailbox". To set this up, see instructions here: https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/issues/2

## Usage Hints

If you only want to use the password-reset functionality and/or equip existing user accounts with an email address topic, please read the following hints: https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/issues/2#note_17729

If you don't want to allow users to self-register accounts but want to make use of the sign-up plugin features, you find more [hints here](https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/issues/26#note_17855). Basically you need to log in as "admin" ("or Administration workspace member) first and then browse (manually) to `/sign-up` and use the form to create new accounts.

In order for an already logged in user have the permission to create a new user the former user either needs to be a member of the administration workspace or be a member of the workspace specified through the "dmx.signup.account_creation_auth_ws_uri" configuration option. Not setting this value means that only the
administration workspace membership check is in place.

### Optional module configuration

The module configuration is done through a configuration topic in DMX itself. However during the *first deployment* default values for the topic can be provided through the platform configuration through
the following properties:
```
dmx.signup.config_project_title =
dmx.signup.config_webapp_title =
dmx.signup.config_webapp_logo_path = 
dmx.signup.config_custom_css_path = 
dmx.signup.config_read_more_url = 
dmx.signup.config_pages_footer = 
dmx.signup.config_tos_label = 
dmx.signup.config_tos_detail = 
dmx.signup.config_pd_label = 
dmx.signup.config_pd_detail = 
dmx.signup.start_page_url = 
dmx.signup.home_page_url = 
dmx.signup.loading_app_hint = 
dmx.signup.logging_out_hint = 
dmx.signup.config_api_enabled = false 
dmx.signup.config_api_description =
dmx.signup.config_api_details =
dmx.signup.config_api_workspace_uri =

In case a value is not provided in the platform configuration a built-in default value is used.

### Setup Custom Workspace Assignment

There is currently just one more special configuration option: You can setup an automatic workspace assignment for self-registering users. If you do so, new users using the sign-up dialog automatically join (become members of) that works. To do so you need to associate that very workspace topic with your active sign-up configuration. 

And here comes the **pitfall**: To take this "custom workspace assignment" into effect you must press "Edit" and "Save" on your current sign-up configuration topic once (or restart the platform). Only in these two cases the sign-up configuration is reloaded and comes into effect, see [#1](https://github.com/mukil/dm4-sign-up/issues/1)).

You'll notice something similar to the following two lines in your server-side log (when editing and Saving your sign-up configuration):
```
Jan 15, 2021 01:28:37 AM systems.dmx.signup.SignupPlugin reloadAssociatedSignupConfiguration
INFORMATION: Configured Custom Sign-up Workspace => "DMX"
Jan 15, 2021 01:28:37 AM systems.dmx.signup.SignupPlugin reloadAssociatedSignupConfiguration
INFORMATION: Sign-up Configuration Loaded (URI="dmx.signup.default_configuration"), Name="My DMX"
```

### Account creation through LDAP plugin

The plugin can be configured to use the dmx-ldap plugin for account creation. When enabled the username and password are stored in LDAP instead of DMX itself. The option is disabled by default and is switched on by setting the property:

```
dmx.signup.ldap_account_creation = true
```

Keep in mind that the dmx-ldap plugin also has a configuration option that controls account creation functionality. This  must be enabled as well. Refer to the [plugins README](https://git.dmx.systems/dmx-plugins/dmx-ldap/-/blob/master/README.md) for more information on its configuration. 

## License

DMX Sign-up is available freely under the GNU Affero General Public License, version 3 or later (see [License](https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/blob/master/LICENSE)).

## Version history

**2.1.0-SNAPSHOT** -- Upcoming

* Account creation through LDAP plugin
* True optional dependency on `dmx-ldap-0.6.1+` plugin
* Configure workspace whose users can create new users
* Configure module configuration through property configuration

**2.0.1** -- Jun 30, 2021

* Compatible with DMX 5.2
* Migrates existing 'User Mailbox' edges into new platform association type 
* Log config settings on bundle activation

**2.0.0** -- Jan 15, 2021

* Compatible with DMX 5.1
* Adapted dialog styles to resemble DMX 5.1 styling
* Password reset-workflow available without sign-up enabled
* Four core configuration options externalized into `config.properties`
* New configuration option to de-activate sign-up (e.g. to only use password-reset functionality)
* Rewritten plugins webclient integration for DMX 5.1
* Mailservices factored-out into [dmx-sendmail](https://git.dmx.systems/dmx-plugins/dmx-sendmail) plugin
* Adapted License to AGPL 3.0
* Adapted all type URIs to new namespace 

**1.6.0** -- Mar 31, 2018

* Minor refactoring of the service API
* Added Javadocs to the main service calls
* Couple of bug fixes:<br/>
  Make confirmation link name configurable<br/>
  JS compatibility for IE10+
* Compatible with DeepaMehta 4.9

**1.5.2** -- Feb 12, 2017

* Allows members of the _Administration_ workspace to create accounts w/out confirmation mails
  (even if email based confirmation workflow is ON)
* Acccount creation does not fail because confirmation workflow active but SMTP unavailable
* Improved logging if confirmation workflow active but SMTP unavailable
* Fixes missing stylesheet on confirmation failure page (e.g. when link expired)
* Clarified resource bundle loading & slightly extended translations

**1.5.1** -- Nov 14, 2016

* Fixes critical error (typo introduced during translations) in password-reset template
* Extends translatable hints for sign-up and login dialog, added german languaged messages
* Fixes some typos in user dialogs and the header style on the account-edit template
* New "API Usage" option now translatable and basically working (see "/sign-up/edit")
* Adds migration to move the "API Membership Request" topic into "System" workspace
* Some general (but minor) improvements

**1.5** -- Aug 05, 2016

* Translatable (HTML dialogs and Emails) using Javas ResourceBundles mechanism (almost complete)
* Introduced a new plugin.property `org.deepamehta.sign-up.language=en` with support for<br/>
  building this plugin in `de` and `fr` language (additionally to the default `en`)
* Added "German" translation to the most important user facing dialogs
* Including (empty by default) navigation HTML fragment which other plugins can override
  (and thus use to inject their own navigation HTML fragment into the sign-up templates)
* Requires the upcoming dm4-thymeleaf version 0.6.1
* Fixes sign-up form for users of MSIE
* Compatible with DeepaMehta 4.8.1

**1.4** -- Jul 11, 2016

New features and changes:<br/>
* Extended dialogs to manage passwort reset and login
* Introduces password reset functionality via Email
* Added migration moved config topic to 'Administration'
* Configuration can thus only be loaded during 'init' hook or by 'admin'
* Compatible with DeepaMehta 4.8

Additional Changes:<br/>
- New Configuration options in particular as required by dm4-kiezatlas-website:<br/>
  Displaying Logout functionality if the user is currently logged-in and visits the login page<br/>
  Added two custom workspace membership features: 1) is set up via a simple _Association_ between the Workspace and the Sign-up Configuraton topic and the other 2) is modelled as a _Note_ relating requests for an additional workspace membership (which works for _private_ or _confidential_ workspaces)<br/>
  A new route `/sign-up/edit` view allowing to manage this custom workspace feature<br/>
  Added options to have redirects after login/logout configurable<br/>
- Signed up mailboxes are for now stored in _admins_ _Private Workspace_ workspace<br/>
- Providing a OSGi mail notification service for other plugins to send mails to the mailbox configured in _System Recipient Mailbox_

Fixes:<br/>
- Bug in client side form validation leading to a possible registration when the username is already taken


**1.1** -- Nov 23, 2015

- "Email Confirmation Required" is now a new configuration option:<br/>
  If `.. Required`, confirmation mails are send out including a token<br/>
  (valid for 60mins) and a link to proceed with the sign-up process<br/>
  Note: This option requires a 'postfix' -> 'Internet Site' like web server setup
- Further, if an "Admin Mailbox" is set, notifications on each account creation are sent to admin
- Updated sources to be compatible with DeepaMehta 4.7
- Updated dependency to bundle dm47-webactivator-0.4.6
- Included a few webpages which inform the user about the sign-up process
- If `new_accounts_are_enabled` is set to false, a notification is sent to the user when her
  account is `Enabled` by an administrator

Note: This plugin is not compatible with previous installations of the dm4-sign-up module.

**1.0.0** -- Dec 25, 2014

- configurable by end-users
- compatible with 4.4
- feature complete

Authors
-------

Copyright (c) 2014-2019 Malte Reißig
Copyright (c) 2020-2021 DMX Systems
