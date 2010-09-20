package com.redhat.qe.sm.base;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.tasks.CandlepinTasks;
import com.redhat.qe.sm.tasks.SubscriptionManagerTasks;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
public class SubscriptionManagerTestScript extends com.redhat.qe.auto.testng.TestScript{
//	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
//	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	protected String serverHostname			= System.getProperty("rhsm.server.hostname");
	protected String serverPort 			= System.getProperty("rhsm.server.port");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl");
	protected String serverInstallDir		= System.getProperty("rhsm.server.installdir");
	protected String serverImportDir		= System.getProperty("rhsm.server.importdir");
	protected String serverBranch			= System.getProperty("rhsm.server.branch");
	protected Boolean isServerOnPremises	= Boolean.valueOf(System.getProperty("rhsm.server.onpremises","false"));
	protected Boolean deployServerOnPremises= Boolean.valueOf(System.getProperty("rhsm.server.deploy","true"));

	protected String client1hostname		= System.getProperty("rhsm.client1.hostname");
	protected String client1username		= System.getProperty("rhsm.client1.username");
	protected String client1password		= System.getProperty("rhsm.client1.password");

	protected String client2hostname		= System.getProperty("rhsm.client2.hostname");
	protected String client2username		= System.getProperty("rhsm.client2.username");
	protected String client2password		= System.getProperty("rhsm.client2.password");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;
	
	protected String clientOwnerUsername	= System.getProperty("rhsm.client.owner.username");
	protected String clientOwnerPassword	= System.getProperty("rhsm.client.owner.password");
	protected String clientUsernames		= System.getProperty("rhsm.client.usernames");
	protected String clientPasswords		= System.getProperty("rhsm.client.passwords");

	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken");
	protected int certFrequency				= Integer.valueOf(System.getProperty("rhsm.client.certfrequency"));
	protected String enablerepofordeps		= System.getProperty("rhsm.client.enablerepofordeps");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert.url");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product");
	
	protected String sshUser				= System.getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");

//	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
//	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
//	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
//	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
//	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
//	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String dbHostname				= System.getProperty("rhsm.server.db.hostname");
	protected String dbSqlDriver			= System.getProperty("rhsm.server.db.sqldriver");
	protected String dbPort					= System.getProperty("rhsm.server.db.port");
	protected String dbName					= System.getProperty("rhsm.server.db.name");
	protected String dbUsername				= System.getProperty("rhsm.server.db.username");
	protected String dbPassword				= System.getProperty("rhsm.server.db.password");

	
	
	protected String urlToRPM				= System.getProperty("rhsm.rpm.url");
	protected Boolean installRPM			= Boolean.valueOf(System.getProperty("rhsm.rpm.install","true"));


//DELETEME
//MOVED TO TASKS CLASSES
//	protected String defaultConfigFile		= "/etc/rhsm/rhsm.conf";
//	protected String rhsmcertdLogFile		= "/var/log/rhsm/rhsmcertd.log";
//	protected String rhsmYumRepoFile		= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
//	public static Connection itDBConnection = null;
	public static Connection dbConnection = null;
	
	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static CandlepinTasks servertasks	= null;
	protected static SubscriptionManagerTasks clienttasks	= null;
	protected static SubscriptionManagerTasks client1tasks	= null;	// client1 subscription manager tasks
	protected static SubscriptionManagerTasks client2tasks	= null;	// client2 subscription manager tasks
	
	protected Random randomGenerator = new Random(System.currentTimeMillis());
	
	public SubscriptionManagerTestScript() {
		super();
		// TODO Auto-generated constructor stub
	}


	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeSuite(groups={"setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws ParseException, IOException{
	
		client = new SSHCommandRunner(clienthostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		
		// will we be connecting to the candlepin server?
		if (!(	serverHostname.equals("") || serverHostname.startsWith("$") ||
				serverInstallDir.equals("") || serverInstallDir.startsWith("$") )) {
			server = new SSHCommandRunner(serverHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			servertasks = new com.redhat.qe.sm.tasks.CandlepinTasks(server,serverInstallDir);

		} else {
			log.info("Assuming the server is already setup and running.");
		}
		
		// will we be testing multiple clients?
		if (!(	client2hostname.equals("") || client2hostname.startsWith("$") ||
				client2username.equals("") || client2username.startsWith("$") ||
				client2password.equals("") || client2password.startsWith("$") )) {
			client1 = client;
			client1tasks = clienttasks;
			client2 = new SSHCommandRunner(client2hostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			client2tasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client2);
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// setup the server
		if (server!=null && isServerOnPremises) {
			servertasks.updateConfigFileParameter("pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule","0 0\\/2 * * * ?");
			servertasks.cleanOutCRL();
			if (deployServerOnPremises)
				servertasks.deploy(serverImportDir,serverBranch);
			
			// also connect to the candlepin server database
			connectToDatabase();  // do this after the call to deploy since it will restart postgresql
		}
		
		// in the event that the clients are already registered from a prior run, unregister them
		unregisterClientsAfterSuite();
		
		// setup the client(s)
		if (installRPM) client1tasks.installSubscriptionManagerRPM(urlToRPM,enablerepofordeps);
		client1tasks.updateConfigFileParameter("hostname", serverHostname);
		client1tasks.updateConfigFileParameter("port", serverPort);
		client1tasks.updateConfigFileParameter("insecure", "1");
		client1tasks.changeCertFrequency(certFrequency,false);
		client1tasks.cleanOutAllCerts();
		if (client2tasks!=null) if (installRPM) client2tasks.installSubscriptionManagerRPM(urlToRPM,enablerepofordeps);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("hostname", serverHostname);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("port", serverPort);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("insecure", "1");
		if (client2tasks!=null) client2tasks.changeCertFrequency(certFrequency,false);
		if (client2tasks!=null) client2tasks.cleanOutAllCerts();
		// transfer a copy of the CA Cert from the candlepin server to the client
		// TEMPORARY WORK AROUND TO AVOID ISSUES:
		// https://bugzilla.redhat.com/show_bug.cgi?id=617703 
		// https://bugzilla.redhat.com/show_bug.cgi?id=617303
		/*
		if (server!=null && isServerOnPremises) {
			log.warning("TEMPORARY WORKAROUND...");
			RemoteFileTasks.getFile(server.getConnection(), "/tmp","/etc/candlepin/certs/candlepin-ca.crt");
			RemoteFileTasks.putFile(commandRunner.getConnection(), "/tmp/candlepin-ca.crt", "/tmp/", "0644");
		}
		*/
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void unregisterClientsAfterSuite() {
		if (client2tasks!=null) client2tasks.unregister_();	// release the entitlements consumed by the current registration
		if (client1tasks!=null) client1tasks.unregister_();	// release the entitlements consumed by the current registration
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void disconnectDatabaseAfterSuite() {
		
		// close the candlepin database connection
		if (dbConnection!=null) {
			try {
				dbConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	// close the connection to the database
		}
	}

//	protected class UserData {
//		public String username=null;
//		public String password=null;
//		public JSONObject jsonOwner=null;
//		public SSHCommandResult registerResult=null;
//		public List<SubscriptionPool> allAvailableSubscriptionPools=new ArrayList<SubscriptionPool>();
//		public UserData() {
//			super();
//			// TODO Auto-generated constructor stub
//		}
////		public UserData(String username, String password) {
////			super();
////			this.username = username;
////			this.password = password;
////		}
////		public void setUsername(String username) {
////			this.username = username;
////		}
////		public void setPassword(String password) {
////			this.password = password;
////		}
////		public void setOwnerId(String ownerId) {
////			this.ownerId = ownerId;
////		}
////		public void setRegisterResult(SSHCommandResult registerResult) {
////			this.registerResult = registerResult;
////		}
////		public void setSubscriptionPools(List<SubscriptionPool> subscriptionPools) {
////			this.subscriptionPools = subscriptionPools;
////		}
//	}
//	@BeforeSuite(groups={"setup"}, dependsOnMethods={"setupBeforeSuite"}, description="create a user report table")
//	public void reportUserTableBeforeSuite() {
//		
//		Map<String,Map<String,UserData>> tableMap = new HashMap<String,Map<String,UserData>>();
//		Map<String,UserData> userMap = new HashMap<String,UserData>();
//		
//		// iterate over all of the Usernames and Passwords (FIXME Ideally this is returned from an api call to the candlepin server)
//		List<UserData> userDataList = new ArrayList<UserData>();
//		for (List<Object>  usernameAndPasssword : getUsernameAndPasswordsDataAsListOfLists()) {
//			UserData userData = new UserData();
//			userData.username = (String) usernameAndPasssword.get(0);
//			userData.password = (String) usernameAndPasssword.get(1);
//
//			// determine this user's ability to register
//			userData.registerResult = clienttasks.register_(userData.username, userData.password, ConsumerType.system, null, null, true);
//				
//			// determine this user's available subscriptions
//			if (userData.registerResult.getExitCode()==0) {
//				userData.allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//			}
//			
//			// determine this user's owner
//			if (userData.registerResult.getExitCode()==0) {
//				String uuid = userData.registerResult.getStdout().split(" ")[0];
//				try {
//					JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceREST(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,"/consumers/"+uuid));	
//					JSONObject jsonOwner = (JSONObject) jsonConsumer.getJSONObject("owner");
//					userData.jsonOwner = new JSONObject(CandlepinTasks.getResourceREST(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,jsonOwner.getString("href")));	
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//			}
//			
//			userDataList.add(userData);
//			clienttasks.unregister_();
//		}
//
//		// now dump out the list of userData to a file
//	    //File file = new File("/var/www/html/hudson/"+serverHostname+".UserTable.html");
//	    //File file = new File("test-output/"+serverHostname+".UserTable.html");
//	    File file = new File("test-output/CandlepinUserReport.html");
//	    try {
//	    	Writer output = new BufferedWriter(new FileWriter(file));
//			
//			// write out the rows of the table
//			output.write("<html>\n");
//			output.write("<table border=1>\n");
//			output.write("<h2>Candlepin User Report</h2>\n");
//			output.write("<b>"+serverHostname+"</b>\n");
//			
//			DateFormat dateFormat = new SimpleDateFormat("MMM d HH:mm:ss yyyy z");
//			
//			
//			output.write("(generated on "+dateFormat.format(System.currentTimeMillis())+")\n");
//			output.write("<tr><th>Owner</th><th>User/password</th><th>Registration Output</th><th>All Available Subscriptions</th></tr>\n");
//			for (UserData userData : userDataList) {
//				if (userData.jsonOwner==null) {
//					output.write("<tr bgcolor=#F47777>");
//				} else {output.write("<tr>");}
//				if (userData.jsonOwner!=null) {
//					output.write("<td>"+userData.jsonOwner.getString("key")+"</td>");
//				} else {output.write("<td/>");};
//				if (userData.username!=null) {
//					output.write("<td>"+userData.username+"/"+userData.password+"</td>");
//				} else {output.write("<td/>");};
//				if (userData.registerResult!=null) {
//					output.write("<td>"+userData.registerResult.getStdout()+userData.registerResult.getStderr()+"</td>");
//				} else {output.write("<td/>");};
//				if (userData.allAvailableSubscriptionPools!=null) {
//					output.write("<td><ul>");
//					for (SubscriptionPool availableSubscriptionPool : userData.allAvailableSubscriptionPools) {
//						output.write("<li>"+availableSubscriptionPool+"</li>");
//					}
//					output.write("</ul></td>");
//				} else {output.write("<td/>");};
//				output.write("</tr>\n");
//			}
//			output.write("</table>\n");
//			output.write("</html>\n");
//		    output.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	// Protected Methods ***********************************************************************
	
	protected void connectToDatabase() {
		try { 
			// Load the JDBC driver 
			Class.forName(dbSqlDriver);	//	"org.postgresql.Driver" or "oracle.jdbc.driver.OracleDriver"
			
			// Create a connection to the database
			String url = dbSqlDriver.contains("postgres")? 
					"jdbc:postgresql://" + dbHostname + ":" + dbPort + "/" + dbName :
					"jdbc:oracle:thin:@" + dbHostname + ":" + dbPort + ":" + dbName ;
			log.info(String.format("Attempting to connect to database with url and credentials: url=%s username=%s password=%s",url,dbUsername,dbPassword));
			dbConnection = DriverManager.getConnection(url, dbUsername, dbPassword); 
			DatabaseMetaData dbmd = dbConnection.getMetaData(); //get MetaData to confirm connection
		    log.fine("Connection to "+dbmd.getDatabaseProductName()+" "+dbmd.getDatabaseProductVersion()+" successful.\n");

		} 
		catch (ClassNotFoundException e) { 
			log.warning("JDBC driver not found!:\n" + e.getMessage());
		} 
		catch (SQLException e) {
			log.warning("Could not connect to backend database:\n" + e.getMessage());
		}
	}

	/* DELETEME  OLD CODE FROM ssalevan
	
	public void getSalesToEngineeringProductBindings(){
		try {
			String products = itDBConnection.nativeSQL("select * from butt;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("Database query for Sales-to-Engineering product bindings failed!  Traceback:\n"+e.getMessage());
		}
	}
	*/
	

	public static void sleep(long milliseconds) {
		log.info("Sleeping for "+milliseconds+" milliseconds...");
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	protected int getRandInt(){
		return Math.abs(randomGenerator.nextInt());
	}
	
	
//	public void runRHSMCallAsLang(SSHCommandRunner sshCommandRunner, String lang,String rhsmCall){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; " + rhsmCall);
//	}
//	
//	public void setLanguage(SSHCommandRunner sshCommandRunner, String lang){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang);
//	}
	

	// Protected Inner Data Class ***********************************************************************
	
	protected class RegistrationData {
		public String username=null;
		public String password=null;
		public JSONObject jsonOwner=null;
		public SSHCommandResult registerResult=null;
		public List<SubscriptionPool> allAvailableSubscriptionPools=null;/*new ArrayList<SubscriptionPool>();*/
		public RegistrationData() {
			super();
		}
		public RegistrationData(String username, String password, JSONObject jsonOwner,	SSHCommandResult registerResult, List<SubscriptionPool> allAvailableSubscriptionPools) {
			super();
			this.username = username;
			this.password = password;
			this.jsonOwner = jsonOwner;
			this.registerResult = registerResult;
			this.allAvailableSubscriptionPools = allAvailableSubscriptionPools;
		}
	}
	
	// this list will be populated by subclass ResisterTests.RegisterWithUsernameAndPassword_Test
	protected List<RegistrationData> registrationDataList = new ArrayList<RegistrationData>();	

	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getGoodRegistrationData")
	public Object[][] getGoodRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getGoodRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getGoodRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
//		for (List<Object> registrationDataList : getBogusRegistrationDataAsListOfLists()) {
//			// pull out all of the valid registration data (indicated by an Integer exitCode of 0)
//			if (registrationDataList.contains(Integer.valueOf(0))) {
//				// String username, String password, String type, String consumerId
//				ll.add(registrationDataList.subList(0, 4));
//			}
//			
//		}
// changing to registrationDataList to get all the valid registeredConsumer
		
		for (RegistrationData registeredConsumer : registrationDataList) {
			if (registeredConsumer.registerResult.getExitCode().intValue()==0) {
				ll.add(Arrays.asList(new Object[]{registeredConsumer.username, registeredConsumer.password}));
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getAvailableSubscriptionPoolsData")
	public Object[][] getAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableSubscriptionPoolsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// assure we are registered
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		if (client2tasks!=null)	{
			client2tasks.unregister();
			client2tasks.register(client2username, client2password, null, null, null, null);
		}
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (client2tasks!=null)	{
			client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		}

		// populate a list of all available SubscriptionPools
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		
		return ll;
	}
	

}
