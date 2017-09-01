/*
 * Copyright © 2016 huangshibao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.readOptions.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javassist.expr.NewArray;

public class ConnectorAttributes {
	
	//private String macAddress;
	//private String ipAddress;
	private TpAttributes tpAttributes;
	private String nodeId;
	private String tpId;
	private DataBroker dataBroker;

	public  ConnectorAttributes(String nodeId, String tpId, DataBroker dataBroker){
		this.nodeId = nodeId;
		this.tpId = tpId;
		this.dataBroker = dataBroker;
		this.tpAttributes = new TpAttributes();
	}

	public void readConnectorAttributes(){
		
		InstanceIdentifier<OvsdbTerminationPointAugmentation> IID = InstanceIdentifier.builder(NetworkTopology.class)
		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
		          .child(Node.class, new NodeKey(new NodeId(nodeId)))
		          .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
		          .augmentation(OvsdbTerminationPointAugmentation.class)
		          .build();

  	ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
	  ListenableFuture<Optional<OvsdbTerminationPointAugmentation>> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL,IID);
    Futures.addCallback(dataFuture, new FutureCallback<Optional<OvsdbTerminationPointAugmentation>>() {
        @Override
        public void onSuccess(final Optional<OvsdbTerminationPointAugmentation> result) {
            if(result.isPresent()) {
             // data are present in data store.
//          	  System.out.println(result.get().getOption()+" "+result.get().getValue());
//          	  tpAttributes.setIpAddress(result.get().getValue());
            	
            	List<Options> options = result.get().getOptions();
            	for (Options option :options){
            		if (option.getOption().equals("port_ip")){
            			tpAttributes.setIpAddress(option.getValue());
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
	
	
	public TpAttributes getTpAttributes() {
		return tpAttributes;
	}

	public void setTpAttributes(TpAttributes tpAttributes) {
		this.tpAttributes = tpAttributes;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public DataBroker getDataBroker() {
		return dataBroker;
	}

	public void setDataBroker(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}
	
	

	
	
}


class TpAttributes{
	
	String tpId;
	String macAddress;
	String ipAddress;
	

	public String getTpId() {
		return tpId;
	}
	public void setTpId(String tpId) {
		this.tpId = tpId;
	}
	public String getMacAddress() {
		return macAddress;
	}
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
}
