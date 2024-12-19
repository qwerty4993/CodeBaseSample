import org.parosproxy.paros.network.HttpMessage
import org.zaproxy.zap.authentication.AuthenticationHelper
import org.zaproxy.zap.authentication.GenericAuthenticationCredentials
import org.zaproxy.zap.extension.script.ScriptVars
import org.apache.commons.httpclient.URI;
import CookieJar;
import org.parosproxy.paros.network.HttpSender
import java.net.HttpCookie


/**
 * The ZAP authentication method provided by this external script.
 *
 * @param {*} helper A helper object that provides methods for building and sending HTTP messages.
 * @param {*} paramsValues The values of the parameters configured in the Session Properties - Authentication panel.
 *                         The paramsValues is a map, having as keys the parameters names (as returned by the
 *                         getRequiredParamsNames() and getOptionalParamsNames() functions).
 * @param {*} credentials An object containing the credentials values, as configured in the
 *                        Session Properties - Users panel. The credential values are the ones returned by
 *                        the getParam(paramName) method. The param names are the ones returned by
 *                        getCredentialsParamsNames() below.
 *
 * @return The HTTP message returned after sending the POST login request and following
 *         redirects to the webacs index.
 */




HttpMessage authenticate(AuthenticationHelper helper, Map<String, String> paramsValues,
                         GenericAuthenticationCredentials credentials) {
    String scriptName = "authentication_script_template_second_request.groovy"

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
    //Class cookieJar = this.class.classLoader.loadClass("CookieJar")



    def help=helper
    def logger = scriptLoggerClass.newInstance(scriptName, this.getBinding().out)

    return scriptUtilsClass.executeScript(logger) {
        logger.logMsg("Starting authentication...")

boolean isAuthenticated = paramsValues.get("sample")?.toBoolean() ?: false

if(!isAuthenticated){
  String baseURL1 = httpUtilsClass.sanitizeURL(paramsValues.get("authurl"))

String token=  paramsValues.get("XSRF-TOKEN1");
  logMsg("token to \n: ${token}")
String apiEndpointq = paramsValues.get("apiEndpoint") 
    String apiUrl1 = baseURL1+"${apiEndpointq}"
 Map<String, String> extraHeaders1 = [:]
extraHeaders1.put("xsrf-token",token)
extraHeaders1.put("Cookie",paramsValues.get("cookies-new"))

 HttpMessage apiRequestMsg = HttpUtils.post(apiUrl1, helper, "method=getpermissions&input=", false, logger, null, extraHeaders1)
AuthenticationHelper.notifyOutputAuthSuccessful(apiRequestMsg)
return apiRequestMsg
}


        CookieJar cookieJar = new CookieJar()

        String baseURL = httpUtilsClass.sanitizeURL(paramsValues.get("authurl"))

        // Build the POST parameters map
        Map<String, String> postData = [
                username: credentials.getParam("Username"),     // Username from credentials
                password: credentials.getParam("Password"),
                key1: paramsValues.get("key1"),                 // Key1 from credentials
                key2: paramsValues.get("key2"),                 // Key2 from credentials
                key3: paramsValues.get("key3"),                 // Key3 from credentials
                input: ''                                        // Assuming input is required
        ]

        // Add any optional parameters if provided
        this.getOptionalParamsNames().each { param ->
            if (paramsValues.get(param)) {
                postData.put(param, paramsValues.get(param))
            }
        }

        def postDataStr = buildPostData(postData)

        // Create the login URL
        String authURL = "${baseURL}/loginservlet"
        logMsg("Authenticating to: ${authURL}")
        logMsg("POST data: ${postData}")
        logMsg("final POST data: ${postDataStr.toString()}")

        // Set up the HTTP message with the required parameters
        HttpMessage loginPostMsg = HttpUtils.post(authURL, help, postDataStr.toString(), false, logger, null, null)

        // Send the request and get the response
        String responseBody = loginPostMsg.getResponseBody().toString()
        //cookieJar.updateCookies(loginPostMsg)
        List<String> setCookieHeaders = loginPostMsg.getResponseHeader().getHeaders('Set-Cookie')
        // Extract cookies from the response


        logMsg("DVWA login appeared to succeed due to response code: ${setCookieHeaders}")
        // If the login was successful, send the API request using the cookies
        if (!isError(loginPostMsg.getResponseHeader().getStatusCode())) {
            // Dynamically get the API endpoint from the parameters
            String apiEndpoint = paramsValues.get("apiEndpoint")  // Get API endpoint from parameters
            if (!apiEndpoint) {
                logMsg("No API endpoint provided in the parameters. Aborting API request.")
                return loginPostMsg
            }

            // Construct the full API URL
            String apiUrl = "${baseURL}${apiEndpoint}"
            Map<String, HttpCookie> cookies = addCookiesFromResponse(loginPostMsg)

// Build the extra headers map
            Map<String, String> extraHeaders = [:]
            cookies.each { name, cookie ->
                extraHeaders.put("Cookie", "${name}=${cookie.getValue()}")

            }
            extraHeaders.put("xsrf-token","")

            // cookieJar.injectIntoHttpState(helper,apiUrl,httpCookie)
            logMsg("Sending API request to: ${apiUrl}")

            // Create the API request message (adjust method as needed, e.g., GET or POST)
            HttpMessage apiRequestMsg = HttpUtils.post(apiUrl, helper, "method=getpermissions&input=", false, logger, null, extraHeaders)
            // Send the API request
            //help.sendAndReceive(apiRequestMsg, true)
            String XSRFTOKEN =apiRequestMsg.getResponseHeader().getHeader('xsrf-token');
 //Map<String, HttpCookie> cookies2 = addCookiesFromResponse(apiRequestMsg)
           
            setCsrfTokenVars(XSRFTOKEN)
             paramsValues.put("sample","False")
paramsValues.put("XSRF-TOKEN1", XSRFTOKEN)
              
cookies.each { name, cookie ->
logMsg("API name test: ${name}")
    if (name == "JSESSIONID") {
 
        paramsValues.put("cookies-new", "${name}=${cookie.getValue()}")
    }
}
AuthenticationHelper.notifyOutputAuthSuccessful(loginPostMsg)
            return apiRequestMsg
        }
        //help.sendAndReceive(loginPostMsg, true)
        
        logMsg("Authentication failed. Not sending API request.")
        return loginPostMsg
    }
}

private String buildCookiesHeader(List<HttpCookie> cookies) {
    if (cookies.isEmpty()) {
        logMsg("No cookies to build header from.")
        return ""
    }

    // Concatenate cookies into a single string
    StringBuilder cookieHeader = new StringBuilder()
    cookies.each { cookie ->
        if (cookieHeader.length() > 0) {
            cookieHeader.append("; ")
        }
        cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue())
    }

    return cookieHeader.toString()
}

// This function is called during the script loading to obtain a list of the names of the
// required configuration parameters, that will be shown in the Session Properties -> Authentication panel
String[] getRequiredParamsNames() {
    return ["authurl", "key1", "key2", "key3", "apiEndpoint",'sample']  // Added apiEndpoint as a required parameter
}

/**
 * Gets the names of the optional parameters for this script.
 * Used to populate optional parameters in the Session Properties - Authentication panel.
 */
String[] getOptionalParamsNames() {
    return ["extraHeaders"]
}

/**
 * Gets a list of all the credential parameter names for this script.
 * Used to read authentication data (username/password etc.) from ZAP.
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
    println "authentication_script_template.groovy: " + msg
}

def isError(statusCode) {
    return statusCode >= 400
}

def buildPostData(postData) {
    StringBuilder postDataStr = new StringBuilder()
    postData.each { key, value ->
        if (postDataStr.length() > 0) {
            postDataStr.append("&")
        }
        postDataStr.append(key)
        postDataStr.append("=")
        postDataStr.append(value)
    }
    return postDataStr
}
def printAndReturnMatchedCookie(HttpMessage loginPostMsg, String cookieName) {
    List<HttpCookie> cookies = loginPostMsg.getResponseHeader().getHttpCookies()
    HttpCookie matchedCookie = cookies.find { cookie -> cookie.getName().equalsIgnoreCase(cookieName) }
    if (matchedCookie != null) {
        logMsg( "Matched Cookie: ${matchedCookie.getName()} = ${matchedCookie.getValue()}")
    }
    return matchedCookie
}

def addCookiesFromResponse(HttpMessage loginPostMsg) {
    Map<String, HttpCookie> cookieMap = [:]
    def setCookieHeaders = loginPostMsg.getResponseHeader().getHeaders('Set-Cookie')
    logMsg("\n setCookieHeaders \n: ${setCookieHeaders}")

    setCookieHeaders.each { header ->
        header.split(',').each { cookieString ->
            logMsg("\n matched cookie \n: ${cookieString}")
            List<HttpCookie> cookies = HttpCookie.parse(cookieString)
            cookies.each { cookie ->
                cookieMap.put(cookie.getName(), cookie)
            }
        }
    }
    return cookieMap
}

def void setCsrfTokenVars(xsrftoken) {
    String ADD_CSRF_HEADER_SCRIPT = "add_csrf_header.groovy"
    logMsg(
            "Configuring ${ADD_CSRF_HEADER_SCRIPT} to add the anti-CSRF token header "
    )
    ScriptVars.setScriptVar(ADD_CSRF_HEADER_SCRIPT, "xsrf-token", xsrftoken)
    ScriptVars.setScriptVar(ADD_CSRF_HEADER_SCRIPT, "XSRF-TOKEN", xsrftoken)
}

// Example usage in a ZAP script
void onHttpRequestSend(HttpMessage msg) {
    addSessionHeader(msg)
}
void addSessionHeader(HttpMessage msg) {
    String sessionToken = ScriptVars.getGlobalVar("sessionToken")
    logMsg("Added session token to request: ${sessionToken}")
}

 String getLoggedInIndicator() {
    logMsg("\n sample cc \n")
 return "LoggedInIndicator"
 }
