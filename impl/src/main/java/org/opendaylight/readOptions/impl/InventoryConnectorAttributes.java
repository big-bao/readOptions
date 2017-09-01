/*
 * Copyright © 2016 huangshibao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.readOptions.impl;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.readOptions.utils.Constants;
import org.opendaylight.readOptions.utils.OfActionUtils;
import org.opendaylight.readOptions.utils.OfMatchUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javassist.expr.NewArray;

public class InventoryConnectorAttributes {

	// private String macAddress;
	// private String ipAddress;
	private TpAttributes tpAttributes;
	private String tpId;
	private DataBroker dataBroker;

	public InventoryConnectorAttributes(String tpId, DataBroker dataBroker) {
		this.tpId = tpId;
		this.dataBroker = dataBroker;
		this.tpAttributes = new TpAttributes();
	}

	public void readConnectorAttributes() {

		InstanceIdentifier<Nodes> IID = InstanceIdentifier.builder(Nodes.class).build();

		ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
		ListenableFuture<Optional<Nodes>> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL, IID);
		Futures.addCallback(dataFuture, new FutureCallback<Optional<Nodes>>() {
			@Override
			public void onSuccess(final Optional<Nodes> result) {
				if (result.isPresent()) {
					// data are present in data store.
					List<Node> nodes = result.get().getNode();
					for (Node node : nodes) {

						for (int i = 0; i < node.getNodeConnector().size(); i++) {
							if (node.getNodeConnector().get(i).getId().toString().contains("LOCAL")
									&& node.getNodeConnector().get(i).getAugmentation(FlowCapableNodeConnector.class)
											.getName().equals(tpId)) {

								tpAttributes.setMacAddress(
										node.getNodeConnector().get(i).getAugmentation(FlowCapableNodeConnector.class)
												.getHardwareAddress().getValue());
								break;
							}
						}
					}

				} else {
					// data are not present in data store.
					System.out.println("data are not present in data store.");
				}
			}

			@Override
			public void onFailure(final Throwable t) {
				// Error during read
				System.out.println("Error during read.");
			}
		});

	}
	
//private Flow createArpResponseFlow(Node node, String macAddressStr, String ipAddress) {
//    	
//    	
//    	//String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
//    	String nodeName = node.getId().getValue().toString();
//        MatchBuilder matchBuilder = new MatchBuilder();
//        FlowBuilder flowBuilder = new FlowBuilder();
//
//        String flowId = "ArpResponder_" + nodeName + "_" + ipAddress;
//
//        flowBuilder.setId(new FlowId(flowId));
//        FlowKey key = new FlowKey(new FlowId(flowId));
//        flowBuilder.setBarrier(true);
//        flowBuilder.setTableId(flowTableId);
//        flowBuilder.setKey(key);
//        flowBuilder.setPriority(1024);
//        flowBuilder.setFlowName(flowId);
//        flowBuilder.setHardTimeout(0);
//        flowBuilder.setIdleTimeout(0);
//
//        // Instructions List Stores Individual Instructions
//        InstructionsBuilder isb = new InstructionsBuilder();
//        List<Instruction> instructions = Lists.newArrayList();
//        InstructionBuilder ib = new InstructionBuilder();
//        ApplyActionsBuilder aab = new ApplyActionsBuilder();
//        ActionBuilder ab = new ActionBuilder();
//        List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList = Lists
//                .newArrayList();
//
//        /*
//        if (segmentationId != null) {
//            final Long inPort = OfMatchUtils.parseExplicitOFPort(String.valueOf(segmentationId));
//            if (inPort != null) {
//                OfMatchUtils.createInPortMatch(matchBuilder, dpid, inPort);
//            } else {
//                OfMatchUtils.createTunnelIDMatch(matchBuilder, BigInteger.valueOf(segmentationId.longValue()));
//            }
//        }
//        */
//
//        OfMatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(Constants.ARP_ETHERTYPE));
//        OfMatchUtils.createArpDstIpv4Match(matchBuilder,
//               OfMatchUtils.iPv4PrefixFromIPv4Address(ipAddress));
//
//        flowBuilder.setMatch(matchBuilder.build());
//
//        
//		// Move Eth Src to Eth Dst
//		ab.setAction(OfActionUtils.nxMoveEthSrcToEthDstAction());
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Set Eth Src
//		ab.setAction(OfActionUtils.setDlSrcAction(macAddressStr));
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Set ARP OP
//		ab.setAction(OfActionUtils.nxLoadArpOpAction(BigInteger.valueOf(0x02L)));
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Move ARP SHA to ARP THA
//		ab.setAction(OfActionUtils.nxMoveArpShaToArpThaAction());
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Move ARP SPA to ARP TPA
//		ab.setAction(OfActionUtils.nxMoveArpSpaToArpTpaAction());
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Load Mac to ARP SHA
//		ab.setAction(OfActionUtils.nxLoadArpShaAction(macAddressStr));
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Load IP to ARP SPA
//		ab.setAction(OfActionUtils.nxLoadArpSpaAction(ipAddress));
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Output of InPort
//		ab.setAction(OfActionUtils.outputAction(new NodeConnectorId(nodeName + ":INPORT")));
//		ab.setOrder(actionList.size());
//		ab.setKey(new ActionKey(actionList.size()));
//		actionList.add(ab.build());
//
//		// Create Apply Actions Instruction
//		aab.setAction(actionList);
//		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
//		ib.setOrder(instructions.size());
//		ib.setKey(new InstructionKey(instructions.size()));
//		instructions.add(ib.build());
//
//		flowBuilder.setInstructions(isb.setInstruction(instructions).build());
//		return flowBuilder.build();
//    	
//    }

	public TpAttributes getTpAttributes() {
		return tpAttributes;
	}

	public void setTpAttributes(TpAttributes tpAttributes) {
		this.tpAttributes = tpAttributes;
	}

	public DataBroker getDataBroker() {
		return dataBroker;
	}

	public void setDataBroker(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

}
