package com.apigee.utilities.deploymentutility;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * User: Darshan
 * Date: 03/22/2015
 * Time: 11:13 PM
 */
public class APIProxyHandle implements DeploymentUtilityConstants {


    private String environment;
    private String apiproxy;
    private String username;
    private String password;

    private String orgPath;
    private String apiProxyPath;
    private ApigeeHTTPClient apigeeHTTPClient;
    private String apiproxyLatestVersion = "1";


    public APIProxyHandle(Properties properties) {
        //this.properties = properties;
        String enterpriseURL = properties.getProperty(ENTERPRISE_URL);
        String org = properties.getProperty(ORG);
        environment = properties.getProperty(ENVIRONMENT);
        username = properties.getProperty(USERNAME);
        password = properties.getProperty(PASSWORD);
        apiproxy = properties.getProperty(APIPROXY);

        orgPath = enterpriseURL + ORG_BASEPATH + URL_PATH_SEPERATOR + org;
        apiProxyPath = orgPath + APIS_BASEPATH + URL_PATH_SEPERATOR + apiproxy;

        apigeeHTTPClient = new ApigeeHTTPClient();
    }

    public String getAPIs() throws IOException {

        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        return apigeeHTTPClient.doGET(orgPath + APIS_BASEPATH, headers, username, password);
    }

    public String getAPIDetails() throws IOException {

        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        return apigeeHTTPClient.doGET(apiProxyPath, headers, username, password);
    }

    public String undeployAPI() throws IOException {

        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        String deployments = apigeeHTTPClient.doGET(apiProxyPath + "/deployments", headers, username, password);


        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(deployments);

        JSONArray environments = jsonObject.getJSONArray("environment");
        String undeployReponse = null;
        for (Object environment1 : environments) {
            JSONObject envmt = (JSONObject) environment1;


            String env = envmt.getString("name");
            if (environment == null || env.equalsIgnoreCase(environment)) {
                JSONArray revisions = envmt.getJSONArray("revision");
                for (Object revision : revisions) {
                    String version = ((JSONObject) revision).getString("name");

                    String url = apiProxyPath + String.format("/revisions/%s/deployments?action=undeploy&force=true&env=%s", version, env);
                    undeployReponse = apigeeHTTPClient.doPOST(url, headers, "", username, password);

                }
            }
        }


        return undeployReponse;

    }

    public String deleteAPI() throws IOException {

        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        return apigeeHTTPClient.doDelete(apiProxyPath, headers, username, password);


    }

    public String deployAPI() throws IOException {

        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        String url = orgPath + APIS_BASEPATH + String.format("?action=import&name=%s", apiproxy);

        String response = apigeeHTTPClient.doPOST(url, headers, new File(apiproxy + ".zip"), username, password);

        apiproxyLatestVersion = ((JSONObject) JSONSerializer.toJSON(response)).getString("revision");

        return response;
    }

    public String activate(String revision) throws IOException {
        Map<String, String> headers = new Hashtable<String, String>();
        headers.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_APPLICATION_OCTET_STREAM);

        if (revision == null) {
            revision = apiproxyLatestVersion;
        }

        String url = apiProxyPath + String.format("/revisions/%s/deployments?action=deploy&env=%s", revision, environment);

        return apigeeHTTPClient.doPOST(url, headers, "", username, password);


    }

    public String createBundle() throws IOException {

        File dirObj = new File(APIPROXY);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apiproxy + ".zip"));

        addDir(dirObj, out);
        out.close();

        return null;

    }

    static void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                addDir(file, out);
                continue;
            }
            FileInputStream in = new FileInputStream(file.getPath());
            System.out.println(" Adding: " + file.getPath());
            out.putNextEntry(new ZipEntry(file.getPath()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }


    public static void main(String[] args) throws IOException {

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
        APIProxyHandle test = new APIProxyHandle(properties);

        try {
            String undeployResponse = test.undeployAPI();
            System.out.println(undeployResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        String deleteResponse = test.deleteAPI();
//        System.out.println(deleteResponse);

        test.createBundle();
        String revision = test.deployAPI();
        String rev = ((JSONObject) JSONSerializer.toJSON(revision)).getString("revision");
        String response = test.activate(rev);
        System.out.println(response);


    }
}
