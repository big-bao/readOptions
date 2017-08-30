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
	private List<TpAttributes> tpAttributes;
	private String nodeId;
	private DataBroker dataBroker;
	

	public  ConnectorAttributes(String nodeId, DataBroker dataBroker){
		this.nodeId = nodeId;
		this.dataBroker = dataBroker;
		this.tpAttributes =  new ArrayList<TpAttributes>();
	}

	public void readConnectorAttributes(){
		
		InstanceIdentifier<Node> IID = InstanceIdentifier.builder(NetworkTopology.class)
		          .child(Topology.class, new TopologyKey(new TopologyId("ovsdb:1")))
		          .child(Node.class, new NodeKey(new NodeId(nodeId)))
		          .build();
		
		ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
	  	  ListenableFuture<Optional<Node>> dataFuture = readTx.read(LogicalDatastoreType.CONFIGURATION,IID);
	      Futures.addCallback(dataFuture, new FutureCallback<Optional<Node>>() {
	          @Override
	          public void onSuccess(final Optional<Node> result) {
	              if(result.isPresent()) {
	               // data are present in data store.
	            	
	            	  //判断List<TerminationPoint>是否为空
	            	  List<TerminationPoint> tp = result.get().getTerminationPoint();
	            	  if(tp.isEmpty()){
	            		  
	            		  //List<TerminationPoint>为空
	            		  
	            	  }else{
	            		  
	            		  //List<TerminationPoint>不为空
	            		  for(TerminationPoint tpPoint: tp){
		            		  List<Options> options = tpPoint.getAugmentation(OvsdbTerminationPointAugmentation.class).getOptions();
			            	  if(options.isEmpty()){
			            		  
			            		  //List<Options>为空
			            	  }else{
			            		  
			            		  //List<Options>不为空
			            		  for(Options option:options){
			            			  
			            			  //判断是否为"port_ip"
			            			  if(option.getOption().equals("port_ip")){
			            				  
			            				  //添加到列表
			            				  TpAttributes tA = new TpAttributes();
			            				  tA.setTpId(tpPoint.getTpId().getValue());
			            				  tA.setIpAddress(option.getValue());
			            				  tA.setMacAddress("111111");
			            				  tpAttributes.add(tA);
			            			  }
			            		  }
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
	
	
	public List<TpAttributes> getTpAttributes() {
		return tpAttributes;
	}

	public void setTpAttributes(List<TpAttributes> tpAttributes) {
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
