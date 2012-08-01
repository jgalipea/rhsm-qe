package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"HealingTests"})
public class HealingTests extends SubscriptionManagerCLITestScript {
	
	
	// Test methods ***********************************************************************
		/**
		 * @author skallesh
		 * @throws JSONException 
		 * @throws Exception
		 */
	@Test(	description="Auto-heal for partial subscription",
			groups={"autohealPartial"},
			enabled=true)	
	public void VerifyAutohealForPartialSubscription() throws Exception {
		Integer healFrequency=2;
		String originalEntitlementCertDir;
	
		String ProductName=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(String)null,null, null, true,null,null, null, null);
		List <String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool  : clienttasks.getCurrentlyAvailableSubscriptionPools()) {

			if(pool.multiEntitlement){
				poolIds.add(pool.poolId);
				SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, poolIds, null, null,"1", null, null, null, null, null);
			
			}}
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			if( installedProduct.status.toString().equalsIgnoreCase("Subscribed")){
				ProductName =installedProduct.productId;
				}}
		clienttasks.restart_rhsmcertd(null, healFrequency, false,null);
		clienttasks.unsubscribe(true, null, null, null, null); 
		String poolId = null;
		for (SubscriptionPool pool  : clienttasks.getCurrentlyAvailableSubscriptionPools(ProductName,sm_serverUrl)) {
			poolId = pool.poolId;    	
         }
		SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, poolId, null, null,null, null, null, null, null, null);
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertEquals(installedProduct.status, "Partially Subscribed");

			 
		}
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertEquals(installedProduct.status, "Subscribed");
		}
	}
	// Test methods ***********************************************************************
	/**
	 * @author skallesh
	 * @throws JSONException 
	 * @throws Exception
	 */
	@Test(	description="Auto-heal with SLA",
			groups={"AutoHealWithSLA"},
			enabled=true)	
	public void VerifyAutohealWithSLA() throws JSONException, Exception {
		Integer healFrequency=2;
		String availableService = null;	
		String filename=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		for (List<Object> availableServiceLevelData : getAllAvailableServiceLevelDataAsListOfLists()) {
			availableService= ((String)availableServiceLevelData.get(2));
		}
		SSHCommandResult subscribeResult = clienttasks.subscribe(true, availableService, (String)null, null, null,null, null, null, null, null, null);
		SSHCommandResult result = clienttasks.service_level_(null, null, null, null, null,availableService,null,null, null, null);		
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		String originalEntitlementCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "entitlementCertDir");
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		if(certs.size()!=0){
		Assert.assertTrue((certs.size()!=0),"autoheal is succesfull with Service level"+availableService); 
		}else
		Assert.assertTrue((certs.size()==0),"There are no products of serviceLevel "+availableService); 
		clienttasks.moveProductCertFiles(filename,false);
	
}
	
	
	/**
	 * @author skallesh
	 */
	@Test(	description="Auto-heal for subscription",
			groups={"AutoHeal"},
			enabled=true)	
	@ImplementsNitrateTest(caseId=119327)
	
	public void VerifyAutohealForSubscription() {
		Integer healFrequency=2;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		SSHCommandResult subscribeResult = clienttasks.subscribe(true, null, (String)null, null, null,null, null, null, null, null, null);
		//Assert.assertEquals(subscribeResult.getExitCode(), Integer.valueOf(0), "The exit code from the subscribe command indicates a success.");
			
		clienttasks.restart_rhsmcertd(null, healFrequency, true, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		String originalEntitlementCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "entitlementCertDir");
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()!=0),"autoheal is successful");
	
}
	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@Test(	description="Auto-heal with SLA",
			groups={"AutoHealFailFOrSLA"},
			enabled=true)	
	public void VerifyAutohealFailForSLA() throws JSONException, Exception {
		Integer healFrequency=2;
		String availableService = null;	
		String filename=null;
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null);
		for (List<Object> availableServiceLevelData : getAllAvailableServiceLevelDataAsListOfLists()) {
			availableService= ((String)availableServiceLevelData.get(2));
		} //TODO what does this loop do?
		SSHCommandResult subscribeResult = clienttasks.subscribe(true, availableService, (String)null, null, null,null, null, null, null, null, null);
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		for (ProductCert productCert : productCerts) {
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
			
			if(installedProduct.status.toString().equalsIgnoreCase("Subscribed")){
				 filename=installedProduct.productId+".pem";
				clienttasks.moveProductCertFiles(filename,true);
				
			}
		}
		
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null, null, true,null,null, null, null));
		
		SSHCommandResult result = clienttasks.service_level_(null, null, null, null, null,availableService,null,null, null, null);		
		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
		clienttasks.unsubscribe(true, null, null, null, null);
		String originalEntitlementCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "entitlementCertDir");
		SubscriptionManagerCLITestScript.sleep(healFrequency*60*1000);
		
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.size()==0),"autoheal has failed"); 
		clienttasks.moveProductCertFiles(filename,false);
	}
	
	@Test(	description="a new system consumer's autoheal attribute defaults to true (on)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test() throws Exception {
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,true, null, null, null, null));
		
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A new system consumer's autoheal attribute value defaults to true.");
	}
	
	@Test(	description="using the candlepin api, a consumer's autoheal attribute can be toggled off/on",
			groups={},
			dependsOnMethods={"VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeCanBeToggledOffForConsumer_Test() throws Exception {
		
		// get the current registered consumer's id
		String consumerId = clienttasks.getCurrentConsumerId();
		
		JSONObject jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,false);
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");
		jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,true);
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled on (expected value=true).");
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744654 - [ALL LANG] [RHSM CLI]config module_ config Server port with blank or incorrect text produces traceback.
	// TODO Cases in Bug 710172 - [RFE] Provide automated healing of expiring subscriptions
	// TODO   subcase Bug 746088 - autoheal is not super-subscribing on the day the current entitlement cert expires
	// TODO   subcase Bug 746218 - auto-heal isn't working for partial subscription
	// TODO Cases in Bug 726411 - [RFE] Support for certificate healing
	
	
	// Configuration methods ***********************************************************************

	protected Integer configuredHealFrequency = null;
	@BeforeClass (groups="setup")
	public void rememberConfiguredHealFrequency() {
		if (clienttasks==null) return;
		configuredHealFrequency	= Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", "healFrequency"));
	}
	
	@AfterClass (groups="setup")
	public void restoreConfiguredHealFrequency() {
		if (clienttasks==null) return;
		clienttasks.restart_rhsmcertd(null, configuredHealFrequency, false, null);
	}
	
	@AfterClass (groups="setup")
	public void restoreProductCerts() {
		if (clienttasks==null) return;
		clienttasks.moveProductCertFiles(null,false);
	}
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
