package rhsm.cli.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference Design Doc:
 * https://engineering.redhat.com/trac/Entitlement/wiki/InstanceBasedDesign
 */
@Test(groups={"InstanceTests"})
public class InstanceTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="test compliance using variations on sockets and system type when subscribing to an instance-based subscription",
			groups={"AcceptanceTests","QuantityNeededToAchieveSocketCompliance_Test","blockedByBug-979492"},
			dataProvider="getAvailableInstanceBasedSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void QuantityNeededToAchieveSocketCompliance_Test(Object bugzilla, Boolean systemIsGuest, Integer systemSockets, SubscriptionPool pool) throws NumberFormatException, JSONException, Exception {
		
		// This dataProvided test was inspired by the following table of scenarios
		// https://engineering.redhat.com/trac/Entitlement/wiki/InstanceBasedDesign#Scenarios
		/*
		+--------------------------------------------------------------------------+
		|                     Quantity needed to Achieve Socket Compliance         |
		|--------------------------------------------------------------------------|
		| Sample Systems |   2010 Pricing Sub  |  2013 Pricing Sub (inst-based)    |
		|                |                     |     instance_multiplier=2         |
		|                |  order quantity=10  |     order quantity=10             |
		|                |  pool quantity=10   |     pool quantity=20              |
		|                |---------------------------------------------------------|
		|                |sockets=2 |sockets=4 | sockets=1 | sockets=2 | sockets=4 | 
		|==========================================================================|
		| Physical       |    1*    |     1*   |     2     |     2*    |    2*     |
		| 1 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Physical       |    1     |     1*   |     4     |     2     |    2*     |
		| 2 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Physical       |    4     |     2    |     16    |     8     |    4      |
		| 8 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    1*    |     1*   |     1     |     1*    |    1*     |
		| 1 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    1     |     1*   |     1     |     1     |    1*     |
		| 2 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    4     |     2    |     1     |     1     |    1      |
		| 8 sockets      |          |          |           |           |           |
		+--------------------------------------------------------------------------+
		*/
		
		// make sure we are unsubscribed from all subscriptions
		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
		
		// get some attributes from the subscription pool
		Integer poolInstanceMultiplier = Integer.valueOf(CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"));
		List<String> poolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
		Integer poolSockets = Integer.valueOf(CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets"));
		String poolVirtLimit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_limit");

		// instrument the system facts from the dataProvider
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.clear();
		factsMap.put("virt.is_guest",Boolean.toString(systemIsGuest));
		factsMap.put("cpu.cpu_socket(s)",String.valueOf(systemSockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// update the facts on the system
		clienttasks.facts(null, true, null, null, null);
		
		// predict the quantity needed to achieve compliance
		// think of this using the old 2010 pricing model and then multiply the answer by the poolInstanceMultiplier
		int expectedQuantityToAchieveCompliance = 1;
		while (expectedQuantityToAchieveCompliance*poolSockets < systemSockets) expectedQuantityToAchieveCompliance++;
		expectedQuantityToAchieveCompliance *= poolInstanceMultiplier;
		
		// assert the initial unsubscribed installed product status
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		List<String> providedProductIdsActuallyInstalled = new ArrayList<String>();
		for (String productId : poolProvidedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
			if (installedProduct!=null) {
				providedProductIdsActuallyInstalled.add(installedProduct.productId);
				List<String> expectedStatusDetails = Arrays.asList(new String[]{"Not covered by a valid subscription."});
				if (installedProduct.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
				Assert.assertEquals(installedProduct.status,"Not Subscribed", "Since we have not yet consumed an instance based entitlement, the status of installed product '"+installedProduct.productName+"' should be this value.");
				Assert.assertEquals(installedProduct.statusDetails,expectedStatusDetails,"Since we have not yet consumed an instance based entitlement, the status details of installed product '"+installedProduct.productName+"' is expected to be: "+expectedStatusDetails);
			}
		}
		
		// start subscribe testing
		if (systemIsGuest) {
			
			// virtual systems -----------------------------------------------------------------------------------

			// virtual systems will be allowed to consume 1 entitlement from the instance based pool and be compliant
			// regardless of sockets (this effectively satisfies the "either-or" behavior when a virtual system
			// consumes from the instance based pool - the quantity consumed decrements by one)

			clienttasks.subscribe(false,null,pool.poolId,null,null,"1",null,null,null,null,null);
			
			// assert the installed provided products are compliant
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After attaching 1 instance-based subscription to a virtual system, installed product '"+installedProduct.productName+"' should be immediately compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
			// now let's unsubscribe from all entitlements and attempt auto-subscribing
			clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
			// TEMPORARY WORKAROUND FOR BUG
			String bugId = "964332"; boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				// issue a sacrificial autosubscribe call to get most of the entitlements attached.  If it times out, the post_auto_attach hooks will not get called
				clienttasks.subscribe_(true,null,(String)null,null,null,null,null,null,null,null,null);
			}
			// END OF WORKAROUND
			clienttasks.subscribe(true,null,(String)null,null,null,null,null,null,null,null,null);
			
			// assert the quantity of consumption
			if (!providedProductIdsActuallyInstalled.isEmpty()) {
				ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
				Assert.assertNotNull(productSubscription, "Found a consumed product subscription to '"+pool.subscriptionName+"' after autosubscribing.");
				Assert.assertEquals(productSubscription.quantityUsed,Integer.valueOf(1),"Autosubscribing a virtual system with instance based products installed should only consume 1 quantity from the instance based pool.");
			} else log.warning("There are no installed product ids '"+poolProvidedProductIds+"' to assert compliance status of instance-based subscription '"+pool.subscriptionName+"'.");
			
			// assert the installed provided products are compliant
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After auto-subscribing a virtual system, installed product '"+installedProduct.productName+"' should be immediately compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
		} else {
			
			// physical systems -----------------------------------------------------------------------------------
			
			// physical systems must consume entitlements from the instance based pool in quantities that are evenly
			// divisible by the instance_multiplier.  Moreover, sockets matter for compliance.  In addition, when a
			// physical system consumes from the instance based pool, a subpool with unlimited quantity available only
			// to the guests on this physical system will be generated.
			
			// start by attempting to subscribe in quantities that are NOT evenly divisible by the instance_multiplier
			for (int qty=0; qty<=poolInstanceMultiplier+1; qty++) {
				SSHCommandResult sshCommandResult = clienttasks.subscribe_(false,null,pool.poolId,null,null,String.valueOf(qty),null,null,null,null,null);
				if (qty==0) {
					Assert.assertEquals(sshCommandResult.getStdout().trim(), "Error: Quantity must be a positive integer.", "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which should be an error.");
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which should be an error.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(255), "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which should be an error.");
				} else if (qty%poolInstanceMultiplier!=0) {
					Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Quantity '%s' is not a multiple of instance multiplier '%s'",qty,poolInstanceMultiplier), "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1)/* TODO figure out if this is a bug.  should it be 255?*/, "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
				} else {
					Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Successfully attached a subscription for: %s",pool.subscriptionName), "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+qty+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
				}
			}
			
			// at this point the attempt to attach the instance based subscription should have been successful when the requested quantity was equal to the instance_multiplier
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNotNull(productSubscription, "Found a consumed product subscription to '"+pool.subscriptionName+"' after manually subscribing.");
			Assert.assertEquals(productSubscription.quantityUsed,Integer.valueOf(poolInstanceMultiplier),"The attached quantity of instance based subscription '"+pool.subscriptionName+"' in the list of consumed product subscriptions.");
			if (poolInstanceMultiplier>=expectedQuantityToAchieveCompliance) {	// compliant when true
				Assert.assertTrue(productSubscription.statusDetails.isEmpty(), "Status Details for consumed product subscription '"+productSubscription.productName+"' should be empty.  Actual="+productSubscription.statusDetails);
			} else {
				List<String> expectedStatusDetails = Arrays.asList(new String[]{String.format("Only covers %s of %s sockets.",poolInstanceMultiplier,systemSockets)});
				if (productSubscription.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
				Assert.assertEquals(productSubscription.statusDetails,expectedStatusDetails, "Status Details for consumed product subscription '"+productSubscription.productName+"'.  Expected="+expectedStatusDetails);
			}
			
			// at this point the installed product id should either be "Subscribed" or "Partially Subscribed" since one of the quantity attempts should have succeeded (when qty was equal to poolInstanceMultiplier), let's assert based on the system's sockets
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					if (poolInstanceMultiplier>=expectedQuantityToAchieveCompliance) {	// compliant when true
						Assert.assertEquals(installedProduct.status,"Subscribed", "After manually attaching a quantity of '"+poolInstanceMultiplier+"' subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"', the status of installed product '"+installedProduct.productName+"' on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
						Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
					} else {
						List<String> expectedStatusDetails = Arrays.asList(new String[]{String.format("Only covers %s of %s sockets.",poolInstanceMultiplier,systemSockets)});
						if (installedProduct.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
						Assert.assertEquals(installedProduct.status,"Partially Subscribed", "After manually attaching a quantity of '"+poolInstanceMultiplier+"' subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"', the status of installed product '"+installedProduct.productName+"' on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
						Assert.assertEquals(installedProduct.statusDetails,expectedStatusDetails,"Status Details for installed product '"+installedProduct.productName+" should be this value: "+expectedStatusDetails);
					}
				}
			}
			
			// now let's unsubscribe from all entitlements and attempt auto-subscribing
			clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
			clienttasks.subscribe(true,null,(String)null,null,null,null,null,null,null,null,null);
			
			// assert the total quantity of consumption
			if (!providedProductIdsActuallyInstalled.isEmpty()) {
				List<ProductSubscription> productSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
				Assert.assertTrue(!productSubscriptions.isEmpty(), "Found at lease one consumed product subscription to '"+pool.subscriptionName+"' after auto-subscribing.");
				Integer totalQuantityUsed = 0;
				for (ProductSubscription prodSub : productSubscriptions) {
					totalQuantityUsed += prodSub.quantityUsed;
					Assert.assertTrue(prodSub.statusDetails.isEmpty(),"Status Details of auto-attached subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"' expected to achieve compliance of provided products '"+providedProductIdsActuallyInstalled+"' installed on a physical system with '"+systemSockets+"' cpu_socket(s) should be empty.  Actual="+prodSub.statusDetails);
				}
				Assert.assertEquals(totalQuantityUsed,Integer.valueOf(expectedQuantityToAchieveCompliance),"Quantity of auto-attached subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"' expected to achieve compliance of provided products '"+providedProductIdsActuallyInstalled+"' installed on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
			} else log.warning("There are no installed product ids '"+poolProvidedProductIds+"' to assert compliance status of instance-based subscription '"+pool.subscriptionName+"'.");
			
			// assert the installed provided products are compliant
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After auto-subscribing a physical system, installed product '"+installedProduct.productName+"' should be compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
			// now we can assert that a host_limited subpool was generated from consumption of this physical pool and is only available to guests of this physical system
			// first, let's flip the virt.is_guest to true and assert that the virtual guest subpool is not (yet) available since the virtUuid is not on the host consumer's list of guestIds
			// factsMap.clear(); // do not clear since it will already contain cpu.cpu_socket(s)
			factsMap.put("virt.is_guest",String.valueOf(true));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null,true,null,null,null);
			List<SubscriptionPool> availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
			for (SubscriptionPool availableInstanceBasedSubscriptionPool : availableInstanceBasedSubscriptionPools) {
				Assert.assertEquals(availableInstanceBasedSubscriptionPool.machineType, "Physical", "Only physical pools to '"+pool.subscriptionName+"' should be available to a guest system when its virt_uuid is not on the host's list of guestIds.");
			}
			
			// now fake this consumer's facts and guestIds to make it think it is a guest of itself (a trick for testing)
			String systemUuid = clienttasks.getCurrentConsumerId();
			factsMap.put("virt.uuid",systemUuid);
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null,true,null,null,null);
			//[root@jsefler-5 ~]# curl -k -u testuser1:password --request PUT --data '{"guestIds":["e6f55b91-aae1-44d6-f0db-c8f25ec73ef5","abcd"]}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/d2ee0c6e-a57d-4e37-8be3-228a44ca2739 
			JSONObject jsonConsumer = CandlepinTasks.setGuestIdsForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, systemUuid,Arrays.asList(new String[]{"abc",systemUuid,"def"}));
			
			// now the host_limited subpool for this virtual system should be available
			availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
			availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("machineType", "Virtual", availableInstanceBasedSubscriptionPools);
			Assert.assertTrue(!availableInstanceBasedSubscriptionPools.isEmpty(),"Host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' is available to its guest.");
			Assert.assertEquals(availableInstanceBasedSubscriptionPools.size(),1,"Only one host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' is available to its guest.");
			Assert.assertEquals(availableInstanceBasedSubscriptionPools.get(0).quantity,poolVirtLimit,"The quantity of entitlements from the host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' should be equal to the subscription's virt_limit '"+poolVirtLimit+"'.");
		}
	}
	@AfterGroups(value={"QuantityNeededToAchieveSocketCompliance_Test"},groups={"setup"})
	public void afterQuantityNeededToAchieveSocketCompliance_Test() {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	@DataProvider(name="getAvailableInstanceBasedSubscriptionPoolsData")
	public Object[][] getAvailableInstanceBasedSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableInstanceBasedSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableInstanceBasedSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		Map<String,Integer> poolProductIdsQuantityMap = new HashMap<String,Integer>();
		for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool pool = (SubscriptionPool)(list.get(0));
			
			if (poolProductIdsQuantityMap.containsKey(pool.productId)) {
				
				// the fact that we are here means that there are multiple pools available for the same instance-based product subscription
				// let's try testing cpu_sockets size that will require entitlements from both pools when auto-subscribing
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"963227"/*,"964332"*/}),	false,	Integer.valueOf(Math.max(poolProductIdsQuantityMap.get(pool.productId),Integer.valueOf(pool.quantity)))+2,	pool}));
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"963227"/*,"964332"*/}),	false,	Integer.valueOf(pool.quantity)+poolProductIdsQuantityMap.get(pool.productId),	pool}));

				poolProductIdsQuantityMap.put(pool.productId, Integer.valueOf(pool.quantity)+poolProductIdsQuantityMap.get(pool.productId));

			} else if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				
				// Object bugzilla, Boolean is_guest, String cpu_sockets, SubscriptionPool pool
				ll.add(Arrays.asList(new Object[]{null,	false,	new Integer(1),	pool}));
				ll.add(Arrays.asList(new Object[]{null,	false,	new Integer(2),	pool}));
				ll.add(Arrays.asList(new Object[]{null,	false,	new Integer(5),	pool}));
				ll.add(Arrays.asList(new Object[]{null,	true,	new Integer(1),	pool}));
				ll.add(Arrays.asList(new Object[]{null,	true,	new Integer(2),	pool}));
				ll.add(Arrays.asList(new Object[]{null,	true,	new Integer(5),	pool}));
				
				// keep a quantity map of the instance based pools we are testing
				poolProductIdsQuantityMap.put(pool.productId, Integer.valueOf(pool.quantity));
			}
		}
		return ll;
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
