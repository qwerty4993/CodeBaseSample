/*
 * ZAP Groovy authentication script template
 */

// Import ZAP libraries as needed.
import org.parosproxy.paros.network.HttpMessage
import org.zaproxy.zap.authentication.AuthenticationHelper
import org.zaproxy.zap.authentication.GenericAuthenticationCredentials
import org.zaproxy.zap.extension.script.ScriptVars

/**
 * The ZAP authentication method provided by this external script.
 *
 * @param {*} helper A helper object that provides methods for building and sending HTTP messages.
 * @param {*} paramsValues The values of the parameters configured in the Session Properties - Authentication panel.
 *                         The paramsValues is a map, having as keys the parameters names (as returned by the
 *                         getRequiredParamsNames() and getOptionalParamsNames() functions).
 * @param {*} credentials An object containing the credentials values, as configured in the
 *                        Session Properties - Users panel. The credential values can be obtained
 *                        via calls to the getParam(paramName) method. The param names are the ones returned by
 *                        the getCredentialsParamsNames() below.
 *
 *
 * @return The HTTP message returned after sending the POST login request and following
 *         redirects to the authenticated landing page.
 */
@SuppressWarnings(["UnusedMethodParameter", "UnusedVariable"])
HttpMessage authenticate(AuthenticationHelper helper, Map<String, String> paramsValues,
                         GenericAuthenticationCredentials credentials) {
  String scriptName = "authentication_script_template.groovy"

  // "Import" shared libraries as needed.
  Class scriptLoggerClass = this.class.classLoader.loadClass("ScriptLogger")
  Class seleniumLoginUtilsClass = this.class.classLoader.loadClass("SeleniumLoginUtils")
  Class httpUtilsClass = this.class.classLoader.loadClass("HttpUtils")
  Class scriptUtilsClass = this.class.classLoader.loadClass("ScriptUtils")

  // Setup a logger instance for logging messages during the authentication process.
  def logger = scriptLoggerClass.newInstance(scriptName, this.getBinding().out)

  // Make sure the classpath has been set.
  if (!ScriptVars.getGlobalVar("classpathSet")) {
    // Print a message so there is an indication of the problem in zap.log
    String msg = """\
        ${scriptName}: Groovy classpath has not been set!
        Standalone script setup_classpath.groovy must be executed first to configure the classpath!
        """.stripIndent()
    logger.logMsg(msg)
    throw new RuntimeException(msg)
  }

  return scriptUtilsClass.executeScript(logger) {
    logger.logMsg("Starting authentication...")

    Map config = [
            indexURL: paramsValues.get("indexURL"),
            authURL: paramsValues.get("authURL"),
            cookieName: paramsValues.get("cookieName"),
            usernameField: paramsValues.get("usernameField"),
            passwordField: paramsValues.get("passwordField"),
            loginButtonField: paramsValues.get("loginButtonField"),
            idAfterLogin: paramsValues.get("idAfterLogin"),
            username: credentials.getParam("Username"),
            password: credentials.getParam("Password"),
    ]

    // Set optional parameters
    this.getOptionalParamsNames().each { param ->
      if (paramsValues.get(param)) {
        config.put(param, paramsValues.get(param))
      }
    }

    def seleniumLoginUtils = seleniumLoginUtilsClass.newInstance(config, helper, logger)
    HttpMessage lastHttpMessage = seleniumLoginUtils.login()

    // Return the final message in the process
    return lastHttpMessage
  }
}

/**
 * This function is called during the script loading to obtain a list of the names of the
 * required configuration parameters, that will be shown in the Session Properties -> Authentication panel
 * for configuration. They can be used to input dynamic data into the script, from the user interface
 * (e.g. a login URL, name of POST parameters etc.)
 *
 * @return A list containing all of the required parameter names for this script.
 */
String[] getRequiredParamsNames() {
  return ["indexURL", "authURL", "cookieName", "usernameField", "passwordField", "loginButtonField", "idAfterLogin"]
}

/**
 * Gets the names of the optional parameters for this script.
 * Used to populate optional parameters in the Session Properties -  Authentication panel.
 * If a script using this template requires no optional parameters, this should be updated to
 * return an empty list.
 *
 * @return A list containing all of the optional parameter names for this script.
 */
String[] getOptionalParamsNames() {
  return ["extraHeaders"]
}

/**
 * Gets a list of all the credential parameter names for this script.
 * Used to read authentication data (username/password etc.) from ZAP.
 * To work with solis_zap, keep these parameters as Username and Password.
 *
 * @return A list containing all of the credential parameter names for this script.
 */
String[] getCredentialsParamsNames() {
  return ["Username", "Password"]
}

/**
 * This optional function is called during the script loading to obtain the logged in indicator.
 * NOTE: This function is optional but if implemented, the function getLoggedOutIndicator() must
 * also be implemented.
 *
 * @return An indicator string present in the application's login response body that indicates that
 *         a user is logged in.
 */
/*
 String getLoggedInIndicator() {
 return "LoggedInIndicator"
 }
 */

/**
 * This optional function is called during the script loading to obtain the logged out indicator.
 * NOTE: This function is optional but if implemented, the function getLoggedInIndicator() must
 * also be implemented.
 *
 * @return An indicator string present in the application's login response body that indicates that
 *         a user is logged out.
 */
/*
 String getLoggedOutIndicator() {
 return "LoggedOutIndicator"
 }
 */

import org.apache.commons.httpclient.URI

import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement

import org.parosproxy.paros.network.HttpMessage
import org.parosproxy.paros.network.HttpSender
import org.zaproxy.zap.authentication.AuthenticationHelper

import Authentication
import CookieJar
import HttpUtils
import ScriptLogger
import ScriptUtils
import SeleniumUtils

/**
 * Class for interacting with applications that utilize Cisco Single Sign On.
 */
class SeleniumLoginUtils implements Authentication {
  /**
   * The list of required configuration keys for the SeleniumLoginUtils constructor.
   */
  static final List REQUIRED_CONFIG_KEYS = ["indexURL", "authURL", "cookieName", "usernameField", "passwordField", "loginButtonField", "username", "password", "idAfterLogin",]

  /**The Cisco CEC login username.*/
  String username
  /**The Cisco CEC login password.*/
  String password
  /**The authenticated landing page of the application.*/
  String indexURL
  /**The URL of the application's login page.*/
  String authURL
  /**The name of the cookie used to track the session.*/
  String cookieName
  /**The value of the name attribute field of the username input*/
  String usernameField
  /**The value of the name attribute field of the password input*/
  String passwordField
  /**The value of the name attribute for the login button*/
  String loginButtonField
  /**The helper object to use to create and send HTTP messages.*/
  def helper
  /**A ScriptLogger instance to use for logging.*/
  ScriptLogger logger
  /**A CookieJar instance to use for managing cookies.*/
  CookieJar cookieJar
  /**A map of extra headers to append/replace during the login process.*/
  Map extraHeaders
  /**The login message to return to Zap*/
  HttpMessage loginGetMsg
  /**Whether or not the login process succeeded.*/
  boolean loginSucceeded
  /**Whether or not an Exception was encountered*/
  boolean exceptionEncountered
  /**html id to look for on page after login*/
  String idAfterLogin

  /**
   * Creates a new SeleniumLoginUtils instance.
   *
   * @param config A map containing the configuration items for this instance.
   *               Must contain all of the keys listed in REQUIRED_CONFIG_KEYS.
   * @param helper A helper object to use for creating and sending HTTP messages.
   * @param logger A ScriptLogger instance to use for logging.
   */
  SeleniumLoginUtils(Map config, helper, ScriptLogger logger) {
    // Ensure provided config is valid.
    ScriptUtils.validateScriptArgs(REQUIRED_CONFIG_KEYS, config)

    // Pull configuration items from config.
    this.indexURL = HttpUtils.sanitizeURL(config.indexURL)
    this.authURL = HttpUtils.sanitizeURL(config.authURL)
    this.usernameField = config.usernameField
    this.passwordField = config.passwordField
    this.loginButtonField = config.loginButtonField
    this.cookieName = config.cookieName
    this.idAfterLogin = config.idAfterLogin
    this.username = config.username
    this.password = config.password

    // Optional arguments.
    // If not present in config, values will be null.
    this.extraHeaders = config.extraHeaders

    this.helper = helper
    this.logger = logger
    this.cookieJar = null
    this.loginGetMsg = null
    this.loginSucceeded = false
    this.exceptionEncountered = false
  }

  /**
   * Performs the login process for Cisco SSO.
   *
   * @return The final HTTP message in the login process.
   */
  @Override
  HttpMessage login() {
    // Reset instance vars in case login was performed previously.
    this.loginGetMsg = null
    this.loginSucceeded = false
    this.exceptionEncountered = false

    // Create a CookieJar to store and manage cookies during the login process.
    this.cookieJar = new CookieJar()

    // Perform login with Selenium
    Set<org.openqa.selenium.Cookie> cookieSet = this.seleniumLogin()

    if (this.exceptionEncountered) {
      this.logger.logMsg("Login appeared to fail. Cause: An Exception occurred in the WebDriver!")
    } else if (!cookieSet) {
      this.logger.logMsg("Login appeared to fail. Cause: No Cookies were found!")
    } else {
      // Inject the cookies into Zap's HttpState
      URI targetUri = new URI(URLDecoder.decode(this.indexURL), false)
      cookieSet.each {
        this.cookieJar.injectIntoHttpState(this.helper, targetUri, this.cookieJar.toHttpCookie(it))
      }
      if (helper instanceof HttpSender) {
        logger.logMsg(this.cookieJar.getCookieDetails(authCookie))
      } else {
        logger.logMsg(this.cookieJar.getHttpStateDetails(this.helper.getCorrespondingHttpState()))
      }

      this.loginSucceeded = true
    }

    this.loginGetMsg = HttpUtils.get(this.indexURL, this.helper, true, this.logger, this.cookieJar, this.extraHeaders)
    AuthenticationHelper.addAuthMessageToHistory(this.loginGetMsg)
    if (this.loginSuccessful()) {
      AuthenticationHelper.notifyOutputAuthSuccessful(this.loginGetMsg)
    } else {
      AuthenticationHelper.notifyOutputAuthFailure(this.loginGetMsg)
    }

    return loginGetMsg
  }

  /**
   * Determines if the Cisco SSO login process was successful.
   *
   * @return True if the login process succeeded. False otherwise.
   */
  @Override
  boolean loginSuccessful() {
    return this.loginSucceeded
  }

  /**
   * Use Selenium to log into the application
   *
   * @return A set of <org.openqa.selenium.Cookie> cookies returned after successfully logging in.
   */
  private Set<org.openqa.selenium.Cookie> seleniumLogin() {
    // Set of cookies
    Set<org.openqa.selenium.Cookie> cookieSet

    // Get a WebDriver
    WebDriver loginDriver = SeleniumUtils.getWebDriver()

    // Get an instance of WebDriverWait
    WebDriverWait wait = SeleniumUtils.getWebDriverWait(loginDriver)
Thread.sleep(2000);

    try {
      // Get the authURL with Selenium
 logger.logMsg("Waiting for Username, Password, and Submit button...")
      loginDriver.get(this.authURL)

      // The below code is an example of a simple form based login using Selenium.
      // This code may need to be altered to fit the target being logged into.

      // Wait for the login page to load
      String userSelect = "input#username"
    
      

      logger.logMsg("Waiting for Username, Password, and Submit button...")
      SeleniumUtils.waitUntilElementVisible(wait, userSelect)
      
      

      // Get the username and password field elements and input the credentials
      WebElement usernameTextField = SeleniumUtils.findWebElement(loginDriver, userSelect)
      
      logger.logMsg("Entering username...")
      SeleniumUtils.sendKeysToElement(usernameTextField, 'charankm@cisco.com')

      
     String nextButton = "button.camp-button.camp-primary.login-btn"
SeleniumUtils.waitUntilElementVisible(wait, nextButton)
    WebElement submitButton = SeleniumUtils.findWebElement(loginDriver, nextButton)

      logger.logMsg("Clicking login button...")
      SeleniumUtils.clickElement(submitButton)
Thread.sleep(5000)
     String passwordSelect = "input#password"
SeleniumUtils.waitUntilElementVisible(wait, passwordSelect)

  WebElement passwordTextField = SeleniumUtils.findWebElement(loginDriver, passwordSelect)
logger.logMsg("Entering password...")
SeleniumUtils.sendKeysToElement(passwordTextField, 'Core#2233')

String signinButton="button#login"
SeleniumUtils.waitUntilElementVisible(wait, signinButton)

      WebElement signinButtonClick= SeleniumUtils.findWebElement(loginDriver, signinButton)
    logger.logMsg("Clicking signin button...")
      SeleniumUtils.clickElement(signinButtonClick)

Thread.sleep(5000)
String finalscuccess=".camp-header-container"
      //logger.logMsg("Waiting for provided id '${this.idAfterLogin}' to appear on page...")
      SeleniumUtils.waitUntilElementVisible(wait, finalscuccess)
 WebElement finalscuccessPage= SeleniumUtils.findWebElement(loginDriver, finalscuccess)

      
 logger.logMsg("Clicking to appear on page ...${finalscuccessPage.isDisplayed()}")
cookieSet = SeleniumUtils.getCookies(loginDriver)
      // Validates that the named login cookie exists
      Cookie seleniumCookie = SeleniumUtils.getCookie(loginDriver, this.cookieName)
      if (seleniumCookie) {
        logger.logMsg("Expected cookie '" + this.cookieName + "' found!")
        logger.logMsg("Cookie Type: ${seleniumCookie.getClass()}")
      }
    } catch (WebDriverException wde) {
      logger.logException(wde)
      this.exceptionEncountered = true
    } catch (IllegalArgumentException iae) {
      logger.logException(iae)
      this.exceptionEncountered = true
    } finally {
      //SeleniumUtils.closeWebDriver(loginDriver)
    }

    return cookieSet
  }
}
