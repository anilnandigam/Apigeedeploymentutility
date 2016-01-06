Step 0 Check out the tool
Step 1 Update the deploy.properties file
The deploy.properties has to be updated for the org you are backing up/restoring etc.
The default backup location is- backup-location=../backups 
Step 2 Run the task
A sample backup command will look like - java -jar deployment-utility.jar backupOrg deploy.properties
The detailed list of tasks can be found bellow



Darshans-MacBook-Pro:bin DarshanS$ pwd
/Users/DarshanS/Apigee/GitRepository/gitlab/tools/DeploymentUtility/bin

Darshans-MacBook-Pro:bin DarshanS$ java -jar deployment-utility.jar
Usage: java -jar deployment-utility.jar <TaskName> <PropertiesFileName>
To get list of supported tasks: java -jar deployment-utility.jar --help

Darshans-MacBook-Pro:bin DarshanS$ java -jar deployment-utility.jar --help
backupOrg
cleanupOrg
restoreOrg
restoreResources
restoreAPIProxies
restoreAPIProducts
restoreAPPDevelopers
restoreAPPs
backupAPIProxies
cleanupAPIProxies
backupResources
cleanupResources
backupApps
cleanupApps
backupAppDevelopers
cleanupAppDevelopers
backupAPIProducts
cleanupAPIProducts
Darshans-MacBook-Pro:bin DarshanS$ java -jar deployment-utility.jar backupOrg deploy.properties 
