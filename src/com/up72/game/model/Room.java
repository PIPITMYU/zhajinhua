/*
 * Powered By [up72-framework]
 * Web Site: http://www.up72.com
 * Since 2006 - 2017
 */

package com.up72.game.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 
 * 
 * @author up72
 * @version 1.0
 * @since 1.0
 */
public class Room implements java.io.Serializable {

	private Long id;
	private Integer roomId;
	private Long createId;
	private String createTime;
	private Integer isPlaying;

	/**
	 * 设置的玩家数量
	 */
	private Integer peopleNum;

	/**
	 * 设置底注
	 */
	private Integer initDiFen;

	/**
	 * 比牌信息
	 */
	private Integer biPaiTime;

	/**
	 * 同牌点比较大小的方式
	 */
	private Integer tongPaiBiJiao;

	/**
	 * 特殊的 是否是235管豹子问题
	 */
	private Integer special;
	private Integer tiShi;// 提示
	private Integer clubId;// 俱乐部id

	private String ip;// 当前房间所在服务器的ip

	// 买的总局数
	private Integer circleNum;

	/**
	 * 标识是否是代开模式
	 */
	private Integer extraType;

	public Long getCreateId() {
		return createId;
	}

	public void setCreateId(Long createId) {
		this.createId = createId;
	}

	public Integer getIsPlaying() {
		return isPlaying;
	}

	public void setIsPlaying(Integer isPlaying) {
		this.isPlaying = isPlaying;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getRoomId() {
		return roomId;
	}

	public void setRoomId(Integer roomId) {
		this.roomId = roomId;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public Integer getClubId() {
		return clubId;
	}

	public void setClubId(Integer clubId) {
		this.clubId = clubId;
	}

	public int hashCode() {
		return new HashCodeBuilder().append(getId()).toHashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof Room == false)
			return false;
		if (this == obj)
			return true;
		Room other = (Room) obj;
		return new EqualsBuilder().append(getId(), other.getId()).isEquals();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getTiShi() {
		return tiShi;
	}

	public void setTiShi(Integer tiShi) {
		this.tiShi = tiShi;
	}

	public Integer getCircleNum() {
		return circleNum;
	}

	public void setCircleNum(Integer circleNum) {
		this.circleNum = circleNum;
	}

	public Integer getExtraType() {
		return extraType;
	}

	public void setExtraType(Integer extraType) {
		this.extraType = extraType;
	}

	public Integer getPeopleNum() {
		return peopleNum;
	}

	public void setPeopleNum(Integer peopleNum) {
		this.peopleNum = peopleNum;
	}

	public Integer getInitDiFen() {
		return initDiFen;
	}

	public void setInitDiFen(Integer initDiFen) {
		this.initDiFen = initDiFen;
	}

	public Integer getBiPaiTime() {
		return biPaiTime;
	}

	public void setBiPaiTime(Integer biPaiTime) {
		this.biPaiTime = biPaiTime;
	}

	public Integer getTongPaiBiJiao() {
		return tongPaiBiJiao;
	}

	public void setTongPaiBiJiao(Integer tongPaiBiJiao) {
		this.tongPaiBiJiao = tongPaiBiJiao;
	}

	public Integer getSpecial() {
		return special;
	}

	public void setSpecial(Integer special) {
		this.special = special;
	}
}
