package com.up72.server.mina.function;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jdk.nashorn.internal.scripts.JS;

import org.apache.mina.core.session.IoSession;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.up72.game.constant.Cnst;
import com.up72.game.dto.resp.Player;
import com.up72.game.dto.resp.RoomResp;
import com.up72.server.mina.bean.ProtocolData;
import com.up72.server.mina.main.MinaServerManager;
import com.up72.server.mina.utils.MyLog;
import com.up72.server.mina.utils.StringUtils;
import com.up72.server.mina.utils.redis.RedisUtil;

public class TCPFunctionExecutor {

	private static final MyLog log = MyLog.getLogger(TCPFunctionExecutor.class);

	public static void execute(IoSession session, ProtocolData readDatas) throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException, Exception {
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		int interfaceId = readDatas.getInterfaceId();
		JSONObject obj = JSONObject.parseObject(readDatas.getJsonString());

		// 路由转换
		Map<String, Object> readData = new ConcurrentHashMap<String, Object>();
		Iterator<String> iterator = obj.keySet().iterator();
		while (iterator.hasNext()) {
			String str = iterator.next();
			readData.put(Cnst.ROUTE_MAP.get(str) == null ? str : Cnst.ROUTE_MAP.get(str), obj.get(str));

		}
		// 转换完成
		System.out.println("==========================>接收到的数据" + readData);
		switch (interfaceId) {
		// 大厅消息段
		case 100002:
			HallFunctions.interface_100002(session, readData);
			break;
		case 100003:
			HallFunctions.interface_100003(session, readData);
			break;
		case 100004:
			HallFunctions.interface_100004(session, readData);
			break;
		case 100005:
			HallFunctions.interface_100005(session, readData);
			break;
		case 100006:
			HallFunctions.interface_100006(session, readData);
			break;
		case 100007:
			HallFunctions.interface_100007(session, readData);
			break;// 经典玩法创建房间
		case 100008:
			HallFunctions.interface_100008(session, readData);
			break;
		case 100009:
			HallFunctions.interface_100009(session, readData);
			break;
		case 100010:
			HallFunctions.interface_100010(session, readData);
			break;
		case 100011:
			HallFunctions.interface_100011(session, readData);
			break;
		case 100012:
			HallFunctions.interface_100012(session, readData);
			break;
		case 100013:
			HallFunctions.interface_100013(session, readData);
			break;
		case 100014:
			HallFunctions.interface_100014(session, readData);
			break;
		case 100015:
			HallFunctions.interface_100015(session, readData);
			break;

		// 推送消息段
		case 100100:
			MessageFunctions.interface_100100(session, readData);
			break;// 大接口
		case 100102:
			MessageFunctions.interface_100102(session, readData);
			break;// 小结算
		case 100103:
			MessageFunctions.interface_100103(session, readData);
			break;// 大结算

		// 游戏中消息段
		case 100200:
			GameFunctions.interface_100200(session, readData);
			break;
		case 100202:
			GameFunctions.interface_100202(session, readData);
			break;
		case 100203:
			GameFunctions.interface_100203(session, readData);
			break;
		case 100204:
			GameFunctions.interface_100204(session, readData);
			break;
		case 100205:
			GameFunctions.interface_100205(session, readData);
			break;
		case 100206:
			GameFunctions.interface_100206(session, readData);
			break;
		case 100207:
			GameFunctions.interface_100207(session, readData);
			break;
		case 100208:
			GameFunctions.interface_100208(session, readData);
			break;

		// 强制解散房间
		case 999800:
			disRoomForce(session, readData);
			break;

		default:
			Map<String, String> user = RedisUtil.hgetAll(Cnst.get_REDIS_PREFIX_USER_ID_USER_MAP(cid).concat(session.getAttribute(Cnst.USER_SESSION_USER_ID) + ""));
			if (user == null) {

			} else {
				log.I("未知interfaceId" + interfaceId);
				MessageFunctions.illegalRequest(interfaceId, session);// 非法请求
			}
			break;
		}

	}

	/**
	 * 强制解散房间
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	private static void disRoomForce(IoSession session, Map<String, Object> readData) throws Exception {
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		System.out.println("*******强制解散房间" + roomId);
		if (roomId != null) {
			RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId), cid);
			if (room != null) {
				MessageFunctions.updateDatabasePlayRecord(room, cid);
				room.setState(Cnst.ROOM_STATE_YJS);
				List<Player> players = RedisUtil.getPlayerList(room, cid);

				RedisUtil.deleteByKey(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(String.valueOf(roomId)));// 删除房间
				if (players != null && players.size() > 0) {
					for (Player p : players) {
						if (p == null)
							continue;
						p.initPlayer(null, Cnst.PLAYER_STATE_DATING, false);
						RedisUtil.updateRedisData(null, p, cid);
					}
					for (Player p : players) {
						if (p == null)
							continue;
						IoSession se = MinaServerManager.tcpServer.getSessions().get(p.getSessionId());
						if (se != null && se.isConnected()) {
							Map<String, Object> readDatas = new HashMap<String, Object>();
							readDatas.put("interfaceId", 100100);
							readDatas.put("openId", p.getOpenId());
							readDatas.put("cId", Cnst.cid);
							MessageFunctions.interface_100100(se, readDatas);
						}
					}
				}

				// 代开
				if (room.getExtraType().intValue() == Cnst.ROOM_EXTRA_TYPE_2) {
					RedisUtil.hdel(Cnst.DAI_KAI_KEY.concat(room.getCreateId() + ""), room.getRoomId() + "");
				}

			} else {
				System.out.println("*******强制解散房间" + roomId + "，房间不存在");
			}
		}

		Map<String, Object> info = new HashMap<>();
		info.put("reqState", Cnst.REQ_STATE_1);
		JSONObject result = MessageFunctions.getJSONObj(interfaceId, 1, info);
		ProtocolData pd = new ProtocolData(interfaceId, result.toJSONString());
		session.write(pd);
	}

	/**
	 * 心跳操作 所有用户的id对应心跳，存在redis的map集合里，集合的key为Cnst.REDIS_HEART_PREFIX，
	 * map的key为userId，value为最后long类型的心跳时间
	 * 当用户掉线之后，会把map中的这个userId删掉，（考虑：是否修改用户的state字段）
	 * 
	 * @param session
	 * @param readData
	 */
	public synchronized static void heart(IoSession session, ProtocolData readData) throws Exception {
		String userIdStr = (String) session.getAttribute(Cnst.USER_SESSION_USER_ID);
		String cid = (String) session.getAttribute(Cnst.USER_SESSION_CID);
		if (userIdStr == null) {
			session.close(true);
		} else {
			try {
				Player p = RedisUtil.getPlayerByUserId(userIdStr, cid);
				if (p != null) {
					long ct = System.currentTimeMillis();
					String lastHeartTime = RedisUtil.hget(Cnst.get_REDIS_HEART_PREFIX(cid), userIdStr);
					if (lastHeartTime == null) {// 说明用户重新上线
						// 需要看用户是否在房间里，如果在，要通知其他玩家
						String roomId = p.getRoomId() + "";
						if (roomId != null) {// 用户在房间里
							RoomResp room = RedisUtil.getRoomRespByRoomId(roomId, cid);
							if (room != null && room.getState() != (Cnst.ROOM_STATE_YJS)) {// 房间还没解散
								// toAdd 通知房间内其他人用户上线
								MessageFunctions.interface_100109(RedisUtil.getPlayerList(room.getRoomId(), cid), Cnst.PLAYER_LINE_STATE_INLINE, p.getUserId(), p.getPlayStatus());
							}
						}
					} else {// 说明用户正常心跳
							// 如果玩家在房间里，需要计算其他用户是否心跳超时
						String roomId = p.getRoomId() + "";
						if (roomId != null) {// 用户在房间里
							RoomResp room = RedisUtil.getRoomRespByRoomId(roomId, cid);
							if (room != null && room.getState() != (Cnst.ROOM_STATE_YJS)) {// 房间还没解散
								// toAdd 计算房间里其他玩家的心跳时间
								List<Long> uids = room.getPlayerIds();
								List<String> outs = new ArrayList<String>();
								for (Long uid : uids) {
									if (uid != null && !(uid + "").equals(userIdStr)) {
										String uidHeartTime = RedisUtil.hget(Cnst.get_REDIS_HEART_PREFIX(cid), uid + "");
										if (uidHeartTime != null) {
											long t = Long.valueOf(uidHeartTime);
											if ((ct - t) > Cnst.HEART_TIME) {
												RedisUtil.hdel(Cnst.get_REDIS_HEART_PREFIX(cid), uid + "");
												// RedisUtil.hset(Cnst.REDIS_PREFIX_USER_ID_USER_MAP.concat(uid+""),
												// "state",
												// Cnst.PLAYER_LINE_STATE_OUT+"",
												// null);
												// log.I("玩家心跳发现房间内其他人长时间没心跳 是否设置此人为不在线状态");
												outs.add(uid + "");
											}
										}
									}
								}

								if (outs.size() > 0) {
									// toAdd 通知其他人，outs里面的玩家掉线
									// log.I("玩家心跳检查到房间其他人长久不在线 是否 提醒提前玩家这个人不在线");
									List<Player> players = RedisUtil.getPlayerList(room, cid);
									for (Player player : players) {
										if (player == null)
											continue;
										boolean isOut = false;
										for (String uid : outs) {
											if (player.getUserId().longValue() == Long.valueOf(uid).longValue()) {
												isOut = true;
												break;
											}
										}
										if (!isOut)
											continue;

										MessageFunctions.interface_100109(RedisUtil.getPlayerList(room.getRoomId(), cid), Cnst.PLAYER_LINE_STATE_OUT, player.getUserId(), player.getPlayStatus());
									}
								}
							}
							// else{
							// //toAdd 初始化用户
							// p.initPlayer(null,Cnst.PLAYER_STATE_DATING,0l);
							// }
						}
					}
					// 更新用户心跳时间
					RedisUtil.hset(Cnst.get_REDIS_HEART_PREFIX(cid), userIdStr, String.valueOf(ct), null);
					// RedisUtil.hset(userKey,
					// "state",Cnst.PLAYER_LINE_STATE_INLINE+"", null);
					if (p.getState() == null || p.getState().intValue() != Cnst.PLAYER_LINE_STATE_INLINE) {
						p.setState(Cnst.PLAYER_LINE_STATE_INLINE);
						// FIXME 如果玩家设置了 会不会大计算看到别人积分呢为0的情况
						RedisUtil.setPlayerByUserId(p, cid);
					}
				} else {
					session.close(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
				session.close(true);
			}
		}
	}

	public static void beneficiate(IoSession s, int protocol_num) {
		log.I("s.getCurrentWriteRequest() --> " + s.getFilterChain());
		log.I("s.getRemoteAddress() --> " + s.getRemoteAddress());
		log.I("s.getServiceAddress() --> " + s.getServiceAddress());
		log.I("请 求 进 来 :" + "\n\tinterfaceId -> [ " + protocol_num + " ]");
	}
}
