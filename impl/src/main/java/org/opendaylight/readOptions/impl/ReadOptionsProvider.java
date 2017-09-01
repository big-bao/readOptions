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
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.TpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ReadOptionsProvider implements DataTreeChangeListener<Options> {

    private static final Logger LOG = LoggerFactory.getLogger(ReadOptionsProvider.class);

    private Registration inventoryListenerRegistration = null;
    private ListenerRegistration<ConnectorAttributesListener> topologyListenerRegistration = null;
    private final DataBroker dataBroker;
    private final SalFlowService salFlowService;

	private ListenerRegistration<ReadOptionsProvider> OptionsListener;

	//private ListenerRegistration<ReadOptionsProvider> OptionsListener;
    
    public ReadOptionsProvider(final DataBroker dataBroker,final SalFlowService salFlowService) {
        this.dataBroker = dataBroker;
        this.salFlowService = salFlowService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
    	
    	InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService, dataBroker);
    	inventoryListenerRegistration = initialFlowWriter.registerAsDataChangeListener(dataBroker);
    	
    	ConnectorAttributesListener listenTopologyToInstallFlow = new ConnectorAttributesListener(salFlowService, dataBroker);
    	topologyListenerRegistration = listenTopologyToInstallFlow.registerAsDataChangeListener(dataBroker);

    	
        LOG.info("ReadOptionsProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     * @throws Exception 
     * 
     */
    public void close() throws Exception{

    	if(inventoryListenerRegistration != null) {
    		inventoryListenerRegistration.close();
        }
    	if(topologyListenerRegistration != null) {
    		topologyListenerRegistration.close();
        }
    	
        LOG.info("ReadOptionsProvider Closed");
    }

    /**
     * 监听数据数变化，并作处理
     */
	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<Options>> changes) {
		// TODO Auto-generated method stub
		 for(final DataTreeModification<Options> change: changes){
		        final DataObjectModification<Options> rootChange = change.getRootNode();
		        
		        Options dataBefore = rootChange.getDataBefore();
		        Options dataAfter  = rootChange.getDataAfter();
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
	
	private void readWriteRetry(final int tries, InstanceIdentifier<Options> IID, Options data) {
		WriteTransaction  writeTx = dataBroker.newWriteOnlyTransaction();
        
		
		writeTx.put(LogicalDatastoreType.CONFIGURATION, IID, data);
		
		Futures.addCallback(writeTx.submit(), new FutureCallback<Void>() {
		    @Override
		    public void onSuccess( final Void result){
		        LOG.info("readWriteRetry: transaction succeeded");
		    }
		    @Override
		    public void onFailure(final Throwable t){
		        LOG.error("readWriteRetry: transaction failed");
		        if(t instanceof OptimisticLockFailedException) {
                    if( (tries - 1) > 0 ) {
                        LOG.debug("Concurrent modification of data - trying again");
                        readWriteRetry(tries - 1, IID, data);
                    }
                    else {
                        LOG.error("Concurrent modification of data - out of retries");
                    }
                }
		    }
		});
		

    }
}