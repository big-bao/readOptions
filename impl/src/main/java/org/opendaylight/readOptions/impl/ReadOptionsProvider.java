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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
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

    private final DataBroker dataBroker;

	private ListenerRegistration<ReadOptionsProvider> OptionsListener;

	//private ListenerRegistration<ReadOptionsProvider> OptionsListener;
    
    public ReadOptionsProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
    	
    	//监听路径并注册
    	InstanceIdentifier<Options> IID = InstanceIdentifier.builder(NetworkTopology.class)
		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
		          .child(Node.class, new NodeKey(new NodeId("mn1/bridge/s1")))
		          .child(TerminationPoint.class, new TerminationPointKey(new TpId("vxlanport1")))
		          .augmentation(OvsdbTerminationPointAugmentation.class)
		          .child(Options.class)
		          .build();
    	DataTreeIdentifier<Options> Options_path = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, IID);
    	this.OptionsListener = dataBroker.registerDataTreeChangeListener(Options_path, this);									    			

    	
//		//方法一：直接读到List<options>的上一层部分
//    	InstanceIdentifier<OvsdbTerminationPointAugmentation> IID = InstanceIdentifier.builder(NetworkTopology.class)
//		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
//		          .child(Node.class, new NodeKey(new NodeId("mn1/bridge/s1")))
//		          .child(TerminationPoint.class, new TerminationPointKey(new TpId("vxlanport1")))
//		          .augmentation(OvsdbTerminationPointAugmentation.class)
//		          .build();
//    	ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
//    	  ListenableFuture<Optional<OvsdbTerminationPointAugmentation>> dataFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,IID);
//          Futures.addCallback(dataFuture, new FutureCallback<Optional<OvsdbTerminationPointAugmentation>>() {
//              @Override
//              public void onSuccess(final Optional<OvsdbTerminationPointAugmentation> result) {
//                  if(result.isPresent()) {
//                   // data are present in data store.
//                	  List<Options> options = result.get().getOptions();
//                	  for (Options aa:options){
//                		  System.out.println(aa.getOption()+ "  "+ aa.getValue());
//                	  }
//                	  System.out.println(result.get().toString());
//                	  //System.out.println(result.get().getOption()+" "+result.get().getValue());
//                  } else {
//                      // data are not present in data store.
//                	  System.out.println("data are not present in data store.");
//                  }
//              }
//              @Override
//              public void onFailure(final Throwable t) {
//                  // Error during read
//            	  System.out.println("Error during read.");
//              }
//          });

    	
//    	//方法二：直接读到List<Options>的 具体option为“port_ip”的一个值
//    	InstanceIdentifier<Options> IID = InstanceIdentifier.builder(NetworkTopology.class)
//		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
//		          .child(Node.class, new NodeKey(new NodeId("mn1/bridge/s1")))
//		          .child(TerminationPoint.class, new TerminationPointKey(new TpId("vxlanport1")))
//		          .augmentation(OvsdbTerminationPointAugmentation.class)
//		          .child(Options.class, new OptionsKey("port_ip"))
//		          .build();
//    	ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
//  	  ListenableFuture<Optional<Options>> dataFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,IID);
//      Futures.addCallback(dataFuture, new FutureCallback<Optional<Options>>() {
//          @Override
//          public void onSuccess(final Optional<Options> result) {
//              if(result.isPresent()) {
//               // data are present in data store.
//            	  System.out.println(result.get().getOption()+" "+result.get().getValue());
//              } else {
//                  // data are not present in data store.
//            	  System.out.println("data are not present in data store.");
//              }
//          }
//          @Override
//          public void onFailure(final Throwable t) {
//              // Error during read
//        	  System.out.println("Error during read.");
//          }
//      });    	
     
		
        LOG.info("ReadOptionsProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     * 
     */
    public void close(){

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
}