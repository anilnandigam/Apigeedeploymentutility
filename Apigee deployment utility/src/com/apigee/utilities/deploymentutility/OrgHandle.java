package com.apigee.utilities.deploymentutility;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * User: Darshan
 * Date: 03/22/2015
 * Time: 9:26 PM
 */
public class OrgHandle implements DeploymentUtilityConstants {

    //todo 1. - we are not ready for the multi threading yet.
    //one of the reasons is the spike arrest on the API management part
    //If we can handle the spike arrest carefully, we should be able to get the multi threading back

    //todo 2. - Logging. Logging should replace the System.out.println statements

    //todo 3. - The utility should be extended to overwrite certain parameters

    private String org;
    private String username;
    private String password;
    private String restoreFromOrg;

    private String orgPath;
    private ApigeeHTTPClient apigeeHTTPClient;

    private String backupLocation = "./Backups";


    public OrgHandle(Properties properties) {
        //this.properties = properties;
        String enterpriseURL = properties.getProperty(ENTERPRISE_URL);
        org = properties.getProperty(ORG);
        username = properties.getProperty(USERNAME);
        password = properties.getProperty(PASSWORD);
        orgPath = enterpriseURL + ORG_BASEPATH + URL_PATH_SEPERATOR + org;

        restoreFromOrg = properties.getProperty(RESTORE_FROM);


        if (properties.getProperty(BackupLocation) != null && !properties.getProperty(BackupLocation).isEmpty()) {
            backupLocation = properties.getProperty(BackupLocation);
        }
        apigeeHTTPClient = new ApigeeHTTPClient();
    }

    public String backupOrg() throws IOException {
        return backupOrg(false);
    }

    public String cleanupOrg() throws IOException {
        return backupOrg(true);
    }

    public String restoreOrg() throws Exception {
        restoreResources();
        restoreAPIProxies();
        restoreAPIProducts();
        restoreAPPDevelopers();
        restoreAPPs();

        return "Success";

    }

    public String backupAPIProxies() throws IOException {

        return backupAPIProxies(false);

    }

    public String cleanupAPIProxies() throws IOException {

        return backupAPIProxies(true);

    }

    private String backupAPIProxies(final boolean delete) throws IOException {

        System.out.println("############# Taking a backup of API Proxies ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        final File backupLocation = new File(getBaseBackupDirectory(false), "apiproxies");
        backupLocation.mkdirs();

        String apisString = apigeeHTTPClient.doGET(orgPath + APIS_BASEPATH, headers, username, password);
        JSONArray apis = (JSONArray) JSONSerializer.toJSON(apisString);
        for (Object apiObj : apis) {
            final String api = (String) apiObj;
            final String apiURL = orgPath + APIS_BASEPATH + "/" + api;
            String apiVersString = apigeeHTTPClient.doGET(apiURL, headers, username, password);

            final JSONObject apiVersionsObj = (JSONObject) JSONSerializer.toJSON(apiVersString);

//            (new Thread() {
//
//                public void run() {
            JSONArray versions = (JSONArray) apiVersionsObj.get("revision");
            JSONObject apiContext = new JSONObject();
            ArrayList<Integer> versionList = new ArrayList<Integer>();

            for (Object verObj : versions) {
                String version = (String) verObj;
                versionList.add(Integer.valueOf(version));
                final File versionFile = new File(backupLocation, File.separator + api + File.separator + version);
                versionFile.getParentFile().mkdirs();
                final String url = orgPath + APIS_BASEPATH + "/" + api + "/revisions/" + version + "?format=bundle";


                try {
                    apigeeHTTPClient.importBundle(url, new Hashtable<String, String>(), new File(versionFile.getAbsolutePath() + ".zip"), username, password);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            JSONObject deployments = null;
            try {
                deployments = (JSONObject) JSONSerializer.toJSON(apigeeHTTPClient.doGET(apiURL + "/deployments", headers, username, password));
            } catch (IOException e) {
                e.printStackTrace();

            }


            JSONArray environments = (JSONArray) deployments.get("environment");


            if (environments != null && environments.size() > 0)

            {

                JSONArray deploymentConfgs = new JSONArray();
                for (Object envo : environments) {
                    JSONObject env = (JSONObject) envo;
                    String environment = env.getString("name");
                    String revision = env.getJSONArray("revision").getJSONObject(0).getString("name");
                    JSONObject envObj = new JSONObject();
                    envObj.put(ENVIRONMENT, environment);
                    envObj.put("revision", revision);
                    deploymentConfgs.add(envObj);

                    if (delete) {
                        //undeploy and delete

                        final String undeployurl = apiURL + String.format("/revisions/%s/deployments?action=undeploy&force=true&env=%s", revision, environment);


                        try {
                            apigeeHTTPClient.doPOST(undeployurl, headers, "", username, password);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }

                }
                apiContext.put(ENVIRONMENTS, deploymentConfgs);

            }

            try {
                apiContext.put("versions", versions);
                apiContext.put("maxversion", Collections.max(versionList));


                FileWriter file = new FileWriter(new File(backupLocation, File.separator + api + File.separator + API_CONTEXT_FILE));
                file.write(apiContext.toString());

                file.flush();
                file.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (delete) {
                //delete
                try {

                    apigeeHTTPClient.doDelete(apiURL, headers, username, password);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

//                }
//            }).start();


        }

        return apis.toString();

    }

    public String backupResources() throws IOException {
        return backupResources(false);
    }

    public String cleanupResources() throws IOException {
        return backupResources(true);
    }

    private String backupResources(final boolean delete) throws IOException {
        System.out.println("############# Taking a backup of Resources ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);
        String envURL = orgPath + "/environments";
        String envs = apigeeHTTPClient.doGET(envURL, headers, username, password);
        JSONArray environments = (JSONArray) JSONSerializer.toJSON(envs);
        File environmentsDir = new File(getBaseBackupDirectory(false), "environments");
        environmentsDir.mkdirs();

        for (Object env : environments) {
            String environment = (String) env;

            File environmentDir = new File(environmentsDir, environment);
            environmentDir.mkdirs();

            final File cachesDir = new File(environmentDir, "caches");
            cachesDir.mkdirs();
            final String cacheURL = envURL + "/" + environment + "/caches";
            JSONArray caches = (JSONArray) JSONSerializer.toJSON(apigeeHTTPClient.doGET(cacheURL, headers, username, password));
            for (Object cacheo : caches) {
                final String cacheName = (String) cacheo;
//                (new Thread() {
//
//                    public void run() {


                try {


                    String cache = apigeeHTTPClient.doGET(cacheURL + "/" + cacheName, headers, username, password);


                    try {

                        FileWriter file = new FileWriter(new File(cachesDir, cacheName + JSON_EXTENSION));
                        file.write(cache);
                        file.flush();
                        file.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    }
//
//                }).start();
//                (new Thread() {
//
//                    public void run() {


                try {


                    if (delete) {
                        apigeeHTTPClient.doDelete(cacheURL + "/" + cacheName, headers, username, password);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    }
//
//                }).start();


            }


        }

        return "Success";


    }

    public String backupApps() throws IOException {
        return backupApps(false);
    }

    public String cleanupApps() throws IOException {
        return backupApps(true);
    }

    private String backupApps(final boolean delete) throws IOException {
        System.out.println("############# Taking a backup of Developer Apps ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        String appsString = apigeeHTTPClient.doGET(orgPath + APPS_BASEPATH, headers, username, password);


        JSONArray apps = (JSONArray) JSONSerializer.toJSON(appsString);
        for (Object appObj : apps) {
            final String app = (String) appObj;
//            (new Thread() {
//
//                public void run() {


            try {
                String developerAppString = apigeeHTTPClient.doGET(orgPath + APPS_BASEPATH + "/" + app, headers, username, password);

                File backupLocation = new File(getBaseBackupDirectory(false), "apps");
                backupLocation.mkdirs();
                try {

                    FileWriter file = new FileWriter(new File(backupLocation, app + JSON_EXTENSION));
                    file.write(developerAppString);
                    file.flush();
                    file.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (delete) {

                    JSONObject json = (JSONObject) JSONSerializer.toJSON(developerAppString);


                    String developeremail = json.getString("developerId");
                    String appName = json.getString("name");
                    apigeeHTTPClient.doDelete(orgPath + "/developers/" + developeremail + "/apps/" + appName, headers, username, password);


                }


            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            }).start();

        }


        return appsString;

    }

    public String backupAppDevelopers() throws IOException {
        return backupAppDevelopers(false);
    }

    public String cleanupAppDevelopers() throws IOException {
        return backupAppDevelopers(true);
    }

    private String backupAppDevelopers(final boolean delete) throws IOException {
        System.out.println("############# Taking a backup of APP Developers ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        String appDevelopersString = apigeeHTTPClient.doGET(orgPath + APP_Developers_BASEPATH, headers, username, password);


        JSONArray appDevelopers = (JSONArray) JSONSerializer.toJSON(appDevelopersString);
        for (Object apiObj : appDevelopers) {
            final String appDeveloper = (String) apiObj;

//            (new Thread() {
//
//                public void run() {


            try {


                String appDeveloperString = apigeeHTTPClient.doGET(orgPath + APP_Developers_BASEPATH + "/" + appDeveloper, headers, username, password);
                File backupLocation = new File(getBaseBackupDirectory(false), "developers");
                backupLocation.mkdirs();
                try {

                    FileWriter file = new FileWriter(new File(backupLocation, appDeveloper + JSON_EXTENSION));
                    file.write(appDeveloperString);
                    file.flush();
                    file.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            }).start();

//            (new Thread() {
//
//                public void run() {


            try {

                if (delete) {
                    apigeeHTTPClient.doDelete(orgPath + APP_Developers_BASEPATH + "/" + appDeveloper, headers, username, password);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            }).start();
        }


        return appDevelopersString;

    }

    public String backupAPIProducts() throws IOException {
        return backupAPIProducts(false);
    }

    public String cleanupAPIProducts() throws IOException {
        return backupAPIProducts(true);
    }

    private String backupAPIProducts(final boolean delete) throws IOException {
        System.out.println("############# Taking a backup of API Products ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        String apiProductsString = apigeeHTTPClient.doGET(orgPath + API_ProductsS_BASEPATH, headers, username, password);


        JSONArray apiProducts = (JSONArray) JSONSerializer.toJSON(apiProductsString);
        for (Object apiObj : apiProducts) {
            final String apiProduct = (String) apiObj;
//            (new Thread() {
//
//                public void run() {


            try {
                String apiProductString = apigeeHTTPClient.doGET(orgPath + API_ProductsS_BASEPATH + "/" + apiProduct, headers, username, password);
                File backupLocation = new File(getBaseBackupDirectory(false), "apiproducts");
                backupLocation.mkdirs();
                try {

                    FileWriter file = new FileWriter(new File(backupLocation, apiProduct + JSON_EXTENSION));
                    file.write(apiProductString);
                    file.flush();
                    file.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            }).start();

//            (new Thread() {
//
//                public void run() {


            try {
                if (delete) {
                    apigeeHTTPClient.doDelete(orgPath + API_ProductsS_BASEPATH + "/" + apiProduct, headers, username, password);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            }).start();


        }


        return "Success";

    }

    public String restoreResources() throws IOException, InterruptedException {

        System.out.println("############# Restoring Resources ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_JSON);

        File environmentsDir = new File(getBaseRestoreDirectory(), "environments");

//        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (File environmentDir : environmentsDir.listFiles()) {
            String environment = environmentDir.getName();
            if(environment.startsWith("."))
                continue;


            for (File resourceType : environmentDir.listFiles()) {


                for (final File resource : resourceType.listFiles()) {
                    final String resourceURL = orgPath + "/environments" + "/" + environment + "/" + resourceType.getName();

//                    threads.add(new Thread() {
//
//                        public void run() {

                    try {

                        apigeeHTTPClient.doPOST(resourceURL, headers, resource, username, password);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                        }
//
//                    });
                }
            }

        }

//        for (Thread thread : threads) {
//            thread.start();
//
//        }
//        for (Thread thread : threads) {
//            thread.join();
//
//        }

        return "Success";
    }

    public String restoreAPIProxies() throws IOException, InterruptedException {
        System.out.println("############# Restoring API Proxies ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        File apiproxiesDir = new File(getBaseRestoreDirectory(), "apiproxies");

//        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (final File apiproxyDir : apiproxiesDir.listFiles()) {


//            threads.add(new Thread() {
//
//                public void run() {
            try {
                String apiProxyName = apiproxyDir.getName();
                if(apiProxyName.startsWith("."))
                    continue;
                File deploymentsFile = new File(apiproxyDir, API_CONTEXT_FILE);
                String s = IOUtils.toString(new FileInputStream(deploymentsFile));
                JSONObject apiContext = (JSONObject) JSONSerializer.toJSON(s);

                System.out.println(apiContext);


                int maxversion = apiContext.getInt("maxversion");


                for (int i = 1; i <= maxversion; i++) {

                    File revision = new File(apiproxyDir, i + ZIP_EXTENSION);
                    if (!revision.exists()) {
                        revision = new File(apiproxyDir, maxversion + ZIP_EXTENSION);

                        //todo should be remove this version later??
                    }

                    final String url = orgPath + APIS_BASEPATH + String.format("?action=import&name=%s", apiProxyName);


                    try {

                        apigeeHTTPClient.doPOST(url, headers, revision, username, password);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }


                if (apiContext.containsKey(ENVIRONMENTS))

                {
                    JSONArray environments = apiContext.getJSONArray(ENVIRONMENTS);

                    for (Object apiObj : environments) {
                        JSONObject deployment = (JSONObject) apiObj;
                        String environment = deployment.getString("environment");
                        String revision = deployment.getString("revision");
                        final String url = orgPath + APIS_BASEPATH + "/" + apiProxyName + String.format("/revisions/%s/deployments?action=deploy&env=%s", revision, environment);


                        try {


                            apigeeHTTPClient.doPOST(url, headers, "", username, password);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//            });
        }

//        for (Thread thread : threads) {
//            thread.start();
//
//        }
//        for (Thread thread : threads) {
//            thread.join();
//
//        }

        return "Success";
    }

    public String restoreAPPs() throws IOException, InterruptedException {
        System.out.println("############# Restoring Developer Apps ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_JSON);

        File backupLocation = new File(getBaseRestoreDirectory(), "apps");

        HashMap<String, String> developersMap = readDevelopers();

//        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (File app : backupLocation.listFiles()) {


            try {

                if (app.getName().startsWith("."))
                    continue;

                String s = IOUtils.toString(new FileInputStream(app));


                final JSONObject appBackup = (JSONObject) JSONSerializer.toJSON(s);
                final String developerId = appBackup.getString(DEVELOPER_ID);
                String developerEmail = developersMap.get(developerId);
                if (developerEmail == null || developerEmail.isEmpty())
                    developerEmail = developersMap.get("default");


                final JSONObject request = new JSONObject();
                request.put("name", appBackup.get("name"));
                request.put("apiProducts", appBackup.get("apiProducts"));
                request.put("scopes", appBackup.get("scopes"));
                request.put("accessType", appBackup.get("accessType"));
                request.put("appFamily", appBackup.get("appFamily"));
                //request.put("appId", appBackup.get("appId"));

                final String appsBasePath = orgPath + APP_Developers_BASEPATH + "/" + developerEmail + "/apps";
                final String finalDeveloperEmail = developerEmail;


                try {

                	String name = appBackup.getString("name").replace(" ", "%20");//(URLEncoder.encode(appBackup.getString("name"),"ISO-8859-1"").replace("+", "%20"));
                    String appBasePath = orgPath + APP_Developers_BASEPATH + "/" + finalDeveloperEmail + "/apps/" + name;

                    //System.out.println("Creating App with:" + request.toString());
                    String appCreatedString = apigeeHTTPClient.doPOST(appsBasePath, headers, request.toString(), username, password);
                    System.out.println("Sleeping for delay due to management Server-- Do Delete new App."+request.toString() +"AppName"+name);
                    Thread.sleep(30000);
                    //System.out.println("Created App with:" + appCreatedString);

                    JSONObject appCreated = (JSONObject) JSONSerializer.toJSON(appCreatedString);


                    JSONArray credentials = appCreated.getJSONArray(DEVELOPER_APP_CREDENTIALS);
                    for (Object credentialO : credentials) {
                        JSONObject credential = (JSONObject) credentialO;
                        String consumerKey = credential.getString(DEVELOPER_APP_CONSUMER_KEY);

                        Map<String, String> headers1 = new Hashtable<String, String>();
                        headers1.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);


                        apigeeHTTPClient.doDelete(appBasePath + "/keys/" + consumerKey, headers1, username, password);
                        System.out.println("Sleeping for delay due to management Server-- Do Delete new App."+consumerKey);
                        Thread.sleep(30000);
                        

                    }

                    JSONArray backupCredentials = appBackup.getJSONArray(DEVELOPER_APP_CREDENTIALS);
                    for (Object backupCredentialO : backupCredentials) {
                        JSONObject backupCredential = (JSONObject) backupCredentialO;


                        String consumerKey = backupCredential.getString(DEVELOPER_APP_CONSUMER_KEY);
                        apigeeHTTPClient.doPOST(appBasePath + "/keys/create", headers, backupCredential.toString(), username, password);

                        JSONArray backupProducts = backupCredential.getJSONArray("apiProducts");
                        JSONObject productsRequest = new JSONObject();
                        JSONArray products = new JSONArray();
                        for (Object backupProductO : backupProducts) {
                            JSONObject backupProduct = (JSONObject) backupProductO;
                            String productName = backupProduct.getString("apiproduct");
                            products.add(productName);
                            String productStatus = backupProduct.getString("status");
                            if ("approved".equalsIgnoreCase(productStatus)) {
                                //todo check if the product has manual approval. If yes, then you have to manually approve the product
                            }
                        }

                        productsRequest.put(DEVELOPER_APP_PRODUCTS, products);


                        String putResponse = apigeeHTTPClient.doPOST(appBasePath + "/keys/" + consumerKey, headers, productsRequest.toString(), username, password);
                        System.out.println("Sleeping for delay due to management Server-- Do Post new App."+productsRequest.toString());
                        Thread.sleep(30000);
                        
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }


        }

//        for (Thread thread : threads) {
//            thread.start();
//
//        }
//        for (Thread thread : threads) {
//            thread.join();
//
//        }

        return "Success";
    }

    public String restoreAPPDevelopers() throws IOException, InterruptedException {

        System.out.println("############# Restoring App Developers ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_JSON);

//        ArrayList<Thread> threads = new ArrayList<Thread>();
        File backupLocation = new File(getBaseRestoreDirectory(), DEVELOPERS);
        for (final File appDeveloper : backupLocation.listFiles()) {

            if(appDeveloper.getName().startsWith("."))
                continue;
//            threads.add(new Thread() {
//
//                public void run() {
//
//
            try {


                apigeeHTTPClient.doPOST(orgPath + APP_Developers_BASEPATH, headers, appDeveloper, username, password);

            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            });
        }

//        for (Thread thread : threads) {
//            thread.start();
//
//        }
//        for (Thread thread : threads) {
//            thread.join();
//
//        }

        return "Success";
    }

    public String restoreAPIProducts() throws IOException, InterruptedException {
        System.out.println("############# Restoring API Products ##############");

        final Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_JSON);

        File backupLocation = new File(getBaseRestoreDirectory(), "apiproducts");

//        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (final File apiProduct : backupLocation.listFiles()) {
            if(backupLocation.getName().startsWith("."))
                continue;
//            threads.add(new Thread() {
//
//                public void run() {
            try {

                apigeeHTTPClient.doPOST(orgPath + API_ProductsS_BASEPATH, headers, apiProduct, username, password);
                System.out.println("Adding a delay in POST API Calls.--");
                Thread.sleep(150000);

            } catch (IOException e) {
                e.printStackTrace();
            }
//                }
//
//            });
        }
//
//        for (Thread thread : threads) {
//            thread.start();
//
//        }
//        for (Thread thread : threads) {
//            thread.join();
//
//        }


        return "Success";
    }


    private String backupOrg(boolean delete) throws IOException {

        String backupDir = getBaseBackupDirectory(true).getAbsolutePath();

        backupAPIProxies(delete);
        backupApps(delete);
        backupAppDevelopers(delete);
        backupAPIProducts(delete);
        backupResources(delete);

        System.out.println("Backup Taken at:" + backupDir);

        return "Success";
    }

    private HashMap<String, String> readDevelopers() throws IOException {
        HashMap<String, String> developers = new HashMap<String, String>();

        File backupLocation = new File(getBaseRestoreDirectory(), DEVELOPERS);
        JSONObject appDeveloper = new JSONObject();
        for (File appDeveloperFile : backupLocation.listFiles()) {
            if(appDeveloperFile.getName().startsWith("."))
                continue;

            String s = IOUtils.toString(new FileInputStream(appDeveloperFile));
            appDeveloper = (JSONObject) JSONSerializer.toJSON(s);

            developers.put(appDeveloper.getString(DEVELOPER_ID), appDeveloper.getString(DEVELOPER_EMAIL));
        }
        developers.put("default", appDeveloper.getString(DEVELOPER_EMAIL));


        return developers;
    }

    private File getBaseBackupDirectory(boolean rollover) {
        File backupDirectory = new File(backupLocation + File.separator + org + "/backups/" + org);

        if (rollover && backupDirectory.exists()) {
            backupDirectory.renameTo(new File(backupDirectory.getAbsolutePath() + "__" + System.currentTimeMillis()));
        }

        backupDirectory.mkdirs();
        return backupDirectory;
    }

    private File getBaseRestoreDirectory() {
        File backupDirectory = new File(backupLocation + File.separator + restoreFromOrg + "/backups/" + restoreFromOrg);
        return backupDirectory;
    }


    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();

        File propertiesFile = new File(".", "deploy.properties");

        System.out.println(propertiesFile.getAbsolutePath());
        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));
        System.out.println(properties);
        if (properties.getProperty(PASSWORD) == null) {
            String prompt = String.format("Enter Password for user %s :", properties.getProperty(USERNAME));
            System.out.println(prompt);
            Console console = System.console();
            String password = String.valueOf(console.readPassword());

            System.out.println(password);
            properties.setProperty(PASSWORD, password);
        }
        OrgHandle test = new OrgHandle(properties);


        String response = test.restoreAPPs();
        System.out.println(response);

        long endTime = System.currentTimeMillis();

        System.out.println("Total Time Taken: (sec): " + (endTime - startTime) / 1000l);

    }
}
