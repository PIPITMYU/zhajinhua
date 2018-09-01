package com.up72.server.mina.utils.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.up72.game.constant.Cnst;
import com.up72.game.dao.impl.UserMapper_loginImpl;
import com.up72.game.dto.resp.Player;
import com.up72.game.dto.resp.RoomResp;
import com.up72.server.mina.utils.MyLog;

/**
 * 所有关于set的方法，如果key没有过期时间，timeout统一传null
 * 
 * @author zc
 *
 */
public class RedisUtil {

	private static MyLog log = MyLog.getLogger(RedisUtil.class);

	public static void hset(String key, String field, String value, Integer timeout) {
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.hset(key, field, value);
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
	}

	public static void hmset(String key, Map<String, String> info, Integer timeout) {
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.hmset(key, info);
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
	}

	public static String hget(String key, String field) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.hget(key, field);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static Boolean hexists(String key, String field) {
		Boolean result = false;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.hexists(key, field);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 获取对象的部分值
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public static Map<String, String> hmget(String key, String[] fields) {
		Map<String, String> result = null;
		List<String> list = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			list = jedis.hmget(key, fields);
			if (list != null) {
				result = new HashMap<String, String>();
				for (int i = 0; i < fields.length; i++) {
					result.put(fields[i], list.get(i));
				}
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 获取对象的部分值
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public static List<String> hmgetList(String key, String[] fields) {
		List<String> list = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			list = jedis.hmget(key, fields);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return list;
	}

	/**
	 * 获取对象的全部值
	 * 
	 * @param key
	 * @return
	 */
	public static Map<String, String> hgetAll(String key) {
		Map<String, String> result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.hgetAll(key);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 向key的list中加入元素，取的时候，按照后进先出的规则存入，对应底下的lrange方法
	 * 
	 * @param key
	 * @param value
	 */
	public static void lpush(String key, Integer timeout, String... value) {
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.lpush(key, value);
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
	}

	/**
	 * 向key的list中加入元素，取的时候，按照后进先出的规则存入，对应底下的lrange方法
	 * 
	 * @param key
	 * @param value
	 */
	public static void rpush(String key, Integer timeout, String... value) {
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.rpush(key, value);
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
	}

	/**
	 * 从开始位置，取到结束位置，包含两边；跟mysql的分页不一样 比如传5，10，取第五位到第十位，一共六个结果
	 * 
	 * @param key
	 * @param start
	 *            开始位置
	 * @param end
	 *            结束位置
	 * @return
	 */
	public static List<String> lrange(String key, int start, int end) {
		List<String> result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.lrange(key, start, end);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 获取key的list的总长度
	 * 
	 * @param key
	 * @return
	 */
	public static Long llen(String key) {
		Long length = 0l;
		Jedis jedis = null;
		try {
			// 获取一个jedis连接
			jedis = MyRedis.getRedisClient().getJedis();
			length = jedis.llen(key);
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return length;
	}

	public static String setObject(String key, Object value, Integer timeout) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.set(key, JSONObject.toJSONString(value, SerializerFeature.DisableCircularReferenceDetect));
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static String setString(String key, String value, Integer timeout) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.set(key, value);
			if (timeout != null) {
				jedis.expire(key, timeout);
			}
		} catch (Exception e) {
			log.E("ERROR", e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static <T> T getObject(String key, Class<T> T) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.get(key);
			if (result != null) {
				return JSONObject.parseObject(result, T);
			}
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return null;
	}

	public static Set<String> getKeys(String pattern) {
		Set<String> result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.keys(pattern);
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static boolean deleteByKey(String... key) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.del(key);
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static boolean hdel(String key, String... field) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.hdel(key, field);
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public static boolean exists(String key) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.exists(key);
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 从列表末尾移除并返回最后一个元素
	 * 
	 * @param key
	 * @return
	 */
	public static String rpop(String key) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.rpop(key);
		} catch (Exception e) {
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 更新redis中的数据
	 * 
	 * @param players
	 * @param room
	 * @param player
	 */
	public synchronized static boolean updateRedisData(RoomResp room, Player player, String cid) {
		boolean result = true;
		if (room != null) {
			// deleteByKey(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(room.getRoomId())));
			result = setRoomRespByRoomId(String.valueOf(room.getRoomId()), room, cid);
		}
		if (player != null) {
			// deleteByKey(Cnst.REDIS_PREFIX_USER_ID_USER_MAP.concat(String.valueOf(player.getUserId())));
			result = setPlayerByUserId(player, cid);
		}
		return result;
	}

	/**
	 * 通过房间号或者房中的玩家id列表，去redis中找齐所有玩家并返回
	 * 
	 * @param roomInfo
	 * @return
	 */
	public synchronized static List<Player> getPlayerList(Object roomInfo, String cid) {
		List<Player> players = new ArrayList<Player>();
		RoomResp room = null;
		if (roomInfo instanceof Integer) {// 传入的是roomId
			room = getRoomRespByRoomId(String.valueOf(roomInfo), cid);

		} else if (roomInfo instanceof RoomResp) {// 传入的是roomResp对象
			room = (RoomResp) roomInfo;
		}
		if (room != null) {
			Jedis jedis = null;
			try {
				List<Long> userIds = room.getPlayerIds();
				jedis = MyRedis.getRedisClient().getJedis();
				for (int i = 0; i < userIds.size(); i++) {
					if (userIds.get(i) != null) {
						Player p = (Player) JSON.parseObject(jedis.get(Cnst.get_REDIS_PREFIX_USER_ID_USER_MAP(cid).concat(String.valueOf(userIds.get(i)))), Player.class);
						if (p != null) {
							players.add(p);
						} 
					} 
				}

			} catch (Exception e) {
				MyRedis.getRedisClient().returnBrokenJedis(jedis);
				log.E("ERROR", e);
			} finally {
				if (jedis != null) {
					MyRedis.getRedisClient().returnJedis(jedis);
				}
			}

		}
		return players != null && players.size() > 0 ? players : null;
	}

	/************************ 通过对应key值get对应对象 ***************************/

	public synchronized static String getStringByKey(String key) {
		String value = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			value = jedis.get(key);
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return value;
	}

	public synchronized static Set<String> getSameKeys(String patten) {
		Set<String> keys = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			keys = jedis.keys(patten.concat("*"));
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		Set<String> result = null;
		if (keys != null && keys.size() > 0) {
			result = new HashSet<String>();
			for (String str : keys) {
				str = str.replace(patten, "");
				result.add(str);
			}
		}
		return result;
	}

	/**
	 * 通过userId从redis中获取玩家信息
	 * 
	 * @param userId
	 * @return
	 */
	public synchronized static Player getPlayerByUserId(String userId, String cid) {
		Player player = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			player = (Player) JSON.parseObject(jedis.get(Cnst.get_REDIS_PREFIX_USER_ID_USER_MAP(cid).concat(userId)), Player.class);
		} catch (Exception e) {
			log.E("ERROR userId = " + userId, e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return player;
	}

	/**
	 * 通过roomId从redis中获取房间信息
	 * 
	 * @param roomId
	 * @return
	 */
	public synchronized static RoomResp getRoomRespByRoomId(String roomId, String cid) {
		RoomResp room = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			String string = jedis.get(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(roomId));
			if (string != null) {
				room = JSON.parseObject(string, RoomResp.class);
			}
		} catch (Exception e) {
			log.E("ERROR roomId=" + roomId, e);
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return room;
	}

	/**
	 * 通过openId获取玩家的userId
	 * 
	 * @param openId
	 * @return
	 */
	public synchronized static Long getUserIdByOpenId(String openId, String cid) {
		Long userId = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			String string = jedis.get(Cnst.get_REDIS_PREFIX_OPENIDUSERMAP(cid).concat(openId));
			if (string != null) {
				userId = Long.valueOf(string);
			}
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return userId;
	}

	/************************ 通过对应key值set对应对象 ***************************/

	public synchronized static boolean setPlayersList(List<Player> players, String cid) {
		boolean result = true;

		Jedis jedis = MyRedis.getRedisClient().getJedis();

		try {
			if (players != null && players.size() > 0) {
				for (Player p : players) {
					if (p == null)
						continue;
					jedis.set(Cnst.get_REDIS_PREFIX_USER_ID_USER_MAP(cid).concat(String.valueOf(p.getUserId())), JSON.toJSONString(p));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
			result = false;
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}

		return result;
	}

	/**
	 * 通过roomId从redis中获取房间信息
	 * 
	 * @param roomId
	 * @return
	 */
	public synchronized static boolean setRoomRespByRoomId(String roomId, RoomResp room, String cid) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.set(Cnst.get_REDIS_PREFIX_ROOMMAP(cid).concat(roomId), JSON.toJSONString(room, SerializerFeature.DisableCircularReferenceDetect));
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 从redis中设置玩家信息
	 * 
	 * @param roomId
	 * @return
	 */
	public synchronized static boolean setPlayerByUserId(Player player, String cid) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.set(Cnst.get_REDIS_PREFIX_USER_ID_USER_MAP(cid).concat(String.valueOf(player.getUserId())), JSON.toJSONString(player));
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	/**
	 * 通过openId获取玩家的userId
	 * 
	 * @param openId
	 * @return
	 */
	public synchronized static boolean setUserIdByOpenId(String openId, String userId, String cid) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.set(Cnst.get_REDIS_PREFIX_OPENIDUSERMAP(cid).concat(openId), userId);
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	public synchronized static boolean deleteByKey(String key) {
		boolean result = true;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			jedis.del(key);
		} catch (Exception e) {
			result = false;
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return result;
	}

	// 获取战绩
	public static List<Map> getPlayRecord(String key) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = MyRedis.getRedisClient().getJedis();
			result = jedis.get(key);
			if (result != null) {
				return JSONObject.parseArray(result, Map.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
			MyRedis.getRedisClient().returnBrokenJedis(jedis);
		} finally {
			if (jedis != null) {
				MyRedis.getRedisClient().returnJedis(jedis);
			}
		}
		return null;
	}

}
