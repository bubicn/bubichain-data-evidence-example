package org.blockchain;

import cn.bubi.SDK;
import cn.bubi.model.request.ContractCallRequest;
import cn.bubi.model.response.ContractCallResponse;
import cn.bubi.model.response.result.ContractCallResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.blockchain.dto.resp.CreateContractResp;
import org.blockchain.utils.ContractUtils;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class TestContract {

    //底层链地址
    String chainUrl = "http://192.168.6.123:19333";
    //合约发布人
    String initiator = "adxSYmvoQXGc831Swotbah5udGZWoqQjSZpmY";
    //合约发布人私钥
    String initiatorPrivateKey = "privbtacCSANUxPn8sXU358Wz3HfXArvmcy7aT9SDdrbPEkYJVGpinUG";
    //合约地址
    String contractAddress ;

    // hash
    String hash ;

    /**
     * 创建合约
     */
    @Test(priority = 0,description = "部署合约")
    public void createContract() {

        //读取合约源代码
        String contractCode = readFile("BPC66.js");
        //合约初始化参数
        String initInput = "{}";
        ContractUtils contractUtils = new ContractUtils(chainUrl);
        CreateContractResp contract = contractUtils.createContract(initiator, initiatorPrivateKey, contractCode, initInput);
        if(contract != null) System.out.println("合约地址："+contract.getContractAddress() + ", hash: " + contract.getHash());
        contractAddress = contract.getContractAddress();

    }
    /**
     * 调用合约：存证
     */
    @Test(priority = 1,description = "存证")
    public void depositData() {

        String data = generateRandomString();
        hash = getSHA256Hash(data);
        // Init input
        JSONObject input = new JSONObject();
        input.put("method", "depositData");
        JSONObject params = new JSONObject();
        params.put("hash", hash);
        params.put("data", data);
        input.put("params", params);

        ContractUtils contractUtils = new ContractUtils(chainUrl);
        contractUtils.invokeContract(initiator, initiatorPrivateKey, contractAddress, input);

        System.out.println("存证数据：" + data);
    }
    /**
     * 调用合约：查询存证内容
     */
    @Test(priority = 2,description = "查询存证内容")
    public void queryDepositData() {
        SDK sdk = SDK.getInstance(chainUrl);

        // Init input
        JSONObject input = new JSONObject();
        input.put("method", "queryDepositData");
        JSONObject params = new JSONObject();
        params.put("hash", hash);
        input.put("params", params);

        // Init request
        ContractCallRequest request = new ContractCallRequest();
        request.setContractAddress(contractAddress);
        request.setFeeLimit(10000L);
        request.setOptType(2);
        request.setInput(input.toJSONString());

        // Call call
        ContractCallResponse response = sdk.getContractService().call(request);
        if (response.getErrorCode() == 0) {
            ContractCallResult result = response.getResult();
            System.out.println("查询到存证内容：" + JSON.toJSONString(result.getQueryRets(), true));
        } else {
            System.out.println("error: " + response.getErrorDesc());
        }
    }
    /**
     * 读取合约源文件
     * @param path：文件名称
     * @return
     */
    public String readFile(String path){
        StringBuilder fileContent = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try{
            inputStream = TestContract.class.getClassLoader().getResourceAsStream(path);
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line);
                fileContent.append(System.lineSeparator()); // 保持原始文件的换行符
            }
            // 打印整个文件内容
            return fileContent.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("读取"+path + "文件失败");
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public String getSHA256Hash(String input) {
        try {
            // 获取SHA-256消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 计算输入字符串的哈希值
            byte[] hashBytes = digest.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public String generateRandomString() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int LENGTH = 8;
        Random random = new Random();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}
