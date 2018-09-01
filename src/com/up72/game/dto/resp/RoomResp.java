package com.up72.game.dto.resp;

import java.util.ArrayList;
import java.util.List;

import com.up72.game.constant.Cnst;
import com.up72.game.model.Room;
import com.up72.server.mina.bean.DissolveRoom;

/**
 * Created by Administrator on 2017/7/8.
 */
public class RoomResp extends Room {
	
    private Integer state;//本房间状态，1等待玩家入坐；2游戏中；3小结算
    private Integer lastNum;//房间剩余局数
    private Integer totalNum;//当前第几局
    
    private Integer diFen;//底分 暗牌人最低注数
    private DissolveRoom dissolveRoom;//申请解散信息

    private Long lastUserId;//最后出牌的玩家
    private Integer lastAction;//最后一个人的动作
    private String lastActionExtra;//最后一个人的扩展动作
    
    private List<Integer> lastActions;//所有人上次的操作
    private List<String>  lastActionExtras;//所有人上一次操作的额外数据
    private String cid;  
    
    private Long currentUserId;//当前玩家UID
    private List<Integer> currentUserAction;//当前玩家动作
 
    private Long xjst;//小局开始时间
    private Integer playStatus;//游戏中房间状态
    private Integer createDisId;
    private Integer applyDisId;
    private Integer outNum;//请求大接口的玩家次数
    
    private List<Integer> paiTypes = new ArrayList<Integer>();
    
    private Integer turnNum;//圈数

    private Integer wsw_sole_main_id;//大接口id 暂时没用
    private Integer wsw_sole_action_id;//吃碰杠出牌发牌id
    
    private String openName;//房主id
    
    private Long all; //牌桌上所有的注
   
    private Integer anPaiOperation;//是否有暗牌人操作注数
    
    
    /**
     * 玩家自己下的注
     */
    private List<Long> zhushu; 
    
    /**
     * 这个不仅是UID的集合 同时也可以确认玩家的位置
     */
    private List<Long> playerIds;//玩家id集合

    private List<Long> biPais;
    
    private Integer xiaoJuNum;//每次小局，这个字段++，回放用
    
    private Long winId; //赢家的ID.发牌定位用

    /** 玩家在本小局赢了多少分 或者输了多少分**/
    private List<Long> xiaoJieSuanScore;//小结算的分数

    private Long firstTakePaiUser;//第一个出牌的玩家
    
    /**
     * 玩家手中的牌 所有人都用这个 0-2是第一个 后面以此类推
     */
    private List<Integer> cards;
   
  
    /**
     * 玩家的积分 初始都是0
     */
    private List<Long> playerScores;
    
    /**
     * 玩家是否看牌
     */
    private List<Integer> kanPaiInfo;
    
    private List<Long> chuju;
    
    /**
     * 每一项的次数
     */
    private List<Integer> everyCount;
    
    /**
     * 每一局的小局分数  是小局分数的列表
     */
    private List<List<Long>> xiaoJuFen = new ArrayList<List<Long>>();
    
    public void initRoom(){
    	this.playStatus = Cnst.ROOM_STATE_CREATED;

    	lastAction = null;
    	lastUserId = null;
    	lastActionExtra = null;
    	all = 0l;

    	chuju = new ArrayList<Long>();
    	
    	currentUserAction = null;
    	currentUserId = null;
    	xiaoJieSuanScore = null;
    	anPaiOperation = 0;
    	cards = new ArrayList<Integer>();
    	
    	lastActions = new ArrayList<Integer>();
    	lastActionExtras = new ArrayList<String>();
    	kanPaiInfo = new ArrayList<Integer>();
    	zhushu =  new ArrayList<Long>();
    	diFen = this.getInitDiFen();
    	
    	biPais = new ArrayList<Long>();
    	
    	turnNum = 1;
    	
    	everyCount = new ArrayList<Integer>();
    	
    	if(lastNum.intValue() == getCircleNum().intValue())
    		playerScores = new ArrayList<Long>();
    	
    	for (int i = 0; i < getPeopleNum(); i++) {
    		lastActions.add(-100);
    		lastActionExtras.add(null);
    		kanPaiInfo.add(0);
    		zhushu.add(0l);
    		if(lastNum.intValue() == getCircleNum().intValue())
    			playerScores.add(0l);
		}
    	
    	for (int i = 0; i < 6; i++) {
    		everyCount.add(0);
		}
    }
	
	public Long getWinId() {
		return winId;
	}

	public void setWinId(Long winId) {
		this.winId = winId;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public Integer getState() {
		return state;
	}
	public void setState(Integer state) {
		this.state = state;
	}
	public Integer getLastNum() {
		return lastNum;
	}
	public void setLastNum(Integer lastNum) {
		this.lastNum = lastNum;
	}
	public Integer getTotalNum() {
		return totalNum;
	}
	public void setTotalNum(Integer totalNum) {
		this.totalNum = totalNum;
	}
	public Integer getDiFen() {
		return diFen;
	}
	public void setDiFen(Integer diFen) {
		this.diFen = diFen;
	}
	public DissolveRoom getDissolveRoom() {
		return dissolveRoom;
	}
	public void setDissolveRoom(DissolveRoom dissolveRoom) {
		this.dissolveRoom = dissolveRoom;
	}

	public Long getLastUserId() {
		return lastUserId;
	}
	public void setLastUserId(Long lastUserId) {
		this.lastUserId = lastUserId;
	}
	public Long getXjst() {
		return xjst;
	}
	public void setXjst(Long xjst) {
		this.xjst = xjst;
	}

	public Integer getLastAction() {
		return lastAction;
	}
	public void setLastAction(Integer lastAction) {
		this.lastAction = lastAction;
	}
	
	public Integer getPlayStatus() {
		return playStatus;
	}
	public void setPlayStatus(Integer playStatus) {
		this.playStatus = playStatus;
	}
	public Integer getCreateDisId() {
		return createDisId;
	}
	public void setCreateDisId(Integer createDisId) {
		this.createDisId = createDisId;
	}
	public Integer getApplyDisId() {
		return applyDisId;
	}
	public void setApplyDisId(Integer applyDisId) {
		this.applyDisId = applyDisId;
	}
	public Integer getOutNum() {
		return outNum;
	}
	public void setOutNum(Integer outNum) {
		this.outNum = outNum;
	}
	public Integer getWsw_sole_main_id() {
		return wsw_sole_main_id;
	}
	public void setWsw_sole_main_id(Integer wsw_sole_main_id) {
		this.wsw_sole_main_id = wsw_sole_main_id;
	}
	public Integer getWsw_sole_action_id() {
		return wsw_sole_action_id;
	}
	public void setWsw_sole_action_id(Integer wsw_sole_action_id) {
		this.wsw_sole_action_id = wsw_sole_action_id;
	}
	public String getOpenName() {
		return openName;
	}
	public void setOpenName(String openName) {
		this.openName = openName;
	}
	
	public List<Long> getPlayerIds() {
		return playerIds;
	}
	public void setPlayerIds(List<Long> playerIds) {
		this.playerIds = playerIds;
	}
	public Integer getXiaoJuNum() {
		return xiaoJuNum;
	}
	public void setXiaoJuNum(Integer xiaoJuNum) {
		this.xiaoJuNum = xiaoJuNum;
	}
    
	public String getLastActionExtra() {
		return lastActionExtra;
	}
	public void setLastActionExtra(String lastActionExtra) {
		this.lastActionExtra = lastActionExtra;
	}

	public Long getCurrentUserId() {
		return currentUserId;
	}
	public void setCurrentUserId(Long currentUserId) {
		this.currentUserId = currentUserId;
	}
	public List<Integer> getCurrentUserAction() {
		return currentUserAction;
	}
	public void setCurrentUserAction(List<Integer> currentUserAction) {
		this.currentUserAction = currentUserAction;
	}
	
	public Long getFirstTakePaiUser() {
		return firstTakePaiUser;
	}
	public void setFirstTakePaiUser(Long firstTakePaiUser) {
		this.firstTakePaiUser = firstTakePaiUser;
	}
	
	public List<Long> getXiaoJieSuanScore() {
		return xiaoJieSuanScore;
	}
	public void setXiaoJieSuanScore(List<Long> xiaoJieSuanScore) {
		this.xiaoJieSuanScore = xiaoJieSuanScore;
	}
	
	public List<List<Long>> getXiaoJuFen() {
		return xiaoJuFen;
	}
	public void setXiaoJuFen(List<List<Long>> xiaoJuFen) {
		this.xiaoJuFen = xiaoJuFen;
	}
	
	public void addXiaoJuFen(List<Long> info){
		this.xiaoJuFen.add(info);
	}
	public List<Integer> getLastActions() {
		return lastActions;
	}
	public void setLastActions(List<Integer> lastActions) {
		this.lastActions = lastActions;
	}
	public List<String> getLastActionExtras() {
		return lastActionExtras;
	}
	public void setLastActionExtras(List<String> lastActionExtras) {
		this.lastActionExtras = lastActionExtras;
	}
	public List<Integer> getCards() {
		return cards;
	}
	public void setCards(List<Integer> cards) {
		this.cards = cards;
	}
	public List<Long> getPlayerScores() {
		return playerScores;
	}
	public void setPlayerScores(List<Long> playerScores) {
		this.playerScores = playerScores;
	}
	public Long getAll() {
		return all;
	}
	public void setAll(Long all) {
		this.all = all;
	}
	public List<Integer> getKanPaiInfo() {
		return kanPaiInfo;
	}
	public void setKanPaiInfo(List<Integer> kanPaiInfo) {
		this.kanPaiInfo = kanPaiInfo;
	}
	
	public List<Long> getChuju() {
		return chuju;
	}

	public void setChuju(List<Long> chuju) {
		this.chuju = chuju;
	}

	public List<Long> getZhushu() {
		return zhushu;
	}
	public void setZhushu(List<Long> zhushu) {
		this.zhushu = zhushu;
	}

	public Integer getTurnNum() {
		return turnNum;
	}

	public void setTurnNum(Integer turnNum) {
		this.turnNum = turnNum;
	}

	public List<Integer> getPaiTypes() {
		return paiTypes;
	}

	public void setPaiTypes(List<Integer> paiTypes) {
		this.paiTypes = paiTypes;
	}

	public List<Integer> getEveryCount() {
		return everyCount;
	}

	public void setEveryCount(List<Integer> everyCount) {
		this.everyCount = everyCount;
	}

	public List<Long> getBiPais() {
		return biPais;
	}

	public void setBiPais(List<Long> biPais) {
		this.biPais = biPais;
	}

	public Integer getAnPaiOperation() {
		return anPaiOperation;
	}

	public void setAnPaiOperation(Integer anPaiOperation) {
		this.anPaiOperation = anPaiOperation;
	}

	
	
}
