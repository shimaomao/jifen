package com.gljr.jifen.pojo;

import java.util.Date;

public class StoreOfflineOrder {
    private Integer id;

    private Integer siId;

    private Integer ucId;

    private Integer uid;

    private Integer trxId;

    private String trxCode;

    private String dtchainBlockId;

    private String extOrderId;

    private Integer totalMoney;

    private Integer integral;

    private Integer extCash;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private String name;

    private String description;

    private Integer trxType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSiId() {
        return siId;
    }

    public void setSiId(Integer siId) {
        this.siId = siId;
    }

    public Integer getUcId() {
        return ucId;
    }

    public void setUcId(Integer ucId) {
        this.ucId = ucId;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getTrxId() {
        return trxId;
    }

    public void setTrxId(Integer trxId) {
        this.trxId = trxId;
    }

    public String getTrxCode() {
        return trxCode;
    }

    public void setTrxCode(String trxCode) {
        this.trxCode = trxCode == null ? null : trxCode.trim();
    }

    public String getDtchainBlockId() {
        return dtchainBlockId;
    }

    public void setDtchainBlockId(String dtchainBlockId) {
        this.dtchainBlockId = dtchainBlockId;
    }

    public String getExtOrderId() {
        return extOrderId;
    }

    public void setExtOrderId(String extOrderId) {
        this.extOrderId = extOrderId;
    }

    public Integer getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(Integer totalMoney) {
        this.totalMoney = totalMoney;
    }

    public Integer getIntegral() {
        return integral;
    }

    public void setIntegral(Integer integral) {
        this.integral = integral;
    }

    public Integer getExtCash() {
        return extCash;
    }

    public void setExtCash(Integer extCash) {
        this.extCash = extCash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTrxType() {
        return trxType;
    }

    public void setTrxType(Integer trxType) {
        this.trxType = trxType;
    }
}