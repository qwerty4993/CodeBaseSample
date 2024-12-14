/*
 * ZAP authentication script for DVWA 1.9.
 */

// Import ZAP libraries.

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
 *         redirects to the webacs index.
 */
HttpMessage authenticate(AuthenticationHelper helper, Map<String, String> paramsValues,
                         GenericAuthenticationCredentials credentials) {
  String scriptName = "dvwa_login.groovy"


  // Make sure the classpath has been set.
  if (!ScriptVars.getGlobalVar("classpathSet")) {
    String msg = """\
        ${scriptName}: Groovy classpath has not been set!
        Standalone script setup_classpath.groovy must be executed first to configure the classpath!
        """.stripIndent()
    println(msg)
    throw new RuntimeException(msg)
  }

  // "Import" shared libraries.
  Class scriptLoggerClass = this.class.classLoader.loadClass("ScriptLogger")
  Class scriptUtilsClass = this.class.classLoader.loadClass("ScriptUtils")
  Class formUtilsClass = this.class.classLoader.loadClass("FormUtils")
  Class httpUtilsClass = this.class.classLoader.loadClass("HttpUtils")
  Class cookieJar = this.class.classLoader.loadClass("CookieJar")


  def help=helper
  def logger = scriptLoggerClass.newInstance(scriptName, this.getBinding().out)

  return scriptUtilsClass.executeScript(logger) {
    logger.logMsg("Starting authentication...")

    String baseURL = httpUtilsClass.sanitizeURL(paramsValues.get("dvwaURL"))

    // Build the POST parameters map
    Map<String, String> postData = [
            username: credentials.getParam("Username"),   // Username from credentials
            password:credentials.getParam("Password")
    ]

    // Add any optional parameters if provided
    this.getOptionalParamsNames().each { param ->
      if (paramsValues.get(param)) {
        postData.put(param, paramsValues.get(param))
      }
    }
    
   def postDataStr= buildpostdata();
    // Create the login URL
    String authURL = "${baseURL}"
    logMsg("Authenticating to: ${authURL}");
    logMsg("POST data: ${postData}");
    logMsg("final POST data: ${postDataStr.toString()}");
    // Set up the HTTP message with the required parameters
    HttpMessage loginPostMsg = HttpUtils.post(authURL, help ,postDataStr.toString(), false, logger, null, null)
    // Return the HTTP message
    String responseBody = loginPostMsg.getResponseBody().toString()
    help.sendAndReceive(loginPostMsg, true);
    logMsg("final POST data: ${responseBody}");
    logMsg("DVWA login appeared to succeed due to response code  .");
    AuthenticationHelper.notifyOutputAuthSuccessful(loginPostMsg);

    logMsg("final getHttpCookies data: ${loginPostMsg.getResponseHeader().getHttpCookies()}");

    AuthenticationHelper.addAuthMessageToHistory(loginPostMsg);
    new CookieJar().updateCookies(loginPostMsg)

    return loginPostMsg
  }
}

// This function is called during the script loading to obtain a list of the names of the
// required configuration parameters, that will be shown in the Session Properties -> Authentication panel
// for configuration. They can be used to input dynamic data into the script, from the user interface
// (e.g. a login URL, name of POST parameters etc.)
String[] getRequiredParamsNames() {
  return ["dvwaURL"]
}

/**
 * Gets the names of the optional parameters for this script.
 * Used to populate optional parameters in the Session Properties -  Authentication panel.
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
 * Prints a log message.
 *
 * @param msg The message to log.
 */
def logMsg(msg) {
  println "dvwa_login.groovy: " + msg
}

def isError(statusCode) {
  if (statusCode >= 400) {
    return true;
  }
  return false;
}

def buildpostdata(postData){
StringBuilder postDataStr = new StringBuilder()
    postData.each { key, value ->
      if (postDataStr.length() > 0) {
        postDataStr.append("&")
      }
      postDataStr.append(key)
      postDataStr.append("=")
      postDataStr.append(value)
    }
return postDataStr;
}
