package org.opendaylight.readOptions.impl;



import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.opendaylight.readOptions.utils.Constants;
import org.opendaylight.readOptions.utils.OfActionUtils;
import org.opendaylight.readOptions.utils.OfMatchUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@SuppressWarnings("deprecation")
public class InitialFlowWriter implements DataChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(InitialFlowWriter.class);
	private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();
	private final String FLOW_ID_PREFIX = "L2switch-";
	private final SalFlowService salFlowService;
	private short flowTableId;
	private int flowPriority;
	private DataBroker dataBroker;
	
	private AtomicLong flowIdInc = new AtomicLong();
	
	public InitialFlowWriter(SalFlowService salFlowService, DataBroker dataBroker) {
        this.salFlowService = salFlowService;
        this.dataBroker = dataBroker;
    }
	
	public ListenerRegistration<DataChangeListener> registerAsDataChangeListener(DataBroker dataBroker) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, this, AsyncDataBroker.DataChangeScope.BASE);
    }
	
	@Override
	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		// TODO Auto-generated method stub
		Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
		
        if(createdData !=null && !createdData.isEmpty()) {
            Set<InstanceIdentifier<?>> nodeIds = createdData.keySet();
            if(nodeIds != null && !nodeIds.isEmpty()) {
                initialFlowExecutor.submit(new InitialFlowWriterProcessor(nodeIds));
            }
        }
	}
	
	/**
     * A private class to process the node updated event in separate thread. Allows to release the
     * thread that invoked the data node updated event. Avoids any thread lock it may cause.
     */
    private class InitialFlowWriterProcessor implements Runnable {
        Set<InstanceIdentifier<?>> nodeIds = null;

        public InitialFlowWriterProcessor(Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public void run() {

            if(nodeIds == null) {
                return;
            }

            for(InstanceIdentifier<?> nodeId : nodeIds) {
                if(Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    InstanceIdentifier<Node> invNodeId = (InstanceIdentifier<Node>)nodeId;
                    if(invNodeId.firstKeyOf(Node.class,NodeKey.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(invNodeId);
                    }
                }
            }

        }
    }
    
    /**
     * Adds a flow, which sends all ARP packets to the controller, to the specified node.
     * @param nodeId The node to write the flow on.
     */
    public void addInitialFlows(InstanceIdentifier<Node> nodeId) {
        LOG.debug("adding initial flows for node {} ", nodeId);

        InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
        InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);
        
        Node node = null;
        try {
        	ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
	        Optional<Node> dataObjectOptional = null;
	        dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeId).get();
	        if (dataObjectOptional.isPresent()) {
	            node = (Node) dataObjectOptional.get();
	            List<NodeConnector> nodeConnectors = node.getNodeConnector(); 
	            String nodeName = null;
	            for (int i = 0; i < nodeConnectors.size(); i++){
	            	
	            	//查找LOCAL端口名称，与OVS节点名称相同，用来索引network-topology树的node-id,同时删除该List的local端口
	            	if (nodeConnectors.get(i).getId().toString().contains("LOCAL")){
	            		nodeName = nodeConnectors.get(i).getAugmentation(FlowCapableNodeConnector.class).getName();
	            		nodeConnectors.remove(i);
	            		break;
	            	}	
	            }
	            
	            //获取交换机在network-topology节点名称
	            String tpNodeId = "mn1/bridge/" + nodeName;
	            
				for (NodeConnector nodeConnector : nodeConnectors) {

					//获取opendaylight中的MAC地址
					String macAddressStr = nodeConnector.getAugmentation(FlowCapableNodeConnector.class)
														.getHardwareAddress().getValue();

					//获取tpId,即端口名称
					String tpId = nodeConnector.getAugmentation(FlowCapableNodeConnector.class).getName();

					//读取network-topology里option的ip
					ConnectorAttributes nodAttributes = new ConnectorAttributes(tpNodeId, tpId, dataBroker);
					nodAttributes.readConnectorAttributes();
					String ipAddress = nodAttributes.getTpAttributes().getIpAddress();
					
					//构造并写流表
					writeArpResponseFlow(nodeId, tableId, flowId,
							createArpResponseFlow(node, macAddressStr, ipAddress));
					LOG.debug("Added initial flows for node {} ", nodeId);
	            }
	        }
            readOnlyTransaction.close();
        } catch (InterruptedException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        } catch (ExecutionException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        }
        
    }
    
    private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId) {
        // get flow table key
        TableKey flowTableKey = new TableKey(flowTableId);

        return nodeId.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
    }
    
    private InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId) {
        // generate unique flow key
        FlowId flowId = new FlowId(FLOW_ID_PREFIX+String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);
        return tableId.child(Flow.class, flowKey);
    }

    private Flow createArpResponseFlow(Node node, String macAddressStr, String ipAddress) {
    	
    	
    	//String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
    	String nodeName = node.getId().getValue().toString();
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        String flowId = "ArpResponder_" + nodeName + "_" + ipAddress;

        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(flowTableId);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(1024);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        ActionBuilder ab = new ActionBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList = Lists
                .newArrayList();

        /*
        if (segmentationId != null) {
            final Long inPort = OfMatchUtils.parseExplicitOFPort(String.valueOf(segmentationId));
            if (inPort != null) {
                OfMatchUtils.createInPortMatch(matchBuilder, dpid, inPort);
            } else {
                OfMatchUtils.createTunnelIDMatch(matchBuilder, BigInteger.valueOf(segmentationId.longValue()));
            }
        }
        */

        OfMatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(Constants.ARP_ETHERTYPE));
        OfMatchUtils.createArpDstIpv4Match(matchBuilder,
               OfMatchUtils.iPv4PrefixFromIPv4Address(ipAddress));

        flowBuilder.setMatch(matchBuilder.build());

        
		// Move Eth Src to Eth Dst
		ab.setAction(OfActionUtils.nxMoveEthSrcToEthDstAction());
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Set Eth Src
		ab.setAction(OfActionUtils.setDlSrcAction(macAddressStr));
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Set ARP OP
		ab.setAction(OfActionUtils.nxLoadArpOpAction(BigInteger.valueOf(0x02L)));
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Move ARP SHA to ARP THA
		ab.setAction(OfActionUtils.nxMoveArpShaToArpThaAction());
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Move ARP SPA to ARP TPA
		ab.setAction(OfActionUtils.nxMoveArpSpaToArpTpaAction());
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Load Mac to ARP SHA
		ab.setAction(OfActionUtils.nxLoadArpShaAction(macAddressStr));
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Load IP to ARP SPA
		ab.setAction(OfActionUtils.nxLoadArpSpaAction(ipAddress));
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Output of InPort
		ab.setAction(OfActionUtils.outputAction(new NodeConnectorId(nodeName + ":INPORT")));
		ab.setOrder(actionList.size());
		ab.setKey(new ActionKey(actionList.size()));
		actionList.add(ab.build());

		// Create Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
		ib.setOrder(instructions.size());
		ib.setKey(new InstructionKey(instructions.size()));
		instructions.add(ib.build());

		flowBuilder.setInstructions(isb.setInstruction(instructions).build());
		return flowBuilder.build();
    	
    }
    
    private Future<RpcResult<AddFlowOutput>> writeArpResponseFlow(InstanceIdentifier<Node> nodeInstanceId,
            InstanceIdentifier<Table> tableInstanceId,
            InstanceIdentifier<Flow> flowPath,
            Flow flow) {
		LOG.trace("Adding flow to node {}",nodeInstanceId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
		final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
		builder.setNode(new NodeRef(nodeInstanceId));
		builder.setFlowRef(new FlowRef(flowPath));
		builder.setFlowTable(new FlowTableRef(tableInstanceId));
		builder.setTransactionUri(new Uri(flow.getId().getValue()));
		return salFlowService.addFlow(builder.build());
		}

}
