package org.blockchain.dto.req;

import org.blockchain.entity.SignEntity;

import java.util.List;

public class SubmitTxReq {
	private String blob;
	private List<SignEntity> listSigner;
	private String hash;
	private String initiator;
	public String getBlob() {
		return blob;
	}
	public void setBlob(String blob) {
		this.blob = blob;
	}
	public List<SignEntity> getListSigner() {
		return listSigner;
	}
	public void setListSigner(List<SignEntity> listSigner) {
		this.listSigner = listSigner;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getInitiator() {
		return initiator;
	}
	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}
	
}