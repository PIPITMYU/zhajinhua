package com.up72.server.mina.function;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.swing.internal.plaf.synth.resources.synth;
import com.up72.game.constant.Cnst;
import com.up72.game.dto.resp.Card;
import com.up72.game.dto.resp.Player;
import com.up72.game.dto.resp.RoomResp;
import com.up72.server.mina.bean.ProtocolData;
import com.up72.server.mina.main.MinaServerManager;
import com.up72.server.mina.utils.StringUtils;
import com.up72.server.mina.utils.redis.RedisUtil;

/**
 * Created by Administrator on 2017/7/10. 推送消息类
 */
public class MessageFunctions extends TCPGameFunctions {

	/**
	 * 发送玩家信息
	 * 
	 * @param session
	 * @param readData
	 */
	public static void interface_100100(IoSession session, Map<String, Object> readData) throws Exception {
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Map<String, Object> info = new HashMap<>();
		if (interfaceId.equals(100100)) {// 刚进入游戏主动请求
			String openId = String.valueOf(readData.get("openId"));

			Player currentPlayer = null;
			String cid = null;
			if (openId == null) {
				illegalRequest(interfaceId, session);
				return;
			} else {
				String ip = (String) session.getAttribute(Cnst.USER_SESSION_IP);
				cid = String.valueOf(readData.get("cId"));
				currentPlayer = HallFunctions.getPlayerInfos(openId, ip, cid, session);
			}
			if (currentPlayer == null) {
				illegalRequest(interfaceId, session);
				return;
			}

			// 更新心跳为最新上线时间
			RedisUtil.hset(Cnst.get_REDIS_HEART_PREFIX(cid), currentPlayer.getUserId() + "", String.valueOf(new Date().getTime()), null);

			if (cid != null) {
				currentPlayer.setCId(cid);
			}
			currentPlayer.setSessionId(session.getId());// 更新sesisonId
			session.setAttribute(Cnst.USER_SESSION_USER_ID, currentPlayer.getUserId() + "");
			session.setAttribute(Cnst.USER_SESSION_CID, cid);
			if (openId != null) {
				RedisUtil.setObject(Cnst.get_REDIS_PREFIX_OPENIDUSERMAP(cid).concat(openId), currentPlayer.getUserId(), null);
			}

			RoomResp room = null;
			List<Player> players = null;

			if (currentPlayer.getRoomId() != null) {// 玩家下有roomId，证明在房间中
				room = RedisUtil.getRoomRespByRoomId(String.valueOf(currentPlayer.getRoomId()),cid);
				if (room != null && room.getState() != Cnst.ROOM_STATE_YJS) {
					info.put("roomInfo", getRoomInfo(room, currentPlayer.getUserId()));
					players = RedisUtil.getPlayerList(room,cid);

					info.put("anotherUsers", getAnotherUserInfo(players, room, currentPlayer.getUserId()));

				} else {
					currentPlayer.initPlayer(null, Cnst.PLAYER_STATE_DATING,false);
				}

			} else if (currentPlayer.getPlayStatus() == null){
				currentPlayer.setPlayStatus(Cnst.PLAYER_STATE_DATING);
			}
			RedisUtil.updateRedisData(room, currentPlayer,cid);

			if (room != null) {
				// room.setWsw_sole_main_id(room.getWsw_sole_main_id()+1);

				info.put("wsw_sole_main_id", room.getWsw_sole_main_id());
				info.put("wsw_sole_action_id", room.getWsw_sole_action_id());
				Map<String, Object> roomInfo = (Map<String, Object>) info.get("roomInfo");
				List<Map<String, Object>> anotherUsers = (List<Map<String, Object>>) info.get("anotherUsers");

				info.remove("anotherUsers");

				// roomInfo
				JSONObject result = getJSONObj(interfaceId, 1, info);
				ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
				session.write(pd);

				info.remove("roomInfo");

				// currentUser
				info.put("currentUser", getCurrentUserInfo(currentPlayer, room));
				result = getJSONObj(interfaceId, 1, info);
				pd = new ProtocolData(interfaceId, result.toJSONString());
				session.write(pd);

				info.remove("currentUser");

				info.put("anotherUsers", anotherUsers);
				result = getJSONObj(interfaceId, 1, info);
				pd = new ProtocolData(interfaceId, result.toJSONString());
				session.write(pd);

				MessageFunctions.interface_100109(players, Cnst.PLAYER_LINE_STATE_INLINE, currentPlayer.getUserId(), currentPlayer.getPlayStatus());
			} else {
				info.put("currentUser", getCurrentUserInfo(currentPlayer, room));
				JSONObject result = getJSONObj(interfaceId, 1, info);
				ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
				session.write(pd);
			}

		} else {
			session.close(true);
		}

	}

	// 封装currentUser
	public static Map<String, Object> getCurrentUserInfo(Player player, RoomResp room) {

		int position = 0;
		long score = Cnst.ROOM_INIT_SCORE;

		if (room != null) {
			List<Long> playerIds = room.getPlayerIds();
			if (playerIds != null) {

				for (int i = 0; i < playerIds.size(); i++) {
					if (playerIds.get(i) == null)
						continue;
					if (playerIds.get(i).longValue() == player.getUserId().longValue()) {
						position = i + 1;

						List<Long> playerScores = room.getPlayerScores();
						if (playerScores != null && playerScores.size() > i && playerScores.get(i) != null){
							score = playerScores.get(i);
						}

						break;
					}
				}
			}
		}

		Map<String, Object> currentUserInfo = new HashMap<String, Object>();
		currentUserInfo.put("version", String.valueOf(Cnst.version));
		currentUserInfo.put("userId", player.getUserId());
		currentUserInfo.put("position", position);
		currentUserInfo.put("playStatus", player.getPlayStatus());
		currentUserInfo.put("userName", player.getUserName());
		currentUserInfo.put("userImg", player.getUserImg());
		currentUserInfo.put("gender", player.getGender());
		currentUserInfo.put("ip", player.getIp());
		currentUserInfo.put("userAgree", player.getUserAgree());
		currentUserInfo.put("money", player.getMoney());
		
//		long score1 = Cnst.ROOM_INIT_SCORE;
//
//		
//		//if (room.getPlayerScores() != null && room.getPlayerScores().size() > position && room.getPlayerScores().get(position - 1) != null){
//			if(room.getZhushu() != null){
//				score1 = room.getZhushu().get(position - 1);
//			}
		
		currentUserInfo.put("score", score);
		currentUserInfo.put("notice", player.getNotice());

		if (room != null && room.getKanPaiInfo() != null && room.getKanPaiInfo().size() > position - 1 && room.getKanPaiInfo().get(position - 1) != 0) {
			JSONArray info = new JSONArray();
			int idx = position - 1;
			for (int i = idx * 3; i < (idx + 1) * 3; i++) {
				info.add(room.getCards().get(i));
			}
			currentUserInfo.put("pais", info);
			currentUserInfo.put("paiType", GameFunctions.getCardType(room, idx));
		}

		return currentUserInfo;
	}

	// 封装anotherUsers FIXME
	public static List<Map<String, Object>> getAnotherUserInfo(List<Player> players, RoomResp room, Long exceptUid) {
		List<Map<String, Object>> anotherUserInfos = new ArrayList<Map<String, Object>>();
		int position = 0;
		for (Player player : players) {
			++position;
			if (player == null)
				continue;
			if (player.getUserId().equals(exceptUid))
				continue;
			Map<String, Object> currentUserInfo = new HashMap<String, Object>();
			currentUserInfo.put("userId", player.getUserId());
			currentUserInfo.put("position", position);
			currentUserInfo.put("playStatus", player.getPlayStatus());
			currentUserInfo.put("userName", player.getUserName());
			currentUserInfo.put("userImg", player.getUserImg());
			currentUserInfo.put("gender", player.getGender());
			currentUserInfo.put("ip", player.getIp());
			currentUserInfo.put("userAgree", player.getUserAgree());
			currentUserInfo.put("money", player.getMoney());

			long score = Cnst.ROOM_INIT_SCORE;

			
			//if (room.getPlayerScores() != null && room.getPlayerScores().size() > position && room.getPlayerScores().get(position - 1) != null){
			if (room.getPlayerScores() != null && room.getPlayerScores().get(position - 1) != null){
				score = room.getPlayerScores().get(position - 1);
			}

			//currentUserInfo.put("score", score);
			currentUserInfo.put("score", score);
			currentUserInfo.put("notice", player.getNotice());
			anotherUserInfos.add(currentUserInfo);
		}
		return anotherUserInfos;
	}

	// 封装房间信息
	public static Map<String, Object> getRoomInfo(RoomResp room, Long userId) {
		Map<String, Object> roomInfo = new HashMap<String, Object>();
		roomInfo.put("userId", room.getCreateId());
		roomInfo.put("openName", room.getOpenName());
		roomInfo.put("createTime", room.getCreateTime());
		roomInfo.put("roomId", room.getRoomId());
		roomInfo.put("state", room.getState());
		roomInfo.put("playStatus", room.getPlayStatus());
		roomInfo.put("lastNum", room.getLastNum());
		roomInfo.put("totalNum", room.getTotalNum());
		roomInfo.put("circleNum", room.getCircleNum());
		roomInfo.put("extraType", room.getExtraType());
		roomInfo.put("peopleNum", room.getPeopleNum());
		roomInfo.put("initDiFen", room.getInitDiFen());
		roomInfo.put("biPaiTime", room.getBiPaiTime());
		roomInfo.put("tongPaiBiJiao", room.getTongPaiBiJiao());
		roomInfo.put("special", room.getSpecial());
		if (room.getState() != Cnst.ROOM_STATE_CREATED) {
			roomInfo.put("xjst", room.getXjst());
			roomInfo.put("all", room.getAll());
			roomInfo.put("lastAction", room.getLastAction());
			roomInfo.put("lastUserId", room.getLastUserId());
			roomInfo.put("lastActionExtra", room.getLastActionExtra());
			roomInfo.put("currentUserId", room.getCurrentUserId());
			roomInfo.put("currentUseAction", room.getCurrentUserAction());
			roomInfo.put("diFen", room.getDiFen());
			roomInfo.put("turnNum", room.getTurnNum());
			roomInfo.put("firstTakePaiUser", room.getFirstTakePaiUser());
			JSONArray jsonArray = new JSONArray();
			if (room.getKanPaiInfo() != null)
				for (int i = 0; i < room.getKanPaiInfo().size(); i++) {
					if (room.getKanPaiInfo().get(i) != null && room.getKanPaiInfo().get(i) == 1)
						jsonArray.add(room.getPlayerIds().get(i));
				}
			roomInfo.put("kanPaiInfo", jsonArray);
			roomInfo.put("chuju", room.getChuju());

			roomInfo.put("everyCount", room.getEveryCount());

			roomInfo.put("biPais", room.getBiPais());

			JSONObject use = new JSONObject();
			if (room.getZhushu() != null && room.getPlayerIds() != null) {
				for (int i = 0; i < room.getZhushu().size(); i++) {
					if (room.getPlayerIds().size() == i)
						break;
					if (room.getPlayerIds().get(i) == null)
						continue;
					;
					if (room.getZhushu().get(i) == null)
						continue;
					use.put(room.getPlayerIds().get(i) + "", room.getZhushu().get(i));

				}
			}
			roomInfo.put("use", use);
		}

		roomInfo.put("tiShi", room.getTiShi());
		if (room.getDissolveRoom() != null) {
			Map<String, Object> dissolveRoom = new HashMap<String, Object>();
			dissolveRoom.put("dissolveTime", room.getDissolveRoom().getDissolveTime());
			dissolveRoom.put("userId", room.getDissolveRoom().getUserId());
			dissolveRoom.put("othersAgree", room.getDissolveRoom().getOthersAgree());
			roomInfo.put("dissolveRoom", dissolveRoom);
		} else {
			roomInfo.put("dissolveRoom", null);
		}
		return roomInfo;
	}

	/**
	 * 小结算
	 * 
	 * @param session
	 * @param readData
	 */
	public static void interface_100102(IoSession session, Map<String, Object> readData) {
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId),cid);

		if (room == null || room.getState() < Cnst.ROOM_STATE_XJS)
			return;// 现在状态不对 不能请求小结算

		if (room.getLastNum() == 0) {
			room.setState(Cnst.ROOM_STATE_YJS);
			RedisUtil.updateRedisData(room, null, cid);
		}
		
		List<Player> players = RedisUtil.getPlayerList(room,cid);
		List<Map<String, Object>> userInfos = new ArrayList<Map<String, Object>>();

		for (int idx = 0; idx < players.size(); idx++) {
			Map<String, Object> map = new HashMap<String, Object>();
			Player p = players.get(idx);
			map.put("userId", p.getUserId());
			map.put("use", room.getZhushu().get(idx));
			map.put("win", room.getChuju() != null && room.getChuju().contains(p.getUserId()) ? 0 : 1);
			JSONArray arr = new JSONArray();
			for (int i = idx * 3; i < (idx + 1) * 3; i++) {
				arr.add(room.getCards().get(i));
			}
			map.put("paiInfo", arr);
			map.put("paiType", GameFunctions.getCardType(room, idx));

			map.put("score", room.getPlayerScores().get(idx));

			userInfos.add(map);
		}

		JSONObject info = new JSONObject();
		info.put("lastNum", room.getLastNum());
		info.put("all", room.getAll());
		info.put("userInfos", userInfos);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);
		
	}

	/**
	 * 大结算
	 * 
	 * @param session
	 * @param readData
	 */
	public synchronized static void interface_100103(IoSession session, Map<String, Object> readData) {
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId),cid);
		String key = roomId + "-" + room.getCreateTime();
		List<Map> userInfos = RedisUtil.getPlayRecord(Cnst.get_REDIS_PLAY_RECORD_PREFIX_OVERINFO(cid).concat(key));//获取战绩
		JSONObject info = new JSONObject();
		info.put("XiaoJuNum", room.getXiaoJuNum());
		String a = Cnst.get_REDIS_PLAY_RECORD_PREFIX_OVERINFO(cid).concat(key);
		if (!RedisUtil.exists(Cnst.get_REDIS_PLAY_RECORD_PREFIX_OVERINFO(cid).concat(key))) {//临时存储大结算数据 key为roomId-createTime
			List<Map<String, Object>> zeroUserInfos = new ArrayList<Map<String, Object>>();
			List<Player> players = RedisUtil.getPlayerList(room,cid);
			int idx = 0;
			for (Player p : players) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("userId", p.getUserId());
				map.put("score", room.getPlayerScores().get(idx));
				map.put("finalScore", room.getPlayerScores().get(idx));
				map.put("position", idx);
				map.put("userName", p.getUserName());
				map.put("userImg", p.getUserImg());
				JSONArray jsonArray = new JSONArray();

				for (int i = idx; i < room.getPaiTypes().size(); i += room.getPlayerIds().size()) {
					jsonArray.add(room.getPaiTypes().get(i));
				}

				map.put("paiTypes", jsonArray);
				zeroUserInfos.add(map);
				++idx;
			}
			info.put("userInfos", zeroUserInfos);
		} else {
			info.put("userInfos", userInfos);//有就添加
		}

		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);

		// 更新 player
		Player p = RedisUtil.getPlayerByUserId(userId + "",cid);
		p.initPlayer(null, Cnst.PLAYER_STATE_DATING,false);

		Integer outNum = room.getOutNum() == null ? 1 : room.getOutNum() + 1;
		if (outNum == room.getPlayerIds().size()) {
			RedisUtil.deleteByKey(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(roomId + ""));
		}
		// 更新outNum
		RedisUtil.updateRedisData(room, p,cid);
	}

	/**
	 * 动作回应
	 */
	public static void interface_100104(JSONObject info, RoomResp room, Integer interfaceId,String cid) {
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		List<Player> players = RedisUtil.getPlayerList(room,cid);
		
		
		for (int i = 0; i < players.size(); i++) {
			IoSession se = MinaServerManager.tcpServer.getSessions().get(players.get(i).getSessionId());
			if (se != null && se.isConnected()) {
				se.write(pd);
			}
		}
	}

	/**
	 * 发牌推送
	 */
	public static void interface_100105(JSONObject info, RoomResp room, Integer interfaceId,String cid) {
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		List<Player> players = RedisUtil.getPlayerList(room,cid);
		for (int i = 0; i < players.size(); i++) {
			IoSession se = MinaServerManager.tcpServer.getSessions().get(players.get(i).getSessionId());
			if (se != null && se.isConnected()) {
				se.write(pd);
			}
		}
	}

	/**
	 * 多地登陆提示
	 * 
	 * @param session
	 */
	public static void interface_100106(IoSession session) {
		Integer interfaceId = 100106;
		JSONObject result = getJSONObj(interfaceId, 1, "out");
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);
		session.close(true);
	}

	/**
	 * 玩家被踢/房间被解散提示
	 * 
	 * @param session
	 */
	public static void interface_100107(IoSession session, Integer type, List<Player> players, Long leaveUserId) {
		Integer interfaceId = 100107;
		Map<String, Object> info = new HashMap<String, Object>();

		if (players == null || players.size() == 0) {
			return;
		}
		info.put("userId", session.getAttribute(Cnst.USER_SESSION_USER_ID));
		info.put("type", type);

		if (leaveUserId != null)
			info.put("leaveUserId", leaveUserId);

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
	 * 方法id不符合
	 * 
	 * @param session
	 */
	public static void interface_100108(IoSession session) {
		Integer interfaceId = 100108;
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("reqState", Cnst.REQ_STATE_9);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);
	}

	/**
	 * 用户离线/上线提示
	 * 
	 * @param state
	 */
	public static void interface_100109(List<Player> players, Integer state, Long userId, Integer playStatus) {
		Integer interfaceId = 100109;
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("userId", userId);
		info.put("state", state);
		info.put("playStatus", playStatus);

		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());

		if (players != null && players.size() > 0) {
			for (Player p : players) {
				if (p != null && !p.getUserId().equals(userId)) {
					IoSession se = MinaServerManager.tcpServer.getSessions().get(p.getSessionId());
					if (se != null && se.isConnected()) {
						se.write(pd);
					}
				}

			}
		}
	}

	/**
	 * 后端主动解散房间推送
	 * 
	 * @param reqState
	 * @param players
	 */
	public static void interface_100111(int reqState, List<Player> players, Integer roomId, Long creater) {
		Integer interfaceId = 100111;
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("reqState", reqState);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		if (players != null && players.size() > 0) {
			for (Player p : players) {
				if (p == null)
					continue;

				if (p.getRoomId() != null && p.getRoomId().equals(roomId)) {
					IoSession se = MinaServerManager.tcpServer.getSessions().get(p.getSessionId());
					if (se != null && se.isConnected()) {
						if (creater != null && creater.equals(p.getUserId())) {
							info.put("money", p.getMoney());
							JSONObject result1 = getJSONObj(interfaceId, 1, info);
							ProtocolData pd1 = new ProtocolData(interfaceId, result1.toJSONString());
							se.write(pd1);
						} else
							se.write(pd);
					}
				}
			}
		}

	}

	/**
	 * 后端主动加入代开房间推送
	 * 
	 * @param reqState
	 * @param players
	 */
	public static void interface_100112(Player player, RoomResp room, Integer extraType,String cid) {
		Integer interfaceId = 100112;
		// 先判断房主是否在线
		Player roomCreater = RedisUtil.getPlayerByUserId(String.valueOf(room.getCreateId()),cid);
		IoSession se = MinaServerManager.tcpServer.getSessions().get(roomCreater.getSessionId());
		if (se != null && se.isConnected()) {
			Map<String, Object> info = new HashMap<String, Object>();
			info.put("roomSn", room.getRoomId());
			info.put("userId", player.getUserId());
			info.put("userName", player.getUserName());
			info.put("userImg", player.getUserImg());
			if (extraType.intValue() != Cnst.ROOM_NOTICE_OUT) {
				int position = 0;
				if (room != null && room.getPlayerIds() != null) {
					for (int i = 0; i < room.getPlayerIds().size(); i++) {
						if (room.getPlayerIds().get(i) != null && room.getPlayerIds().get(i).longValue() == player.getUserId().longValue()) {
							position = i;
							break;
						}
					}
				}

				info.put("position", position);
			}
			info.put("extraType", extraType);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
			se.write(pd);
		} else {
			return;
		}

	}

	/**
	 * 代开房间因为任何原因关闭
	 * 
	 * @param room
	 */
	public static void interface_100140(RoomResp room,String cid) {
		if (room == null)
			return;
		if (room.getExtraType().intValue() != Cnst.ROOM_EXTRA_TYPE_2)
			return;
		Integer interfaceId = 100140;
		// 先判断房主是否在线
		Player roomCreater = RedisUtil.getPlayerByUserId(String.valueOf(room.getCreateId()),cid);
		IoSession se = MinaServerManager.tcpServer.getSessions().get(roomCreater.getSessionId());
		if (se != null && se.isConnected()) {
			Map<String, Object> info = new HashMap<String, Object>();
			info.put("roomId", room.getRoomId());
			JSONObject result = getJSONObj(interfaceId, 1, info);
			ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
			se.write(pd);
		} else {
			return;
		}

	}
}
