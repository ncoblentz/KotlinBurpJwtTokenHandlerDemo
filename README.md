# Kotlin Burp Jwt Session Token Handler Demo

This project demonstrates how to:
1. Create a session handling macro that extracts a JWT from a login macro response and uses it in future requests both as a cookie and authorization bearer header
2. Allow the tester to configure the Jwt, cookie, and header and save it as persistent project-level setting 

To automatically persist project level settings in Burp and offer a user interface for modifying those settings, this project uses https://github.com/ncoblentz/BurpMontoyaLibrary. It allows an extension writer to define the settings they want to persist, the level of persistence (project level or Burp preference level), and then automatically receive a UI for updating those settings without having to deal with Java's Swing Library.

## Setup

This project was initially created using the template found at: https://github.com/ncoblentz/KotlinBurpExtensionBase. That template also describes how to:
- Build this and other projects based on the template
- Load the built jar file in Burp Suite
- Debug Burp Suite extensions using IntelliJ
- Provides links to documentation for building Burp Suite Plugins

## Handling Jwt Session Tokens Automatically

The following examples use traffic generated from visiting and interacting with the following project hosted locally:
- https://github.com/juice-shop/juice-shop
 
### Defining and Using The Settings

Start by defining the settings in the `init` function of your Burp Extension. 

![properties.png](Documentation/properties.png)

Assign them to class instance variables (or properties), so they can be accessed throughout the extension.
![definesetting.png](Documentation/definesetting.png)

Pass a list of the settings to the `GenericExtensionSettingFormGenerator` and build the form. It will create a user interface automatically to allow management of the settings. Register a right click context menu to access that settings UI. 

![librarygeneratesui.png](Documentation/librarygeneratesui.png)

### Using The Settings

Use the settings by accessing the `currentValue` property for that setting
![settingsuse1.png](Documentation/settingsuse1.png)

![settingsuse2.png](Documentation/settingsuse2.png)

### Extract the Jwt from the Login REST call

When logging into Juice Shop, the application returns a `token` parameter containing the Jwt. This plugin defines a session handling action that will extract that token when used by Burp's "Is In Session" rule.

![login.png](Documentation/login.png)

First step is to tell Burp we intended to offer a session handling action.

![sessionhandling.png](Documentation/sessionhandling.png)

Then, create a `performAction` method to satisfy the `SessionHandlingAction` interface requirements.

![performaction.png](Documentation/performaction.png)

Within that function, extract the token as shown below.

![extracttoken.png](Documentation/extracttoken.png)

Afterward, apply that token to the current request. In Juice Shop, some features look at the "Authorization: Bearer " header and others look at the "token" cookie.

![apply.png](Documentation/apply.png)

### Changing the Settings in Burp

Access the settings through the right-click context menu

![contextmenu.png](Documentation/contextmenu.png)

Configure the settings for Juice Shop

![configure.png](Documentation/configure.png)

### Create a Login Macro

Select an existing login request from proxy history as a login macro that will be used to automatically login and obtain a Jwt.

![loginmacro.png](Documentation/loginmacro.png)

### Set Up a Session Handling Rule

Create a session handling rule with two entries. For the first entry, select these settings.
![firstrule.png](Documentation/firstrule.png)

For the second entry, use these settings.

![secondrule.png](Documentation/secondrule.png)

Set the scope to include all URLs and the repeater tool.

![scope.png](Documentation/scope.png)

### Try it Out

First open session tracer, so you can track what's happening.

![sessiontracer.png](Documentation/sessiontracer.png)

Find a REST request and sent it to repeater. modify the cookie and bearer token, so it will generate a 401. Then send the request and check whether the session handling rule worked by watching the session tracer.

![requestwithsessiontracer.png](Documentation/requestwithsessiontracer.png)