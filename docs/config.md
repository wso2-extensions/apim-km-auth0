# Integrate WSO2 API Manager 3.2.0 with Auth0

In this guide, we explain how to integrate the WSO2 API Manager with an external Identity provider Auth0 as the OAuth Authorization Server 
to manage the OAuth clients and tokens required by WSO2 API Manager. This is a client implementation that consumes APIs exposed by Auth0

## Follow the instructions below to configure the third-party Key Manager

### Step 1 : Prerequisites

1.  Create an Auth0 account. Get the URL for the tenant. and then sign into the dashboard.

   ![alt text](images/dashboard.png)


2.  The you need to create an application to use the management API. Then need's to allow that application to use the management API.

    ![alt text](images/management-api.png)

    ![alt text](images/New%20Application.png)
    
    ![alt text](images/permision-to-use-app.png)
   
   Make sure you have granted all the permissions to Create, Manage Apps and Resource servers.    

### Step 2: Configure WSO2 API Manager

1.  Log into the admin portal of the API Manager. And add a new Key Manager.
    
    ![alt text](images/add-app-admin.png)
    
2.  Then select the Key manager type as Auth0 and provide the relevant fields accordingly.

    ![alt text](images/km-tyoe.png)
 
    **List of well know address could be found in advance section of the Auth0 Application settings**    
        ![alt text](images/endpoints.png)
    
3.  The client ID, Client secret of the application created to invoke Manage API should be provided for the settings. You can get to the audience 
value from the Manage API.
    ![alt text](images/connector-configs.png)
4.  Finally you can save the configs.

### Step 3 : Create new application and generate keys

1.  Create new application form the developer portal.
    ![alt text](images/dev-app-create.png)

2.  Then click either production or sandbox, Select Auth0 fill the relevant fields accordingly.
    ![alt text](images/app-creation-form.png)
    
    *Please note that Audience of the API field is mandatory to generate an access token for Auth0. Therefore please provide it when the application keys generating.*
    
    You can get the audience of the api by checking the API.
    ![alt text](images/resoure-api.png)

3.  Once the keys generated, It will reflect in the UI.
    
    ![alt text](images/created-app.png)
    
5. Finally token will be generated successfully.

    ![alt text](images/success.png)
    

       


    
    
    
 
