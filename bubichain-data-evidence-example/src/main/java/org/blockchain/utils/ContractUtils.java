package org.blockchain.utils;

import cn.bubi.SDK;
import cn.bubi.blockchain.TransactionService;
import cn.bubi.blockchain.impl.TransactionServiceImpl;
import cn.bubi.encryption.key.PrivateKey;
import cn.bubi.encryption.utils.hex.HexFormat;
import cn.bubi.model.request.AccountGetNonceRequest;
import cn.bubi.model.request.TransactionBuildBlobRequest;
import cn.bubi.model.request.TransactionGetInfoRequest;
import cn.bubi.model.request.TransactionSubmitRequest;
import cn.bubi.model.request.operation.ContractCreateOperation;
import cn.bubi.model.request.operation.ContractInvokeByAssetOperation;
import cn.bubi.model.response.AccountGetNonceResponse;
import cn.bubi.model.response.TransactionBuildBlobResponse;
import cn.bubi.model.response.TransactionGetInfoResponse;
import cn.bubi.model.response.TransactionSubmitResponse;
import cn.bubi.model.response.result.TransactionBuildBlobResult;
import cn.bubi.model.response.result.data.Signature;
import cn.bubi.model.response.result.data.TransactionHistory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.blockchain.dto.req.SubmitTxReq;
import org.blockchain.dto.resp.BlobDataResp;
import org.blockchain.dto.resp.CreateContractResp;
import org.blockchain.entity.SignEntity;
import org.blockchain.enums.SdkErrorCodeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContractUtils {
    private Integer BC_SUCCESS = 0;
    private Integer DEFAULT_DECIMAL = 8;
    private long DEFAULT_GASPRICE = 1000;
    private String chainUrl;

    public ContractUtils(String chainUrl){
        this.chainUrl = chainUrl;
    }

    public CreateContractResp createContract(String initiator,String initiatorPrivateKey, String contractCode, String initInput){
        CreateContractResp createContractRespDto = new CreateContractResp();
        //生成交易Blob
        BlobDataResp blobDataResp = buildBlob(initiator,contractCode,initInput,chainUrl);
        //签名
        List<String> privateKeys = new ArrayList<>();
        privateKeys.add(initiatorPrivateKey);
        List<SignEntity> signBlobs = signBlob(blobDataResp,privateKeys);
        //提交
        SubmitTxReq submitTxReq = new SubmitTxReq();
        submitTxReq.setBlob(blobDataResp.getBlob());
        submitTxReq.setListSigner(signBlobs);
        submitTxReq.setHash(blobDataResp.getHash());
        TransactionSubmitResponse bcResponse = submitTx(chainUrl,submitTxReq);
        if(bcResponse.getErrorCode().equals(SdkErrorCodeEnum.SUCCESS.getCode())){
            //同步查询交易结果
            TransactionHistory transactionHistory = queryTxResult(chainUrl,submitTxReq.getHash());
            if(transactionHistory!=null && BC_SUCCESS.equals(transactionHistory.getErrorCode())) {
                //成功
//                System.out.println("创建合约 查询结果成功hash:" + blobDataResp.getHash());
                JSONArray arrayJson = JSON.parseArray(transactionHistory.getErrorDesc());
                String contractAddress = arrayJson.getJSONObject(0).getString("contract_address");
                createContractRespDto.setContractAddress(contractAddress);
                createContractRespDto.setHash(blobDataResp.getHash());
                return createContractRespDto;
            } else {
                System.out.println("创建合约 结果交易失败：" + "errorCode:" + transactionHistory.getErrorCode() + " hash:"+blobDataResp.getHash());
            }
        }else{
            System.out.println("创建合约 提交交易到blockchain异常" + bcResponse.getErrorCode() + " hash:" + blobDataResp.getHash());
        }
        return null;
    }

    public void invokeContract(String sourceAddress,String sourcePrivateKey, String contractAddress, JSONObject input){
        //生成交易Blob
        BlobDataResp blobDataResp = buildInvokeContractBlob(sourceAddress,contractAddress,input,chainUrl);
        //签名
        List<String> privateKeys = new ArrayList<>();
        privateKeys.add(sourcePrivateKey);
        List<SignEntity> signBlobs = signBlob(blobDataResp,privateKeys);
        //提交
        SubmitTxReq submitTxReq = new SubmitTxReq();
        submitTxReq.setBlob(blobDataResp.getBlob());
        submitTxReq.setListSigner(signBlobs);
        submitTxReq.setHash(blobDataResp.getHash());
        TransactionSubmitResponse bcResponse = submitTx(chainUrl,submitTxReq);
        if(bcResponse.getErrorCode().equals(SdkErrorCodeEnum.SUCCESS.getCode())){
            //同步查询交易结果
            TransactionHistory transactionHistory = queryTxResult(chainUrl,submitTxReq.getHash());
            if(transactionHistory!=null && BC_SUCCESS.equals(transactionHistory.getErrorCode())) {
                //成功
//                System.out.println("调用合约成功:" + JSON.toJSONString(transactionHistory, true));

            } else {
                System.out.println("调用合约 结果交易失败：" + "errorCode:" + transactionHistory.getErrorCode());
            }
        }else{
            System.out.println("调用合约 提交交易到blockchain异常" + bcResponse.getErrorCode());
        }
    }

    private BlobDataResp buildInvokeContractBlob(String sourceAddress, String contractAddress, JSONObject input, String chainUrl) {
        ContractInvokeByAssetOperation operation = new ContractInvokeByAssetOperation();
        operation.setSourceAddress(sourceAddress);
        operation.setContractAddress(contractAddress);
        operation.setInput(input.toJSONString());
        //获取交易nonce
        long accNonce = getAccountNonce(chainUrl,sourceAddress)+1;
        TransactionBuildBlobRequest transactionBuildBlobRequest = new TransactionBuildBlobRequest();
        transactionBuildBlobRequest.setSourceAddress(sourceAddress);

        transactionBuildBlobRequest.setNonce(accNonce);
        Long txFee = amount10Pow("1", DEFAULT_DECIMAL);
        transactionBuildBlobRequest.setFeeLimit(txFee);
        transactionBuildBlobRequest.setGasPrice(DEFAULT_GASPRICE);
        transactionBuildBlobRequest.addOperation(operation);
        // 获取交易BLob串
        TransactionService transactionService = new TransactionServiceImpl();
        TransactionBuildBlobResponse transactionBuildBlobResponse = transactionService.buildBlob(transactionBuildBlobRequest);
        TransactionBuildBlobResult transactionBuildBlobResult = transactionBuildBlobResponse.getResult();
        BlobDataResp blobData = new BlobDataResp();
        blobData.setBlob(transactionBuildBlobResult.getTransactionBlob());
        blobData.setHash(transactionBuildBlobResult.getHash());
        return blobData;
    }

    private BlobDataResp buildBlob(String initiator, String payLoad, String initInput , String bcUrl){
        ContractCreateOperation operation = new ContractCreateOperation();
        operation.setSourceAddress(initiator);
        operation.setInitBalance(amount10Pow("1", DEFAULT_DECIMAL));
        operation.setPayload(payLoad);
        operation.setInitInput(initInput);
        //获取交易nonce
        long accNonce = getAccountNonce(bcUrl,initiator)+1;
        TransactionBuildBlobRequest transactionBuildBlobRequest = new TransactionBuildBlobRequest();
        transactionBuildBlobRequest.setSourceAddress(initiator);

        transactionBuildBlobRequest.setNonce(accNonce);
        Long txFee = amount10Pow("11", DEFAULT_DECIMAL);
        transactionBuildBlobRequest.setFeeLimit(txFee);
        transactionBuildBlobRequest.setGasPrice(DEFAULT_GASPRICE);
        transactionBuildBlobRequest.addOperation(operation);
        // 获取交易BLob串
        TransactionService transactionService = new TransactionServiceImpl();
        TransactionBuildBlobResponse transactionBuildBlobResponse = transactionService.buildBlob(transactionBuildBlobRequest);
        TransactionBuildBlobResult transactionBuildBlobResult = transactionBuildBlobResponse.getResult();
        BlobDataResp blobData = new BlobDataResp();
        blobData.setBlob(transactionBuildBlobResult.getTransactionBlob());
        blobData.setHash(transactionBuildBlobResult.getHash());
        return blobData;
    }

    public List<SignEntity> signBlob(BlobDataResp blobDataResp, List<String> privateKeys){
        List<SignEntity> listSigner = new ArrayList<SignEntity>();
        for(String privateKey : privateKeys){
            PrivateKey privateObj = new PrivateKey(privateKey);
            byte[] signByte = privateObj.sign(HexFormat.hexStringToBytes(blobDataResp.getBlob()));
            String signBlob = HexFormat.byteToHex(signByte);
            SignEntity entity = new SignEntity();
            entity.setPublicKey(privateObj.getEncPublicKey());
            entity.setSignBlob(signBlob);
            listSigner.add(entity);
        }
        return listSigner;
    }

    public TransactionSubmitResponse submitTx(String bcUrl, SubmitTxReq submitTxReq){
        TransactionSubmitRequest transactionSubmitRequest = new TransactionSubmitRequest();
        transactionSubmitRequest.setTransactionBlob(submitTxReq.getBlob());

        List<SignEntity> listSigner = submitTxReq.getListSigner();
        int length = listSigner.size();
        Signature[] signatures = new Signature[length];
        for(int i=0;i<length;i++){
            SignEntity signEntity = listSigner.get(i);
            Signature signature = new Signature();
            signature.setPublicKey(signEntity.getPublicKey());
            signature.setSignData(signEntity.getSignBlob());
            signatures[i]=signature;
        }

        transactionSubmitRequest.setSignatures(signatures);
        SDK sdk = SDK.getInstance(bcUrl);
        TransactionSubmitResponse transactionSubmitResponse = sdk.getTransactionService().submit(transactionSubmitRequest);
        return transactionSubmitResponse;
    }

    public TransactionHistory queryTxResult(String bcUrl, String hash) {
        TransactionHistory transactionHistory = null;
        int count = 20;
        while (count > 0) {
            try {
                // 循环查询交易结果的次数
                Thread.sleep(1000L);
                transactionHistory = getTransactionByHash(bcUrl,hash);
//                System.out.println("----------------------->:查询交易返回的结果：{"+hash+"},{"+JSON.toJSONString(transactionHistory)+"}");
                if (transactionHistory != null){
                    break;//查询交易成功则跳出
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            count--;
        }
        return transactionHistory;
    }

    public TransactionHistory getTransactionByHash(String bcUrl,String txhash) {
        try{
            SDK sdk = SDK.getInstance(bcUrl);
            TransactionGetInfoRequest request = new TransactionGetInfoRequest();
            request.setHash(txhash);
            TransactionGetInfoResponse response = sdk.getTransactionService().getInfo(request);
            if(SdkErrorCodeEnum.NOT_EXIST.getCode().equals( response.getErrorCode())) {
                return null;
            }
            if(SdkErrorCodeEnum.SUCCESS.getCode().equals( response.getErrorCode())) {
                List<TransactionHistory> listHis = Arrays.asList(response.getResult().getTransactions());
                for (TransactionHistory transactionHistory : listHis) {
                    if(txhash.equals(transactionHistory.getHash())) {
                        return transactionHistory;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Long getAccountNonce(String bcUrl,String address){
        SDK sdk = SDK.getInstance(bcUrl);
        AccountGetNonceRequest request = new AccountGetNonceRequest();
        request.setAddress(address);
        AccountGetNonceResponse response = sdk.getAccountService().getNonce(request);
        return response.getResult().getNonce();
    }


    private long amount10Pow(String str,Integer decimal){
        return (new BigDecimal(str).multiply(new BigDecimal(Math.pow(10,decimal)))).stripTrailingZeros().longValue();
    }
}
