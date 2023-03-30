
    var EMPTY_STRING = ""
    var OK_STRING = "OK"
    var inputInvalidated = false

    // Sign-up Configuration Object Initialized via Thymeleaf
    var signupConfig = {
        "lastAuthorizationMethod": "Basic",
        "authorizationMethods": ["Basic"],
        "customWorkspaceEnabled" : false,
        "customWorkspaceURI" : "",
        "appLoadingMessage" : "Loading Webclient",
        "appLoggingOutHint" : "Logging out...",
        "appStartPageURL"   : "/",
        "appHomePageURL"    : "/",
        "passwordLength"    : "Your password must be at least 8 characters long.",
        "passwordMatch"     : "Your passwords do not match.",
        "checkTerms"        : "First, please check our terms and conditions.",
        "usernameInvalid"   : "This username would be invalid.",
        "usernameTaken"     : "This username is already taken.",
        "emailInvalid"      : "This E-Mail Address would be invalid.",
        "emailTaken"        : "This E-Mail address is already registered.",
        "notAuthorized"     : "You're not authorized. Sorry."
    }


    // --- Plain DMX login method used by "/sign-up/login" page. --- //

    function doLogout() {
        xhr = new XMLHttpRequest()
        xhr.onload = function(e) {
            if (xhr.response === "") {
                renderFriendlyMessage(signupConfig.appLoggingOutHint)
                window.document.location.assign(signupConfig.appHomePageURL)
            } else {
                renderWarning(xhr.response)
            }
        }
        xhr.open("POST", "/access-control/logout", false)
        xhr.send()
    }

    function doLogin(username, pass, success) {

        var id = (typeof username === "undefined") ? document.getElementById("username").value : username
        // when ldap account creation is configured, we make the user-id is always authorized in lower-case
        if (signupConfig["accountCreationMethodIsLdap"]) {
          id = id.toLowerCase()
        }
        var secret = (typeof pass === "undefined") ? document.getElementById("password").value : pass

        checkAuthorization(id, secret)

        function checkAuthorization(id, secret) {

            var authorization = authorization()
            if (authorization === undefined) return null

            xhr = new XMLHttpRequest()
            xhr.onload = function(e) {
                  if (xhr.response === "") { // login success
                      if (typeof success !== "undefined") {
                        console.log("Login successfull triggering callback", success)
                        success()
                      } else {
                        console.log("Login successfull standard redirect")
                        renderFriendlyMessage(signupConfig.appLoadingMessage)
                        redirectToStartPageURL()
                      }
                  } else {
                      console.log("Login unsuccessfull", xhr.response)
                      renderWarning(signupConfig.notAuthorized)
                  }
            }
            xhr.open("POST", "/access-control/login", false)
            xhr.setRequestHeader("Authorization", authorization)
            xhr.send()

            /** Returns value for the "Authorization" header. */
            function authorization() {
                try {
                    var selectElement = document.getElementById("auth_method");
                    var authMethod = if (selectElement != null) selectElement.options[selectElement.selectedIndex].value ? signupConfig["lastAuthorizationMethod"]

                    document.cookie = "last_authorization_method=" + authMethod;

                    // See https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/btoa
                    return authMethod + " " + window.btoa(id + ":" + secret) // IE >= 10 compatible
                } catch (error) {
                    console.error("Error encoding Auth-Header", error)
                }
            }
        }

    }


    // --- Plain JavaScript form

    // Assigns username to a Note topic residing in System workspace, if apiWorkspaceUri is set
    function doCheckCustomWorkspaceAggrement() {
        if (signupConfig.apiWorkspaceURI !== "") {
            // console.log("Custom Workspace URI", signupConfig.customWorkspaceURI)
            xhr = new XMLHttpRequest()
            xhr.open("POST", "/sign-up/confirm/membership/custom", false) // Synchronous request
            xhr.send()
        }
    }

    function saveAccountEdits() {
        console.log("Todo: Save edits to account information")
    }

    // --- Plain JavaScript form validation used by "/sign-up/" page. --- //

    // ### empty passwords and crazy mailbox-domains.
    // This is the form.onsubmit() implementation.
    function createAccount() {

        function doCreateRequest() {
            var usernameVal = encodeURIComponent(document.getElementById("username").value)
            var mailbox = encodeURIComponent(document.getElementById("mailbox").value)
            var skipField = document.getElementById("skip-confirmation")
            var skipConfirmation = ""
            if (skipField && skipField.value === "on") {
                skipConfirmation = "/true"
            }

            // With Basic auth hash the password with SHA256 with ldap only "btoa" it
            var passwordVal = encodeURIComponent(
                        		signupConfig["accountCreationMethodIsLdap"] ?
                        		 window.btoa(document.getElementById("pass-one").value) :
                                 '-SHA256-' + SHA256(document.getElementById("pass-one").value))

            // employing the w3school way to go to GET the sign-up resource
            window.document.location.assign("//" +  window.location.host + "/sign-up/handle/" + usernameVal + "/"
               + passwordVal +"/" + mailbox + skipConfirmation)
        }
        // any of these should prevent submission of form
        if (!isValidUsername()) return false
        if (checkPassword() !== OK_STRING) return false
        if (comparePasswords() !== OK_STRING) return false
        if (checkMailbox() === null) return false
        if (checkAgreements() !== OK_STRING) return false
        if (inputInvalidated) {
            checkUserNameAvailability(function(response) {
                if (response) {
                    checkMailboxAvailability(function(response) {
                        if (response) doCreateRequest()
                    })
                }
            })
        } else {
            doCreateRequest()
        }
    }

    // This is the form.onsubmit() implementation.
    function createCustomAccount() {

        function getPassword() {
            return signupConfig["isAccountCreationPasswordEditable"]
                ? document.getElementById("pass-one").value
                : generateRandomString(23)
        }

        function doCreateRequest() {
            var mailbox = encodeURIComponent(document.getElementById("username").value)
            // when sign-up creates ldap accounts, make sure, these are always in lower-case
            if (signupConfig["accountCreationMethodIsLdap"]) {
              mailbox = mailbox.toLowerCase()
            }
            var displayName = encodeURIComponent(document.getElementById("displayname").value)
            var passwordVal = encodeURIComponent(signupConfig["accountCreationMethodIsLdap"] ?
                      window.btoa(getPassword()) : '-SHA256-' + SHA256(getPassword()))
            // send a GET to handle the account creation request (SignupPlugin.java@handleCustomSignupRequest)
            window.document.location.assign("//" +  window.location.host + "/sign-up/custom-handle/"
                    + mailbox + "/" + displayName + "/" + passwordVal)
        }
        // any of these should prevent submission of form
        if (!isValidMailboxUsername()) return false
        doCreateRequest()
    }

    function generateRandomString(length) {
        var characterSeed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789!?#-_abcdefghijklmnopqrstuvxyz";
        var value = "";
        for (var i = 0; i < length; i++) {
            // generate a random number between 0 and seed length
            var index = Math.round((characterSeed.length * Math.random()));
            value = value + "" +  characterSeed[index];
        }
        return value;
    }

    function isValidMailboxUsername() {
        var mailboxField = document.getElementById("username") // fixme: maybe its better to acces the form element
        if (mailboxField.value.indexOf("@") === -1 || mailboxField.value.indexOf(".") === -1) {
            renderWarning(signupConfig.emailInvalid)
            disableSignupForm()
            return null
        }
        enableSignupForm()
        renderWarning(EMPTY_STRING)
        return OK_STRING
    }

    function isValidUsername() {
        var usernameInput = document.getElementById("username") // fixme: maybe its better to acces the form element
        var userInput = usernameInput.value
        if (userInput.length <= 1) {
            renderWarning(signupConfig.usernameInvalid)
            disableSignupForm()
            return false
        }
        enableSignupForm()
        return true
    }

    function checkUserNameAvailability(handler) {
        var usernameInput = document.getElementById("username") // fixme: maybe its better to acces the form element
        var userInput = usernameInput.value
        xhr = new XMLHttpRequest()
        if (userInput) {
            xhr.onload = function(e) {
                var response = JSON.parse(xhr.response)
                if (!response.isAvailable) {
                    renderWarning(signupConfig.emailTaken)
                    disableSignupForm()
                    inputInvalidated = true
                    if (handler) handler(false)
                } else {
                    enableSignupForm()
                    renderWarning(EMPTY_STRING)
                    if (handler) handler(true)
                }
            }
            xhr.open("GET", "/sign-up/check/" + userInput, true) // Asynchronous request
            xhr.send()   
        }
    }
    
    function checkMailboxAvailability(handler) {
        var mailboxField = document.getElementById("mailbox") // fixme: maybe its better to acces the form element
        var mailBox = mailboxField.value
        if (mailBox) {
            xhr = new XMLHttpRequest()
            xhr.onload = function(e) {
                var response = JSON.parse(xhr.responseText)
                if (!response.isAvailable) {
                    renderWarning(signupConfig.emailTaken)
                    disableSignupForm()
                    inputInvalidated = true
                    if (handler) handler(false)
                } else {
                    enableSignupForm()
                    renderWarning(EMPTY_STRING)
                    if (handler) handler(true)
                }
            }
            xhr.open("GET", "/sign-up/check/mailbox/" + mailBox, true) // Asynchronous request
            xhr.send()   
        }
    }

    function checkPassword() {
        var passwordField = document.getElementById("pass-one") // fixme: maybe its better to acces the form element
        if (passwordField.value.length <=7) {
            renderWarning(signupConfig.passwordLength)
            disableSignupForm()
            return null
        }
        enableSignupForm()
        renderWarning(EMPTY_STRING)
        return OK_STRING
    }

    function checkMailbox() {
        var mailboxField = document.getElementById("mailbox") // fixme: maybe its better to acces the form element
        if (mailboxField.value.indexOf("@") === -1 || mailboxField.value.indexOf(".") === -1) {
            renderWarning(signupConfig.emailInvalid)
            disableSignupForm()
            return null
        }
        enableSignupForm()
        renderWarning(EMPTY_STRING)
        return OK_STRING
    }

    function comparePasswords() {
        var passwordFieldTwo = document.getElementById("pass-one") // fixme: maybe its better to acces the form element
        var passwordFieldOne = document.getElementById("pass-two") // fixme: maybe its better to acces the form element
        if (passwordFieldOne.value !== passwordFieldTwo.value) {
            renderWarning(signupConfig.passwordMatch)
            disableSignupForm()
            return null
        }
        enableSignupForm()
        renderWarning(EMPTY_STRING)
        checkPassword()
        return OK_STRING
    }

    function checkAgreements() {
        var tosCheck = document.getElementById("toscheck").checked
        var privateOk = document.getElementById("privateinfo").checked
        //
        if (tosCheck && privateOk) {
            renderWarning(EMPTY_STRING)
            enableSignupForm()
            return OK_STRING
        }
        renderWarning(signupConfig.checkTerms)
        disableSignupForm()
        return null
    }

    function resetPassword() {
        // var name = document.getElementById("name").value.trim()
        var emailAddress = document.getElementById("mailbox").value.trim()
        xhr = new XMLHttpRequest()
        xhr.onload = function(e) {
            console.log("Loaded Password Reset Response", e)
        }
        xhr.open("GET", "/sign-up/password-token/" + emailAddress, true) // Asynchronous request
        xhr.send()
        redirectToTokenInfoPage()
    }

    function updatePassword() {
        comparePasswords()
        var token = document.getElementById("token-info").value
        var username = document.getElementById("username").value
        var pwInput = document.getElementById("pass-one").value
        var secret = encodeURIComponent(signupConfig["accountCreationMethodIsLdap"] ?
                         window.btoa(pwInput) : '-SHA256-' + SHA256(pwInput))
        // a) Form-based way
        // window.document.location.replace("/sign-up/password-reset/" + token + "/" + secret)
        // b) Custom ajax-way 
        xhr = new XMLHttpRequest()
        xhr.onload = function(e) {
            console.log("Updated Password for ", username, "start auto-login sequence in 1sec")
            setTimeout(function (e) {
              doLogin(username, pwInput, function(e) {
                console.log("Autologin successfull", signupConfig.redirectUrl, signupConfig.appStartPageURL)
                // if (!signupConfig.redirectUrl) {     // commented by jri 2022/07/26
                renderFriendlyMessage(signupConfig.appLoadingMessage)
                redirectToStartPageURL()
                // } else {
                //   console.log("Redirect URL", signupConfig.redirectUrl)
                // }
              }, 1000)
            })
        }
        xhr.open("GET", "/sign-up/password-reset/" + token + "/" + secret, true)
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send()
    }

    function voidFunction() {
        // a custom void(); return false; }
    }

    function showCustomWorkspaceTermsText() {
        var textArea = document.getElementById('api-info')
            textArea.setAttribute("style", "display: block;")
    }

    function showLabsPrivateText() {
        var textArea = document.getElementById('private-details')
            textArea.setAttribute("style", "display: block;")
    }

    function showLabsTermsText() {
        var textArea = document.getElementById('tos-details')
            textArea.setAttribute("style", "display: block;")
    }

    function renderWarning(message) {
        var textNode = document.createTextNode(message)
        var messageElement = document.getElementById('message')
        while(messageElement.hasChildNodes()) {
            // looping over lastChild thx to http://stackoverflow.com/questions/5402525/remove-all-child-nodes
            messageElement.removeChild(messageElement.lastChild);
        }
        messageElement.appendChild(textNode)
    }

    function renderFriendlyMessage(message) {
        var textNode = document.createTextNode(message)
        var messageElement = document.getElementById('message-view')
        if (messageElement !== null) {
            while(messageElement.hasChildNodes()) {
                // looping over lastChild thx to http://stackoverflow.com/questions/5402525/remove-all-child-nodes
                messageElement.removeChild(messageElement.lastChild);
            }
            messageElement.appendChild(textNode)
        }
    }

    function disableSignupForm() {
        document.getElementById("create").setAttribute("disabled", "true")
        document.getElementById("create").setAttribute("style", "background-color: #a9a9a9;")
    }

    function enableSignupForm() {
        document.getElementById("create").removeAttribute("disabled")
        document.getElementById("create").removeAttribute("style")
    }

    function redirectToStartPageURL() {
        setTimeout(function (e) {
            window.location.href = signupConfig.appStartPageURL
        }, 1500)
    }

    function redirectToTokenInfoPage() {
        setTimeout(function (e) {
            window.location.replace("/sign-up/token-info")
        }, 500)
    }
