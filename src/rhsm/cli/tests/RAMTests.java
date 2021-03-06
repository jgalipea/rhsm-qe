package rhsm.cli.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author skallesh
 *
 *
 */

@Test(groups={"RAMTests"})
public class RAMTests extends SubscriptionManagerCLITestScript {
	Map<String, String> factsMap = new HashMap<String, String>();
	int value=(int) 1.049e+6;


	// Test methods ***********************************************************************

	@Test(	description="",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void TODO_Test() throws Exception {
		
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify subscription of Ram/socket based subscription", 
			groups = { "RamSocketSubscription"}, enabled = true)
	public void RamSocketSubscription() throws JSONException,Exception {
		factsMap.put("memory.memtotal", String.valueOf(value*10));
		Integer sockets = 4;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			
		}clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){

				Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
		}
	}
		clienttasks.deleteFactsFileWithOverridingValues("/custom.facts");

 }
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify healing of partially subscribed Ram/socket based subscription", 
			groups = { "RamSocketSubscription","blockedByBug-907638"}, enabled = true)
	public void HealingPartialRamSocketSubscription() throws JSONException,Exception {
		factsMap.put("memory.memtotal", String.valueOf(value*10));
		Integer sockets = 4;
		int healFrequency=2;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		for(SubscriptionPool pool :clienttasks.getCurrentlyAllAvailableSubscriptionPools()){
			if(pool.subscriptionName.contains("RAM/Cores Package")){
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			
			}}clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){

				Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
		}
	}clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
	SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
	for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
		if(installed.productName.contains("RAM")){
			Assert.assertEquals(installed.status.trim(), "Subscribed");
	}}
		clienttasks.deleteFactsFileWithOverridingValues("/custom.facts");

 }
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Ram Subscription with disabled certv3 from candlepin ", 
			groups = { "DisableCertV3ForRamBasedSubscription"}, enabled = false)
	public void DisableCertV3ForRamBasedSubscription() throws JSONException,Exception {
		
		servertasks.updateConfigFileParameter("candlepin.enable_cert_v3", "false");
		servertasks.restartTomcat();
		SubscriptionManagerCLITestScript.sleep( 1*60 * 1000);
		clienttasks.restart_rhsmcertd(null, null, false, null);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){

				Assert.assertEquals(installed.status.trim(), "Not Subscribed");
		}
	}
		
 }
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto Heal for Ram subscription .", 
			groups = { "AutoHealRamBasedSubscription","blockedByBug-907638","blockedByBug-976867"}, enabled = true)
	public void AutoHealRamBasedSubscription() throws JSONException,Exception {
		int healFrequency=2;
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		SubscriptionManagerCLITestScript.sleep(healFrequency * 60 * 1000);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productName.contains("RAM")){
				Assert.assertEquals(installed.status.trim(), "Subscribed");
		}}
		
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto-attach for Ram based subscription", 
			groups = { "AutoSubscribeRamBasedSubscription"}, enabled = true)
	public void AutoSubscribeRamBasedSubscription() throws JSONException,Exception {
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
				for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
					if(installed.productName.contains("RAM")){
						Assert.assertEquals(installed.status.trim(), "Subscribed");
				}else throw new SkipException(
					"Couldnot auto-subscribe ram based subscription");
	}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Partial subscription of Ram subscription. ", 
			groups = { "PartailSubscriptionOfRamBasedSubscription"}, enabled = true)
	public void PartailSubscriptionOfRamBasedSubscription() throws JSONException,Exception {
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			
		}clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){

				Assert.assertEquals(installed.status.trim(), "Subscribed");
		}
			factsMap.put("memory.memtotal", String.valueOf(value*10));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
		}
		
		clienttasks.facts_(true, null, null, null, null);
		clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){

				Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
			}}
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Ram info in product and entitlement certificate", 
			groups = { "RamBasedSubscriptionInfoInEntitlementCert"}, enabled = true)
	public void RamBasedSubscriptionInfoInEntitlementCert() throws JSONException,Exception {
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, false, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null);
		}
		client.runCommandAndWaitWithoutLogging("find "+clienttasks.entitlementCertDir+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String certificates = client.getStdout();
		List<EntitlementCert> ramInfo =parseRamInfo(certificates);
		Assert.assertNotNull(ramInfo.size());
	}
		
		
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify subscription of Ram based subscription", 
			groups = { "SubscribeToRamBasedSubscription","blockedByBug-907315"}, enabled = true)
	public void SubscribeToRamBasedSubscription() throws JSONException,Exception {
		int expected=1;
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		factsMap.put("memory.memtotal", String.valueOf(value*1));
		clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
		int ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			if(pool.subscriptionName.contains("8GB")){
			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
			}}
		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
			if(installed.productId.contains("ram")){
			if(ramvalue<=4){
			Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
			factsMap.put("memory.memtotal", String.valueOf(value*5));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
		}else if(ramvalue>4 && ramvalue<=8){
			expected=2;
			Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
			factsMap.put("memory.memtotal", String.valueOf(value*9));
			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
			clienttasks.facts(null, true, null, null, null);
			ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
		}
		}}
		
	}
	
	static public int KBToGBConverter(int memory) {
		int value=(int) 1.049e+6;
		int result=(memory/value);
		return result;

	}
	
	 public List<SubscriptionPool> getRamBasedSubscriptions() {
		 List<SubscriptionPool> RAMBasedPools= new ArrayList<SubscriptionPool>();
		 for(SubscriptionPool pools:clienttasks.getCurrentlyAvailableSubscriptionPools()){
			 if(pools.subscriptionName.contains("RAM Limiting Package")){
				 RAMBasedPools.add(pools) ;
			 }
		 }
		 
		return RAMBasedPools;
	}
	/* @AfterGroups(groups="setup",value={"DisableCertV3ForRamBasedSubscription"})
	 public void restartTomcatWithCertV3Enabled() {
	 clienttasks.unregister(null, null, null);
	 servertasks.updateConfigFileParameter("candlepin.enable_cert_v3", "true");
	 servertasks.restartTomcat();
	 clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
	 }*/

	 
		static public List<EntitlementCert> parseRamInfo(String rawCertificates) {
			Map<String,String> regexes = new HashMap<String,String>();
			List certData = new ArrayList();
			regexes.put("RAM LIMIT",			"Order:(?:(?:\\n.+)+)RAM Limit: (.+)");

			// split the rawCertificates process each individual rawCertificate
			String rawCertificateRegex = "\\+-+\\+\\n\\s+Entitlement Certificate\\n\\+-+\\+";
			List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
			for (String rawCertificate : rawCertificates.split(rawCertificateRegex)) {
				
				// strip leading and trailing blank lines and skip blank rawCertificates
				rawCertificate = rawCertificate.replaceAll("^\\n*","").replaceAll("\\n*$", "");
				if (rawCertificate.length()==0) continue;
				List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
				for(String field : regexes.keySet()){
					Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
					addRegexMatchesToList(pat, rawCertificate, certDataList, field);
				}
				
				// assert that there is only one group of certData found in the list
				if (certDataList.size()!=1) Assert.fail("Error when parsing raw entitlement certificate.  Expected to parse only one group of certificate data.");
				certData.add(certDataList.get(0));
				
				// create a new EntitlementCert
			}
			return certData;
		}

				
		
		static protected boolean addRegexMatchesToList(Pattern regex, String to_parse, List<Map<String,String>> matchList, String sub_key) {
			boolean foundMatches = false;
			Matcher matcher = regex.matcher(to_parse);
			int currListElem=0;
			while (matcher.find()){
				if (matchList.size() < currListElem + 1) matchList.add(new HashMap<String,String>());
				Map<String,String> matchMap = matchList.get(currListElem);
				matchMap.put(sub_key, matcher.group(1).trim());
				matchList.set(currListElem, matchMap);
				currListElem++;
				foundMatches = true;
			}
	        if (!foundMatches) {
	        	//log.warning("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
	        	log.finer("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
	        }
			return foundMatches;
		}
	// Candidates for an automated Test:
	// TODO http://qe-india.pad.engineering.redhat.com/48?
	/*
	1) Subscribe Ram based subscription 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe RAm based  subscription. 
	Expected Result :
	Single Ram based  subscription should subscribe and no error message should display on console. 
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221898/?from_plan=5846
	Note : Till 4 GB Consumer Ram , only one Ram based subscription should subscribe. 
	Note : For 5 Gb Consumer RAM , Sm should select (contract list) 2 Ram based subscriotion  to subscribe. 
	I'm not 100% sure what these notes mean.  Important to note that for the initial cut of RAM Subs, stacking/multi-entitlement will not be supported. We are still unsure of how this is supposed to work. From what I understand, this *may not* be decided until RHEL 7.
	Execute the same scenario from CLI. 
	*************************************************************************************************************
	2) Ram info in product and entitlement certificate .
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe Ram based subscription .
	Check Product and entitlement cert with rct tool 
	Expected REsult : 
	Ram Info should availbe in product and entitlement cert.
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221899/?from_plan=5846
	*************************************************************************************************************
	3) Partial subscribe Ram subscription. 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe single RAm based  subscription. 
	Client should be in compliant status. 
	Add 5 GB ram in client . 
	Expected REsult : 
	Now client status should be partial subscribe. 
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221900/?from_plan=5846
	*************************************************************************************************************
	4) Auto Heal for Ram subscription .
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM and "Ram based subscription" product cert.
	Steps to verify : 
	Register client to candlepin (No auto subscribe).
	Set Autoheal option for 2 min and restart rhsmcertd service. 
	Expected REsult : 
	After 2 min Auto Heal should subscribe single "Ram based subscription".
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221902/?from_plan=5846
	execute the same scenario for 5 GB Ram.
	Expected Result :  Auto heal should subscribe 2 quantity of Ram based subscription. 
	Stacking/multi-entitlement requirements not yet determined and will likely not be ready for 6.4
	**************************************************************************************************************
	5 : Auto subscribe for RAM based subscription 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM with Product cert (Ram based subscription)
	Steps to verify : 
	Register client to candlepin using autosubscribe option. 
	Expected REsult : 
	Client should register and subscribe single entitlement of "Ram based subscription"
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221904/?from_plan=5846
	**************************************************************************************************************
	There are some additional test cases that should be supported regarding client versions and the entitlement certificate versions that they support.
	- Older clients that do not contain RAM supporting code should not be able to consume a RAM subscription from candlepin.
	- Client compatibility is based on the consumer fact 'system.certificate_version'
	6. An older client with 'system.certificate_version' < 3.1 should be able to list RAM entitlements, but not be able to consume one.
	Prerequisites:
	Install an older subscription manager.
	Verify system.certificate_version fact < 3.1
	OR Verify system.certificate_version fact is not present (rhel57,rhel58,rhel61,rhel62,rhel63)
	Steps to Verify:
	Register client to candlepin, do not autosubscribe.
	List subscriptions - RAM Subscriptions should be present.
	Subscribe to RAM subscription.
	Expected Result:
	A message should be presented to the user stating that the client should be updated in order to use this subscription.
	https://tcms.engineering.redhat.com/case/221914/?from_plan=5846
	==============================================
	7. A client with system.certificate_version = 3.0 recieves a version 3.1 certificate when consuming a non-RAM based subscription
	NOTE: This test case is really verifying that candlepin is creating the appropriate certificates and handing them back to subscription-manager. I'm not sure if this test case fits here or not. I'll let you decide that! :)
	Prerequisites:
	Install an older subscription manager.
	Verify system.certificate_version fact == 3.0
	Steps to Verify:
	Register client to candlepin, do not autosubscribe.
	Subscribe to a NON RAM subscription.
	Expected Results:
	Subscribe operation should be successful.
	Running rct cat-cert on the resulting certificate should show a certificate version of 3.1
	jsefler's questions:
	I assume there is a new RAM  attribute that will appear as an OID in the OrderNamespace?  What will  be the subscription attribute name?
	RAM will only be supported on v3.1 certificates.
	Subscription attribute will be 'ram'
	What is the name of the system fact that contains the RAM?
	System fact that is used for determining RAM is 'memory.memtotal'
	If a system has more RAM than the subscription provides AND the subscription is not multi-entitlement=yes, will the subscription show in the "subscription list --available" ?
	- no support for multi-entitlement/stacking at this point (what I hear is it may not be until RHEL7 - but things often change).
	- I would assume that it would be shown. Same as sockets today.
	Will the TESTDATA contain a new Awesome 2Gig RAM Subscription? that requires a tag that is provided by a TESTDATA generated product cert?
	Test data has been added
	- Product: RAM Limiting Product
	- Subscription: RAM Limiting Package (8GB)
	 */
	
	//[root@jsefler-6 ~]# curl --stderr /dev/null -k -u testuser1:password https://jsefler-f14-6candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f82e3b49b570013b49b691ba055b | python -msimplejson/tool
	//{
	//    "accountNumber": "12331131231", 
	//    "activeSubscription": true, 
	//    "attributes": [], 
	//    "consumed": 0, 
	//    "contractNumber": "133", 
	//    "created": "2012-11-29T01:09:59.866+0000", 
	//    "endDate": "2013-11-28T00:00:00.000+0000", 
	//    "exported": 0, 
	//    "href": "/pools/8a90f82e3b49b570013b49b691ba055b", 
	//    "id": "8a90f82e3b49b570013b49b691ba055b", 
	//    "owner": {
	//        "displayName": "Admin Owner", 
	//        "href": "/owners/admin", 
	//        "id": "8a90f82e3b49b570013b49b58f170002", 
	//        "key": "admin"
	//    }, 
	//    "productAttributes": [
	//        {
	//            "created": "2012-11-29T01:09:59.866+0000", 
	//            "id": "8a90f82e3b49b570013b49b691ba055c", 
	//            "name": "ram", 
	//            "productId": "ram-8gb", 
	//            "updated": "2012-11-29T01:09:59.866+0000", 
	//            "value": "8"
	//        }, 
	//        {
	//            "created": "2012-11-29T01:09:59.866+0000", 
	//            "id": "8a90f82e3b49b570013b49b691bb055d", 
	//            "name": "type", 
	//            "productId": "ram-8gb", 
	//            "updated": "2012-11-29T01:09:59.866+0000", 
	//            "value": "MKT"
	//        }, 
	//        {
	//            "created": "2012-11-29T01:09:59.867+0000", 
	//            "id": "8a90f82e3b49b570013b49b691bb055e", 
	//            "name": "arch", 
	//            "productId": "ram-8gb", 
	//            "updated": "2012-11-29T01:09:59.867+0000", 
	//            "value": "ALL"
	//        }, 
	//        {
	//            "created": "2012-11-29T01:09:59.867+0000", 
	//            "id": "8a90f82e3b49b570013b49b691bb055f", 
	//            "name": "version", 
	//            "productId": "ram-8gb", 
	//            "updated": "2012-11-29T01:09:59.867+0000", 
	//            "value": "1.0"
	//        }, 
	//        {
	//            "created": "2012-11-29T01:09:59.867+0000", 
	//            "id": "8a90f82e3b49b570013b49b691bb0560", 
	//            "name": "variant", 
	//            "productId": "ram-8gb", 
	//            "updated": "2012-11-29T01:09:59.867+0000", 
	//            "value": "ALL"
	//        }
	//    ], 
	//    "productId": "ram-8gb", 
	//    "productName": "RAM Limiting Package (8GB)", 
	//    "providedProducts": [
	//        {
	//            "created": "2012-11-29T01:09:59.867+0000", 
	//            "id": "8a90f82e3b49b570013b49b691bb0561", 
	//            "productId": "801", 
	//            "productName": "RAM Limiting Product", 
	//            "updated": "2012-11-29T01:09:59.867+0000"
	//        }
	//    ], 
	//    "quantity": 10, 
	//    "restrictedToUsername": null, 
	//    "sourceEntitlement": null, 
	//    "startDate": "2012-11-28T00:00:00.000+0000", 
	//    "subscriptionId": "8a90f82e3b49b570013b49b67e6e023d", 
	//    "subscriptionSubKey": "master", 
	//    "updated": "2012-11-29T22:12:20.077+0000"
	//}
	//[root@jsefler-6 ~]# 

//	@BeforeGroups(groups={"setup"}, value={"VerifyEntitlementCertContainsExpectedOIDs_Test"})
//	public void createFactsFileWithOverridingValues() {
//		Map<String,String> factsMap = new HashMap<String,String>();
//		factsMap.put("system.certificate_version", "1.0");
//		clienttasks.createFactsFileWithOverridingValues(factsMap);
//	}
//	@Test(	description="Make sure the entitlement cert contains all expected OIDs",
//			groups={"debugTest","VerifyEntitlementCertContainsExpectedOIDs_Test","AcceptanceTests","blockedByBug-744259","blockedByBug-754426" },
//			dataProvider="getAllAvailableSubscriptionPoolsData",
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)
//	public void VerifyEntitlementCertContainsExpectedOIDs_Test(SubscriptionPool pool) throws JSONException, Exception {
//		// skip RAM-based subscriptions
//		if (CandlepinTasks.getPoolAttributeValue(sm_clientUsername, sm_clientPassword, sm_clientOrg, pool.poolId, "ram")!=null) {
//			if (Float.valueOf(clienttasks.getFactValue("system.certificate_version")) < 3.1) {
//				SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null);
//				Assert.assertEquals(subscribeResult.getStderr().trim(), "Please upgrade to a newer client to use subscription: "+pool.subscriptionName, "Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.1");
//				Assert.assertEquals(subscribeResult.getStdout().trim(), "", "Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.1");
//				Assert.assertEquals(subscribeResult.getExitCode(), new Integer(255), "Exitcode from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.1");
//				throw new SkipException("This test is not designed for RAM-based subscriptions requiring system.certificate_version >= 3.1");
//			}
//		}
//	}
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
