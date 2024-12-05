'use strict';

function _saveObj(key, value){
    Chain.store(key, JSON.stringify(value));
}

function queryDepositData(params) {
    let hash= params.hash;
    let data  = JSON.parse(Chain.load(hash));
    Utils.assert(data !== false, '10000,The data hashnot existed');
    return data;
}
function depositData(params){
    Utils.assert(params.hash!== undefined, '10003,The hashparams error');
    let hash= params.hash;
    let data  = JSON.parse(Chain.load(hash));
    Utils.assert(data === false, '10001,The data hash is already existed');
    let dataJson = {};
    dataJson = params.data;
    Chain.store(hash,JSON.stringify(dataJson));
}

function init(input){
    return;
}

function main(input_str){
    let input = JSON.parse(input_str);
    if(input.method === 'depositData'){
        depositData(input.params);
    }
    else{
        throw '<Main interface passes an invalid operation type>';
    }
}

function query(input_str){
    let input  = JSON.parse(input_str);
    let object ={};
    if(input.method === 'queryDepositData'){
        object = queryDepositData(input.params);
    }
    else{
        throw '<unidentified operation type>';
    }
    return JSON.stringify(object);
}