/**
 * This file was automatically generated by the Mule Development Kit
 */
package org.mule.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mule.DefaultMessageCollection;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

public class MuleRequesterModuleCollectionTest extends FunctionalTestCase
{

	@Override
    protected String getConfigResources()
    {
        return "mule-config-collection.xml";
    }

    @Test
    public void testDefaultRequest() throws Exception
    {
        muleContext.getClient().dispatch("vm://in", "some string", new HashMap<String, Object>());
        runFlowAndExpect("testDefaultRequest", Collections.singletonList("some string"));
    }

    @Test
    public void testDefaultTimeoutForAll() throws Exception
    {
        runFlowAndExpect("testDefaultTimeoutForAll", Collections.EMPTY_LIST);
    }
    
    @Test
    public void testDefaultTimeoutForOne() throws Exception
    {
    	muleContext.getClient().dispatch("vm://delayOne", "some string", new HashMap<String, Object>());
        runFlowAndExpect("testDefaultTimeoutForOne", Collections.singletonList("some string"));
    }

    @Test
    public void testDefaultTimeoutForN() throws Exception
    {
    	List<String> expectedList = new ArrayList<String>();
    	expectedList.add("a");
    	expectedList.add("b");
    	expectedList.add("1");
    	muleContext.getClient().dispatch("vm://delayN", "a", new HashMap<String, Object>());
    	muleContext.getClient().dispatch("vm://delayN", "1", new HashMap<String, Object>());
    	muleContext.getClient().dispatch("vm://delayN", "b", new HashMap<String, Object>());
    	runFlowAndExpectCollection("testDefaultTimeoutForN", expectedList);
    }

    @Test
    public void testCustomTimeoutForAll() throws Exception
    {
        runFlowAndExpect("testCustomTimeoutForAll", Collections.EMPTY_LIST);
    }

    @Test
    public void testTransformer() throws Exception
    {
        muleContext.getClient().dispatch("vm://in", loadResource("resource.txt"), new HashMap<String, Object>());
        runFlowAndExpect("testTransformer", Collections.singletonList("some file resource"));
    }

    @Test
    public void testResourceFromPayload() throws Exception
    {
        muleContext.getClient().dispatch("vm://testpayload", "my payload", new HashMap<String, Object>());
        runFlowAndExpect("testResourceFromPayload",  Collections.singletonList("my payload"));
    }

    @Test
    public void testResourceFromProperty() throws Exception
    {
        muleContext.getClient().dispatch("vm://testproperties", "my property", new HashMap<String, Object>());
        runFlowAndExpect("testResourceFromProperties",  Collections.singletonList("my property"));
    }
    
    @Test
    public void testPropertyPreservedForN() throws Exception
    {
		muleContext.getClient().dispatch("vm://testPropertyPreservedForN", "a", Collections.singletonMap("myproperty", (Object)"1"));
		muleContext.getClient().dispatch("vm://testPropertyPreservedForN", "b", Collections.singletonMap("myproperty", (Object)"2"));
		muleContext.getClient().dispatch("vm://testPropertyPreservedForN", "c", Collections.singletonMap("myproperty", (Object)"3"));
        Flow flow = lookupFlowConstruct("testPropertyPreservedForN");
        MuleEvent event = FunctionalTestCase.getTestEvent(null);
        MuleEvent responseEvent = flow.process(event);
    	assertNotNull(responseEvent.getMessage());
    	MuleMessage[] messages = ((DefaultMessageCollection) responseEvent.getMessage()).getMessagesAsArray();
    	Comparator<MuleMessage> comparator = new Comparator<MuleMessage>() {
			@Override
			public int compare(MuleMessage o1, MuleMessage o2) {
				return ((String)o1.getPayload()).compareTo((String)o2.getPayload());
			}
		};
		Arrays.sort(messages, comparator);
		for (int i = 0; i < messages.length; i++) {
			assertEquals(messages[i].getInboundProperty("myproperty"), String.valueOf(i + 1));
		}
    }
    
    public void testThrowExceptionOnError() throws Exception
    {
        Flow flow = lookupFlowConstruct("testThrowExceptionOnTimeout");
        MuleEvent event = FunctionalTestCase.getTestEvent(null);
        try {
            MuleEvent responseEvent = flow.process(event);
        } catch (Exception e) {
            assertEquals(MessagingException.class, e.getClass());
            assertEquals("No message received in the configured timeout - 1000", e.getCause().getMessage());
        }
    }

   /**
    * Run the flow specified by name and assert equality on the expected output
    *
    * @param flowName The name of the flow to run
    * @param expect The expected output
    */
    protected <T> void runFlowAndExpect(String flowName, T expect) throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = FunctionalTestCase.getTestEvent(null);
        MuleEvent responseEvent = flow.process(event);

        assertEquals(expect, responseEvent.getMessage().getPayload());
    }

    /**
     * Run the flow specified by name and assert equality on the expected output
     *
     * @param flowName The name of the flow to run
     * @param expect The expected output
     */
    protected <T> void runFlowAndExpectCollection(String flowName, List<String> expect) throws Exception
    {
    	Flow flow = lookupFlowConstruct(flowName);
    	MuleEvent event = FunctionalTestCase.getTestEvent(null);
    	MuleEvent responseEvent = flow.process(event);
    	
    	@SuppressWarnings("unchecked")
		List<String> result = (List<String>)responseEvent.getMessage().getPayload();
    	assertNotNull(result);
    	assertEquals("Size of lists don't match", expect.size(), result.size());
    	for (String item : expect) {
			assertTrue(result.contains(item));
		}
    }
    
    /**
     * Run the flow specified by name and assert equality on the expected output
     *
     * @param flowName The name of the flow to run
     * @param expect The expected output
     */
    protected <T> void runFlowAndExpectProperties(String flowName, Map<String,Object> expect) throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = FunctionalTestCase.getTestEvent(null);
        MuleEvent responseEvent = flow.process(event);
        Set<String> keySet = expect.keySet();
        for (String key : keySet) {
            assertTrue("Response does not contain the property "+ key,responseEvent.getMessage().getPropertyNames(PropertyScope.INBOUND).contains(key));
            assertEquals(expect.get(key),responseEvent.getMessage().getProperty(key,PropertyScope.INBOUND));
        }
    }

    /**
    * Run the flow specified by name using the specified payload and assert
    * equality on the expected output
    *
    * @param flowName The name of the flow to run
    * @param expect The expected output
    * @param payload The payload of the input event
    */
    protected <T, U> void runFlowWithPayloadAndExpect(String flowName, T expect, U payload) throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = FunctionalTestCase.getTestEvent(payload);
        MuleEvent responseEvent = flow.process(event);

        assertEquals(expect, responseEvent.getMessage().getPayload());
    }

    /**
     * Retrieve a flow by name from the registry
     *
     * @param name Name of the flow to retrieve
     */
    protected Flow lookupFlowConstruct(String name)
    {
        return (Flow) FunctionalTestCase.muleContext.getRegistry().lookupFlowConstruct(name);
    }
}
