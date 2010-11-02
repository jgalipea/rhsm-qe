package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 *  @author ssalevan
 *  @author jsefler
 *
 */
@Test(groups={"list"})
public class ListTests extends SubscriptionManagerCLITestScript{
	
	@Test(	description="subscription-manager-cli: list available entitlements",
//			dependsOnGroups={"sm_stage2"},
//			groups={"sm_stage3"},
			enabled=true)
	@ImplementsTCMS(id="41678")
	public void EnsureAvailableEntitlementsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String availableSubscriptionPools = clienttasks.listAvailableSubscriptionPools().getStdout();
		Assert.assertContainsMatch(availableSubscriptionPools, "Available Subscriptions");
		
		// TODO
		log.warning("TODO: Once known, we still need to assert the following expected results:");
		log.warning(" * List produced matches the known data contained on the Candlepin server");
		log.warning(" * Confirm that the marketing names match.. see prereq link https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/sm-prerequisites");
		log.warning(" * Match the marketing names w/ https://www.redhat.com/products/");
	}
	
	
	@Test(	description="subscription-manager-cli: list consumed entitlements",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4","not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41679")
	public void EnsureConsumedEntitlementsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		String consumedProductSubscriptionsAsString = clienttasks.listConsumedProductSubscriptions().getStdout();
		Assert.assertContainsMatch(consumedProductSubscriptionsAsString, "Consumed Product Subscriptions");
	}
	
	//TODO assert that all of the product entitlement certs in /etc/pki/entitlement/products are present in list --consumed
	@Test(	description="subscription-manager-cli: list consumed entitlements",
			groups={},
			enabled=false)
	//@ImplementsTCMS(id="")
	public void TODOEnsureConsumedEntitlementsListed_Test() {

	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should be the only available subscription to a consumer registered as type person",
			groups={"EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		String RHELPersonalSubscription = getProperty("sm.rhpersonal.productName", "");
		if (RHELPersonalSubscription.equals("")) throw new SkipException("This testcase requires specification of a RHPERSONAL_PRODUCTNAME.");
		
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.person, null, null, null, null);
		
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			if (subscriptionPool.subscriptionName.equals(RHELPersonalSubscription)) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool!=null,RHELPersonalSubscription+" is available to this consumer registered as type person");
		Assert.assertEquals(subscriptionPools.size(),1, RHELPersonalSubscription+" is the ONLY subscription pool available to this consumer registered as type person");
	}
	@AfterGroups(groups={}, value="EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test", alwaysRun=true)
	public void teardownAfterEnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		if (clienttasks!=null) clienttasks.unregister_();
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should not be an available subscription to a consumer registered as type system",
			groups={"EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		String RHELPersonalSubscription = getProperty("sm.rhpersonal.productName", "");
		if (RHELPersonalSubscription.equals("")) throw new SkipException("This testcase requires specification of a RHPERSONAL_PRODUCTNAME.");

		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.system, null, null, null, null);
		SubscriptionPool rhelPersonalPool = null;
		
		rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (subscriptionPool.subscriptionName.equals(RHELPersonalSubscription)) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool==null,RHELPersonalSubscription+" is NOT available to this consumer registered as type system");
		
		// also assert that RHEL Personal is included in --all --available subscription pools
		rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (subscriptionPool.subscriptionName.equals(RHELPersonalSubscription)) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool!=null,RHELPersonalSubscription+" is included in --all --available subscription pools");
	}
	@AfterGroups(groups={}, value="EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test", alwaysRun=true)
	public void teardownAfterEnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		if (clienttasks!=null) clienttasks.unregister_();
	}
	
	@Test(	description="subscription-manager-cli: list installed products",
//			dependsOnGroups={"sm_stage2"},
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureInstalledProductsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);

		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		String installedProductsAsString = clienttasks.listInstalledProducts().getStdout();
		//List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <InstalledProduct> installedProducts = InstalledProduct.parse(installedProductsAsString);

		// assert some stdout
		if (installedProducts.size()>0) {
			Assert.assertContainsMatch(installedProductsAsString, "Installed Product Status");
		}
		
		// assert the number of installed product matches the product certs installed
		Assert.assertEquals(installedProducts.size(), productCerts.size(), "A single product is reported as installed for each product cert found in "+clienttasks.productCertDir);

		// assert that each of the installed product certs are listed in installedProducts as "Not Subscribed"
		for (InstalledProduct installedProduct : installedProducts) {
			boolean foundInstalledProductMatchingProductCert=false;
			for (ProductCert productCert : productCerts) {
				if (installedProduct.productName.equals(productCert.name)) {
					foundInstalledProductMatchingProductCert = true;
					break;
				}
			}
			Assert.assertTrue(foundInstalledProductMatchingProductCert, "The installed product cert for '"+installedProduct.productName+"' is reported by subscription-manager as installed.");
			Assert.assertEquals(installedProduct.status, "Not Subscribed", "A newly registered system should not be subscribed to installed product '"+installedProduct.productName+"'.");
		}

	}
	
	// Data Providers ***********************************************************************
	
	
}
