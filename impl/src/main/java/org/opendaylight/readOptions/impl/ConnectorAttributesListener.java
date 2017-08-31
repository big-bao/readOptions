/*
 * Copyright © 2016 huangshibao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.readOptions.impl;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
	private String nodeId;
	private ListenerRegistration<ConnectorAttributesListener> NodeListener;
	
	public ConnectorAttributesListener(String nodeId,DataBroker dataBroker) {
		// TODO Auto-generated constructor stub
		this.dataBroker = dataBroker;
		this.nodeId = nodeId;
		
		//监听路径并注册
    	InstanceIdentifier<Node> IID = InstanceIdentifier.builder(NetworkTopology.class)
		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
		          .child(Node.class, new NodeKey(new NodeId(this.nodeId)))
		          .build();
    	DataTreeIdentifier<Node> Options_path = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, IID);
    	this.NodeListener = this.dataBroker.registerDataTreeChangeListener(Options_path, this);
	}

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
		// TODO Auto-generated method stub
		for(final DataTreeModification<Node> change: changes){
	        final DataObjectModification<Node> rootChange = change.getRootNode();
	        Node dataBefore = rootChange.getDataBefore();
	        Node dataAfter  = rootChange.getDataAfter();
	        switch(rootChange.getModificationType()){

	            case WRITE:
	                LOG.info("Write - before : {} after : {}", dataBefore, dataAfter);
	                break;
	            case SUBTREE_MODIFIED:
	                LOG.info("Write - before : {} after : {}", dataBefore, dataAfter);
	                break;
	            case DELETE:
	                LOG.info("Write - before : {} after : {}", dataBefore, dataAfter);
	                break;
	        }
	    }
	}

}
