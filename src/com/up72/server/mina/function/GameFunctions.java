package com.up72.server.mina.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.up72.game.constant.Cnst;
import com.up72.game.dto.resp.Player;
import com.up72.game.dto.resp.RoomResp;
import com.up72.server.mina.bean.DissolveRoom;
import com.up72.server.mina.bean.ProtocolData;
import com.up72.server.mina.main.MinaServerManager;
import com.up72.server.mina.utils.RoomRecordUtil;
import com.up72.server.mina.utils.StringUtils;
import com.up72.server.mina.utils.redis.RedisUtil;
import com.up72.server.mina.utils.dcuse.JieSuan;

/**
 * Created by Administrator on 2017/7/13. 游戏中
 */

public class GameFunctions extends TCPGameFunctions {
	final static Object object = new Object();

	public final static MyCompartor compartor = new GameFunctions().new MyCompartor();
	public final static MyCompartorCareA compartorCareA = new GameFunctions().new MyCompartorCareA();

	/**
	 * 用户点击准备，用在小结算那里，
	 * 
	 * @param session
	 * @param readData
	 */
	public static void interface_100200(IoSession session, Map<String, Object> readData) {
		logger.I("准备,interfaceId -> 100200");

		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));

		String userIdStr = (String) session.getAttribute(Cnst.USER_SESSION_USER_ID);
		Long userId = Long.valueOf(userIdStr);

		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Player currentPlayer = RedisUtil.getPlayerByUserId(userIdStr, cid);
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(currentPlayer.getRoomId()), cid);

		List<Player> players = RedisUtil.getPlayerList(room, cid);

		if (room.getState() == Cnst.ROOM_STATE_GAMIING) {
			return;
		}

		if (room.getLastNum() < 1) {
			return;// 没有次数了 不能准备了
		}

		if (currentPlayer == null || currentPlayer.getPlayStatus() == Cnst.PLAYER_STATE_PREPARED) {
			return;
		}

		// 第一局房主必须最后准备
		boolean checkFirst = true;
		if (room.getLastNum() == room.getCircleNum() && userId.equals(room.getCreateId()) && Cnst.ROOM_TYPE_1 == room.getExtraType()) {
//------------------------------------------------
			//房间人数满了,才可以开局.
//			int num = 0;
//			for (Player p : players) {
//				if (p == null)
//					continue;
//				if (p.getUserId().equals(userId))
//					continue;
//				if (p.getPlayStatus().equals(Cnst.PLAYER_STATE_PREPARED)) {
//					++num;
//				}
//			}
//            //准备的人数,是否小于房间人数-1
//			if (num < room.getPeopleNum() - 1) {
//				// 房主必须最后准备
//				checkFirst = false;
//			}
//----------------------------------------------------
			//房间满2人就可以开局
			if(players.size() >= 2){
				checkFirst = true;
			}
		}

		if (checkFirst) {
			currentPlayer.initPlayer(currentPlayer.getRoomId(), Cnst.PLAYER_STATE_PREPARED, false);
			RedisUtil.setPlayerByUserId(currentPlayer, cid);
			players = RedisUtil.getPlayerList(room, cid);
		}

		boolean allPrepared = true;

		//是否所有人都准备好了
		for (Player p : players) {
			if (p != null && !p.getPlayStatus().equals(Cnst.PLAYER_STATE_PREPARED)) {
				allPrepared = false;
				break;
			}
		}

		// 房间人数满了,才可以开始
		// if (allPrepared && players != null && players.size() == room.getPeopleNum().intValue()) {
		// 有2人就可以开始了 房主模式下
		if (allPrepared && players != null && players.size() >= 2 && Cnst.ROOM_TYPE_1 == room.getExtraType()) {
			// 房间剩余局数 == 总局数
			if (room.getLastNum() == room.getCircleNum()) {
				room.setPeopleNum(players.size());//重新设置玩家实际人数
				createRecord(room, players);
			}
			startGame(room, players, cid);

			RedisUtil.updateRedisData(room, null, cid);
		}

		// 代开模式下,自动开始
		if (allPrepared && players != null && players.size() == room.getPeopleNum().intValue() && Cnst.ROOM_TYPE_2 == room.getExtraType()) {
			// 房间剩余局数 == 总局数
			if (room.getLastNum() == room.getCircleNum()) {
				createRecord(room, players);
			}
			startGame(room, players, cid);

			RedisUtil.updateRedisData(room, null, cid);
		}

		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> userInfo = new ArrayList<Map<String, Object>>();
		// old
		for (Player p : players) {
			if (p == null)
				continue;
			Map<String, Object> i = new HashMap<String, Object>();
			i.put("userId", p.getUserId());
			i.put("playStatus", p.getPlayStatus());
			userInfo.add(i);
		}
		info.put("userInfos", userInfo);
		Map<String, Object> roominfo = new HashMap<String, Object>();
		roominfo.put("state", room.getState());
		roominfo.put("playStatus", room.getPlayStatus());
		info.put("roomInfos", roominfo);
		if (!checkFirst)
			info.put("creatorInfo", 1);

		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());

		for (Player p : players) {
			if (p == null)
				continue;
			IoSession se = MinaServerManager.tcpServer.getSessions().get(p.getSessionId());
			if (se != null && se.isConnected()) {
				se.write(pd);
			}
		}
	}

	/**
	 * 开局发牌
	 * 
	 * @param roomId
	 */
	public static void startGame(RoomResp room, List<Player> players, String cid) {

		// 记录小局次数
		room.setXiaoJuNum(room.getXiaoJuNum() == null ? 0 : room.getXiaoJuNum() + 1);
		// 小局开始时间
		room.setXjst(System.currentTimeMillis());
		Long winId = room.getWinId();// 赢牌人的ID
		room.initRoom();
		// 初始化房间手牌
		List<Integer> cards = new ArrayList<Integer>();
		for (int i = 0; i < Cnst.CARD_ARRAY.length; i++) {
			cards.add(Cnst.CARD_ARRAY[i]);
		}

		if (room.getXiaoJuNum() == 0) {
			int firstIdx = (int) (Math.random() * players.size());
			// 第一局 第一个出牌的人 随机
			room.setFirstTakePaiUser(players.get(firstIdx).getUserId());
		} else {// 第二局出牌的人是第一局赢得下家.
			for (int i = 0; i < room.getPlayerIds().size(); i++) {
				if (winId.equals(room.getPlayerIds().get(i))) {
					if (i == room.getPlayerIds().size() - 1) {
						room.setFirstTakePaiUser(room.getPlayerIds().get(0));
					} else {
						room.setFirstTakePaiUser(room.getPlayerIds().get(i + 1));
					}
				}
			}
		}

		// 直接发牌 每人三张
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			for (int j = 0; j < 3; j++) {
				Integer card = cards.get((int) (Math.random() * (cards.size())));
				cards.remove(card);
				room.getCards().add(card);
			}
		}

		for (int i = 0; i < players.size(); i++) {
			room.getZhushu().set(i, (long) room.getInitDiFen());
			room.getPlayerScores().set(i, room.getPlayerScores().get(i) - room.getInitDiFen());// FIXME
			room.setAll(room.getAll() + room.getInitDiFen());
		}

		// 房间设置为游戏环节
		room.setState(Cnst.ROOM_STATE_GAMIING);
		// 设置游戏环节具体一步
		room.setPlayStatus(Cnst.ROOM_PLAYSTATE_GAMING);
		room.setLastNum(room.getLastNum() - 1);// 房间剩余局数

		List<Integer> actions = new ArrayList<Integer>();

		if (room.getBiPaiTime().intValue() < room.getTurnNum()) {// 圈数
			actions.add(Cnst.ACTION_BIJIAO);
		}
		actions.add(Cnst.ACTION_KAN);
		actions.add(Cnst.ACTION_GENZHU);
		actions.add(Cnst.ACTION_JIAZHU);
		actions.add(Cnst.ACTION_QIPAI);

		room.setCurrentUserAction(actions);
		room.setCurrentUserId(room.getFirstTakePaiUser());

		// 更新 room players
		RedisUtil.setObject(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(room.getRoomId() + ""), room, Cnst.ROOM_LIFE_TIME_COMMON);
		for (Player p : players) {
			p.setPlayStatus(Cnst.PLAYER_STATE_GAME);
		}
		// 如果第一次添加到定时解散任务 并且保存到DB
		if (room.getXiaoJuNum() == 0) {
			notifyDisRoomTask(room, Cnst.DIS_ROOM_TYPE_1);
			addRoomToDB(room, Integer.parseInt(cid));
		}
		RedisUtil.setPlayersList(players, cid);

		{
			// 记录玩家牌面信息
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("interfaceId", "2");
			JSONArray a1 = new JSONArray();
			for (Integer c : room.getCards()) {
				a1.add(c);
			}

			// FIXME 缩写
			map.put("paiInfos", a1);

			map = getNewMap(map);
			addRecord(room, map);
		}
	}

	/**
	 * 请求发牌
	 * 
	 * @param session
	 * @param readData
	 */
	public static void interface_100207(IoSession session, Map<String, Object> readData) {
		logger.I("准备,interfaceId -> 100207");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));

		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId), cid);
		Player player = RedisUtil.getPlayerByUserId(userId + "", cid);
		Integer playStatus = room.getPlayStatus();

		JSONObject roomInfos = new JSONObject();// 封装房间信息
		JSONObject userInfos = new JSONObject();// 封装玩家信息

		int idx = 0;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(idx).longValue() == player.getUserId().longValue()) {
				idx = i;
			}
		}

		JSONObject info = new JSONObject();
		info.put("reqState", Cnst.REQ_STATE_1);
		userInfos.put("playStatus", player.getPlayStatus());
		userInfos.put("state", player.getState());
		userInfos.put("score", room.getPlayerScores().get(idx));
		// userInfos.put("score", -100);
		userInfos.put("use", room.getZhushu().get(idx));
		info.put("userInfos", userInfos);

		roomInfos.put("action", room.getCurrentUserAction());
		roomInfos.put("actionPlayer", room.getCurrentUserId());
		roomInfos.put("playStatus", playStatus);
		roomInfos.put("state", room.getState());
		roomInfos.put("lastNum", room.getLastNum());
		roomInfos.put("all", room.getAll());

		// roominfos.put("playerScores",)
		info.put("roomInfos", roomInfos);

		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);

		// 更新redis roomMap
		RedisUtil.updateRedisData(room, player, cid);

	}

	/**
	 * 玩家动作
	 * 
	 * @param session
	 * @param readData
	 */
	public static void interface_100202(IoSession session, Map<String, Object> readData) throws Exception {
		logger.I("准备,interfaceId -> 100202");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer action = StringUtils.parseInt(readData.get("action"));// 动作
		Player player = RedisUtil.getPlayerByUserId(userId + "", cid);
		if (action == null){
			return;
		}
		RoomResp room = RedisUtil.getRoomRespByRoomId(roomId + "", cid);

		//FIXME 15轮开始比牌
		if(room.getTurnNum() == Cnst.MAX_TURN_NUM ){
		//if(room.getTurnNum() == 4){ //测试用
 			//直接比牌!找到最牌大的人就返回!
			List<Long> playerIds = room.getPlayerIds();
			Long winP =null;
			List<Long> chuju = room.getChuju();
			for (int i = 0; i < playerIds.size(); i++) {
				if(chuju.contains(playerIds.get(i))){
					continue;
				}else{
					if(winP==null){
						winP=playerIds.get(i);
						continue;
					}
				}
				player = RedisUtil.getPlayerByUserId(winP + "", cid);
				winP = biJiao(room, player, playerIds.get(i), readData, cid);
			}
			//RedisUtil.updateRedisData(room, null, cid);
			room.setCurrentUserId(null);
			room.setCurrentUserAction(null);
			
			JieSuan.xiaoJieSuan(room, cid);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("lastAction", room.getLastAction());
			jsonObject.put("lastActionExtra", room.getLastActionExtra());
			jsonObject.put("lastUserId", room.getLastUserId());
			jsonObject.put("currentUserAction", room.getCurrentUserAction());
			jsonObject.put("currentUserId", room.getCurrentUserId());
			jsonObject.put("playStatus", room.getPlayStatus());
			jsonObject.put("all", room.getAll());
			jsonObject.put("lastUserScore", room.getZhushu().get(0));//FIXME分错
			jsonObject.put("state", room.getState());
			jsonObject.put("diFen", room.getDiFen());
			MessageFunctions.interface_100104(jsonObject, room, 100104, cid);
			return;
		}
		
		
		
		// 如果不算游戏中不允许这样操作
		if (room.getState() != Cnst.ROOM_STATE_GAMIING) {
			return;
		}
		
		// ======room
		Integer playStatus = room.getPlayStatus();// 房间状态

		if (Cnst.ROOM_PLAYSTATE_GAMING != playStatus)
			return;// 小结算

		if (room.getCurrentUserAction() == null || !room.getCurrentUserAction().contains(action)) {
			// 没有这个人的操作
			return;
		}

		if (room.getCurrentUserId() == null || !room.getCurrentUserId().equals(userId)) {
			// 没有这个人的操作
			return;
		}

		// 最多玩15轮,找出第15轮中,最后一个玩家的动作.
		// 1,如果是看牌,就没有操作.
		// 2,其他不管是弃牌,比牌,跟注,还是加注,都发起所有人比牌的动作.
		if (action.intValue() == Cnst.ACTION_KAN) {
			handlerKanPai(room, player, readData, cid);
		} else if (action.intValue() == Cnst.ACTION_BIJIAO) {
			handlerBiJiao(room, player, readData, cid);
		} else if (action.intValue() == Cnst.ACTION_GENZHU || action.intValue() == Cnst.ACTION_JIAZHU) {
			handlerGenZhuOrJiaZhu(room, player, readData, action, cid);
		} else if (action.intValue() == Cnst.ACTION_QIPAI) {
			handlerQiPai(room, player, readData, cid);
		}
	}

	/**
	 * 跟注或者加注
	 * 
	 * @param room
	 * @param player
	 * @param readData
	 * @param action
	 */
	private static void handlerGenZhuOrJiaZhu(RoomResp room, Player player, Map<String, Object> readData, Integer action, String cid) {
		int zhu = room.getDiFen();
		int idx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == player.getUserId().longValue()) { // 当前加注人的ID
				idx = i;
				break;
			}
		}

		boolean needSendDiFen = false;
		if (action != Cnst.ACTION_GENZHU) {
			// 加注固定为 2.4.6.8.10
			zhu = StringUtils.parseInt(readData.get("actionExtra"));// 点击加注的传来的分数
			// int max = room.getInitDiFen() << 5;
			int max = 20; // 暗牌最高为10,名牌最高为20
			if (room.getKanPaiInfo().get(idx) == 0) {// 没有看牌,加注最高加到封顶的上一级
				if (zhu > (max / 2)) {
					return;
				}
			} else {
				if (zhu > max) {
					return;
				}
			}

			if (room.getKanPaiInfo().get(idx) == 1) {// 自己看牌了
				room.setDiFen(zhu);
			} else {// 没有看牌
				room.setDiFen(zhu * 2);
			}

		} else {// 跟注的情况
			if (room.getKanPaiInfo().get(idx) != 1) {// 如果自己没有看牌
				if (room.getInitDiFen() < zhu) {
					zhu = zhu / 2;
				} else {
					room.setDiFen(room.getDiFen() * 2);
					needSendDiFen = true;
				}
			}
		}

		for (int i = 0; i < 6; i++) {
			if ((zhu >> i) == room.getInitDiFen()) {
				room.getEveryCount().set(i, room.getEveryCount().get(i) + 1);
				break;
			}
		}

		// 暗牌人操作过了
		if (room.getKanPaiInfo().get(idx) == 0) {
			room.setAnPaiOperation(1);
		}

		room.setLastAction(action);
		room.setLastUserId(room.getCurrentUserId());
		room.setLastActionExtra(zhu + "");

		room.getLastActions().set(idx, room.getLastAction());
		room.getLastActionExtras().set(idx, room.getLastActionExtra());

		// 注数
		room.getZhushu().set(idx, room.getZhushu().get(idx) + zhu);
		room.setAll(room.getAll() + zhu);

		// 积分
		room.getPlayerScores().set(idx, room.getPlayerScores().get(idx) - zhu);
		// 找到下一个人让他选择动作
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			int tmpIdx = (idx + 1 + i) % room.getPlayerIds().size();

			// 如果当前用户是第一个出牌的人,证明完成了一轮,或一圈了.
			if (room.getFirstTakePaiUser().equals(room.getPlayerIds().get(tmpIdx))) {
				room.setTurnNum(room.getTurnNum() + 1);
			}

			if (room.getChuju().contains(room.getPlayerIds().get(tmpIdx))) {
				continue;
			}

			room.setCurrentUserId(room.getPlayerIds().get(tmpIdx));

			ArrayList<Integer> arrayList = new ArrayList<Integer>();

			if (room.getBiPaiTime().intValue() < room.getTurnNum()) {
				arrayList.add(Cnst.ACTION_BIJIAO);
			}
			arrayList.add(Cnst.ACTION_GENZHU);
			arrayList.add(Cnst.ACTION_JIAZHU);
			arrayList.add(Cnst.ACTION_QIPAI);

			if (room.getKanPaiInfo().get(tmpIdx) != 1)
				arrayList.add(Cnst.ACTION_KAN);
			room.setCurrentUserAction(arrayList);
			break;
		}

		RedisUtil.updateRedisData(room, null, cid);

		JSONObject jsonObject = new JSONObject();

		jsonObject.put("lastAction", room.getLastAction());
		jsonObject.put("lastActionExtra", room.getLastActionExtra());
		jsonObject.put("lastUserId", room.getLastUserId());
		jsonObject.put("currentUserAction", room.getCurrentUserAction());
		jsonObject.put("currentUserId", room.getCurrentUserId());
		jsonObject.put("playStatus", room.getPlayStatus());
		jsonObject.put("all", room.getAll());
		jsonObject.put("lastUserScore", room.getZhushu().get(idx));
		jsonObject.put("state", room.getState());

		if (action == Cnst.ACTION_JIAZHU || needSendDiFen) {// 如果是加注，需要修改底注
			jsonObject.put("diFen", room.getDiFen());// room的底注在加注时已经计算,直接使用就行
		}
		MessageFunctions.interface_100104(jsonObject, room, 100104, cid);
	}

	/**
	 * 处理弃牌
	 * 
	 * @param room
	 * @param player
	 * @param readData
	 */
	private static void handlerQiPai(RoomResp room, Player player, Map<String, Object> readData, String cid) {
		int idx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == player.getUserId().longValue()) {
				idx = i;
				break;
			}
		}

		if (idx == -1)
			return;

		room.setLastAction(Cnst.ACTION_QIPAI);
		room.setLastActionExtra(null);
		room.setLastUserId(player.getUserId());
		room.getChuju().add(player.getUserId());

		room.getLastActions().set(idx, room.getLastAction());
		room.getLastActionExtras().set(idx, room.getLastActionExtra());

		// 找到下一个人
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			int tmpIdx = (idx + 1 + i) % room.getPlayerIds().size();
			if (room.getChuju().contains(room.getPlayerIds().get(tmpIdx))) {
				continue;
			}
			room.setCurrentUserId(room.getPlayerIds().get(tmpIdx));

			// 如果当前用户是第一个出牌的人,证明完成了一轮,或一圈了.
			if (room.getCurrentUserId().equals(room.getFirstTakePaiUser())) {
				room.setTurnNum(room.getTurnNum() + 1);
			}

			ArrayList<Integer> arrayList = new ArrayList<Integer>();

			// 判断第几轮才可以比牌
			if (room.getBiPaiTime().intValue() < room.getTurnNum()) {
				arrayList.add(Cnst.ACTION_BIJIAO);
			}

			arrayList.add(Cnst.ACTION_GENZHU);
			arrayList.add(Cnst.ACTION_JIAZHU);
			arrayList.add(Cnst.ACTION_QIPAI);

			if (room.getKanPaiInfo().get(tmpIdx) != 1)
				arrayList.add(Cnst.ACTION_KAN);
			room.setCurrentUserAction(arrayList);
			break;
		}

		if (room.getChuju().size() == room.getPlayerIds().size() - 1) {
			room.setCurrentUserId(null);
			room.setCurrentUserAction(new ArrayList<Integer>());

			JieSuan.xiaoJieSuan(room, cid);

		} else
			RedisUtil.updateRedisData(room, null, cid);

		JSONObject jsonObject = new JSONObject();

		jsonObject.put("lastAction", room.getLastAction());
		jsonObject.put("lastActionExtra", room.getLastActionExtra());
		jsonObject.put("lastUserId", room.getLastUserId());
		jsonObject.put("currentUserAction", room.getCurrentUserAction());
		jsonObject.put("currentUserId", room.getCurrentUserId());
		jsonObject.put("playStatus", room.getPlayStatus());
		jsonObject.put("state", room.getState());
		MessageFunctions.interface_100104(jsonObject, room, 100104, cid);
	}

	
	/**
	 * 所有人一起比较牌的大小,返回牌最大的人.
	 * 
	 * @param room
	 * @param player
	 * @param readData
	 */
	private static Long biJiao(RoomResp room, Player player,Long computerUserId, Map<String, Object> readData, String cid) {
		// 被比牌的人的ID  computerUserId
		int idx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == player.getUserId().longValue()) {
				idx = i;
				break;
			}
		}
		// 开始比较 自己的牌
		List<Integer> list1 = new ArrayList<Integer>();
		// 别人的牌
		List<Integer> list2 = new ArrayList<Integer>();

		for (int i = idx * 3; i < idx * 3 + 3; i++) {
			list1.add(room.getCards().get(i));
		}

		int otherIdx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == computerUserId.longValue()) {
				otherIdx = i;
				break;
			}
		}

		for (int i = otherIdx * 3; i < otherIdx * 3 + 3; i++) {
			list2.add(room.getCards().get(i));
		}

		Collections.sort(list1, compartor);
		Collections.sort(list2, compartor);

		List<List<Integer>> handler1 = handlerCard(list1);
		List<List<Integer>> handler2 = handlerCard(list2);

		int cardType1 = getCardType(handler1);
		int cardType2 = getCardType(handler2);

		long winner = 0l;
		if (cardType1 != cardType2) {
			if (cardType1 < cardType2) {
				// 比牌的人赢
				winner = player.getUserId();

				if (room.getSpecial().intValue() == Cnst.ROOM_SPECIAL_RULE_1 && cardType1 == Cnst.CARD_TYPE_BAOZI && cardType2 == Cnst.CARD_TYPE_SPECIAL) {
					winner = computerUserId;
				}
			} else {
				winner = computerUserId;

				if (room.getSpecial().intValue() == Cnst.ROOM_SPECIAL_RULE_1 && cardType2 == Cnst.CARD_TYPE_BAOZI && cardType1 == Cnst.CARD_TYPE_SPECIAL) {
					winner = player.getUserId();
				}
			}
		} else {
			// 同牌比较
			boolean win = computer(handler1, handler2, cardType1, room.getTongPaiBiJiao());

			if (win)
				winner = player.getUserId();
			else
				winner = computerUserId;
		}

		// 判断是否已经结束
		long loser = computerUserId;
		if (winner == computerUserId){
			loser = player.getUserId();
		}
		room.getChuju().add(loser);

		// 判断剩余的人数
//		if (room.getChuju().size() == room.getPlayerIds().size() - 1) {
//			// 游戏结束
//			room.setCurrentUserId(null);
//			room.setCurrentUserAction(null);
//			JieSuan.xiaoJieSuan(room, cid);
//		} else {
//			//迭代
//		}
		return winner;
	}
	
	/**
	 * 处理比较
	 * 
	 * @param room
	 * @param player
	 * @param readData
	 */
	private static void handlerBiJiao(RoomResp room, Player player, Map<String, Object> readData, String cid) {
		// 被比牌的人的ID
		Long computerUserId = StringUtils.parseLong(readData.get("actionExtra"));// 动作

		if (!room.getPlayerIds().contains(computerUserId))
			return;// 房间没这个人
		if (room.getChuju() != null && room.getChuju().contains(computerUserId))
			return;// 这个人出局了

		// 本人现跟注
		int zhu = room.getDiFen();

		int idx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == player.getUserId().longValue()) {
				idx = i;
				break;
			}
		}
		// 判断是否看牌
		if (room.getKanPaiInfo().get(idx) != 1 && room.getInitDiFen() < zhu) {
			zhu = zhu / 2;
		}

		for (int i = 0; i < 6; i++) {
			if ((zhu >> i) == room.getInitDiFen()) {
				room.getEveryCount().set(i, room.getEveryCount().get(i) + 1);
				break;
			}
		}

		// 注数
		room.getPlayerScores().set(idx, room.getPlayerScores().get(idx) - zhu);
		room.getZhushu().set(idx, room.getZhushu().get(idx) + zhu);
		room.setAll(room.getAll() + zhu);

		if (room.getKanPaiInfo().get(idx) == 0)
			room.setAnPaiOperation(1);

		// 开始比较 自己的牌
		List<Integer> list1 = new ArrayList<Integer>();
		// 别人的牌
		List<Integer> list2 = new ArrayList<Integer>();

		for (int i = idx * 3; i < idx * 3 + 3; i++) {
			list1.add(room.getCards().get(i));
		}

		int otherIdx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getPlayerIds().get(i).longValue() == computerUserId.longValue()) {
				otherIdx = i;
				break;
			}
		}

		for (int i = otherIdx * 3; i < otherIdx * 3 + 3; i++) {
			list2.add(room.getCards().get(i));
		}

		Collections.sort(list1, compartor);
		Collections.sort(list2, compartor);

		List<List<Integer>> handler1 = handlerCard(list1);
		List<List<Integer>> handler2 = handlerCard(list2);

		int cardType1 = getCardType(handler1);
		int cardType2 = getCardType(handler2);

		long winner = 0l;
		if (cardType1 != cardType2) {
			if (cardType1 < cardType2) {
				// 比牌的人赢
				winner = player.getUserId();

				if (room.getSpecial().intValue() == Cnst.ROOM_SPECIAL_RULE_1 && cardType1 == Cnst.CARD_TYPE_BAOZI && cardType2 == Cnst.CARD_TYPE_SPECIAL) {
					winner = computerUserId;
				}
			} else {
				winner = computerUserId;

				if (room.getSpecial().intValue() == Cnst.ROOM_SPECIAL_RULE_1 && cardType2 == Cnst.CARD_TYPE_BAOZI && cardType1 == Cnst.CARD_TYPE_SPECIAL) {
					winner = player.getUserId();
				}
			}
		} else {
			// 同牌比较
			boolean win = computer(handler1, handler2, cardType1, room.getTongPaiBiJiao());

			if (win)
				winner = player.getUserId();
			else
				winner = computerUserId;
		}

		JSONObject json = new JSONObject();
		json.put("zhuDong", player.getUserId());
		json.put("beiDong", computerUserId);
		json.put("win", winner);
		json.put("use", room.getZhushu().get(idx));
		json = getNewObj(json);
		room.setLastActionExtra(json.toJSONString());
		room.setLastAction(Cnst.ACTION_BIJIAO);
		room.setLastUserId(player.getUserId());

		room.getLastActions().set(idx, room.getLastAction());
		room.getLastActionExtras().set(idx, room.getLastActionExtra());

		room.getBiPais().add(player.getUserId());
		room.getBiPais().add(computerUserId);

		// 判断是否已经结束
		long loser = computerUserId;
		if (winner == computerUserId)
			loser = player.getUserId();

		room.getChuju().add(loser);

		// 判断剩余的人数
		if (room.getChuju().size() == room.getPlayerIds().size() - 1) {
			// 游戏结束
			room.setCurrentUserId(null);
			room.setCurrentUserAction(null);
			JieSuan.xiaoJieSuan(room, cid);
		} else {
			// 找到下一个人让他选择动作
			for (int i = 0; i < room.getPlayerIds().size(); i++) {
				int tmpIdx = (idx + 1 + i) % room.getPlayerIds().size();
				if (room.getChuju().contains(room.getPlayerIds().get(tmpIdx)))
					continue;

				room.setCurrentUserId(room.getPlayerIds().get(tmpIdx));

				// 如果当前用户是第一个出牌的人,证明完成了一轮,或一圈了.
				if (room.getCurrentUserId().equals(room.getFirstTakePaiUser())) {
					room.setTurnNum(room.getTurnNum() + 1);
				}
				ArrayList<Integer> arrayList = new ArrayList<Integer>();
				arrayList.add(Cnst.ACTION_BIJIAO);
				arrayList.add(Cnst.ACTION_GENZHU);
				arrayList.add(Cnst.ACTION_JIAZHU);
				arrayList.add(Cnst.ACTION_QIPAI);

				if (room.getKanPaiInfo().get(tmpIdx) != 1)
					arrayList.add(Cnst.ACTION_KAN);
				room.setCurrentUserAction(arrayList);
				break;
			}

			RedisUtil.updateRedisData(room, null, cid);
		}
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("lastAction", room.getLastAction());
		jsonObject.put("lastActionExtra", room.getLastActionExtra());
		jsonObject.put("lastUserId", room.getLastUserId());
		jsonObject.put("currentUserAction", room.getCurrentUserAction());
		jsonObject.put("currentUserId", room.getCurrentUserId());
		jsonObject.put("playStatus", room.getPlayStatus());
		jsonObject.put("all", room.getAll());
		jsonObject.put("lastUserScore", room.getZhushu().get(idx));
		jsonObject.put("state", room.getState());
		jsonObject.put("diFen", room.getDiFen());
		MessageFunctions.interface_100104(jsonObject, room, 100104, cid);

	}

	/**
	 * 比较手牌的大小
	 * 
	 * @param handler1
	 * @param handler2
	 * @param cardType
	 * @param tongPaiBiJiao
	 * @return
	 */
	public static boolean computer(List<List<Integer>> handler1, List<List<Integer>> handler2, int cardType, int tongPaiBiJiao) {

		if (cardType == Cnst.CARD_TYPE_BAOZI) {
			int a1 = handler1.get(2).get(0) % 100;
			int a2 = handler2.get(2).get(0) % 100;

			if (a1 == 1)
				return true;
			if (a2 == 1)
				return false;

			return a1 > a2;
		} else if (cardType == Cnst.CARD_TYPE_TONGHUASHUN) {
			// 同花顺

			// 比较点
			int a1 = handler1.get(0).get(2) % 100;
			if (a1 == 13) {
				if (handler1.get(0).get(0) % 100 == 1)
					a1 = 14;
			}

			int a2 = handler2.get(0).get(2) % 100;
			if (a2 == 13) {
				if (handler2.get(0).get(0) % 100 == 1)
					a2 = 14;
			}

			if (a1 != a2)
				return a1 > a2;

			// 比较颜色
			if (tongPaiBiJiao == Cnst.ROOM_SAME_COMPUTER_TYPE_1)
				return false;

			// 否则比较花色
			if (a2 != 14) {
				// 345
				int b1 = handler1.get(0).get(2) / 100;
				int b2 = handler2.get(0).get(2) / 100;
				return b1 < b2;
			} else {
				// AQK
				int b1 = handler1.get(0).get(0) / 100;
				int b2 = handler2.get(0).get(0) / 100;
				return b1 < b2;

			}
		} else if (cardType == Cnst.CARD_TYPE_TONGHUA) {
			// 先比较点数 在比较花色
			List<Integer> list1 = new ArrayList<Integer>();
			for (List<Integer> l : handler1) {
				list1.addAll(l);
			}
			List<Integer> list2 = new ArrayList<Integer>();
			for (List<Integer> l : handler2) {
				list2.addAll(l);
			}
			Collections.sort(list1, compartorCareA);
			Collections.sort(list2, compartorCareA);

			int a1 = 0;
			int a2 = 0;
			for (int i = list1.size() - 1; i > -1; i--) {
				a1 = list1.get(i) % 100;
				a2 = list2.get(i) % 100;

				if (a1 == 1)
					a1 = 14;

				if (a2 == 1)
					a2 = 14;

				if (a1 != a2)
					return a1 > a2;
			}

			// 比较颜色
			if (tongPaiBiJiao == Cnst.ROOM_SAME_COMPUTER_TYPE_1)
				return false;

			// 比较花色
			a1 = list1.get(list1.size() - 1) / 100;
			a2 = list2.get(list2.size() - 1) / 100;
			return a1 < a2;
		} else if (cardType == Cnst.CARD_TYPE_SHUNZI) {
			List<Integer> list1 = new ArrayList<Integer>();
			list1.addAll(handler1.get(0));
			List<Integer> list2 = new ArrayList<Integer>();
			list2.addAll(handler2.get(0));

			Collections.sort(list1, compartorCareA);
			Collections.sort(list2, compartorCareA);

			int a1 = 0;
			int a2 = 0;
			for (int i = list1.size() - 1; i > -1; i--) {
				a1 = list1.get(i) % 100;
				a2 = list2.get(i) % 100;

				if (a1 == 1)
					a1 = 14;

				if (a2 == 1)
					a2 = 14;

				if (a1 != a2)
					return a1 > a2;
			}

			// 比较颜色
			if (tongPaiBiJiao == Cnst.ROOM_SAME_COMPUTER_TYPE_1)
				return false;

			// 比较花色
			a1 = list1.get(list1.size() - 1) / 100;
			a2 = list2.get(list2.size() - 1) / 100;
			return a1 < a2;
		} else if (cardType == Cnst.CARD_TYPE_DUIZI) {
			int a1 = handler1.get(1).get(0) % 100;
			int a2 = handler2.get(1).get(0) % 100;

			if (a1 == 1)
				a1 = 14;

			if (a2 == 1)
				a2 = 14;

			if (a1 != a2)
				return a1 > a2;
			a1 = handler1.get(0).get(0) % 100;
			a2 = handler2.get(0).get(0) % 100;

			if (a1 == 1)
				a1 = 14;

			if (a2 == 1)
				a2 = 14;

			if (a1 != a2)
				return a1 > a2;
			// 比较颜色
			if (tongPaiBiJiao == Cnst.ROOM_SAME_COMPUTER_TYPE_1)
				return false;

			a1 = handler1.get(0).get(0) / 100;
			a2 = handler2.get(0).get(0) / 100;
			return a1 < a2;
		} else if (cardType == Cnst.CARD_TYPE_DANPAI || cardType == Cnst.CARD_TYPE_SPECIAL) {
			List<Integer> list1 = new ArrayList<Integer>();
			list1.addAll(handler1.get(0));
			List<Integer> list2 = new ArrayList<Integer>();
			list2.addAll(handler2.get(0));

			Collections.sort(list1, compartorCareA);
			Collections.sort(list2, compartorCareA);

			int a1 = 0;
			int a2 = 0;
			for (int i = list1.size() - 1; i > -1; i--) {
				a1 = list1.get(i) % 100;
				a2 = list2.get(i) % 100;

				if (a1 == 1)
					a1 = 14;

				if (a2 == 1)
					a2 = 14;

				if (a1 != a2)
					return a1 > a2;
			}

			// 比较颜色
			if (tongPaiBiJiao == Cnst.ROOM_SAME_COMPUTER_TYPE_1)
				return false;

			// 比较花色
			a1 = list1.get(list1.size() - 1) / 100;
			a2 = list2.get(list2.size() - 1) / 100;
			return a1 < a2;
		}

		return false;
	}

	/**
	 * 取出手牌中的具体数字
	 * 
	 * @param list1
	 * @return
	 */
	public static List<List<Integer>> handlerCard(List<Integer> list1) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();

		ArrayList<Integer> one = new ArrayList<Integer>();
		ArrayList<Integer> two = new ArrayList<Integer>();
		ArrayList<Integer> three = new ArrayList<Integer>();

		List<Integer> con = new ArrayList<Integer>();
		for (int i = 0; i < list1.size(); i++) {
			int num = 1;
			int meta = list1.get(i);

			if (con.contains(meta % 100))
				continue;

			con.add(meta % 100);

			ArrayList<Integer> arrayList = new ArrayList<Integer>();
			arrayList.add(meta);
			for (int j = i + 1; j < list1.size(); j++) {
				if ((meta % 100) == (list1.get(j) % 100)) {
					++num;
					arrayList.add(list1.get(j));
				} else
					break;
			}

			if (num == 1)
				one.add(meta);
			else if (num == 2)
				two.addAll(arrayList);
			else
				three.addAll(arrayList);
		}
		result.add(one);
		result.add(two);
		result.add(three);

		return result;
	}

	public static int getCardType(List<List<Integer>> handlerList) {

		// 判断豹子
		if (handlerList.get(2).size() == 3)
			return Cnst.CARD_TYPE_BAOZI;

		if (handlerList.get(0).size() == 3) {
			List<Integer> list = handlerList.get(0);

			int a1 = list.get(0) % 100;
			int a2 = list.get(1) % 100;
			int a3 = list.get(2) % 100;
			boolean isShun = false;
			if (a1 == a2 - 1 && a2 == a3 - 1) {
				// 这种不能判断AKQ
				isShun = true;
			} else if (a2 == a3 - 1 && a3 == 13 && a1 == 1) {
				isShun = true;
			}

			if (isShun) {
				// 同花顺 或者顺子
				int b1 = list.get(0) / 100;
				int b2 = list.get(1) / 100;
				int b3 = list.get(2) / 100;
				if (b1 == b2 && b2 == b3)
					return Cnst.CARD_TYPE_TONGHUASHUN;
				else
					return Cnst.CARD_TYPE_SHUNZI;
			}
		}

		{
			int meta = -1;
			int num = 0;
			for (int i = 0; i < handlerList.size(); i++) {
				List<Integer> list = handlerList.get(i);

				for (Integer integer : list) {
					if (meta == -1) {
						++num;
						meta = integer / 100;
						continue;
					}

					if (meta != integer / 100)
						break;
					++num;
				}
				if (num == 3)
					return Cnst.CARD_TYPE_TONGHUA;
			}
		}

		if (handlerList.get(1).size() == 2)
			return Cnst.CARD_TYPE_DUIZI;

		if (handlerList.get(0).size() == 3) {
			// 看看是否是235
			List<Integer> list = handlerList.get(0);
			int a1 = list.get(0) % 100;
			int a2 = list.get(1) % 100;
			int a3 = list.get(2) % 100;

			if (a1 == 2 && a2 == 3 && a3 == 5)
				return Cnst.CARD_TYPE_SPECIAL;

			return Cnst.CARD_TYPE_DANPAI;
		}

		return Cnst.CARD_TYPE_DANPAI;
	}

	/**
	 * 处理看牌
	 * 
	 * @param room
	 * @param player
	 * @param readData
	 */
	private static void handlerKanPai(RoomResp room, Player player, Map<String, Object> readData, String cid) {

		int idx = -1;
		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (player.getUserId().equals(room.getPlayerIds().get(i))) {
				idx = i;
				break;
			}
		}
		if (idx == -1)
			return;

		room.getKanPaiInfo().set(idx, 1); // 玩家是否看牌

		for (int i = 0; i < room.getKanPaiInfo().size(); i++) {
			// player.get
		}

		// 设置此人的动作
		List<Integer> actions = new ArrayList<Integer>();
		actions.add(Cnst.ACTION_QIPAI);
		actions.add(Cnst.ACTION_JIAZHU);
		actions.add(Cnst.ACTION_GENZHU);

		if (room.getBiPaiTime().intValue() < room.getTurnNum()) {
			actions.add(Cnst.ACTION_BIJIAO);
		}

		room.setCurrentUserAction(actions);
		room.setCurrentUserId(player.getUserId());

		room.setLastAction(Cnst.ACTION_KAN);
		room.setLastUserId(player.getUserId());
		room.setLastActionExtra(null);

		room.getLastActions().set(idx, room.getLastAction());
		room.getLastActionExtras().set(idx, room.getLastActionExtra());

		RedisUtil.updateRedisData(room, null, cid);

		JSONObject json = new JSONObject();
		json.put("lastAction", room.getLastAction());
		json.put("lastActionExtra", room.getLastActionExtra());
		json.put("lastUserId", room.getLastUserId());
		json.put("currentUserAction", room.getCurrentUserAction());
		json.put("currentUserId", room.getCurrentUserId());
		json.put("playStatus", room.getPlayStatus());
		JSONArray arr = new JSONArray();

		for (int i = 0; i < room.getPlayerIds().size(); i++) {
			if (room.getKanPaiInfo().get(i).intValue() == 1)
				arr.add(room.getPlayerIds().get(i));
		}
		json.put("kanPaiInfo", arr);
		json.put("paiType", getCardType(room, idx));
		json.put("state", room.getState());
		MessageFunctions.interface_100104(json, room, 100104, cid);

		// 返回看牌的信息 给看牌人
		{
			JSONObject jsonObject = new JSONObject();
			JSONArray info = new JSONArray();
			for (int i = idx * 3; i < (idx + 1) * 3; i++) {
				info.add(room.getCards().get(i));
			}
			jsonObject.put("paiInfo", info);
			jsonObject.put("paiType", getCardType(room, idx));

			sendKanPaiInfo(player.getSessionId(), jsonObject);
		}

	}

	/**
	 * 返回看牌人的牌的信息
	 * 
	 * @param sessionId
	 * @param info
	 */
	private static void sendKanPaiInfo(Long sessionId, JSONObject info) {
		if (sessionId == null)
			return;
		JSONObject result = getJSONObj(100141, 1, info);
		ProtocolData pd = new ProtocolData(100141, result.toJSONString());

		IoSession se = MinaServerManager.tcpServer.getSessions().get(sessionId);
		if (se != null && se.isConnected()) {
			se.write(pd);
		}
	}

	/**
	 * 查看包踹人牌
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100208(IoSession session, Map<String, Object> readData) throws Exception {
		// logger.I("查看包踹人牌,interfaceId -> 100208");
		// Integer interfaceId =
		// StringUtils.parseInt(readData.get("interfaceId"));
		// // String roomId = StringUtils.toString((readData.get("roomSn")));
		// String userId = String.valueOf(readData.get("userId"));
		// Player p = RedisUtil.getPlayerByUserId(userId);
		// List<Integer> pais = new ArrayList<Integer>();
		// for (Card c : p.getCurrentCardList()) {
		// pais.add(c.getOrigin());
		// }
		// JSONObject info = new JSONObject();
		//
		// info.put("userId", userId);
		// info.put("pais", pais);
		// JSONObject result = getJSONObj(interfaceId, 1, info);
		// ProtocolData pd = new ProtocolData(interfaceId,
		// result.toJSONString());
		// session.write(pd);
	}

	/**
	 * 玩家申请解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100203(IoSession session, Map<String, Object> readData) throws Exception {
		logger.I("玩家请求解散房间,interfaceId -> 100203");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId), cid);
		if (room.getDissolveRoom() != null) {
			return;
		}
		DissolveRoom dis = new DissolveRoom();
		dis.setDissolveTime(new Date().getTime());
		dis.setUserId(userId);
		List<Map<String, Object>> othersAgree = new ArrayList<>();
		List<Player> players = RedisUtil.getPlayerList(room, cid);
		for (Player p : players) {
			if (p == null)
				continue;
			if (!p.getUserId().equals(userId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("userId", p.getUserId());
				map.put("agree", 0);// 1同意；2解散；0等待
				othersAgree.add(map);
			}
		}
		dis.setOthersAgree(othersAgree);
		room.setDissolveRoom(dis);

		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", dis.getDissolveTime());
		info.put("userId", dis.getUserId());
		info.put("othersAgree", dis.getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		for (Player p : players) {
			if (p == null)
				continue;
			IoSession se = session.getService().getManagedSessions().get(p.getSessionId());
			if (se != null && se.isConnected()) {
				se.write(pd);
			}
		}

		for (Player p : players) {

			RedisUtil.updateRedisData(null, p, cid);
		}

		RedisUtil.setObject(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(roomId + ""), room, Cnst.ROOM_LIFE_TIME_DIS);

		// 解散房间超时任务开启
		startDisRoomTask(room.getRoomId(), Cnst.DIS_ROOM_TYPE_2, cid);
	}

	/**
	 * 同意或者拒绝解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */

	public static void interface_100204(IoSession session, Map<String, Object> readData) throws Exception {
		logger.I("同意或者拒绝解散房间,interfaceId -> 100203");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer userAgree = StringUtils.parseInt(readData.get("userAgree"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId), cid);
		if (room == null) {// 房间已经自动解散
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_4);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
			session.write(pd);
			return;
		}
		if (room.getDissolveRoom() == null) {
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_7);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
			session.write(pd);
			return;
		}
		List<Map<String, Object>> othersAgree = room.getDissolveRoom().getOthersAgree();
		for (Map<String, Object> m : othersAgree) {
			if (String.valueOf(m.get("userId")).equals(String.valueOf(userId))) {
				m.put("agree", userAgree);
				break;
			}
		}
		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", room.getDissolveRoom().getDissolveTime());
		info.put("userId", room.getDissolveRoom().getUserId());
		info.put("othersAgree", room.getDissolveRoom().getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());

		if (userAgree != 1) {
			// 有玩家拒绝解散房间
			room.setDissolveRoom(null);
			notifyDisRoomTask(room, Cnst.DIS_ROOM_TYPE_2);// 取消解散房间计时
			RedisUtil.setObject(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(roomId + ""), room, Cnst.ROOM_LIFE_TIME_CREAT);
		}
		int agreeNum = 0;
		int rejectNunm = 0;

		for (Map<String, Object> m : othersAgree) {
			if (m.get("agree").equals(1)) {
				agreeNum++;
			} else if (m.get("agree").equals(2)) {
				rejectNunm++;
			}
		}
		RedisUtil.updateRedisData(room, null, cid);

		List<Player> players = RedisUtil.getPlayerList(room, cid);

		if (agreeNum == room.getPeopleNum() - 1 || rejectNunm >= 1) {
			if (agreeNum == room.getPeopleNum() - 1) {

				backPlayerScore(room, cid);

				room.setState(Cnst.ROOM_STATE_YJS);

				MessageFunctions.updateDatabasePlayRecord(room, cid);

				for (Player p : players) {
					if (p == null)
						continue;
					p.initPlayer(null, Cnst.PLAYER_STATE_DATING, false);
				}
				room.setDissolveRoom(null);
				RedisUtil.updateRedisData(room, null, cid);
				RedisUtil.setPlayersList(players, cid);
				// 关闭解散房间计时任务
				notifyDisRoomTask(room, Cnst.DIS_ROOM_TYPE_2);
				// BackFileUtil.write(null, 100103, room,null,null);//写入文件内容

				RoomRecordUtil.clearLiuJu(room.getRoomId(), Long.valueOf(room.getCreateTime()));

				RoomRecordUtil.save(room.getRoomId(), Long.valueOf(room.getCreateTime()));

				if (room.getExtraType().intValue() == Cnst.ROOM_EXTRA_TYPE_2) {
					RedisUtil.hdel(Cnst.DAI_KAI_KEY.concat(room.getCreateId() + ""), room.getRoomId() + "");
				}

				MessageFunctions.interface_100140(room, cid);
			}
		}

		for (Player p : players) {
			if (p == null)
				continue;
			IoSession se = session.getService().getManagedSessions().get(p.getSessionId());
			if (se != null && se.isConnected()) {
				se.write(pd);
			}
		}

	}

	/**
	 * 退出房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100205(IoSession session, Map<String, Object> readData) throws Exception {
		logger.I("退出房间,interfaceId -> 100205");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));

		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId), cid);
		if (room == null) {
			roomDoesNotExist(interfaceId, session);
			return;
		}
		if (room.getState() == Cnst.ROOM_STATE_CREATED) {
			List<Player> players = RedisUtil.getPlayerList(room, cid);
			Map<String, Object> info = new HashMap<>();
			info.put("userId", userId);
			if (room.getCreateId().equals(userId) && room.getExtraType().intValue() == Cnst.ROOM_EXTRA_TYPE_1) {// 房主退出
				int circle = room.getCircleNum();

				info.put("type", Cnst.EXIST_TYPE_DISSOLVE);

				for (Player p : players) {
					if (p == null)
						continue;
					if (p.getUserId().equals(userId)) {
						p.setMoney(p.getMoney() + Cnst.moneyMap.get(circle));
						break;
					}
				}

				// 关闭解散房间计时任务
				notifyDisRoomTask(room, Cnst.DIS_ROOM_TYPE_1);

				RedisUtil.deleteByKey(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(String.valueOf(roomId)));

				for (Player p : players) {
					if (p == null)
						continue;
					p.initPlayer(null, Cnst.PLAYER_STATE_DATING, false);
				}
			} else {// 正常退出
				info.put("type", Cnst.EXIST_TYPE_EXIST);
				existRoom(room, players, userId);
				RedisUtil.updateRedisData(room, null, cid);

				if (room.getExtraType().intValue() == Cnst.ROOM_EXTRA_TYPE_2 && !userId.equals(room.getCreateId())) {
					Player p = null;
					for (Player player : players) {
						if (player != null) {
							if (player.getUserId().longValue() == userId.longValue()) {
								p = player;
							}
						}
					}
					MessageFunctions.interface_100112(p, room, Cnst.ROOM_NOTICE_OUT, cid);
				}
			}
			JSONObject result = getJSONObj(interfaceId, 1, info);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());

			for (Player p : players) {
				if (p == null)
					continue;
				RedisUtil.updateRedisData(null, p, cid);
			}

			for (Player p : players) {
				if (p == null)
					continue;
				IoSession se = session.getService().getManagedSessions().get(p.getSessionId());
				if (se != null && se.isConnected()) {
					se.write(pd);
				}
			}

		} else {
			roomIsGaming(interfaceId, session);
		}
	}

	private static void existRoom(RoomResp room, List<Player> players, Long userId) {
		for (Player p : players) {
			if (p == null)
				continue;
			if (p.getUserId().equals(userId)) {
				p.initPlayer(null, Cnst.PLAYER_STATE_DATING, false);
				break;
			}
		}
		List<Long> pids = room.getPlayerIds();
		if (pids != null) {
			for (int i = 0; i < pids.size(); i++) {
				if (userId.equals(pids.get(i))) {
//					pids.set(i, null);
					pids.remove(i);
					break;
				}
			}
		}
	}

	/**
	 * 语音表情
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100206(IoSession session, Map<String, Object> readData) throws Exception {
		logger.I("准备,interfaceId -> 100206");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		String userId = String.valueOf(readData.get("userId"));
		String type = String.valueOf(readData.get("type"));
		String idx = String.valueOf(readData.get("idx"));

		Map<String, Object> info = new HashMap<>();
		info.put("roomId", roomId);
		info.put("userId", userId);
		info.put("type", type);
		info.put("idx", idx);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		List<Player> players = RedisUtil.getPlayerList(roomId, cid);
		for (Player p : players) {
			if (p == null)
				continue;
			if (!p.getUserId().equals(userId)) {
				IoSession se = session.getService().getManagedSessions().get(p.getSessionId());
				if (se != null && se.isConnected()) {
					se.write(pd);
				}
			}
		}
	}

	/**
	 * 记录 1.动作信息 叫地主 加倍 和出牌 2.三个玩家初始牌面信息 3.底牌信息 4.地主产生信息 5.小结算 6.大结算 7.底分发生变化
	 * 叫地主的分*流局 8.加倍改变 炸弹等
	 * 
	 * @param roomId
	 * @param record
	 */
	public static void addRecord(RoomResp room, Map<String, Object> record) {
		RoomRecordUtil.addRecord(room.getRoomId(), Long.valueOf(room.getCreateTime()), record, false);
	}

	/**
	 * 创建记录
	 * 
	 * @param room
	 * @param players
	 */
	public static void createRecord(RoomResp room, List<Player> players) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("interfaceId", "1");
		String score = null;

		// score = String.valueOf(players.get(0).getScore());
		// map.put("firstUserId", players.get(0).getUserId());
		// map.put("firstUserName", players.get(0).getUserName());
		// map.put("firstUserImg", players.get(0).getUserImg());
		// map.put("firstUserMoneyRecord", score);
		// map.put("firstUserMoneyRemain", score);
		//
		//
		// score = String.valueOf(players.get(1).getScore());
		// map.put("secondUserId", players.get(1).getUserId());
		// map.put("secondUserName", players.get(1).getUserName());
		// map.put("secondUserImg", players.get(1).getUserImg());
		// map.put("secondUserMoneyRecord", score);
		// map.put("secondUserMoneyRemain", score);
		//
		// score = String.valueOf(players.get(2).getScore());
		// map.put("thirdUserId", players.get(2).getScore());
		// map.put("thirdUserName", players.get(2).getUserName());
		// map.put("thirdUserImg", players.get(2).getUserImg());
		// map.put("thirdUserMoneyRecord", score);
		// map.put("thirdUserMoneyRemain", score);

		map.put("roomId", room.getRoomId() + "");
		map.put("createTime", room.getCreateTime());
		map.put("endTime", System.currentTimeMillis() + "");
		map.put("circleNum", room.getCircleNum() + "");
		map.put("state", room.getState() + "");
		map.put("tiShi", room.getTiShi() == null ? "0" : room.getTiShi() + "");
		map.put("extraType", room.getExtraType() + "");

		// 简化
		map = getNewMap(map);

		RoomRecordUtil.addRecord(room.getRoomId(), Long.valueOf(room.getCreateTime()), map, true);
	}

	class MyCompartor implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {

			Integer i1 = (Integer) o1;

			Integer i2 = (Integer) o2;

			int a1 = i1 % 100;
			int a2 = i2 % 100;
			if (a1 != a2) {
				return a1 - a2;
			}
			int b1 = i1 / 100;
			int b2 = i2 / 100;
			return b1 - b2;
		}
	}

	class MyCompartorCareA implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {

			Integer i1 = (Integer) o1;

			Integer i2 = (Integer) o2;

			int a1 = i1 % 100;
			int a2 = i2 % 100;
			if (a1 == 1)
				a1 = 14;
			if (a2 == 1)
				a2 = 14;

			if (a1 != a2) {
				return a1 - a2;
			}
			int b1 = i1 / 100;
			int b2 = i2 / 100;
			return b1 - b2;
		}
	}

	public static int getCardType(RoomResp room, int idx) {
		List<Integer> list = new ArrayList<Integer>();

		for (int i = idx * 3; i < idx * 3 + 3; i++) {
			list.add(room.getCards().get(i));
		}
		Collections.sort(list, compartor);
		List<List<Integer>> handlerCard = handlerCard(list);
		return getCardType(handlerCard);
	}

	public static void interface_100209(IoSession session, Map<String, Object> readData) {
		logger.I("准备,interfaceId -> 100209");
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));

		Long userId = Long.valueOf((String) session.getAttribute(Cnst.USER_SESSION_USER_ID));

		if (readData.get("p_x") != null) {
			Integer x = Integer.valueOf(String.valueOf(readData.get("p_x")));
			Integer y = Integer.valueOf(String.valueOf(readData.get("p_y")));
			RedisUtil.setString(Cnst.REDIS_PREFIX_USER_ID_POSITION.concat(userId + ""), x + "_" + y + "_" + System.currentTimeMillis(), Cnst.POSITION_EXPIRE_TIME);
		}

		RoomResp room = RedisUtil.getRoomRespByRoomId(roomId + "", cid);

		if (!room.getPlayerIds().contains(userId))
			return;// 没必要理会 因为不在这个房间
		List<Long> playerIds = room.getPlayerIds();
		JSONObject json = new JSONObject();
		boolean send = false;
		for (int i = 0; i < playerIds.size(); i++) {
			if (playerIds.get(i) == null)
				continue;
			Long long1 = playerIds.get(i);
			String stringByKey = RedisUtil.getStringByKey(Cnst.REDIS_PREFIX_USER_ID_POSITION.concat(long1 + ""));
			for (int j = i + 1; j < playerIds.size(); j++) {
				if (playerIds.get(j) == null)
					continue;
				send = true;

				Long long2 = playerIds.get(j);

				if (stringByKey == null) {
					json.put(long1 + "_" + long2, -1);
					continue;
				}
				String stringByKey1 = RedisUtil.getStringByKey(Cnst.REDIS_PREFIX_USER_ID_POSITION.concat(long2 + ""));
				if (stringByKey1 == null) {
					json.put(long1 + "_" + long2, -1);
					continue;
				}

				String[] split = stringByKey.split("_");
				int x1 = Integer.valueOf(split[0]);
				int y1 = Integer.valueOf(split[1]);

				String[] split2 = stringByKey1.split("_");
				int x2 = Integer.valueOf(split2[0]);
				int y2 = Integer.valueOf(split2[1]);

				x1 = x1 - x2;
				y1 = y1 - y2;

				json.put(long1 + "_" + long2, (int) Math.floor(Math.sqrt(x1 * x1 + y1 * y1)));
			}
		}
		if (send) {
			JSONObject result = getJSONObj(interfaceId, 1, json);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());

			List<Player> players = RedisUtil.getPlayerList(room, cid);
			if (players != null && players.size() > 0) {
				for (Player p : players) {
					if (p == null)
						continue;
					if (p.getRoomId() != null && p.getRoomId().equals(roomId)) {
						IoSession se = MinaServerManager.tcpServer.getSessions().get(p.getSessionId());
						if (se != null && se.isConnected()) {
							se.write(pd);
						}
					}
				}
			}
		}

	}
}
