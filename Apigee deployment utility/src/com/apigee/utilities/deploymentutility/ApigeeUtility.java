package com.apigee.utilities.deploymentutility;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


/**
 * User: Darshan
 * Date: 03/22/2015
 * Time: 10:21 PM
 */
public class ApigeeUtility implements DeploymentUtilityConstants {

    public void printPublicMethods() {

        Class<OrgHandle> orgHandleClass = OrgHandle.class;
        Method[] methods = orgHandleClass.getMethods();

        ArrayList<Method> methods1 = new ArrayList<Method>(Arrays.asList(orgHandleClass.getSuperclass().getMethods()));


        for (Method method : methods) {
            if (!(methods1.contains(method) || "main".equalsIgnoreCase(method.getName()))) {
                if (Modifier.isPublic(method.getModifiers()))
                    System.out.println(method.getName());
            }


        }

    }

    public void execute(String methodName, Properties properties) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {


        Class<?> orgHandleClass = Class.forName(OrgHandle.class.getName());

        OrgHandle orghandle = new OrgHandle(properties);

        Method method = orgHandleClass.getMethod(methodName);
        System.out.println(method);

        System.out.println(orghandle.getClass());
        method.invoke(orghandle);


    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {


    	ApigeeUtility test = new ApigeeUtility();

        if (args.length == 0) {
            test.printUsage();
            System.exit(0);
        }

        if ("--help".equalsIgnoreCase(args[0])) {
            test.printPublicMethods();
            System.exit(0);
        }

        long startTime = System.currentTimeMillis();

        String propertiesFilePath = args.length > 1 ? args[1] : null;
        Properties properties = test.initiate(propertiesFilePath);

        test.execute(args[0], properties);


        long endTime = System.currentTimeMillis();

        System.out.println("Total Time Taken: (sec): " + (endTime - startTime) / 1000l);


    }

    private Properties initiate(String propertiesFilePath) throws IOException {

        if (propertiesFilePath == null || propertiesFilePath.isEmpty()) {
            propertiesFilePath = "./deploy.properties";
        }

        File propertiesFile = new File(propertiesFilePath);
        System.out.println(propertiesFile.getAbsolutePath());

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

        return properties;
    }

    private void printUsage() {
        System.out.println("Usage: java -jar deployment-utility.jar <TaskName> <PropertiesFileName>");
        System.out.println("To get list of supported tasks: java -jar deployment-utility.jar --help");
    }

}