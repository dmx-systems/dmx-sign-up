
# DeepaMehta 4 Sign-up

This plugin introduces a configurable user registration process for DeepaMehta 4 _User Accounts_.

Most (if not all) user facing message are translatable by now. A german language version of this bundle can be build from source after setting the language option to `de` using the `plugin.properties` file.

This plugin adds:
*   A "User Mailbox" association type to associate "Email Address" w. "User Accounts".
*   A `Sign-up`-link next to the `Login`-button in the DeepaMehta 4 Webclient
*   A `Sign-up Configuration` topic associated to the `DeepaMehta 4 Sign up` Plugin
     (part of the "System" workspace and thus editable by all members of it)

The special features of the **registration ui** is comprised of:
*   Username existence check
*   Email existence check
*   Simple GUI-Notification mechanism
*   Minimal CSS Definition

The special features of the **login ui** is comprised of:
*   Simple GUI-Notification mechanism
*   Automatic redirect
*   Minimal CSS Definition

The **special logic** of this plugin is comprised of:
*   Optionally: Configure a sign-up process with an Email based confirmation workflow<br/>
    Sends confirmation mail with token to the users registering Email address<br/>
    Allows for the password reset functionality to take place also via an Email based confirmation workflow
*   Optionally: Send notifications to system administrator after a new user account was sucessfully created
*   Optionally: If `new_accounts_are_enabled=true`, an account activation notice is sent
*   Optionally: If the email based confirmation workflow is used a "Passwort reset" workflow is available

**Note:** If `Email Confirmation Required` is set to _true_ the confirmation tokens the system sends out are **not persisted** and get lost after a bundle/system restart. Once a token was send out the link containing it is valid for sixty minutes.

Each "Sign-up Plugin Configuration" topic allows you to control 13 settings to adapt the sign-up process to your look & needs.

The standard sign-up configuration topic initially resides in the "Administration" workspace and the "Custom Workspace Membership Request" note resides in the "System" workspace.

Email addresses of new user accounts are all placed in the "System" workspace too.

## Requirements

DeepaMehta 4 is a platform for collaboration and knowledge management.
https://github.com/jri/deepamehta

To be able to install this module you first and additionally have to install the following DeepaMehta 4 Plugins.

*    `dm48-thymeleaf-0.6.1`-Bundle - Build from [sources](https://github.com/jri/dm4-thymeleaf)

**Operations:** For the plugins mailbox validation process to run you must install this plugin with deepamehta4 on a web server with a 
`postfix` -> `Internet Site` like mail send functionality.

## Download & Installation

You can find the latest stable version of this plugin bundled for download at [http://download.deepamehta.de/](http://download.deepamehta.de/).

As mentioned above, you currently need to download and install the aditonally required `dm48-thymeleaf-0.6
.1`-Bundle, too.

After downloading the two bundle-files, place them in the `bundle-deploy` folder of your DeepaMehta installation and restart DeepaMehta 4.

## Plugin Configuration

The central topic for configuring the sign-up dialog for your DeepaMehta 4 installation is of type `Sign-up Configuration`. Editing this topic via your dm4-webclient allows you to interactively control/adapt many options.

Setting a configuration option to an empty value usually means deactivating the features depending on it.

The sign-up configuration is associated with the "Plugin" topic representing this plugin ("DeepaMehta 4 Sign up"). It can be edited by all members of the `Administration` workspace.

### Setup Custom Workspace Assignment

There is currently just one special configuration option. To setup a workspace which self-registering users should automatically join (become members of) you need to associate that very workspace topic with your active sign-up configuration. And here comes the pitfall: To take this "custom workspace assignment" into effect you must either restart the platform or press "Edit" on your sign-up configuration topic once (in both cases the configuration is reloaded and therewith comes into immediate effect, see [#1](https://github.com/mukil/dm4-sign-up/issues/1)).

## Licensed under the GPL License 3.0

GPL v3 - https://www.gnu.org/licenses/gpl.html

## Version history

**1.7.0** -- UPCOMING

* Integrated new tendu configuration options
* Integrated dm4-ldap module (optional)
* ...

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

-------------------------------
Author: Malte Reißig, 2013-2016

