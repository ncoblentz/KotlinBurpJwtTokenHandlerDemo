import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.sessions.ActionResult
import burp.api.montoya.http.sessions.SessionHandlingAction
import burp.api.montoya.http.sessions.SessionHandlingActionData
import com.nickcoblentz.montoya.settings.*
import de.milchreis.uibooster.model.Form
import de.milchreis.uibooster.model.FormBuilder
import java.util.regex.Pattern

// Montoya API Documentation: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html
// Montoya Extension Examples: https://github.com/PortSwigger/burp-extensions-montoya-api-examples

class JwtTokenHandlerExtension : BurpExtension, SessionHandlingAction {
    private lateinit var shouldUpdateCookieSetting: BooleanExtensionSetting
    private lateinit var cookieNameSetting: StringExtensionSetting
    private lateinit var shouldUpdateHeaderSetting: BooleanExtensionSetting
    private lateinit var headerValuePrefixSetting: StringExtensionSetting
    private lateinit var headerNameSetting: StringExtensionSetting
    private lateinit var accessTokenPatternSetting: StringExtensionSetting
    private var accessToken: String? = ""
    private lateinit var api: MontoyaApi
    private val tokenPattern = "\"token\" *: *\"([^\"]+)\""

    override fun initialize(api: MontoyaApi?) {

        // In Kotlin, you have to explicitly define variables as nullable with a ? as in MontoyaApi? above
        // This is necessary because the Java Library allows null to be passed into this function
        // requireNotNull is a built-in Kotlin function to check for null and throw an Illegal Argument exception if it is null
        // after checking for null, the Kotlin compiler knows that any reference to api below will not = null and you no longer have to check it
        requireNotNull(api) {"api : MontoyaApi is not allowed to be null"}

        // Assign the MontoyaApi instance (not nullable) to a class instance variable to be accessible from other functions in this class
        this.api = api

        // This will print to Burp Suite's Extension output and can be used to debug whether the extension loaded properly
        api.logging().logToOutput("Started loading the extension...")

        // Name our extension when it is displayed inside of Burp Suite
        api.extension().setName("Jwt Token Handler")

        // Code for setting up your extension starts here...

        // Configure our settings
        accessTokenPatternSetting = StringExtensionSetting(
            api,
            "Access Token RegEx Pattern",
            "BKSATH.pattern",
            "\"access_token\" *: *\"([^\"]+)\"",
            ExtensionSettingSaveLocation.PROJECT
        )
        headerNameSetting = StringExtensionSetting(
            api,
            "Name of Header",
            "BKSATH.header",
            "Authorization",
            ExtensionSettingSaveLocation.PROJECT
        )
        headerValuePrefixSetting = StringExtensionSetting(
            api,
            "Header Value Prefix (include your space)",
            "BKSATH.prefix",
            "Bearer ",
            ExtensionSettingSaveLocation.PROJECT
        )
        shouldUpdateHeaderSetting = BooleanExtensionSetting(
            api,
            "Update the header?",
            "BKSATH.updateHeader",
            false,
            ExtensionSettingSaveLocation.PROJECT
        )
        cookieNameSetting = StringExtensionSetting(
            api,
            "Name of Header",
            "BKSATH.cookie",
            "token",
            ExtensionSettingSaveLocation.PROJECT
        )
        shouldUpdateCookieSetting = BooleanExtensionSetting(
            api,
            "Update the cookie?",
            "BKSATH.updateCookie",
            false,
            ExtensionSettingSaveLocation.PROJECT
        )

        val extensionSetting = listOf(headerNameSetting,headerValuePrefixSetting,accessTokenPatternSetting,shouldUpdateHeaderSetting,cookieNameSetting,shouldUpdateCookieSetting)
        val gen = GenericExtensionSettingsFormGenerator(extensionSetting, "Jwt Token Handler")
        val settingsFormBuilder: FormBuilder = gen.getSettingsFormBuilder()
        val settingsForm: Form = settingsFormBuilder.run()
        api.userInterface().registerContextMenuItemsProvider(ExtensionSettingsContextMenuProvider(api, settingsForm))
        api.extension().registerUnloadingHandler(ExtensionSettingsUnloadHandler(settingsForm))

        // Tell Burp we have a session handling action for it to find
        api.http().registerSessionHandlingAction(this)

        // Just a simple hello world to start with
        api.logging().logToOutput("Hello Extension Writer!")

        // Code for setting up your extension ends here

        // See logging comment above
        api.logging().logToOutput("...Finished loading the extension")

    }

    // This method is used to display the name of our session handling action when selecting a Burp extension from the session handling rules
    override fun name(): String {
        return "JwtTokenSessionHandlingAction"
    }

    // This method is called when you choose this extension in the following two scenarios:
    // 1. Burp -> Settings -> Session -> Add -> Add -> Invoke a Burp Extension -> Choose this Extension
    //    - Each request (actionData.request()) will pass through this function as is. You can manipulate it and then return the manipulated request
    //    - We want to simply append an existing Jwt to the request and place it within the "token" cookie and an "Authorization: Bearer" header
    // 2. Burp -> Settings -> Session -> Add -> Add -> Check Session Is Valid -> Check Mark: After running the macro, invoke a Burp extension handler -> Choose this Extension
    //    - Only requests that are checked whether they are in session will pass through this function
    //    - If a macro (recorded set of requests and responses) are executed first, we will have access to those macros through actionData.macroRequestResponses()
    //    - We want to extract the Jwt token from one of the responses
    //    - Then we want to add it as a header and cookie as described above in #1
    override fun performAction(actionData: SessionHandlingActionData?): ActionResult {
        api.logging().logToOutput("Entered performAction")

        // actionData should never be null, but if it ever is, we will throw an Illegal Argument Exception
        // we also will no longer have to check for null when using actionData, the compiler knows it can't be null below this line
        requireNotNull(actionData) {"actionData : SessionHandlingActionData is not allowed to be null"}

        // We will modify this request and append the Jwt to it
        var modifiedRequest = actionData.request()

        // Obtain the Jwt from a Macro if Present
        if(actionData.macroRequestResponses().isNotEmpty()) {
            api.logging().logToOutput("Found macro one or more http request/response pairs")

            // Iterate through each of the macro request/response pairs, looking for that token
            for(httpRequestResponse in actionData.macroRequestResponses()) {
                if(httpRequestResponse.hasResponse()) {

                    // Use a regex (it's more flexible than parsing JSON), to identify and extract the token
                    val searchPattern = Pattern.compile(tokenPattern, Pattern.CASE_INSENSITIVE)
                    val matcher = searchPattern.matcher(httpRequestResponse.response().toString())
                    while (matcher.find() && matcher.groupCount() > 0) {

                        // Save that token to a class instance variable, so we can use it in future invocations of this method without parsing session macros
                        accessToken = matcher.group(1)
                        api.logging().logToOutput("Found Access Token: $accessToken")
                    }
                }
            }

            // Apply the token (if not empty) to the current HTTP request.
            // This will occur in scenario 1 and 2 discussed above
            accessToken?.let {
                //accessToken (checked for not null) is now called "it")
                if (it.isNotEmpty()) {
                    api.logging().logToOutput("accessToken is Not Empty, adding header and cookie")
                    modifiedRequest = modifiedRequest.addOrUpdateHeader("Authorization","Bearer $it")
                    modifiedRequest = modifiedRequest.addOrUpdateCookie("token",it)
                }
            }
        }
        api.logging().logToOutput("Leaving performAction")

        // Continue execution with the modified request
        return ActionResult.actionResult(modifiedRequest, actionData.annotations())
    }
}