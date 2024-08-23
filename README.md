# Kotlin Burp Jwt Session Token Handler Demo

This project demonstrates how to:
1. Create a session handling macro that extracts a JWT from a login macro response and uses it in future requests both as a cookie and authorization bearer header
2. Allow the tester to configure the Jwt, cookie, and header and save it as persistent project-level setting 

## Setup

This project was initially created using the template found at: https://github.com/ncoblentz/KotlinBurpExtensionBase. That template also describes how to:
- Build this and other projects based on the template
- Load the built jar file in Burp Suite
- Debug Burp Suite extensions using IntelliJ
- Provides links to documentation for building Burp Suite Plugins

## Example Usage and Demos

The following examples use traffic generated from visiting and interacting with the following demo site:
- https://github.com/juice-shop/juice-shop

