
# DMX Sign-up

This plugin introduces service function for configurable user registration process for DMX _User Accounts_. 

The plugin can be configured so users need to opt-in, utilizing confirmation emails (active for 60mins).


This plugin adds:
*   A "User Mailbox" association type to associate "Email Address" w. "User Accounts".

The special features of the **self-registration ui** is comprised of:
*   Administration workspace members can create accounts without email confirmation

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

DMX 5.3: DMX is a platform for collaboration and knowledge management.
https://github.com/dmx-systems/dmx-platform

To be able to install this module you first and additionally have to install the following DMX Plugins.

*    `dmx-sendmail-2.1.0+`-Bundle - Build from [sources](https://git.dmx.systems/dmx-plugins/dmx-sendmail)
*    Optional: `dmx-ldap-0.6.2-SNAPSHOT`+`-Bundle - Build from [sources](https://git.dmx.systems/dmx-plugins/dmx-ldap)

You can find the respective plugin version and its dependencies for download at [https://download.dmx.systems/plugins/](https://download.dmx.systems/plugins/).

**Operations:** For the plugins mailbox validation process to run you must install these plugins with DMX on a web server with a 
`postfix` -> `Internet Site` like mail send functionality.

After downloading all bundle-files, place them in the `bundle-deploy` folder of your DMX installation and restart DMX.

## Plugin Configuration

The following options must be configured via dmx-platform's `config.properties`:

```
dmx.signup.confirm_email_address = true
dmx.signup.admin_mailbox = signup-test@dmx.systems
dmx.signup.system_mailbox = nomail@dmx.systems
dmx.signup.account_creation = admin
dmx.signup.account_creation_password_handling = generated
dmx.signup.account_creation_auth_ws_uri =
dmx.signup.ldap_account_creation = false
dmx.signup.token_expiration_time = 2
dmx.signup.username_policy = unconfined
dmx.signup.expected_password_complexity = complex
```

### Configure account creation logic

The sign-up can work in three different modes:

| Value    | Description                                                                                 |
|----------|---------------------------------------------------------------------------------------------|
| disabled | nobody can create new accounts                                                              |
| admin    | only administrator (privileged users) can create new accounts                               |
| public   | anybody can create new accounts through self-registration in addition to the administrators |

The desired working mode is configured by setting the property: 
```
dmx.signup.account_creation = disabled | admin | public
```

The default value is "disabled" and also goes into effect when the value has been misspelled. 

If you only want to use the password-reset functionality and/or equip existing user accounts with an email address topic, please read the following hints: https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/issues/2#note_17729

If you don't want to allow users to self-register accounts but want to make use of the sign-up plugin features, you find more [hints here](https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/issues/26#note_17855). Basically you need to log in as "admin" ("or Administration workspace member) first and then browse (manually) to `/sign-up` and use the form to create new accounts.

In order for an already logged in user have the permission to create a new user the former user either needs to be a member of the administration workspace or be a member of the workspace specified through the "dmx.signup.account_creation_auth_ws_uri" configuration option. Not setting this value means that only the
administration workspace membership check is in place.

### Configure account creation password handling

The plugin can be configured so that during sign-up one can enter the password to be used for the account or the
password is generated and users are required to do an initial password reset in order to set the password to a value
of their liking.

The behavior is configured through the option:

```
dmx.signup.account_creation_password_handling = editable | generated
```

### Token validity
The validity of the tokens used for account creation and password reset in *hours* is set through the property:
```
dmx.signup.token_expiration_time = 2
```

The default value is 2, so a user has two hours from the moment of creating the account/requesting the password reset to
click on the link that is contained in the account creation/password reset request email. 

### Configure username policy

The sign-up plugin supports three different policies regarding username handling

| Value                   | Description                                                                           |
|-------------------------|---------------------------------------------------------------------------------------|
| unconfined              | Username, email address and display name can be chosen freely of each other           |
| username_is_email       | Only email adress and display name can be set, the former turns into the DMX username |
| displayname_is_username | Only username name and email adress can be set, the former becomes the display name.  |

The policy is chosen by setting the configuration key
```
dmx.signup.username_policy = unconfined | username_is_email | displayname_is_username
```

to any of the possible values. The default value is ```unconfined```. 

### Account creation through LDAP plugin

The plugin can be configured to use the dmx-ldap plugin for account creation. When enabled the username and password are stored in LDAP instead of DMX itself. The option is disabled by default and is switched on by setting the property:

```
dmx.signup.ldap_account_creation = true
```

Keep in mind that the dmx-ldap plugin also has a configuration option that controls account creation functionality. This  must be enabled as well. Refer to the [plugins README](https://git.dmx.systems/dmx-plugins/dmx-ldap/-/blob/master/README.md) for more information on its configuration. 

### Dependency on LDAP plugin bind account usage

The dmx-ldap plugin can be configured to use a bind account or not. Many of the advanced functionalities that are needed
for the sign-up plugin (e.g. password change, account creation) require the bind (or manager) account.

If the bind account is not used account creation and password change will not work!

### Restricting authorization (login) methods and/or defining their order

The plugin allows restricting the allowed authorization methods by setting the property:
```
dmx.signup.restrict_auth_methods = LDAP, BASIC
```

The value is a comma-separated list of auth method values. Please note that the order of the restriction list is
preserved in the web frontend. As such the property can also be used to enforce a certain auth method order.

When the property is not set, all known auth methods are allowed and their order is defined by the platform.

### Configuring expected password complexity
It is possible to select the expected password complexity by setting the property:
```
dmx.signup.expected_password_complexity = complex
```

The possible values are "complex", "simple" or "none" with "complex" being the default if no value was set or the value
having been mistyped.

The complex password rules are:
 * minimum and maximum characters according to separate settings (see next section)
 * at least one lower-case letter
 * at least on upper-case letter
 * at least one digit
 * at least one special character
 * no whitespace
 * no simple sequences (eg. 123456, qwertz, ABCDEF etc.)

The simple password rules are:
* minimum and maximum characters according to separate settings (see next section)
* no whitespace

The password rules are enforced on account creation and password change.

### Configuring expected password length
When the expected password complexity is "complex" you can configure the expected password lengths as follows:

```
dmx.signup.expected_min_password_length = 8
dmx.signup.expected_max_password_length = 64
```

The values should be positive and the max value higher or equal than the min value. If no value is specified the
defaults are a minimum of 8 characters and a maximum of 64 characters. 

## Legacy migrations

Sign Up plugin 2.x contained configuration options stored in topics. Some of those options have become obsolete in
version 3.x and also the idea of storing such values in topics is not desired anymore. For this the following migration
path has been laid out.

Version 3.0.x moves all the existing configuration values into a topic destined for keeping legacy values. This topic
is then never touched at all and can be left or removed by the administrator. The topic type is for this is
```
  dmx.signup.legacy.configuration
```

Running version 3.1.x of the sign-up plugin will remove all configuration topics and topic types, including the legacy
ones.

Running version 3.2.x of the sign-up plugin will *not* have any migration code left and will never use configuration
option in topics anymore.

## License

DMX Sign-up is available freely under the GNU Affero General Public License, version 3 or later (see [License](https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/blob/master/LICENSE)).

## Version history
**3.2.0** -- TBD

* Password generation done in backend code
* Add expected password complexity option
* Add expected password length option
* Depend on dmx-ldap 0.8

**3.1.0** -- Feb, 10, 2024

* During migration removes legacy configuration topics and topic types
* During migration removes API workspace related configuration topics and topic types

**3.0.2** -- Feb, 10, 2024

* Last version to store configuration values in a legacy configuration topic
* Removed API workspace and custom workspace handling

**3.0.1** -- Dec, 20, 2023

* Removed unneeded Thymeleaf dependency

**3.0.0** -- Nov, 22, 2023

* Compatible with DMX 5.3.3
* Compatible with dmx-ldap 0.7.0
* All UI-related code moved into its own plugin
* Introduced username policies

**2.1.0** -- Jul, 14, 2023

* Compatible with DMX 5.3
* Account creation through LDAP plugin
* True optional dependency on `dmx-ldap-0.6.2+` plugin
* Configure workspace whose users can create new users
* Configure module configuration through property configuration
* Validity of account creation and password reset token configurable
* 
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
Copyright (c) 2020-2022 DMX Systems
