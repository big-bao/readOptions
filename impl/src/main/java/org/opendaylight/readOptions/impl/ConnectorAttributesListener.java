/*
 * Copyright © 2016 huangshibao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.readOptions.impl;

import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorAttributesListener implements DataTreeChangeListener<Node> {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectorAttributesListener.class);
	private DataBroker dataBroker;
	private final SalFlowService salFlowService;
	private String ipAddress;
	// private String nodeId;

	public ConnectorAttributesListener(SalFlowService salFlowService, DataBroker dataBroker) {
		// TODO Auto-generated constructor stub
		this.dataBroker = dataBroker;
		this.salFlowService = salFlowService;

	}

	public ListenerRegistration<ConnectorAttributesListener> registerAsDataChangeListener(DataBroker dataBroker) {
		// 监听路径并注册
		InstanceIdentifier<Node> IID = InstanceIdentifier.builder(NetworkTopology.class)
				.child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
				.child(Node.class).build();
		DataTreeIdentifier<Node> Options_path = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, IID);
		return this.dataBroker.registerDataTreeChangeListener(Options_path, this);
	}

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
		// TODO Auto-generated method stub
		String inventoryNodeId = null;
		for (final DataTreeModification<Node> change : changes) {
			
			final DataObjectModification<Node> rootChange = change.getRootNode();
			Node dataBefore = rootChange.getDataBefore();
			Node dataAfter = rootChange.getDataAfter();

			List<TerminationPoint> tPoints = dataAfter.getTerminationPoint();
			for (TerminationPoint tPoint : tPoints) {
				List<Options> options = tPoint.getAugmentation(OvsdbTerminationPointAugmentation.class).getOptions();
				for (Options option : options) {
					if (option.getOption().equals("port_ip")) {
						ipAddress = option.getValue();
						String strTopoNodeId = dataAfter.getNodeId().getValue();
						String[] str = strTopoNodeId.split("/");
						inventoryNodeId = str[2];
					}
				}
			}
		}
		
		InventoryConnectorAttributes inventoryConnectorAttributes = new InventoryConnectorAttributes(inventoryNodeId, dataBroker);
		inventoryConnectorAttributes.readConnectorAttributes();
		String macAddress = inventoryConnectorAttributes.getTpAttributes().getMacAddress();
		
		//构造并写流表
		
		
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

}
