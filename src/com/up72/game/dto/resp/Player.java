package com.up72.game.dto.resp;

import com.up72.game.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/6/26.
 */
public class Player extends User {

	private Integer roomId;// 房间密码，也是roomSn
	private Integer state;// out离开状态（断线）;inline正常在线
	private String ip;//所在服务器ip 需与加入房间ip一致
	private String notice;// 跑马灯信息
	private Integer playStatus;// dating用户在大厅中; in刚进入房间，等待状态; prepared准备状态; gameing over
	private Long sessionId;
	private Long updateTime;//更新时间 登录时候 如果超过3天就更新一次用户数据
	private Boolean win; //每局的赢家.
	
	public void initPlayer(Integer roomId,Integer playStatus,Boolean win){
		this.roomId = roomId;
		this.playStatus = playStatus;
		this.win = win;
	}
	public Integer getRoomId() {
		return roomId;
	}

	public void setRoomId(Integer roomId) {
		this.roomId = roomId;
	}


	public Integer getState() {
		return state;
	}


	public Boolean getWin() {
		return win;
	}
	public void setWin(Boolean win) {
		this.win = win;
	}
	public void setState(Integer state) {
		this.state = state;
	}
	
	public String getIp() {
		return ip;
	}


	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getNotice() {
		return notice;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}


	public Integer getPlayStatus() {
		return playStatus;
	}


	public void setPlayStatus(Integer playStatus) {
		this.playStatus = playStatus;
	}

	public Long getSessionId() {
		return sessionId;
	}


	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}
	
	public Long getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Long updateTime) {
		this.updateTime = updateTime;
	}
}
